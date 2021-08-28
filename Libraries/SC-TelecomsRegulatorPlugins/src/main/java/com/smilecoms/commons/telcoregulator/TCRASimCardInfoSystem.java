/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.telcoregulator;



import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.helpers.BaseHelper;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.Done;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.EventList;
import com.smilecoms.commons.sca.EventQuery;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tz.co.infowise.SubRegistration;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * TCRASimCardInfoSystem API defines various interfaces supported by the CSIS as
 * a means of data exchange between Smile and CSIS.
 *
 * Central SIM-Card Information System (CSIS) is a system that is expected to
 * store subscriber registration records received from Mobile Network Operators
 * (MNOs) e.g. Smile after being authenticated by National Identification
 * Authority (NIDA). CSIS will verify the registration record against National
 * ID Database (hosted by NIDA).
 *
 * @author sabza
 */
public class TCRASimCardInfoSystem {

    private static final Logger log = LoggerFactory.getLogger(TCRASimCardInfoSystem.class);
    private static final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss'Z'Z");//20170620125300Z+0300
    private static final DateFormat otherDataDF = new SimpleDateFormat("yyyy-MM-dd");// 2002-09-08

    private String operatorCode;
    private String givenPassword;
    private String privateKeyFilename;
    private String CSISEndpoint;
    private String useProxy;
    private String version;
    private String jks_file;
    private String socketTimeout;
    private String connTimeout;
    private String proxyip;
    private String proxyport;
    private final String delimiter = ";";

    public TCRASimCardInfoSystem() {
        init();//update event_data set STATUS = 'N', EVENT_TIMESTAMP = now(), PROCESSED_TIMESTAMP = NULL where EVENT_DATA_ID = 2149725839;
    }

    /**
     * Submit New SIM-Card Registration Request to CSIS. This method is AUTO
     * called when a new SIM-Card is registered in SEP, this will result in the
     * SIM-Card also being registered with TCRA's CSIS.
     *
     * @param prodInstanceId identifies a new SIM
     * @param verifyOnly Allows to simulate call to CSIS.
     * @return response from CSIS.
     */
    public String submitRegistrationRequest(int prodInstanceId, boolean verifyOnly) throws Exception {
        log.debug("Entering submitRegistrationRequest");
        ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(prodInstanceId, com.smilecoms.commons.sca.StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP);

        ServiceInstance sim = getSIMServiceInstanceForPI(pi);
        if (sim == null) {
            log.debug("This is not a SIM service so not going to submit to CSIS");
            return "No SIM";
        }

        ServiceInstance voice = getVoiceServiceInstanceForPI(pi);
        if (voice == null) {
            log.debug("There is no SmileVoice not going to submit to CSIS");
            return "No SmileVoice";
        }

        log.debug("All pre-checks done for PI [{}], verifyOnly mode is [{}]", pi.getProductInstanceId(), verifyOnly);

        Customer simOwner = SCAWrapper.getAdminInstance().getCustomer(pi.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Customer createdBy = SCAWrapper.getAdminInstance().getCustomer(pi.getCreatedByCustomerProfileId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS);
        Organisation agentOrg = SCAWrapper.getAdminInstance().getOrganisation(pi.getCreatedByOrganisationId(), StOrganisationLookupVerbosity.MAIN);
        Organisation simOrgOwner = null;
        
        if (pi.getOrganisationId() > 0) {
            simOrgOwner = SCAWrapper.getAdminInstance().getOrganisation(pi.getOrganisationId(), StOrganisationLookupVerbosity.MAIN);
        }
        
        log.debug("Finised getting SCA data, preparing data for submission to CSIS");
        
        SubRegistration subRegistration = new SubRegistration();
        subRegistration.setVersion(Double.parseDouble(version));//Attribute
        subRegistration.setOperatorCode(operatorCode);
        subRegistration.setOperatorPassword(generateEncryptedOperatorPassword());

        String currentTime = df.format(new java.util.Date());
        subRegistration.setTimestamp(currentTime);

        SubRegistration.RegistrationRequest rr = getRegistrationRequest(pi, simOwner, createdBy, agentOrg, simOrgOwner);
        subRegistration.setRegistrationRequest(rr);

        String eventKey = subRegistration.getRegistrationRequest().getRegistrationDetails().getOperatorConversationId();
        String reg = marshallSoapObjectToString(subRegistration);

        createEvent("submitRegistrationRequest", eventKey, reg);
        
        boolean Proxy=false;
        if(useProxy.equalsIgnoreCase("true")){
            Proxy=true;
        }
        
        String ret = doXMLPost1(CSISEndpoint, reg, Proxy, verifyOnly, "submitRegistrationRequest");

        createEvent("submitRegistrationResponse", eventKey, ret);

        log.debug("Exiting submitRegistrationRequest");
        return ret;
    }
    

    public String registrationResult(String eventKey, boolean verifyOnly) {

        EventQuery eq = new EventQuery();
        eq.setEventKey(eventKey);
        eq.setEventSubType("RegistrationResult");//Temporal de-activate
        eq.setResultLimit(1);

        int prodInstanceId = Integer.parseInt(eventKey.split("\\-")[0]);
        
        log.debug("Going to look for CSIS in events data using event key {}", eventKey);
        EventList events = SCAWrapper.getAdminInstance().getEvents(eq);

        String data = "";
        long id = 0;
        if (events.getNumberOfEvents() == 1) {
            log.debug("Found event using event key {}", eventKey);
            data = events.getEvents().get(0).getEventData();
            id = events.getEvents().get(prodInstanceId).getEventId();
        }

        if (data == null || data.isEmpty()) {
            log.debug("Data for event id {} is empty so nothing to process: {}", data, id);
            return "Failed";
        }

        log.debug("Data to process is: {}", data);
        SubRegistration.RegistrationResult registrationResult = (SubRegistration.RegistrationResult) unmarshallXmlStringToObject(data, SubRegistration.RegistrationResult.class);

        if (registrationResult.getResultCode() == 99) {

            log.debug("Going to call SCA to deactivate service");
            ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(prodInstanceId, com.smilecoms.commons.sca.StProductInstanceLookupVerbosity.MAIN_SVC);
            ProductOrder pOrder = new ProductOrder();
            pOrder.setAction(StAction.NONE);
            pOrder.setProductInstanceId(prodInstanceId);

            pOrder.setOrganisationId(pi.getOrganisationId());
            pOrder.setProductSpecificationId(pi.getProductSpecificationId());
            pOrder.setCustomerId(pi.getCustomerId());

            ServiceInstanceOrder siOrder = new ServiceInstanceOrder();

            for (ProductServiceInstanceMapping psim : pi.getProductServiceInstanceMappings()) {
                psim.getServiceInstance().setStatus("TD"); // De-activate the service instance
                siOrder.setServiceInstance(psim.getServiceInstance());
            }

            siOrder.setAction(StAction.UPDATE);

            pOrder.getServiceInstanceOrders().add(siOrder);
            SCAWrapper.getAdminInstance().processOrder(pOrder);
            log.debug("Ddone calling SCA to deactivate service");
        }

        return "Ok";
    }

    public String submitChurnRequest() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String submitRegistrationImageRequest() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String submitRegistrationNamesRequest() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private String doXMLPost(String strURL, String data, boolean useProxy, boolean verifyOnly, String methodName) {
        if (verifyOnly) {
            log.debug("Not doing post as verifyOnly is set to true");
            return "verifyOnly";
        }
        PostMethod post = null;
        HttpClient httpclient = null;
        String ret = null;
        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(data.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            // Get HTTP client
            httpclient = new HttpClient();
            // use  SimpleHttpConnectionManager cause one can close its ports and prevent close-wait states
            httpclient.setHttpConnectionManager(new SimpleHttpConnectionManager());
            HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
                @Override
                public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                    // do not retry
                    return false;
                }
            };

            HttpParams httpParams = new HttpClientParams();
            httpParams.setParameter(HttpClientParams.RETRY_HANDLER, myretryhandler);
            httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 30000);
            httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
            httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
            httpclient.setParams((HttpClientParams) httpParams);

            if (useProxy) {
                String proxyHost = BaseUtils.getProperty("env.http.proxy.host", "");
                int proxyPort = BaseUtils.getIntProperty("env.http.proxy.port", 0);
                if (!proxyHost.isEmpty() && proxyPort > 0) {
                    log.debug("Post will use a proxy server [{}][{}]", proxyHost, proxyPort);
                    httpclient.getHostConfiguration().setProxy(proxyHost, proxyPort);
                } else {
                    httpclient.getHostConfiguration().setProxyHost(null);
                }
            } else {
                httpclient.getHostConfiguration().setProxyHost(null);
            }

            post.addRequestHeader("Data-Signature", getDataSignature(data));

            if (log.isDebugEnabled()) {
                log.debug("********** CSIS Request Headers **********");
                for (Header header : post.getRequestHeaders()) {
                    String name = header.getName();
                    String value = header.getValue();
                    log.debug(name + ":" + value);
                }
                log.debug("********** CSIS End of Request Headers **********");
            }

            if (log.isDebugEnabled()) {
                log.debug("About to post to method...");
            }

            long start = System.currentTimeMillis();
            int result = httpclient.executeMethod(post);
            long end = System.currentTimeMillis();

            long callDuration = end - start;
            log.debug("Finished making a remote call to CSIS. Request took " + callDuration + "ms");

            String statName = "Lib_TCRASimCardInfoSystem." + methodName;
            BaseUtils.addStatisticSample(statName, BaseUtils.STATISTIC_TYPE.latency, callDuration);

            if (log.isDebugEnabled()) {
                log.debug("********** CSIS Response Headers Received **********");
                for (Header header : post.getResponseHeaders()) {
                    String name = header.getName();
                    String value = header.getValue();
                    log.debug(name + ":" + value);
                }
                log.debug("********** CSIS End of Response Headers **********");
            }

            String resultBody = null;
            if (post.getResponseBodyAsStream() != null) {
                resultBody = StringEscapeUtils.unescapeXml(BaseHelper.parseStreamToString(post.getResponseBodyAsStream(), "UTF-8"));
            }

            // Display status code
            if (log.isDebugEnabled()) {
                log.debug("Finished post to method");
                log.debug("HTTP Post Response status code: " + result);
                log.debug("Response body: ");
                log.debug(resultBody);
            }

            if (result == 200 && resultBody != null) {
                ret = resultBody;
            } else {

                if (result != 200 && !resultBody.equals("OK")) {
                    //BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Non 200 result code returned when processing event with HTTP Notification. [URL: " + strURL + "] [Request: " + data + "] [Response: " + resultBody + "]");
                }
            }

        } catch (Exception e) {
            log.warn("Error posting to address " + strURL + " : " + e.toString());
            ret = "Error posting to " + strURL + " : " + e.toString();
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                post.releaseConnection();
            } catch (Exception ex) {
                log.warn("Error releasing http post connection:" + ex.toString());
            }
            try {
                ((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
            } catch (Exception ex) {
                log.warn("Error releasing http client connection:" + ex.toString());
            }
        }

        return ret;
    }

    private SubRegistration.RegistrationRequest getRegistrationRequest(ProductInstance pi, Customer simOwner, Customer customerAgent, Organisation org, Organisation simOrgOwner) {

        SubRegistration.RegistrationRequest rr = new SubRegistration.RegistrationRequest();
        SubRegistration.RegistrationRequest.RegistrationDetails rd = new SubRegistration.RegistrationRequest.RegistrationDetails();

        rr.setRegistrationDetails(rd);

        bindSCADataToRegistrationDetails(rd, pi, simOwner, customerAgent, org, simOrgOwner);

        return rr;
    }

    private void bindSCADataToRegistrationDetails(SubRegistration.RegistrationRequest.RegistrationDetails rd, ProductInstance pi,
            Customer simOwner, Customer customerAgent, Organisation org, Organisation simOrgOwner) {
        log.debug("in bindSCADataToRegistrationDetails");
        rd.setType("New");//Attribute {New, Reregistration, SIMSwap, Porting}
        String operatorConversationId = String.valueOf(pi.getProductInstanceId()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        //String operatorConversationId = getSHA256(simOwner.getAlternativeContact1());
        rd.setOperatorConversationId(operatorConversationId);

        //<!--if NIDA verified--> WHAT DOES THIS MEAN?
        ServiceInstance sim = getSIMServiceInstanceForPI(pi);
        String NIDATransactionId = "";
        String NIDAVerifiedOnDate = "";
        for (AVP avp : sim.getAVPs()) {
            if (avp.getAttribute().equals("NIDATransactionId")) {
                NIDATransactionId = avp.getValue();
            }
            if (avp.getAttribute().equals("NIDAVerifiedOnDate")) {
                NIDAVerifiedOnDate = avp.getValue();
            }
        }
        rd.setTransactionID(NIDATransactionId);
        String piVerificationDate = df.format(Utils.getDateFromString(NIDAVerifiedOnDate, "dd/MM/yyyy HH:mm:SS"));////20170620125300Z+0300
        rd.setVerificationDate(piVerificationDate);
        rd.setNationalIDNumber(simOwner.getIdentityNumber());
        rd.setMsisdn(getSmileVoiceNumber(pi));

        rd.setImsi(getIMSI(pi));
        rd.setIccid(pi.getPhysicalId());

        //<!--Individual/Minor/Corporate-->
        if(simOwner.getClassification().equalsIgnoreCase("foreigner"))
        {
            rd.setRepresentationType("Foreigner");
        } else if(simOwner.getClassification().equalsIgnoreCase("diplomat"))
        {
            rd.setRepresentationType("Diplomat");
        } else if(simOwner.getClassification().equalsIgnoreCase("minor"))
        {
            rd.setRepresentationType("Minor");
        } else if(pi.getOrganisationId() > 0)
        {
            rd.setRepresentationType("Corporate");
        } else {
            rd.setRepresentationType("Individual");
        }

        rd.setCustomerType("Prepaid");//{Diplomatic, Internal, VIP, Prepaid, Postpaid}

        //<!--Self/Employee/Owner/Director/Partner/Parent/Guardian {Self, Employee, Owner, Director, Partner, Parent, Guardian}-->
        if(rd.getRepresentationType().equalsIgnoreCase("Minor"))
        {
            rd.setRelationship("Guardian");
        } else {
            rd.setRelationship("Self");
        }

        //<!--If Corporate, this will be the company's representative If representation type is minor, the below info is of his/her guardian-->
        rd.setFirstName(simOwner.getFirstName());
        rd.setLastName(simOwner.getLastName());
        rd.setMiddleName(simOwner.getMiddleName());

        String dateOfBirth = otherDataDF.format(Utils.getJavaDate(simOwner.getCreatedDateTime()));
        rd.setDateOfBirth(dateOfBirth);
        rd.setSex(simOwner.getGender());
        rd.setNationality("Tanzanian");
        if (!simOwner.getNationality().equalsIgnoreCase("TZ")) {
            List<String[]> worldCountries = BaseUtils.getPropertyFromSQL("global.country.codes");
            for (String[] row : worldCountries) {
                if (row[1].equals(simOwner.getNationality())) {
                    rd.setNationality(row[0]);
                    break;
                }
            }
        }

        rd.setOtherIDNumber("");
        rd.setOtherIDType("");  //{Voter ID, Driving Licence, Passport}
        if (!simOwner.getIdentityNumberType().equalsIgnoreCase("nationalid")) {
            rd.setOtherIDNumber(simOwner.getIdentityNumber());
            rd.setOtherIDType(simOwner.getIdentityNumberType());  //{Voter ID, Driving Licence, Passport}
        }
        String cardRegistrationDate = df.format(Utils.getJavaDate(pi.getCreatedDateTime()));
        rd.setCardRegistrationDate(cardRegistrationDate);
        rd.setPlaceOfRegistration(simOwner.getAddresses().get(0).getZone());

        rd.setAgentType("Smile Employee");//Freelancer/Wholeseler/Franchise etc
        rd.setAgentName(customerAgent.getFirstName() + " " + customerAgent.getLastName());
        rd.setAgentMsisdn(Utils.getCleanDestination(customerAgent.getAlternativeContact1()));

        rd.setRegion(simOwner.getAddresses().get(0).getState());//Vincent to verify
        rd.setDistrict(simOwner.getAddresses().get(0).getTown());
        rd.setWard(simOwner.getAddresses().get(0).getZone());
        rd.setPostcode(simOwner.getAddresses().get(0).getCode());
        rd.setPhysicalAddress(simOwner.getAddresses().get(0).getLine1() + ", " + simOwner.getAddresses().get(0).getLine2());

        //<!--if representation type is corporate-->
        if (pi.getOrganisationId() > 0) {
            rd.setCorporateName(simOrgOwner.getOrganisationName());
            String corporateRegistrationDate = otherDataDF.format(Utils.getJavaDate(simOrgOwner.getCreatedDateTime()));
            rd.setCorporateRegistrationDate(corporateRegistrationDate);
            rd.setTinNumber(simOrgOwner.getTaxNumber());
        }

        if(!(rd.getRepresentationType().equalsIgnoreCase("Individual") || rd.getRepresentationType().equalsIgnoreCase("Foreigner") || rd.getRepresentationType().equalsIgnoreCase("Diplomat")))
        {
            //<!--if minor, userFirstName,userLastName are mandatory-->
            //<!--if corporate, msisdn is mandatory; other fields optional-->
            SubRegistration.RegistrationRequest.RegistrationDetails.OtherNumbers otherNumbers = new SubRegistration.RegistrationRequest.RegistrationDetails.OtherNumbers();
            SubRegistration.RegistrationRequest.RegistrationDetails.OtherNumbers.OtherNumber otherNumber = new SubRegistration.RegistrationRequest.RegistrationDetails.OtherNumbers.OtherNumber();
            otherNumber.setMsisdn(simOwner.getAlternativeContact1());
            otherNumbers.getOtherNumber().add(otherNumber);
            rd.setOtherNumbers(otherNumbers);
        }

        if (pi.getOrganisationId() > 0) 
        {
            //<!--other Registrations; more of Corporate representation type-->
            SubRegistration.RegistrationRequest.RegistrationDetails.OtherRegistrations otherRegistrations = new SubRegistration.RegistrationRequest.RegistrationDetails.OtherRegistrations();
            SubRegistration.RegistrationRequest.RegistrationDetails.OtherRegistrations.OtherRegistration otherRegistration = new SubRegistration.RegistrationRequest.RegistrationDetails.OtherRegistrations.OtherRegistration();
            otherRegistration.setRegistrationDate("");
            otherRegistration.setRegistrationNumber("");
            otherRegistration.setRegistrationType("");
            otherRegistrations.getOtherRegistration().add(otherRegistration);
            rd.setOtherRegistrations(otherRegistrations);
        }

        //<!-- start of images...to be skipped in initial implementation -->
        //SubRegistration.RegistrationRequest.RegistrationDetails.RegistrationImages registrationImages = new SubRegistration.RegistrationRequest.RegistrationDetails.RegistrationImages();
        //SubRegistration.RegistrationRequest.RegistrationDetails.RegistrationImages.RegistrationImage registrationImage = new SubRegistration.RegistrationRequest.RegistrationDetails.RegistrationImages.RegistrationImage();
        //registrationImage.setImageContent("");
        //registrationImage.setImageFormat("");
        //registrationImage.setImageType("");
        //registrationImages.getRegistrationImage().add(registrationImage);
        //rd.setRegistrationImages(registrationImages);

    }

    public static String marshallSoapObjectToString(Object inputObject) {

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
            Marshaller marshaller = Utils.getJAXBMarshallerForSoap(inputClass);
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
            log.debug("Finished marshalling object to xml. Result is [{}]", xml);
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

    private String generateEncryptedOperatorPassword() {
        //1. Operator Password is the uppercased sha-256 hash of the concatenation (with a given delimiter) of the operator code, given password and the timestamp
        //... (yyyyMMddHHmmss'Z'Z formatted - tolerance of ± 15-minute time with reference to the time the request is being received/sent in/from CSIS).
        //2. Given parameters --> operatorCode: 00002, givenPassword: operator@123, timestamp: 20181114040609Z+0300, Recommended Delimiter is semicolon (;)
        //3. Checksum = uppercase(sha256(operatorCode + delimiter + givenPassword + delimiter + timestamp));
        //4. Example concatenated string --> 00002;operator@123;20181114040609Z+0300
        //5. Example operatorPassword --> E9E4B88DAEABD337CD28B852FFC8A048BEB3524789917A225774E00F5DE21F9

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'Z'Z");
        String timestamp = sdf.format(new java.util.Date());
        String textToHash = operatorCode + delimiter + givenPassword + delimiter + timestamp;
        return getSHA256(textToHash);
    }

    private String getSHA256(String text) {
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

    private String getDataSignature(String entityData) throws Exception {
        /**
         * TCRA CSIS rules for signing the payload.
         *
         * 1. The whole request body will have to be digitally signed using
         * "SHA256withRSA" algorithm and later be BASE64 encoded
         *
         * 2. Public/Private key pairs to be used in signature algorithm shall
         * be agreed between CSIS and MNO implementation teams
         *
         * 3. The signature will be appended as the parameter (with the name
         * "Data-Signature") on the request header.
         *
         * 4. Upon verifying the signature; CSIS will be using the operator's
         * public certificate present in her repository to determine whether or
         * not there was anomaly on the data received.
         *
         * 5. The methodology is the same when CSIS responds/queries back the
         * Operator, CSIS will have to share her public key in correspondence to
         * the signing of data to be sent to the Operator.
         */
        RSAPrivateKey rsaPrivateKey = getRSAPrivateKey(privateKeyFilename);
        String signedData = sign(entityData, rsaPrivateKey);
        return signedData;
    }

    private static RSAPrivateKey getRSAPrivateKey(String keyFilename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        /**
         * Steps to followed in generating key pair.
         *
         * 1. Generate key pair: openssl genrsa -out private.pem 1024
         *
         * 2. Extract public key: openssl rsa -in private.pem -out public.pem
         * -outform PEM -pubout
         *
         * 3. Convert the private key from PEM to DER: openssl pkcs8 -topk8
         * -inform PEM -outform DER -in private.pem -nocrypt > private.der
         */
        byte[] encodedKey = readFile(keyFilename);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        return privateKey;
    }

    private static RSAPublicKey getRSAPublicKey(String keyFilename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encodedKey = readFile(keyFilename);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    public static String sign(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes("UTF-8"));

        byte[] signature = privateSignature.sign();

        return Base64.getEncoder().encodeToString(signature);
    }

    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        /**
         * We get an instance of the RSA cipher and set it up in encrypt mode
         * where we also pass in the public key used to encrypt the message. We
         * pass in the bytes of the plainText string in one go and end up with a
         * byte array of encrypted bytes. These bytes then get base 64 encoded
         * and returned.
         */
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = encryptCipher.doFinal(plainText.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        /**
         * We get a RSA cipher, initialize it with the private key this time,
         * and decrypt the bytes and turn them into a String again.
         */
        byte[] bytes = Base64.getDecoder().decode(cipherText);

        Cipher decriptCipher = Cipher.getInstance("RSA");
        decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(decriptCipher.doFinal(bytes), "UTF-8");
    }

    public static boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes("UTF-8"));

        byte[] signatureBytes = Base64.getDecoder().decode(signature);

        return publicSignature.verify(signatureBytes);
    }

    private static byte[] readFile(String filename) throws IOException {
        File file = new File(filename);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = new byte[(int) file.length()];
        bis.read(bytes);
        bis.close();
        return bytes;
    }

    private void init() {
        //String filename = "/tmp/testkeys/private.der";
        operatorCode = BaseUtils.getSubProperty("env.csis.config", "OperatorCode");
        givenPassword = BaseUtils.getSubProperty("env.csis.config", "GivenPassword");
        privateKeyFilename = BaseUtils.getSubProperty("env.csis.config", "PrivateKeyFilename");
        CSISEndpoint = BaseUtils.getSubProperty("env.csis.config", "CSISEndpoint");
        useProxy = BaseUtils.getSubProperty("env.csis.config", "Proxy");
        version = BaseUtils.getSubProperty("env.csis.config", "Version");
        jks_file = BaseUtils.getSubProperty("env.csis.config", "CSIS_JKS_FILE");
        socketTimeout = BaseUtils.getSubProperty("env.csis.config", "SocketTimeOut");
        connTimeout = BaseUtils.getSubProperty("env.csis.config", "ConnTimeOut");
        proxyip = BaseUtils.getSubProperty("env.csis.config", "ProxyIp");
        proxyport = BaseUtils.getSubProperty("env.csis.config", "ProxyPort");
    }

    private static ServiceInstance getSIMServiceInstanceForPI(ProductInstance pi) {
        List<ServiceInstance> siList = getServiceInstanceListWithAVPsForPI(pi);
        for (ServiceInstance si : siList) {
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("ProvisioningFlow") && avp.getValue().equals("lte")) {
                    return si;
                }
            }
        }
        return null;
    }

    private static ServiceInstance getVoiceServiceInstanceForPI(ProductInstance pi) {
        List<ServiceInstance> siList = getServiceInstanceListWithAVPsForPI(pi);
        for (ServiceInstance si : siList) {
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("ProvisioningFlow") && avp.getValue().equals("voice")) {
                    return si;
                }
            }
        }
        return null;
    }

    private static List<ServiceInstance> getServiceInstanceListWithAVPsForPI(ProductInstance pi) {
        List<ServiceInstance> siList = new ArrayList();
        for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
            siList.add(m.getServiceInstance());
        }
        return siList;
    }

    private static String getIMSI(ProductInstance pi) {
        try {
            String imsi = "NA";
            ServiceInstance si = getSIMServiceInstanceForPI(pi);

            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equalsIgnoreCase("PrivateIdentity")) {
                    imsi = Utils.getCleanDestination(avp.getValue());
                    break;
                }
            }
            int atLoc = imsi.indexOf("@");
            if (atLoc != -1) {
                imsi = imsi.substring(0, atLoc);
            }
            return imsi;
        } catch (Exception e) {
            log.warn("Error getting IMSI: ", e);
            return "NA";
        }
    }

    private static String getSmileVoiceNumber(ProductInstance pi) {
        try {
            String phone = "NA";
            ServiceInstance si = getVoiceServiceInstanceForPI(pi);

            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                    phone = Utils.getCleanDestination(avp.getValue());
                    break;
                }
            }
            int atLoc = phone.indexOf("@");
            if (atLoc != -1) {
                phone = phone.substring(0, atLoc);
            }
            return phone;
        } catch (Exception e) {
            log.warn("Error getting service instance Phone Number: ", e);
            return "NA";
        }
    }

    private Done createEvent(String eventSubType, String eventKey, String eventData) {
        Event event = new Event();
        event.setEventSubType(eventSubType);
        event.setEventType("TCRA_CSIS");
        event.setEventKey(eventKey);
        event.setEventData(eventData);
        Done done;
        try {
            done = SCAWrapper.getAdminInstance().createEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to create event for CSIS via SCA call, cause: {}", ex.toString());
            done = new Done();
        }
        return done;
    }
    
    private String doXMLPost1(String strURL, String data, boolean useProxy, boolean verifyOnly, String methodName)  throws Exception{
        if (verifyOnly) {
            log.debug("Not doing post as verifyOnly is set to true");
            return "verifyOnly";
        }
        HttpHost host = null;
        log.info("Inside doXMLPost1");
        KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File(jks_file));
        try {
            trustStore.load(instream, "changeit".toCharArray());
        } finally {
            instream.close();
        }
        log.info("Keystore set");
        // Trust own CA and all self-signed certs
        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();
        // Allow TLSv1 protocol only
        //SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext,new String[] { "TLSv1" },null,SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
        log.info("SSL connection set");
        
        if (useProxy) {
            String proxyHost = proxyip;
            int proxyPort = Integer.parseInt(proxyport);
            if (!proxyHost.isEmpty() && proxyPort > 0) {
                log.debug("Post will use a proxy server [{}][{}]", proxyHost, proxyPort);
                host = new HttpHost(proxyHost, proxyPort);
            }
        }
        
        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setSocketTimeout(Integer.parseInt(socketTimeout))
            .setConnectTimeout(Integer.parseInt(connTimeout))
            .setProxy(host)
            .build();
        
        log.info("Request config set");
        
        HttpPost post = null;
        CloseableHttpClient httpclient;
        String ret = null;
        
        try {
            // Get file to be posted
            post = new HttpPost(strURL);
            //HttpEntity entity = new ByteArrayRequestEntity(data.getBytes(), "text/xml; charset=utf-8");

            log.info("Entity is:"+data);
            
            post.setEntity(new StringEntity(data));
            // Get HTTP client
            httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultRequestConfig(defaultRequestConfig).build();
            
            log.info("Http client set");
            
            post.addHeader("Content-type", "text/xml");
            post.addHeader("Data-Signature", getDataSignature(data));

            if (log.isDebugEnabled()) {
                log.debug("********** CSIS Request Headers **********");
                for (org.apache.http.Header header : post.getAllHeaders()) {
                    String name = header.getName();
                    String value = header.getValue();
                    log.debug(name + ":" + value);
                }
                log.debug("********** CSIS End of Request Headers **********");
            }

            if (log.isDebugEnabled()) {
                log.debug("About to post to method...");
            }

            log.info("Posting now");
            long start = System.currentTimeMillis();
            CloseableHttpResponse response = httpclient.execute(post);
            int result=response.getStatusLine().getStatusCode();
            //int result = httpclient.executeMethod(post);
            log.info("post completed with status "+result);
            long end = System.currentTimeMillis();

            long callDuration = end - start;
            log.debug("Finished making a remote call to CSIS. Request took " + callDuration + "ms");

            String statName = "Lib_TCRASimCardInfoSystem." + methodName;
            BaseUtils.addStatisticSample(statName, BaseUtils.STATISTIC_TYPE.latency, callDuration);

            if (log.isDebugEnabled()) {
                log.debug("********** CSIS Response Headers Received **********");
                for (org.apache.http.Header header : response.getAllHeaders()) {
                    String name = header.getName();
                    String value = header.getValue();
                    log.debug(name + ":" + value);
                }
                log.debug("********** CSIS End of Response Headers **********");
            }

            String resultBody = null;
            if (response.getEntity() != null) {
                resultBody = EntityUtils.toString(response.getEntity());
            }

            // Display status code
            if (log.isDebugEnabled()) {
                log.debug("Finished post to method");
                log.debug("HTTP Post Response status code: " + result);
                log.debug("Response body: ");
                log.debug(resultBody);
            }

            if (result == 200 && resultBody != null) {
                ret = resultBody;
            } else {

                if (result != 200 && !resultBody.equals("OK")) {
                    //BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "ET", "Non 200 result code returned when processing event with HTTP Notification. [URL: " + strURL + "] [Request: " + data + "] [Response: " + resultBody + "]");
                }
            }

        } catch (Exception e) {
            log.warn("Error posting to address " + strURL + " : " + e.toString());
            ret = "Error posting to " + strURL + " : " + e.toString();
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                post.releaseConnection();
            } catch (Exception ex) {
                log.warn("Error releasing http post connection:" + ex.toString());
            }
        }

        return ret;
    }

}
