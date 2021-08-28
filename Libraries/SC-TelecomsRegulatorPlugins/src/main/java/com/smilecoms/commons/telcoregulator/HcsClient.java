/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.telcoregulator;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bhaskarhg
 */
public class HcsClient {

    private static final Logger log = LoggerFactory.getLogger(HcsClient.class.getName());
    private static List<String> cellId = null;
    private Random random = new Random();

    public String submitHcsRequest(String eventData) {
        log.info("Entering method submitHcsRequest with eventData [{}]", eventData);
        HttpGet request1 = null;
        CloseableHttpClient httpclient;
        String data[] = eventData.split("#");

        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("hhmmss.sss");
        String formattedDate = dateFormat.format(date);

        String ceirurl = BaseUtils.getSubProperty("env.hcs.client.config", "CEIRServerURL");
        String ceirport = BaseUtils.getSubProperty("env.hcs.client.config", "CEIRServerPort");
        String ceirversion = BaseUtils.getSubProperty("env.hcs.client.config", "CEIRServerVersion");
        String imei = data[0];
        String imsi = data[1];
        String msisdn = data[2];
        String status = data[3];
        String equipmentStatus = "";

        Set<String> imsiExclude = BaseUtils.getPropertyAsSet("env.hcs.imsi.ecluded");
        if (imsiExclude.contains(imsi)) {
            return "";
        }
        if (status.equalsIgnoreCase("W")) {
            log.debug("Checking for White or Grey Listing");
            String dbout = getCeirData(imei,imsi);            
            equipmentStatus = dbout.equalsIgnoreCase("GL") ? "G" : "W";
        } else {
            equipmentStatus = "B";
        }

        boolean isMsisdnAvilable = true;
        if (imsi.equalsIgnoreCase(msisdn)) {
            isMsisdnAvilable = false;
        }
        String url = "http://" + ceirurl
                + ":"
                + ceirport
                + "/"
                + "EQUEV?"
                + "VRSN=" + ceirversion
                + "&IMEI=" + imei
                //                + "&SVN=null"
                + "&IMSI=" + imsi;
        if (isMsisdnAvilable) {
            url = url + "&MTON=" + extractMSISDN_TON()
                    + "&MNPI=" + extractMSISDN_NPI()
                    + "&MSDN=" + msisdn;
        }
        url = url + "&ESTS=" + equipmentStatus
                + "&RMR"
                + "&TIME=" + formattedDate.replace("\\.", "%2E")
                + "&CELL=" + makeCellId(getLatestLocation(imsi))
                + "&RMME=SM-UGKLAMME01";

        log.info("Request URL: " + url);

        String result = null;
        HttpHost host = null;
        boolean Proxy = false;
        if (BaseUtils.getSubProperty("env.hcs.client.config", "proxy").equalsIgnoreCase("true")) {
            Proxy = true;
        }

        if (Proxy) {
            String proxyHost = BaseUtils.getSubProperty("env.hcs.client.config", "proxyip");
            int proxyPort = Integer.parseInt(BaseUtils.getSubProperty("env.hcs.client.config", "proxyport"));
            if (!proxyHost.isEmpty() && proxyPort > 0) {
                log.debug("Post will use a proxy server [{}][{}]", proxyHost, proxyPort);
                host = new HttpHost(proxyHost, proxyPort);
            }
        }

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.hcs.client.config", "SocketTimeOut")))
                .setConnectTimeout(Integer.parseInt(BaseUtils.getSubProperty("env.hcs.client.config", "ConTimeOut")))
                .setProxy(host)
                .build();

        try {
            URI uri = new URI(url);
            httpclient = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
            request1 = new HttpGet(uri);
            log.info("Started sending request to CEIR");
            CloseableHttpResponse response1 = httpclient.execute(request1);
            log.info("Completed sending request to CEIR Response status: code [{}]", response1.getStatusLine());
            if (response1.getEntity() != null) {
                result = Utils.parseStreamToString(response1.getEntity().getContent(), "utf-8");
            }

            Event event = new Event();
            event.setEventType("HSS");
            event.setEventSubType("CEIR_RESPONSE");
            event.setEventKey(imei);
            event.setEventData("Request: "+url+"\nResponse: "+response1.getStatusLine().getStatusCode());
            //this ensures event is created async
            event.setSCAContext(new SCAContext());
            event.getSCAContext().setAsync(Boolean.TRUE);
            SCAWrapper.getAdminInstance().createEvent(event);
        } catch (Exception e) {
            log.error("Error in sending request to CEIR [{}]", e);
        } finally {
            try {
                request1.releaseConnection();
            } catch (Exception e) {
                log.error("Error in closing request to CEIR [{}]", e);
            }
        }
        return result;
    }

    public String getCeirData(String imei, String imsi) {
        log.info("inside getCeirData with imei:"+imei+" & imsi:"+imsi);
        PreparedStatement ps = null;
        Connection conn = null;
        String result = "";

        String sqlQuery = BaseUtils.getProperty("env.ceir.gray.check", "");
        log.debug("Query for grey check is [{}]", sqlQuery);
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
                addParams(ps, imei, imsi);            
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                result = rs.getString(1);
            }
            log.info("Listing Result is :" + result);
        } catch (Exception ex) {
            log.error("error while gettingData " + ex.getMessage());
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
        return result;
    }

    private void addParams(PreparedStatement ps, String imei, String imsi) throws SQLException {
        ps.setString(1, imei);
        ps.setString(2, imsi);
        ps.setString(3, imei);
        ps.setString(4, imsi);
        ps.setString(5, imei);
        ps.setString(6, imsi);
        ps.setString(7, imei);
        ps.setString(8, imei);
        ps.setString(9, imei);
        ps.setString(10, imei);
    }

    private String getCellId() {
        //641330003:641330000100
        //641-33-0001-100
       
        String defaultCell = BaseUtils.getSubProperty("env.hcs.client.config", "CELL");
        try {
            Set<String> cellIds = BaseUtils.getPropertyFromSQLAsSet("env.ceir.sectors");
            List<String> cellList = new ArrayList<>(cellIds);
            int index = random.nextInt(cellIds.size());
            String cell = cellList.get(index);
            String tai = cell.split(":")[0];
            String ecgi = cell.split(":")[1];
            tai = tai.substring(5);
            ecgi = ecgi.substring(9);
            defaultCell = BaseUtils.getSubProperty("env.hcs.client.config", "MCC") + tai + "-" + ecgi;
            if (!Utils.isNumeric(ecgi) || !Utils.isNumeric(tai)) {
                defaultCell = BaseUtils.getSubProperty("env.hcs.client.config", "CELL");
            }
        } catch (Exception e) {
            log.debug("Error in retriving cellIds [{}]", e.toString());
        }
        return defaultCell;
    }
    
    private String makeCellId(String cell) {
        //641330003:641330000100
        //641-33-0003-100
       
        String defaultCell = BaseUtils.getSubProperty("env.hcs.client.config", "CELL");
        try {
                if(!cell.isEmpty() && !cell.equals(""))
                {          
                    String tai = cell.split(":")[0];
                    String ecgi = cell.split(":")[1];
                    tai = tai.substring(5);
                    ecgi = ecgi.substring(9);
                    defaultCell = BaseUtils.getSubProperty("env.hcs.client.config", "MCC") + tai + "-" + ecgi;
//                    if (!Utils.isNumeric(ecgi) || !Utils.isNumeric(tai)) {
//                        defaultCell = BaseUtils.getSubProperty("env.hcs.client.config", "CELL");
//                    }
                }            
        } catch (Exception e) {
            log.debug("Error in making cellId [{}]", e.toString());
        }
        return defaultCell;
    }

    private String extractMSISDN_NPI() {

        return BaseUtils.getSubProperty("env.hcs.client.config", "MNPI");
    }

    private String extractMSISDN_TON() {

        return BaseUtils.getSubProperty("env.hcs.client.config", "MTON");
    }
    
    private String getLatestLocation(String imsi)
    {
        log.info("inside getLatestLocation with imsi:"+imsi);
        PreparedStatement ps = null;
        Connection conn = null;
        String cellId="";
        
        String sqlQuery = BaseUtils.getProperty("env.ceir.latest.location.query", "");
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
                cellId = rs.getString(1);
            }
            log.info("Cell Id is :" + cellId);
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
        
        return cellId;
    }
}
