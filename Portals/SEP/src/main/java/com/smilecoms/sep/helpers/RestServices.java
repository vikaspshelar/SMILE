/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.CustomerQueryOtherMNORequest;
import com.smilecoms.commons.sca.CustomerQueryOtherMNOResponse;
import com.smilecoms.sep.helpers.ext.tnf.Stackholder;
import com.smilecoms.sep.helpers.ext.tnf.StackholderResponse;
import com.smilecoms.commons.sca.SIMVerifyRequest;
import com.smilecoms.commons.sca.SIMVerifyResponse;
import com.smilecoms.sep.helpers.ext.tnf.SubRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.TimeZone;


/**
 *
 * @author abhilash
 */
public class RestServices {
    
    protected static Logger log = LoggerFactory.getLogger(RestServices.class);
    //private static final Client restClient = Client.create();
    static URLConnectionClientHandler ch  = new URLConnectionClientHandler(new ConnectionFactory());
    private static final Client restClient = new Client(ch);
    
    public static CustomerQueryOtherMNOResponse getCustomerDetailsOtherMNO(CustomerQueryOtherMNORequest request) throws IOException, Exception
    {
        log.info("inside getCustomerDetailsOtherMNO");
        String delimiter=";";
        String operatorCode = BaseUtils.getSubProperty("env.csis.config", "OperatorCode");
        String givenPassword = BaseUtils.getSubProperty("env.csis.config", "GivenPassword");
        String privateKeyFilename = BaseUtils.getSubProperty("env.csis.config", "PrivateKeyFilename");
        String CSISEndpoint = BaseUtils.getSubProperty("env.csis.config", "CSISEndpoint");
        String version = BaseUtils.getSubProperty("env.csis.config", "Version");
        
        CustomerQueryOtherMNOResponse resp = new CustomerQueryOtherMNOResponse();
        SubRegistration reg = new SubRegistration();
        SubRegistration.RegistrationNamesRequest regReq = new SubRegistration.RegistrationNamesRequest();
        SubRegistration.RegistrationNamesResponse regRes = new SubRegistration.RegistrationNamesResponse();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'Z'Z");
        sdf.setTimeZone(TimeZone.getTimeZone("EAT"));
        String timestamp = sdf.format(new java.util.Date());
        String textToHash=operatorCode+delimiter+givenPassword+delimiter+timestamp;
        long msisdn = request.getRegistrationNamesRequest().getMsisdn();
        reg.setVersion(Double.parseDouble(version));
        reg.setOperatorCode(operatorCode);
        reg.setOperatorPassword(Utilities.getSHA256(textToHash));
        reg.setTimestamp(timestamp);
        regReq.setMsisdn(msisdn);
        reg.setRegistrationNamesRequest(regReq);
        try {
        if(operatorCode!=null && !operatorCode.isEmpty() && textToHash!=null && !textToHash.isEmpty()
                && timestamp!=null && !timestamp.isEmpty())
        {
            log.debug("WS Input Request is::"+Utilities.marshallXmlObjectToString(reg));  
        
            RSAPrivateKey rsaPrivateKey = getRSAPrivateKey(privateKeyFilename);
            String signedData = sign(Utilities.marshallXmlObjectToString(reg), rsaPrivateKey);
            log.debug("Data Signature is:"+signedData);

            WebResource webResource = restClient.resource(CSISEndpoint);
            ClientResponse clientResponse = webResource.type("application/xml").header("content-type", "application/xml").header("Data-Signature", signedData).post(ClientResponse.class,Utilities.marshallXmlObjectToString(reg));
            String response=clientResponse.getEntity(String.class);
            log.debug("WS response is::"+response);
            if(clientResponse != null && clientResponse.getStatus()==200) 
            {
                reg=(SubRegistration)Utilities.unmarshallXmlStringToObject(response, SubRegistration.class);
                resp=Utilities.convertRegResponseForDisplay(reg);
            } else {
                log.error("Non 200 Status code was returned by the web service");
            }
        } else {
            throw new Exception("One of the necessary parameters is null or empty");
        }
        } catch(Exception ex)
        {
            log.error("Error while getting the details from csis web service"+ex);
        }        
        return resp;
    }
    
    public static SIMVerifyResponse getSimComplianceDetails(SIMVerifyRequest req)
    {
        log.info("inside getSimComplianceDetails");
        restClient.setReadTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.tzpolice.lormis.config", "ReadTimeout")));
        String LormisEndpoint = BaseUtils.getSubProperty("env.tzpolice.lormis.config", "URI");
        String vendor = BaseUtils.getSubProperty("env.tzpolice.lormis.config", "Vendor");
        String password = BaseUtils.getSubProperty("env.tzpolice.lormis.config", "Password");
        
        SIMVerifyResponse resp = new SIMVerifyResponse();        
        Stackholder SVR = new Stackholder();
        Stackholder.StackholderVerifyReq SHVR = new Stackholder.StackholderVerifyReq();
        StackholderResponse response = new StackholderResponse();
        
        long CN=req.getStackholderVerifyReq().getControlNumber();
        String IN=req.getStackholderVerifyReq().getIdentityNumber();
        SHVR.setControlNumber(CN);
        SHVR.setIdentityNumber(IN);
        SHVR.setVendorName(vendor);
        SVR.setStackholderVerifyReq(SHVR);
       
        String controlNumber=String.valueOf(req.getStackholderVerifyReq().getControlNumber());
        String idNumber=req.getStackholderVerifyReq().getIdentityNumber();        
        
        try{
            if(null!=vendor && !vendor.isEmpty() && null!=password && !password.isEmpty() 
                    && null!=controlNumber && !controlNumber.isEmpty() && null!=idNumber && !idNumber.isEmpty())
            {
                SVR.setStackholderSignature(Utilities.getSHA256(controlNumber+idNumber+vendor+password));

                log.debug("Input Request is::"+Utilities.marshallXmlObjectToString(SVR));
                
                WebResource webResource = restClient.resource(LormisEndpoint);
                ClientResponse clientResponse = webResource.type("application/xml").post(ClientResponse.class,Utilities.marshallXmlObjectToString(SVR));
                log.debug("Client response status is:"+clientResponse.getStatus());
                if(clientResponse != null && clientResponse.getStatus()==200) {
                    response=clientResponse.getEntity(StackholderResponse.class);
                    resp=Utilities.convertLormisResponseForDisplay(response,CN,IN);
                } else {
                    log.error("Non 200 Status code was returned by the web service");
                }
            } else {
                throw new Exception("One of the necessary parameters is null or empty");
            }        
        } catch (Exception ex) {
            log.error("Exception while getting details from TPF lormis web service:"+ex);
        }
        return resp;
    }    
    
    public static RSAPrivateKey getRSAPrivateKey(String keyFilename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encodedKey = readFile(keyFilename);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        return privateKey;
    }
    
    private static byte[] readFile(String filename) throws IOException {
        File file = new File(filename);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = new byte[(int) file.length()];
        bis.read(bytes);
        bis.close();
        return bytes;
    }

    public static String sign(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes("UTF-8"));
        byte[] signature = privateSignature.sign();

        return Base64.getEncoder().encodeToString(signature);
    }
}