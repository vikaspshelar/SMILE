/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.verify.defaced.restclient;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
public class VerifyDefacedCustomerRestClient implements BaseListener {

    private static final Logger log = LoggerFactory.getLogger(VerifyDefacedCustomerRestClient.class);

    public static VerifyDefacedCustomerResponse verifyDefacedCustomer(VerifyDefacedCustomerRequest request, String verifiedBy, String entityType, String entityId) throws Exception {
        WebClient client = null;

        try {

            String URL = VerifyDefacedCustomerHelper.props.getProperty("WindowsServerRestDFCURL");

            log.warn("Connecting to Verify Defaced Customer URL using: [{}]", URL);

            client = WebClient
                    .create(URL,
                            Collections.singletonList(new JacksonJsonProvider()))
                    .path("alt-verify").accept(MediaType.APPLICATION_JSON_TYPE)
                    .type(MediaType.APPLICATION_JSON);

            client.header("Cache-Control", "no-cache");
            log.warn("***" + client.getCurrentURI() + "***" + client.getBaseURI());

            HTTPConduit conduit2 = (HTTPConduit) WebClient.getConfig(client).getConduit();
            HTTPClientPolicy httpClientPolicy = conduit2.getClient();

            httpClientPolicy.setConnectionTimeout(Integer.parseInt(VerifyDefacedCustomerHelper.props.getProperty("ConnectionTimeout")));
            httpClientPolicy.setReceiveTimeout(Integer.parseInt(VerifyDefacedCustomerHelper.props.getProperty("ReadTimeout")));

            httpClientPolicy.setAllowChunking(false);

            httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);

            conduit2.setClient(httpClientPolicy);

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_DEFAULT);

            //Object to JSON in String
            String jsonInString = mapper.writeValueAsString(request);

            log.warn("TEST REQUEST JSON:" + jsonInString);

            Response response = client.post(jsonInString);

            InputStream is = (InputStream) response.getEntity();
            String result = IOUtils.toString(is, "UTF-8");
            log.warn("TEST Response from Defaced Customer [{}].", result);

            ObjectMapper mapper1 = new ObjectMapper();
//            mapper1.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            log.warn("TEST Defaced Before Customer Response result: [{}]", result);
            
            VerifyDefacedCustomerResponse verifyDefacedCustomerResponse = mapper1.readValue(result, VerifyDefacedCustomerResponse.class);
            
//            log.warn("TEST Defaced after Customer Response result: [{}]", verifyDefacedCustomerResponse.getCode()+" "+verifyDefacedCustomerResponse.getId()+" "+verifyDefacedCustomerResponse.getResult().getQuestionCode());

            log.debug("Defaced Customer Response Status: [{}]", verifyDefacedCustomerResponse.getCode());

            if (verifyDefacedCustomerResponse.getCode() != null && verifyDefacedCustomerResponse.getCode().equalsIgnoreCase("120")) { //For Successful
                log.debug("Verify Successful: Defaced Customer: Responce Code [{}]",
                        verifyDefacedCustomerResponse.getCode());
            } else if (verifyDefacedCustomerResponse.getCode() != null && verifyDefacedCustomerResponse.getCode().equalsIgnoreCase("130")) {
                log.error("Verify Defaced Customer Failed, Status: [{}]", verifyDefacedCustomerResponse.getCode());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

            if (verifyDefacedCustomerResponse.getResult() != null && verifyDefacedCustomerResponse.getCode().equalsIgnoreCase("120")) {
                Event event = new Event();
                event.setEventType("SEP");
                event.setEventSubType("VerifyDefacedQueryResponse");
                event.setEventKey(entityId);
                event.setEventData(entityType + "|Response Status:" + verifyDefacedCustomerResponse.getCode() + "|" + request.getNin() + "|" + verifiedBy + "|"
                        + verifyDefacedCustomerResponse.getId() + "|" + sdf.format(new Date()));
                SCAWrapper.getAdminInstance().createEvent(event);
            } else {
                Event event = new Event();
                event.setEventType("SEP");
                event.setEventSubType("VerifyDefacedQueryResponse");
                event.setEventKey(entityId);
                event.setEventData(entityType + "|Response Status:" + verifyDefacedCustomerResponse.getCode() + "|" + request.getNin() + "|" + verifiedBy + "|"
                        + verifyDefacedCustomerResponse.getId() + "|" + sdf.format(new Date()));
                SCAWrapper.getAdminInstance().createEvent(event);
            }

            return verifyDefacedCustomerResponse;
        } catch (java.net.SocketTimeoutException ex) {
            log.error("Error[{}]", ex);

            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("VerifyDefacedQueryResponse");
            event.setEventKey(entityId);
            event.setEventData(entityType + "|" + request.getNin() + "|" + verifiedBy + "|"
                    + "ERROR:" + ex.getMessage());
            SCAWrapper.getAdminInstance().createEvent(event);
            throw new Exception("Connection timeout while trying to reach VerifyDefacedQueryResponse");
        } catch (Exception ex) {
            log.error("Error[{}]", ex);

            Event event = new Event();
            event.setEventType("IM");
            event.setEventSubType("VerifyDefacedQueryResponse");
            event.setEventKey(entityId);
            event.setEventData(entityType + "|" + request.getNin() + "|" + verifiedBy + "|"
                    + "ERROR:" + ex.getMessage());
            SCAWrapper.getAdminInstance().createEvent(event);
            throw ex;
        } finally {
            if (client != null) {
                //client.getResponse().;
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
        VerifyDefacedCustomerHelper.initialise();
        BaseUtils.registerForPropsChanges(this);
        BaseUtils.deregisterForPropsAvailability(this);
    }

    @Override
    public void propsHaveChangedTrigger() {
        VerifyDefacedCustomerHelper.initialise();
    }

    @PostConstruct
    public void startUp() {
        log.warn("VerifyDefacedCustomerHelper starting up.");
        BaseUtils.registerForPropsAvailability(this);

    }

    @PreDestroy
    public void shutDown() {
        log.warn("VerifyDefacedCustomerHelper shutting down.");
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
