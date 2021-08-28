/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.nida.restclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class NidaRestClient implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(NidaRestClient.class);
    public static final String FINGER_LEFT_THUMB = "LEFT_THUMB";
    public static final String FINGER_RIGHT_THUMB = "RIGHT_THUMB";
    public static final String FINGER_RIGHT_INDEX_FINGER = "RIGHT_INDEX_FINGER";
    public static final String FINGER_LEFT_INDEX_FINGER = "LEFT_INDEX_FINGER";

    public static NidaResponse verifyCustomerWithNIDA(String idNumber, byte[] fingerPrintWSQ, String verifiedBy, String entityType, String entityId) throws Exception {

        /*
           curl -X POST \ https://compliance.registersim.com:8443/identity/verify \ 
                       -H 'Accept: application/json' \ 
                       -H 'Authorization: Basic c21pbGV0ejprZXVldUhScmhOY3BiODd0ZWdiMjZVOUQ=' \ 
                       -H 'Cache-Control: no-cache' \ 
                       -H 'Postman-Token: f92009b7-e797-1280-829e-f04aec45f795' \ 
                       -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' \ 
                       -F identityNumber=19891005111010000126 \ 
                       -F fingerCode=R2 \ 
                       -F fingerprintPath=https://s3.amazonaws.com/ereg2/vodacom/images/20180116/713000034_723694d758f65236_20180116203153_finger-print_.wsq */
        WebClient client = null;

        try {

            String URL = NidaHelper.props.getProperty("WindowsServerRestURL");

            log.warn("Connecting to NIDA URL using: [{}]", URL);

            client = WebClient
                    .create(URL,
                            Collections.singletonList(new JacksonJsonProvider()))
                    .path("verify-fingerprint").accept(MediaType.APPLICATION_JSON_TYPE)
                    .type(MediaType.APPLICATION_JSON);

            //ClientConfiguration config = WebClient.getConfig(client);
            //config.getInInterceptors().add(new LoggingInInterceptor());
            //config.getOutInterceptors().add(new LoggingOutInterceptor());
            // Replace 'user' and 'password' by the actual values
            // String authorizationHeader = "Basic "
            // + org.apache.cxf.common.util.Base64Utility.encode((NidaHelper.props.getProperty("AIMUsername") + 
            //     ":" 
            //       +  NidaHelper.props.getProperty("AIMPassword")).getBytes());
            // web clients
            // client.header("Authorization", authorizationHeader);
            client.header("Cache-Control", "no-cache");
            // client.header("Content-type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
            HTTPConduit conduit2 = (HTTPConduit) WebClient.getConfig(client).getConduit();
            HTTPClientPolicy httpClientPolicy = conduit2.getClient();

            //Set Proxy;
            /*
            String proxCon = System.getenv("HTTP_PROXY");
            log.debug("Found value for env HTTP_PROXY: [{}]", proxCon);
            if (proxCon != null) {
                String[] poxyConArray = proxCon.split(":");
                String host = poxyConArray[1].substring(2);
                int port = Integer.parseInt(poxyConArray[2]);
                log.debug("SQUID is on host[{}] and listening on port[{}]", host, port);
                httpClientPolicy.setProxyServer(host);
                httpClientPolicy.setProxyServerPort(port);
                httpClientPolicy.setProxyServerType(ProxyServerType.HTTP);
            }*/
            httpClientPolicy.setConnectionTimeout(Integer.parseInt(NidaHelper.props.getProperty("ConnectionTimeout")));
            httpClientPolicy.setReceiveTimeout(Integer.parseInt(NidaHelper.props.getProperty("ReadTimeout")));

            httpClientPolicy.setAllowChunking(false);

            httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);

            /*
            TLSClientParameters tlsCP = new TLSClientParameters();
            // other TLS/SSL configuration like setting up TrustManagers
            tlsCP.setDisableCNCheck(true);
            KeyStore trustStore = KeyStore.getInstance("JKS");
            String trustStoreLoc = NidaHelper.props.getProperty("TrustStoreLocation"); // /var/smile/install/scripts/NIDA/cacerts
            String keyPassword2 = NidaHelper.props.getProperty("TrustStorePassword");// "changeit";
            trustStore.load(new FileInputStream(trustStoreLoc), keyPassword2.toCharArray());
            TrustManager[] myTrustStoreKeyManagers = getTrustManagers(trustStore);
            tlsCP.setTrustManagers(myTrustStoreKeyManagers);
                        
            conduit2.setTlsClientParameters(tlsCP); */
            conduit2.setClient(httpClientPolicy);

            NidaRequest request = new NidaRequest();

            request.setNin(idNumber);
            request.setTemplate(getFingerPrintData(fingerPrintWSQ, "RIGHT_THUMB"));
            request.setFingerCode("R1");

            ObjectMapper mapper = new ObjectMapper();

            //Object to JSON in String
            String jsonInString = mapper.writeValueAsString(request);

            /*String base64EncodedFinger = getFingerPrintData(fingerPrintWSQ, "RIGHT_THUMB");
            
            String input = "{\"nin\": \""+ idNumber + "\"," + 
                            "\"template\": \"" + base64EncodedFinger +  "\"," + 
                            "\"fingerCode\": \"R1\"}"; */
            log.warn("REQUEST JSON:" + jsonInString);

            // JSONObject inputJsonObj = new JSONObject();
            //inputJsonObj.put("input", "Value");
            Response response = client.post(jsonInString);

            InputStream is = (InputStream) response.getEntity();
            String result = IOUtils.toString(is, "UTF-8");
            log.warn("Response from NIDA [{}].", result);

            // ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            NidaResponse nidaResponse = mapper.readValue(result, NidaResponse.class);

            log.debug("NIDA Response Code: [{}]", nidaResponse.getCode());

            if (BaseUtils.getBooleanProperty("env.nida.best.finger.enable", false)) {
                if (nidaResponse.getCode() != null && nidaResponse.getCode().equalsIgnoreCase("141")) { //If fingerprint not matching
                    log.debug("Verification no Successful: NIDA Status: Responce Code [{}] send alternative fingerprint",
                            nidaResponse.getCode());
                    ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
                    map.put("R2", "RIGHT_INDEX_FINGER");
                    map.put("L1", "LEFT_THUMB");
                    map.put("L2", "LEFT_INDEX_FINGER");
                    for (Map.Entry<String, String> m : map.entrySet()) {
                        log.debug("Finger code [{}] and Finger is [{}]",
                                m.getKey(), m.getValue());
                        request = new NidaRequest();

                        request.setNin(idNumber);
                        request.setTemplate(getFingerPrintData(fingerPrintWSQ, m.getValue()));
                        request.setFingerCode(m.getKey());

                        mapper = new ObjectMapper();

                        //Object to JSON in String
                        jsonInString = mapper.writeValueAsString(request);

                        log.warn("REQUEST JSON:" + jsonInString);

                        response = client.post(jsonInString);

                        is = (InputStream) response.getEntity();
                        result = IOUtils.toString(is, "UTF-8");
                        log.warn("Response from NIDA [{}].", result);

                        // ObjectMapper mapper = new ObjectMapper();
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                        nidaResponse = mapper.readValue(result, NidaResponse.class);
                        if (nidaResponse.getCode() != null && !nidaResponse.getCode().equalsIgnoreCase("141")) {
                            break;
                        }
                    }
                }
            }

            if (nidaResponse.getCode() != null && nidaResponse.getCode().equalsIgnoreCase("00")) { //For Successful
                log.debug("Verify Successful: NIDA Status: Responce Code [{}]",
                        nidaResponse.getCode());

            } else {
                log.error("NIDA Verification Failed, Code: [{}]", nidaResponse.getCode());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

            Event event = new Event();
            event.setEventType("SEP");
            event.setEventSubType("NIDAQueryResponse");
            event.setEventKey(entityId);
            event.setEventData(entityType + "|Response Code:" + nidaResponse.getCode() + "|" + idNumber + "|" + verifiedBy + "|"
                    + nidaResponse.getTransactionId() + "|" + sdf.format(new Date()));

            SCAWrapper.getAdminInstance().createEvent(event);
            return nidaResponse;
        } catch (java.net.SocketTimeoutException ex) {
            log.error("Error[{}]", ex);

            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("NIDAQueryResponse");
            event.setEventKey(entityId);
            event.setEventData(entityType + "|" + idNumber + "|" + verifiedBy + "|"
                    + "ERROR:" + ex.getMessage());
            SCAWrapper.getAdminInstance().createEvent(event);
            throw new Exception("Connection timeout while trying to reach NIDA");
        } catch (Exception ex) {
            log.error("Error[{}]", ex);

            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("NIDAQueryResponse");
            event.setEventKey(entityId);
            event.setEventData(entityType + "|" + idNumber + "|" + verifiedBy + "|"
                    + "ERROR:" + ex.getMessage());
            SCAWrapper.getAdminInstance().createEvent(event);
            throw ex;
        } finally {
            if (client != null) {
                //client.getResponse().;
            }
        }
    }

    private static TrustManager[] getTrustManagers(KeyStore trustStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        String alg = KeyManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory fac = TrustManagerFactory.getInstance(alg);
        fac.init(trustStore);
        return fac.getTrustManagers();
    }

    public static String getFingerPrintData(byte[] fingerprints, String fingerType) throws Exception {

        InputStream fis = null;
        DataInputStream in = null;
        String base64FingerPrintData = null;

        try {

            log.debug("VerificationFinger to be used is: [{}]", fingerType);

            if (fingerprints == null) {
                throw new Exception("Fingerprint data not supplied.");
            }

            fis = new ByteArrayInputStream(fingerprints);
            in = new DataInputStream(fis);

            byte[] leftIndexWSQImage;
            byte[] leftThumbWSQImage;
            byte[] rightThumbWSQImage;
            byte[] rightIndexWSQImage;

            int leftIndexWSQImageSize, leftThumbWSQImageSize, rightThumbWSQImageSize, rightIndexWSQImageSize;
            double version = in.readDouble();

            log.info("Fingerprint file version [{}].", version);

            leftIndexWSQImageSize = in.readInt();
            leftThumbWSQImageSize = in.readInt();
            rightThumbWSQImageSize = in.readInt();
            rightIndexWSQImageSize = in.readInt();

            log.info("Fingerprint sizes : leftIndexWSQImageSize:" + leftIndexWSQImageSize
                    + " :: leftThumbWSQImageSize:" + leftThumbWSQImageSize
                    + " :: rightThumbWSQImageSize:" + rightThumbWSQImageSize + " :: rightIndexWSQImageSize:" + rightIndexWSQImageSize);

            leftIndexWSQImage = new byte[leftIndexWSQImageSize];
            leftThumbWSQImage = new byte[leftThumbWSQImageSize];
            rightThumbWSQImage = new byte[rightThumbWSQImageSize];
            rightIndexWSQImage = new byte[rightIndexWSQImageSize];

            in.readFully(leftIndexWSQImage, 0, leftIndexWSQImageSize);
            in.readFully(leftThumbWSQImage, 0, leftThumbWSQImageSize);
            in.readFully(rightThumbWSQImage, 0, rightThumbWSQImageSize);
            in.readFully(rightIndexWSQImage, 0, rightIndexWSQImageSize);

            // Write images to files;
            writeFile(leftIndexWSQImage, "/root/leftIndexWSQImage.wsq");
            writeFile(leftThumbWSQImage, "/root/leftThumbWSQImage.wsq");
            writeFile(rightThumbWSQImage, "/root/rightThumbWSQImage.wsq");
            writeFile(rightIndexWSQImage, "/root/rightIndexWSQImage.wsq");

            if (fingerType.equalsIgnoreCase(FINGER_LEFT_INDEX_FINGER)) {
                //return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(leftIndexWSQImage);
                return new String(Base64.encodeBase64(leftIndexWSQImage), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_LEFT_THUMB)) {
                // return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(leftThumbWSQImage);
                return new String(Base64.encodeBase64(leftThumbWSQImage), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_RIGHT_THUMB)) {
                // return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rightThumbWSQImage);
                return new String(Base64.encodeBase64(rightThumbWSQImage), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_RIGHT_INDEX_FINGER)) {
                //return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rightIndexWSQImage);
                return new String(Base64.encodeBase64(rightIndexWSQImage), "UTF-8");
            } else {
                throw new Exception("No finger specified for extraction.");
            }

        } catch (Exception e) {
            log.error("Error while trying to extract fingerprint data for customer:", e);
            throw e;
        } finally {

            if (fis != null) {
                fis.close();
            }

            if (in != null) {
                in.close();
            }
        }

        // throw new Exception("No finger specified for extraction.");
    }

    public static void writeFile(byte[] data, String path) throws Exception {

        FileOutputStream fos = new FileOutputStream(path);
        fos.write(data);
        fos.flush();
        fos.close();

    }

    @Override
    public void propsAreReadyTrigger() {
        NidaHelper.initialise();
        BaseUtils.registerForPropsChanges(this);
        BaseUtils.deregisterForPropsAvailability(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        NidaHelper.initialise();
    }

    @PostConstruct
    public void startUp() {
        log.warn("NidaHelper starting up.");
        BaseUtils.registerForPropsAvailability(this);

    }

    @PreDestroy
    public void shutDown() {
        log.warn("NidaHelper shutting down.");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.deregisterForPropsChanges(this);
    }
}
