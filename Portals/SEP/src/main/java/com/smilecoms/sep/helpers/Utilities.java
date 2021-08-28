/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.SIMVerifyResponse;
import com.smilecoms.commons.sca.CustomerQueryOtherMNOResponse;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sep.helpers.ext.tnf.StackholderResponse;
import com.smilecoms.sep.helpers.ext.tnf.SubRegistration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author abhilash
 */
public class Utilities {
    
    protected static Logger log = LoggerFactory.getLogger(Utilities.class);
    
    public static String getSHA256(String text) {
        log.debug("Text to hash is [{}]", text);
        String hash = "";
        try {
            MessageDigest mda = MessageDigest.getInstance("SHA-256");
            byte[] digest = mda.digest(text.getBytes());
            hash = Codec.binToHexString(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.warn("Error occured trying to hash string message digest: {}", ex);
        }
        return hash.toUpperCase();
    }
    
    public static String marshallXmlObjectToString(Object inputObject) {

        if (log.isDebugEnabled()) {
            log.debug("Marshalling object to xml...");
        }
        if (inputObject == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot marshall a null object - will return null");
            }
            return null;
        }

        String xml;
        try {
            Class inputClass = inputObject.getClass();
            StringWriter writer = new StringWriter();
            Marshaller marshaller = Utils.getJAXBMarshallerForXML(inputClass);
//            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
//            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            synchronized (marshaller) {
                marshaller.marshal(inputObject, writer);
            }
            xml = writer.toString();
        } catch (Exception e) {
            log.debug("Error marshalling request object to a string. Will return <Error Parsing>. Error: [{}]", e.toString());
            log.warn("Error: ", e);
            xml = "<Error Parsing>";
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished marshalling object to xml");
        }
        return xml;
    }
    
    public static Object unmarshallXmlStringToObject(String xmlString, Class clazz) {

        if (log.isDebugEnabled()) {
            log.debug("Unmarshalling xml string...");
        }
        if (xmlString == null || xmlString.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot unmarshall a empty string - will return null");
            }
            return null;
        }

        Object object;
        try {
            Unmarshaller unmarshaller = Utils.getJAXBUnmarshaller(clazz);
            synchronized (unmarshaller) {
                object = unmarshaller.unmarshal(new StringReader(xmlString));
            }
        } catch (Exception e) {
            log.warn("Error Unmarshalling", e);
            object = "<Error Parsing>";
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished unmarshalling xml to object");
        }
        return object;
    }
    
    public static SIMVerifyResponse convertLormisResponseForDisplay(StackholderResponse lormisResponse, long controlNumber, String idNumber) 
    {
        log.info("Inside convertLormisResponseForDisplay with parameters:"+controlNumber+" and "+idNumber);
        SIMVerifyResponse response = new SIMVerifyResponse();
        SIMVerifyResponse.StackholderResp SR = new SIMVerifyResponse.StackholderResp();
        try {            
                if(lormisResponse.getStackholderResp().getStatus()==100)
                {
                    System.out.println("Record found as status is 100");
                    SR.setFullname(lormisResponse.getStackholderResp().getFullname());
                    SR.setIdentityType(lormisResponse.getStackholderResp().getIdentityType());
                    SR.setIdentityNumber(lormisResponse.getStackholderResp().getIdentityNumber());
                    SR.setStatus(lormisResponse.getStackholderResp().getStatus());
                    SR.setStatusDescription(lormisResponse.getStackholderResp().getStatusDescription());
                    SR.setPdf(lormisResponse.getStackholderResp().getPdf());
                    SR.setRbNumber(lormisResponse.getStackholderResp().getRbNumber());
                    SR.setDateofloss(lormisResponse.getStackholderResp().getDateofloss());
                    SR.setLossLocation(lormisResponse.getStackholderResp().getLossLocation());
                    SR.setLossRegion(lormisResponse.getStackholderResp().getLossRegion());
                    SR.setControlNumber(lormisResponse.getStackholderResp().getControlNumber());
                    SR.setItemtype(lormisResponse.getStackholderResp().getItemtype());
                    SR.setItemname(lormisResponse.getStackholderResp().getItemname());
                    SR.setItemnumber(lormisResponse.getStackholderResp().getItemname());        
                    SR.setOtherDetails(lormisResponse.getStackholderResp().getOthersDetails());
                    generateLormisPdfFile(lormisResponse.getStackholderResp().getPdf(),lormisResponse.getStackholderResp().getControlNumber());
                }
                else
                {
                    System.out.println("Record not found as status is non 100");
                    SR.setStatus(lormisResponse.getStackholderResp().getStatus());
                    SR.setStatusDescription(lormisResponse.getStackholderResp().getStatusDescription());
                    SR.setIdentityNumber(idNumber);
                    SR.setControlNumber(controlNumber);
                }                        

                response.setStackholderResp(SR);
                response.setStackholderSignature(lormisResponse.getStackholderSignature());
            } catch (Exception ex){
                log.error("Exception in convertLormisResponseForDisplay: "+ex);
            }
        return response;
    }   
    
    public static CustomerQueryOtherMNOResponse convertRegResponseForDisplay(SubRegistration RegResponse) 
    {
        log.info("Inside convertRegResponseForDisplay with response code:"+RegResponse.getRegistrationNamesResponse().getResponseCode());
        CustomerQueryOtherMNOResponse response = new CustomerQueryOtherMNOResponse();
        CustomerQueryOtherMNOResponse.RegistrationNamesResponse RNR = new CustomerQueryOtherMNOResponse.RegistrationNamesResponse();
        try {            
                if(Integer.parseInt(RegResponse.getRegistrationNamesResponse().getResponseCode())==0)
                {
                    log.info("Record found as response code is 0");
                    RNR.setFirstName(RegResponse.getRegistrationNamesResponse().getFirstName());
                    RNR.setMiddleName(RegResponse.getRegistrationNamesResponse().getMiddleName());
                    RNR.setLastName(RegResponse.getRegistrationNamesResponse().getLastName());
                }
                else
                {
                    log.info("Record not found as status is non 0");
                    RNR.setResponseCode(RegResponse.getRegistrationNamesResponse().getResponseCode());
                    RNR.setResponseDesc(RegResponse.getRegistrationNamesResponse().getResponseDesc());
                }

                response.setRegistrationNamesResponse(RNR);
            } catch (Exception ex){
                log.error("Exception in convertRegResponseForDisplay: "+ex);
            }
        return response;
    }  
    
    public static void generateLormisPdfFile(String pdfContent, long controlNumber) throws IOException {
        log.debug("Storing the PDF contents for Control Number:" +controlNumber);
        String fileLocation = System.getProperty("java.io.tmpdir") + File.separator + "/"+controlNumber+".txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileLocation));
        writer.write(pdfContent);
        writer.close();
    }
    
    /*

    public static TransactionHistoryTPGW generateTransactionHistoryTPGW(TransactionQueryTPGW query)
    {
        TransactionHistoryTPGW history = new TransactionHistoryTPGW();
        List<TransactionRecordTPGW> l = new ArrayList<>();
        l=getAccoutHistoryFromDB(query.getAccountId(),query.getStartDate(),query.getEndDate());
        
        if(l.size()>0){
        history.getTransactionRecords().addAll(l);
        history.setResultsReturned(history.getTransactionRecords().size());
        }
        
        return history;
    }

    public static List<TransactionRecordTPGW> getAccoutHistoryFromDB(Long accountID, javax.xml.datatype.XMLGregorianCalendar startDate,  javax.xml.datatype.XMLGregorianCalendar endDate) {
        log.info("inside getAccoutHistoryFromDB with accountID:"+accountID+" , startDate:"+startDate+" , endDate:"+endDate);
        TransactionRecordTPGW record = new TransactionRecordTPGW();
        List<TransactionRecordTPGW> transList = new ArrayList<>();
        PreparedStatement ps = null;
        Connection conn = null;

        String sqlQuery = BaseUtils.getProperty("env.account.history.tpgw", "");
        log.debug("Query for account history is [{}]", sqlQuery);
        if (sqlQuery == null || "".equals(sqlQuery.trim())) {
            return null;
        }

        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            if (conn == null) {
                return null;
            }
            ps = conn.prepareStatement(sqlQuery);
            if (ps == null) {
                return null;
            } else {
                ps.setLong(1, accountID);
                ps.setTimestamp(2, new Timestamp(startDate.toGregorianCalendar().getTimeInMillis()));
                ps.setTimestamp(3, new Timestamp(endDate.toGregorianCalendar().getTimeInMillis()));           
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                log.info("Record Result is :");
                record.setDestination(rs.getString(1));
                String sId=extractSaleId(rs.getString(2));
                if(sId != null)
                {
                    record.setSaleId(Integer.parseInt(sId.trim()));
                    getTransactionId(sId);
                }
                transList.add(record);
            }
            
        } catch (Exception ex) {
            log.error("error while getAccoutHistoryFromDB " + ex.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.error("error while closing ps in getAccoutHistoryFromDB " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("error while closing conn in getAccoutHistoryFromDB " + e.getMessage());
                }
            }
        }
        return transList;
    }

*/
    public static String extractSaleId(String desc)
    {
        Pattern pattern = Pattern.compile("Payment for Sale(.*)");
        Matcher matcher = pattern.matcher(desc);

        if(matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
    
    public static String getTransactionId(String saleId)
    {
        log.info("inside getTransactionId with saleId:"+saleId);
        String tid="";
        PreparedStatement ps = null;
        Connection conn = null;

        String sqlQuery = BaseUtils.getProperty("env.sale.info.tpgw", "");
        log.debug("Query for getting Transaction ID is [{}]", sqlQuery);
        if (sqlQuery == null || "".equals(sqlQuery.trim())) {
            return "";
        }
        
        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            if (conn == null) {
                return "";
            }
            ps = conn.prepareStatement(sqlQuery);
            if (ps == null) {
                return "";
            } else {
                ps.setString(1, saleId);          
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                log.info("Record Result is :");
                tid=rs.getString(1);
            }
            
        } catch (Exception ex) {
            log.error("error while getTransactionId() " + ex.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.error("error while closing ps in getTransactionId() " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("error while closing conn in getTransactionId() " + e.getMessage());
                }
            }
        }
        
        return tid;
    }
}
