/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.foreigner.verify.restclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.im.tz.nida.restclient.NidaHelper;
import com.smilecoms.im.tz.nida.restclient.NidaRequest;
import com.smilecoms.im.tz.nida.restclient.NidaResponse;
import static com.smilecoms.im.tz.nida.restclient.NidaRestClient.FINGER_LEFT_INDEX_FINGER;
import static com.smilecoms.im.tz.nida.restclient.NidaRestClient.FINGER_LEFT_THUMB;
import static com.smilecoms.im.tz.nida.restclient.NidaRestClient.FINGER_RIGHT_INDEX_FINGER;
import static com.smilecoms.im.tz.nida.restclient.NidaRestClient.FINGER_RIGHT_THUMB;
import static com.smilecoms.im.tz.nida.restclient.NidaRestClient.writeFile;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import jdk.nashorn.internal.parser.JSONParser;
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
 * @author bhaskarhg
 */
@Singleton
@Startup
@Local({BaseListener.class})
public class VerifyForeignerRestClient implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(VerifyForeignerRestClient.class);
    public static final String FINGER_LEFT_THUMB = "LEFT_THUMB";
    public static final String FINGER_RIGHT_THUMB = "RIGHT_THUMB";
    public static final String FINGER_RIGHT_INDEX_FINGER = "RIGHT_INDEX_FINGER";
    public static final String FINGER_LEFT_INDEX_FINGER = "LEFT_INDEX_FINGER";

    public static VerifyForeignerResponse verifyForeignerCustomer(String documentNo, String countryCode, byte[] fingerPrintWSQ, String verifiedBy, String entityType, String entityId) throws Exception {
        WebClient client = null;

        try {

            String URL = VerifyForeignerHelper.props.getProperty("WindowsServerRestVFURL");

            log.warn("Connecting to Verify Foreigner URL using: [{}]", URL);

            client = WebClient
                    .create(URL,
                            Collections.singletonList(new JacksonJsonProvider()))
                    .path("foreigner-verify").accept(MediaType.APPLICATION_JSON_TYPE)
                    .type(MediaType.APPLICATION_JSON);

            client.header("Cache-Control", "no-cache");
            log.warn("***" + client.getCurrentURI() + "***" + client.getBaseURI());

            HTTPConduit conduit2 = (HTTPConduit) WebClient.getConfig(client).getConduit();
            HTTPClientPolicy httpClientPolicy = conduit2.getClient();

            httpClientPolicy.setConnectionTimeout(Integer.parseInt(VerifyForeignerHelper.props.getProperty("ConnectionTimeout")));
            httpClientPolicy.setReceiveTimeout(Integer.parseInt(VerifyForeignerHelper.props.getProperty("ReadTimeout")));

            httpClientPolicy.setAllowChunking(false);

            httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);

            conduit2.setClient(httpClientPolicy);

            VerifyForeignerRequest request = new VerifyForeignerRequest();
            FingerPrints[] fingers = new FingerPrints[1];
            FingerPrints finger = new FingerPrints();
            finger.setCode("R1");
            finger.setImage(getFingerPrintData(fingerPrintWSQ, "RIGHT_THUMB"));
            fingers[0] = finger;

            request.setDocumentNo(documentNo);
            request.setCountryCode(countryCode);
            request.setFingerPrints(fingers);

            ObjectMapper mapper = new ObjectMapper();

            //Object to JSON in String
            String jsonInString = mapper.writeValueAsString(request);

            log.warn("REQUEST JSON:" + jsonInString);

            Response response = client.post(jsonInString);

            InputStream is = (InputStream) response.getEntity();
            String result = IOUtils.toString(is, "UTF-8");
            log.warn("Response from Verify Foreigner [{}].", result);

            // ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            VerifyForeignerResponse verifyForeignerResponse = mapper.readValue(result, VerifyForeignerResponse.class);

            log.debug("Verify Foreigner Response Status: [{}]", verifyForeignerResponse.getStatus());

            if (verifyForeignerResponse.getStatus() != null && verifyForeignerResponse.getStatus().equalsIgnoreCase("0")) { //For Successful
                log.debug("Verify Successful: Verify Foreigner Status: Responce Code [{}]",
                        verifyForeignerResponse.getStatus());

            } else {
                log.error("Verify Foreigner Verification Failed, Status: [{}]", verifyForeignerResponse.getStatus());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

            if (verifyForeignerResponse.getInfo() != null) {
                Event event = new Event();
                event.setEventType("SEP");
                event.setEventSubType("VerifyForeignerQueryResponse");
                event.setEventKey(entityId);
                event.setEventData(entityType + "|Response Status:" + verifyForeignerResponse.getStatus() + "|" + documentNo + "|" + verifiedBy + "|"
                        + verifyForeignerResponse.getInfo().getPassportNo() + "|" + sdf.format(new Date()));
                SCAWrapper.getAdminInstance().createEvent(event);
            }

            return verifyForeignerResponse;
        } catch (java.net.SocketTimeoutException ex) {
            log.error("Error[{}]", ex);

            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("VerifyForeignerQueryResponse");
            event.setEventKey(entityId);
            event.setEventData(entityType + "|" + documentNo + "|" + verifiedBy + "|"
                    + "ERROR:" + ex.getMessage());
            SCAWrapper.getAdminInstance().createEvent(event);
            throw new Exception("Connection timeout while trying to reach VerifyForeignerQueryResponse");
        } catch (Exception ex) {
            log.error("Error[{}]", ex);

            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("VerifyForeignerQueryResponse");
            event.setEventKey(entityId);
            event.setEventData(entityType + "|" + documentNo + "|" + verifiedBy + "|"
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

            log.info("Fingerprint rightThumbWSQImage ****" + " " + new String(Base64.encodeBase64(rightThumbWSQImage)));

            // Write images to files;
            writeFile(leftIndexWSQImage, "/root/leftIndexWSQImage.wsq");
            writeFile(leftThumbWSQImage, "/root/leftThumbWSQImage.wsq");
            writeFile(rightThumbWSQImage, "/root/rightThumbWSQImage.wsq");
            writeFile(rightIndexWSQImage, "/root/rightIndexWSQImage.wsq");

            if (fingerType.equalsIgnoreCase(FINGER_LEFT_INDEX_FINGER)) {
                //return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(leftIndexWSQImage);
                return new String(Base64.encodeBase64(trimZeros(leftIndexWSQImage)), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_LEFT_THUMB)) {
                // return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(leftThumbWSQImage);
                return new String(Base64.encodeBase64(trimZeros(leftThumbWSQImage)), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_RIGHT_THUMB)) {
                // return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rightThumbWSQImage);
                return new String(Base64.encodeBase64(trimZeros(rightThumbWSQImage)), "UTF-8");
            } else if (fingerType.equalsIgnoreCase(FINGER_RIGHT_INDEX_FINGER)) {
                //return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rightIndexWSQImage);
                return new String(Base64.encodeBase64(trimZeros(rightIndexWSQImage)), "UTF-8");
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
    }

    public static void writeFile(byte[] data, String path) throws Exception {

        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(data);
            fos.flush();
        }

    }

    @Override
    public void propsAreReadyTrigger() {
        VerifyForeignerHelper.initialise();
        BaseUtils.registerForPropsChanges(this);
        BaseUtils.deregisterForPropsAvailability(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        VerifyForeignerHelper.initialise();
    }

    @PostConstruct
    public void startUp() {
        log.warn("VerifyForeignerHelper starting up.");
        BaseUtils.registerForPropsAvailability(this);

    }

    @PreDestroy
    public void shutDown() {
        log.warn("VerifyForeignerHelper shutting down.");
        BaseUtils.deregisterForPropsAvailability(this);
        BaseUtils.deregisterForPropsChanges(this);
    }

    public static byte[] trimZeros(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

}
