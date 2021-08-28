/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.sra.model.UCCTokenResponse;
import com.smilecoms.sra.model.ValidateRefugeeRequest;

import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.IOUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rajeshkumar
 */
public class UCCHelper {

    private static final Logger log = LoggerFactory.getLogger(UCCHelper.class);


    public static final String NIRA_CARD_STATUS_VALID = "Valid";
    public static final String TRANSACTION_STATUS_OK = "Ok";

    //public static Properties props = null;

    private static final String CREATETOKENURL = "/refugees/api/login/";
    private static final String VALIDATEREFUGEEURL = "/refugees/api/validate/";


    /**
     *
     * @param req
     * @return
     * @throws Exception
     */
    public static boolean validateRefugee(ValidateRefugeeRequest req) {
        log.debug("validate refugee request data :" + req);
        String token = getToken();
        if (null == token || token.isEmpty()) {
            log.info("token is null or empty");
            return false;
        }
        log.debug("validateRefugee token is [{}]",token);
        WebClient wc = getClient(VALIDATEREFUGEEURL);
        wc.header("Authorization", "JWT " + token);
        Response response = wc.post(Entity.json(req));

        int httpStatus = response.getStatus();
        if (httpStatus > 202) {
            log.debug("validateRefugee httpStatus = " + httpStatus);
            return false;
        }
        MappingJsonFactory factory = new MappingJsonFactory();
        try {
            JsonParser parser = factory.createJsonParser((InputStream) response.getEntity());
            Map output = parser.readValueAs(HashMap.class);
            log.debug("validateRefugee output :[{}]",output);
            String ResultData = (String) output.get("result");
            log.debug("UCC response is " + ResultData);
            if (ResultData.equalsIgnoreCase("Match")) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            log.error("exception in getToken", ex);
        }finally {
            wc.close();
        }
        return false;
    }

    /**
     *
     * @return
     */
    private static String getToken() {
        String token = null;
        WebClient wc = getClient(CREATETOKENURL);

        Map<String, String> data = new HashMap<>();
        Map<String, String> properties = BaseUtils.getPropertyAsMap("env.ucc.refugee.config");
        log.debug("env.ucc.refugee.config is :"+properties);
        data.put("username", properties.get("UCCUsername"));
        data.put("password", properties.get("UCCPassword"));
        wc.type(MediaType.APPLICATION_JSON);
        log.debug("data sent for getToken:"+data);
        Response response = null;
        try {
            response = wc.post(Entity.json(data));
        } catch (Exception ex){
            log.error("getToken exception",ex);
            return null;
        }
        int httpStatus = response.getStatus();
        if (httpStatus > 202) {
            log.debug("getToken httpStatus = " + httpStatus);
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = (InputStream) response.getEntity();
            String result = IOUtils.toString(is, "UTF-8");
            log.debug("Token response from UCC [{}].", result);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            UCCTokenResponse tokenResp = mapper.readValue(result, UCCTokenResponse.class);
            log.debug("tokenResp = "+tokenResp);
            return tokenResp.getToken();
        } catch (Exception ex) {
            log.error("exception in getToken", ex);
        } finally {
             wc.close();
        }
        return token;
    }

    private static WebClient getClient(String url) {
        Map<String, String> properties = BaseUtils.getPropertyAsMap("env.ucc.refugee.config");
        String uccBaseUrl = properties.get("UCCRestUrl");
        log.debug("getClient uccBaseUrl [{}]",uccBaseUrl);
        WebClient wc = WebClient.create(uccBaseUrl + url);
        HTTPConduit conduit = WebClient.getConfig(wc).getHttpConduit();

        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setProxyServer(properties.get("ProxyHost"));
        policy.setProxyServerPort(Integer.parseInt(properties.get("ProxyPort")));
        int contimeout = properties.get("UCCConnectionTimeout") == null ? 60 : Integer.valueOf(properties.get("UCCConnectionTimeout"));
        int receiveTimeout = properties.get("UCCReceiveTimeout") == null ? 60 : Integer.valueOf(properties.get("UCCReceiveTimeout"));
        policy.setConnectionTimeout(contimeout * 1000);
        policy.setReceiveTimeout(receiveTimeout * 1000);
        policy.setAllowChunking(false);
        policy.setConnection(ConnectionType.KEEP_ALIVE);
        policy.setProxyServerType(ProxyServerType.HTTP);

        conduit.setClient(policy);

        TLSClientParameters tlsCP = new TLSClientParameters();
        tlsCP.setDisableCNCheck(true);
        conduit.setTlsClientParameters(tlsCP);
        wc.type(MediaType.APPLICATION_JSON);
        wc.accept(MediaType.APPLICATION_JSON);

        return wc;
    }
      
    public static String getDataFromDB(String sqlQuery,String imsi)
    {
        log.info("inside getDataFromDB with imsi:"+imsi);
        PreparedStatement ps = null;
        Connection conn = null;
        String data="";
        
        
        log.debug("Query for getting location is [{}]", sqlQuery);
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
                ps.setString(1, "%"+imsi+"%");           
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                data = rs.getString(1);
            }
            log.info("Data from DB is :" + data);
        } catch (Exception ex) {
            log.error("error while getting cell id " + ex.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.error("error while closing ps " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("error while closing conn " + e.getMessage());
                }
            }
        }
        
        return data;
    }
    
    public static String makeCellId(String location)
    {
        String cellId="";
        if(!location.isEmpty() && !location.equals(""))
        {          
            String tai = location.split(":")[0];
            String ecgi = location.split(":")[1];
            tai = tai.substring(5);
            ecgi = ecgi.substring(9);
            cellId = BaseUtils.getSubProperty("env.hcs.client.config", "MCC") + tai + "-" + ecgi;
        }   
        return cellId;
    }

}
