/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.am.np.ng;

import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.OrganisationQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PhoneNumberRange;
import com.smilecoms.xml.schema.am.Photograph;
import com.telcordia.inpac.ws.AttachedDocType;
import com.telcordia.inpac.ws.NPCWebService;
import com.telcordia.inpac.ws.NPCWebServicePortType;
import com.telcordia.inpac.ws.binding.MultiNbrRejectReasonType;
import com.telcordia.inpac.ws.binding.NPCData;
import com.telcordia.inpac.ws.binding.NumberListType;
import com.telcordia.inpac.ws.binding.NumberListWithFlagReasonType;
import com.telcordia.inpac.ws.binding.NumberListWithFlagType;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class NGPortingHelper {
    
    public static final int  MAX_MSG_HEADER_ID = 1000000000;
    public static final String CUSTOMER_TYPE_INDIVIDUAL = "0"; // 0 = Individual;
    public static final String CUSTOMER_TYPE_ORGANISATION = "1"; // 1 = Corporate
    
    private static final Logger log = LoggerFactory.getLogger(NGPortingHelper.class);
    
    public static final String MSG_ID_PORTING_APPROVAL_REQUEST = "1001";
    public static final String MSG_ID_PORT_REVERSAL_REQUEST = "3001";  //EMERGENCY_REPATRIATION_REQUEST
    public static final String MSG_ID_PORT_DEACTIVATION_REQUEST = "1006";
    public static final String MSG_ID_PORT_REVERSAL_DEACTIVATION_REQUEST = "3006";
    public static final String MSG_ID_PORTING_APPROVAL_RESPONSE = "1004";
    public static final String MSG_ID_PORTING_REVERSAL_APPROVAL_RESPONSE = "3004";
    public static final String MSG_ID_PORT_DEACTIVATION_RESPONSE = "1009";
    public static final String MSG_ID_PORT_REVERSAL_DEACTIVATION_RESPONSE = "3009";
    public static final String MSG_ID_NUMBER_RETURN_REQUEST = "2001";
    public static final String MSG_ID_NUMBER_RETURN_RESPONSE = "2004";
         
    public static final String SUB_TYPE_INDIVIDUAL = "0";
    public static final String SUB_TYPE_CORPORATE = "1";
    
    public static final String MNP_SERVICE_NOT_ACTIVE_ERROR_CODE = "OPR1001"; // The service has been suspended under instructions from the customer
    public static final String MNP_SERVICE_NOT_ACTIVE_ERROR_DESC = "The service has been suspended under instructions from the customer";
    public static final String MNP_INVALID_CUSTOMER_ERROR_CODE = "OPR1005"; // No longer with this customer. The number being ported is ceased and is in quarantine/being reallocated."
    public static final String MNP_INVALID_CUSTOMER_ERROR_DESC = "No longer with this customer. The number being ported is ceased and is in quarantine/being reallocated.";
    public static final String MNP_INVALID_NUMBER_ERROR_CODE = "OPR1004";
    public static final String MNP_INVALID_NUMBER_ERROR_DESC = "The port request is for a list of numbers, but the primary number given is not recognised.";
    public static final String MNP_ERROR_CODE_OTHER = "OPR1006";
    public static final String MNP_OUT_OF_SEQUENCE = "SYS005000";
    public static final String MNP_NUMBER_RINGFENCED = "SYS005005";
    public static final String MNP_SIM_SWAP_LESS_THAN_7_DAYS_AGO_ERROR_CODE = "OPR1007"; 
    public static final String MNP_SIM_SWAP_LESS_THAN_7_DAYS_AGO_ERROR_DESC = "The SIM Card associated with this number was replaced within the last 7 days and is subject to a port restriction";
    
    public static String mapCustomerType(String custType) {
        
        String retCustType = SUB_TYPE_INDIVIDUAL;
        
        if(custType != null && custType.equalsIgnoreCase("corporate")) {
            retCustType = SUB_TYPE_CORPORATE;
        }
        
        return retCustType;
   }
    
   
            
    public static List <NumberListType>  mapXMLRoutingInformationListToNPCDB(com.smilecoms.xml.schema.am.RoutingInfoList inRoutingInfoList) {
        
        List <NumberListType> numberList = new ArrayList();
        
        NumberListType currentNumberEntry = null;
                
        for(com.smilecoms.xml.schema.am.RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            currentNumberEntry = new NumberListType();
            inRoutingInfo.getPhoneNumberRange().setPhoneNumberStart(Utils.getFriendlyPhoneNumberKeepingCountryCode(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart()));
            inRoutingInfo.getPhoneNumberRange().setPhoneNumberEnd(Utils.getFriendlyPhoneNumberKeepingCountryCode(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
            currentNumberEntry.setStartNumber(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            currentNumberEntry.setEndNumber(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            
            numberList.add(currentNumberEntry);
        }
        return numberList;
    }
    
    public static List <NumberListType>  mapXMLPhoneNumberRangeListToNPCDB(List <PhoneNumberRange> phoneNumberRangeList) {
        
        List <NumberListType> numberList = new ArrayList();
        
        NumberListType currentNumberEntry = null;
                
        for(PhoneNumberRange phoneNumberRange : phoneNumberRangeList) {
            currentNumberEntry = new NumberListType();
            phoneNumberRange.setPhoneNumberStart(Utils.getFriendlyPhoneNumberKeepingCountryCode(phoneNumberRange.getPhoneNumberStart()));
            phoneNumberRange.setPhoneNumberEnd(Utils.getFriendlyPhoneNumberKeepingCountryCode(phoneNumberRange.getPhoneNumberEnd()));
            currentNumberEntry.setStartNumber(phoneNumberRange.getPhoneNumberStart());
            currentNumberEntry.setEndNumber(phoneNumberRange.getPhoneNumberEnd());
            
            numberList.add(currentNumberEntry);
        }
        return numberList;
    }
    
    public static List <NumberListWithFlagReasonType>  mapXMLRoutingInformationListToNPCDBNumberListWithFlagReason(com.smilecoms.xml.schema.am.RoutingInfoList inRoutingInfoList, String flag, String reason) {
        
        List <NumberListWithFlagReasonType> numberList = new ArrayList();
        
        NumberListWithFlagReasonType currentNumberEntry = null;
                
        for(com.smilecoms.xml.schema.am.RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            currentNumberEntry = new NumberListWithFlagReasonType();
            inRoutingInfo.getPhoneNumberRange().setPhoneNumberStart(Utils.getFriendlyPhoneNumberKeepingCountryCode(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart()));
            inRoutingInfo.getPhoneNumberRange().setPhoneNumberEnd(Utils.getFriendlyPhoneNumberKeepingCountryCode(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
            currentNumberEntry.setStartNumber(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            currentNumberEntry.setEndNumber(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            currentNumberEntry.setNumberAcceptFlag(flag);
            //Add the rejection reason here ..
            if(reason != null && reason.trim().length() > 0) { // Add the reason segment ...
                MultiNbrRejectReasonType r = new MultiNbrRejectReasonType();
                r.setNumberRejectReason(reason);
                currentNumberEntry.getNumberRejectReasons().add(r);
            }
            numberList.add(currentNumberEntry);
        }
        return numberList;
    }
    
    public static List <NumberListWithFlagType>  mapXMLRoutingInformationListToNPCDBNumberListWithFlag(com.smilecoms.xml.schema.am.RoutingInfoList inRoutingInfoList, String flag, String reason) {
        
        List <NumberListWithFlagType> numberList = new ArrayList();
        
        NumberListWithFlagType currentNumberEntry = null;
                
        for(com.smilecoms.xml.schema.am.RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            currentNumberEntry = new NumberListWithFlagType();
            inRoutingInfo.getPhoneNumberRange().setPhoneNumberStart(Utils.getFriendlyPhoneNumberKeepingCountryCode(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart()));
            inRoutingInfo.getPhoneNumberRange().setPhoneNumberEnd(Utils.getFriendlyPhoneNumberKeepingCountryCode(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
            currentNumberEntry.setStartNumber(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            currentNumberEntry.setEndNumber(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            currentNumberEntry.setNumberAcceptFlag(flag);
            
            numberList.add(currentNumberEntry);
        }
        return numberList;
    }
    
    
    public static Customer getCustomer(int custId) {
        // log.debug("Getting customer data for the recipient [{}]", custId);
        CustomerQuery custQ = new CustomerQuery();
        custQ.setCustomerId(custId);
        custQ.setResultLimit(1);
        custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer cust = SCAWrapper.getAdminInstance().getCustomer(custQ);
        return cust;
    }
    
    public static Organisation getOrganisation(int orgId) {
        // log.debug("Getting customer data for the recipient [{}]", custId);
        OrganisationQuery orgQ = new OrganisationQuery();
        orgQ.setOrganisationId(orgId);
        orgQ.setResultLimit(1);
        orgQ.setVerbosity(StOrganisationLookupVerbosity.MAIN);
        Organisation org = SCAWrapper.getAdminInstance().getOrganisation(orgQ);
        return org;
    }
    
    public static void showXML(Object xmlObj) throws Exception {
        
            Marshaller m = Utils.getJAXBMarshallerForSoap(xmlObj.getClass());
            java.io.StringWriter sw = new StringWriter();
            // Set UTF-8 Encoding
            
            // DataWriter dataWriter = new DataWriter(sw, "UTF-8", DumbEscapeHandler.theInstance);
            m.marshal(xmlObj, sw);
            log.debug("XML = [{}]", sw.toString());
            
    }
    
    public static List <AttachedDocType> getPortingFormsAsAttachedDocTypeList(List <Photograph> portingForms) {
            List <AttachedDocType> attachedDocs = new ArrayList();
            for (com.smilecoms.xml.schema.am.Photograph p : portingForms) {
                AttachedDocType attachedDoc = new AttachedDocType();
                attachedDoc.setDocumentFile(Utils.decodeBase64(p.getData()));
                attachedDoc.setDocumentName(p.getPhotoType());
                attachedDoc.setDocumentType("PRF"); // For Port Request Form.
                attachedDocs.add(attachedDoc);
            }            
            return attachedDocs;
    }
    
   
    public static NPCWebServicePortType getNPCDBConnection() throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NPCDB web service");
        java.net.Authenticator myAuth = new java.net.Authenticator() {
            @Override
            protected java.net.PasswordAuthentication getPasswordAuthentication() {
                return new java.net.PasswordAuthentication(MnpHelper.props.getProperty("NpcdbWSUsername"),
                        MnpHelper.props.getProperty("NpcdbWSPassword").toCharArray());
            }
        };
        java.net.Authenticator.setDefault(myAuth);

        try {
            URL wsdlUri = MnpHelper.class.getResource(MnpHelper.props.getProperty("NpcdbSvcWsdlUri"));
            String endpointURL = MnpHelper.props.getProperty("NpcdbSvcEndpoint");
            String serviceName = MnpHelper.props.getProperty("NpcdbSvcName");
            String targetNamespace = MnpHelper.props.getProperty("NpcdbSvcNamespace");
            
            
            String username = MnpHelper.props.getProperty("NpcdbWSUsername");
            String password = MnpHelper.props.getProperty("NpcdbWSPassword");
            
            log.debug("Connecting to NPCDB using parameters [WSDL URi: {}, Endpoint URL: {}, ServiceName: {},  Username:  {}, Password: {}, TargetNamespace: {}]", 
                    new Object [] {wsdlUri, endpointURL, serviceName, username, password, targetNamespace});
            
            NPCWebService npcdbService = new NPCWebService(wsdlUri, new QName(targetNamespace, serviceName));
            
            // NPCWebService npcdbService = new NPCWebService(new URL(endpointURL));
            log.debug("NPCDB Service created successfully.");
            NPCWebServicePortType npcdbServicePort = (NPCWebServicePortType) npcdbService.getNPCWebServiceHttpSoap12Endpoint();
            log.debug("NPCDB Port created successfully.");
                
            //BindingProvider bindingProvider = (BindingProvider) npcdbServicePort;
            // Map requestContext = bindingProvider.getRequestContext();
            // requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
            // requestContext.put(BindingProvider.USERNAME_PROPERTY, username);
            // requestContext.put(BindingProvider.PASSWORD_PROPERTY, password);
            /* requestContext.put("com.sun.xml.ws.client.ContentNegotiation", "none");
            requestContext.put("com.sun.xml.ws.connect.timeout", 3000);
            requestContext.put("com.sun.xml.ws.request.timeout", 50000); */
            
            ((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
            ((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);
            ((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
            ((BindingProvider) npcdbServicePort).getRequestContext().put("javax.xml.ws.client.connectionTimeout", 1000);
            ((BindingProvider) npcdbServicePort).getRequestContext().put("javax.xml.ws.client.receiveTimeout", 50000);
            // ((BindingProvider) npcdbServicePort).getRequestContext().put("http.nonProxyHosts", "10.1.2.41");*/
            
            log.debug("NPCDB using endpoint URL: " + ((BindingProvider) npcdbServicePort).getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
            
            log.debug("Binding properties configured successfully.");
            return npcdbServicePort;
            
        } catch (Exception ex) {
            log.error("Error while trying to connect to NPCDB [{}]", ex.getMessage());
            throw ex;
        }
        
        
    }
    
    
    public static NPCData getNPCDataFromResponse(String stResponse) throws Exception {
        
        if(stResponse == null || stResponse.isEmpty()) {
            throw new Exception("Invalid or empty response from NPC.");
        } else {
            Pattern patternNPCData = Pattern.compile("<NPCData>(?s)(.+?)</NPCData>"); // ?s to enable DOTALL so we can match newline characters.
            Matcher matcher = patternNPCData.matcher(stResponse);
            
            if(matcher.find()) { //If the response contains NPCData
                Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);    
                return (NPCData) um.unmarshal(new StringReader(stResponse));
            } else {
                throw new Exception(stResponse);
            }
        }
    }
}
