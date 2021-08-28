/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers.ug;

import com.smilecoms.commons.base.BaseUtils;

import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.IMEICheckRequest;
import com.smilecoms.commons.sca.IMEICheckResponse;
import com.smilecoms.commons.sca.IMEIStatusChangeRequest;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.SSH;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
                log.debug("refugee data matched with UCC");
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
    
    public static IMEICheckResponse checkIMEIStatus(IMEICheckRequest req)
    {
        log.info("Inside checkIMEIStatus with imei "+ req.getImei());
        IMEICheckResponse resp = new IMEICheckResponse();
        String status=getIMEIStatusFromDB(req.getImei());
        String temp=status==null?"NA":(status.isEmpty()?"NA":status);
        resp.setStatus(temp);
        return resp;
    }

    public static boolean changeIMEIStatus(IMEIStatusChangeRequest iMEIStatusChangeRequest)
    {
        log.info("Inside changeIMEIStatusInDB() with imei:"+iMEIStatusChangeRequest.getImei()+" & status:"+iMEIStatusChangeRequest.getStatus()
        +" Action:"+iMEIStatusChangeRequest.getAction()+" IMEI:"+iMEIStatusChangeRequest.getEquipmentType());
        String action=iMEIStatusChangeRequest.getAction();
        String type=iMEIStatusChangeRequest.getEquipmentType();
        String sqlQuery="";
        String input="";
        int count=0;
        switch(type)
        {
            case "EQU": sqlQuery=action.equalsIgnoreCase("CRE")?BaseUtils.getSubProperty("env.ceir.config", "EQUBlockQuery"):BaseUtils.getSubProperty("env.ceir.config", "EQUUnblockQuery");
                       break;
            case "CLHU": sqlQuery=action.equalsIgnoreCase("CRE")?BaseUtils.getSubProperty("env.ceir.config", "CLHUBlockQuery"):BaseUtils.getSubProperty("env.ceir.config", "CLHUUnblockQuery");
                       break;
            case "EMST": sqlQuery=action.equalsIgnoreCase("CRE")?BaseUtils.getSubProperty("env.ceir.config", "EMSTBlockQuery"):BaseUtils.getSubProperty("env.ceir.config", "EMSTUnblockQuery");
                       break;
            case "FSIM": sqlQuery=action.equalsIgnoreCase("CRE")?BaseUtils.getSubProperty("env.ceir.config", "FSIMBlockQuery"):BaseUtils.getSubProperty("env.ceir.config", "FSIMUnblockQuery");
                       break;
            default: log.error("Wrong equipment type entered");
        }
        
        count=changeIMEIStatusInDB(sqlQuery,iMEIStatusChangeRequest);
        String host=BaseUtils.getSubProperty("env.ceir.config", "HostName");
        String script=BaseUtils.getSubProperty("env.ceir.config", "ScriptName");
        String property_path=BaseUtils.getSubProperty("env.ceir.config", "PropertyPath");
        String output="";
        if(count==1)
        {
            //makeFile(iMEIStatusChangeRequest);
            input=makeInputData(iMEIStatusChangeRequest);
            
            Event event = new Event();
            event.setEventType("CEIR_TASK");
            event.setEventSubType("RAN");
            event.setEventKey("root");
            String cmd = "/bin/bash " + script +" " +property_path +" \"" + input + "\"";
            event.setEventData(cmd);
            SCAWrapper.getUserSpecificInstance().createEvent(event);
            
            output=(SSH.executeRemoteOSCommand(BaseUtils.getProperty("env.general.task.ssh.user", "root"),
                            BaseUtils.getProperty("env.general.task.ssh.password", "newroot"),
                            host, cmd));
            log.info("\"Result Of \" "+ cmd +" is:"+output);
        }
        
        return output.trim().equalsIgnoreCase("success");
    }
    
    private static String getIMEIStatusFromDB(String imei)
    {
        log.info("inside getIMEIStatusFromDB() with imei:"+imei);
        PreparedStatement ps = null;
        Connection conn = null;
         ResultSet rs = null;
        String sqlQuery=BaseUtils.getSubProperty("env.ceir.config", "GetStatusQuery");
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
                for(int i=1;i<=8;i++) {
                ps.setLong(i,Double.valueOf(imei.substring(0, 14)).longValue()); 
                }
            }

            rs = ps.executeQuery();

            while (rs.next()) {
                data = rs.getString(1);
            }
            log.info("Status from DB is :" + data);
        } catch (Exception ex) {
            log.error("error while getting data in getIMEIStatusFromDB " + ex.getMessage());
        } finally {           
            if(rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.error("error while closing rs in getIMEIStatusFromDB " + e.getMessage());
                }
            }            
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.error("error while closing ps in getIMEIStatusFromDB " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("error while closing conn in getIMEIStatusFromDB " + e.getMessage());
                }
            }
        }        
        return data;
    }
    
    private static int changeIMEIStatusInDB(String sqlQuery,IMEIStatusChangeRequest iMEIStatusChangeRequest)
    {
        log.info("Inside changeIMEIStatusInDB with sql query:"+sqlQuery+" EquipmentType:"+iMEIStatusChangeRequest.getEquipmentType()
                +" Action:"+iMEIStatusChangeRequest.getAction());
        PreparedStatement ps = null;
        Connection conn = null;
        String type=iMEIStatusChangeRequest.getEquipmentType();
        String imei=iMEIStatusChangeRequest.getImei();
        String endimei=iMEIStatusChangeRequest.getImeiend();
        String imsi=iMEIStatusChangeRequest.getImsi();
        int count=0;               

        if (sqlQuery == null || "".equals(sqlQuery.trim())) {
            return count;
        }

        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDB");
            if (conn == null) {
                return count;
            }
            ps = conn.prepareStatement(sqlQuery);
            if (ps == null) {
                return count;
            } else {
                log.info("IMEI is: "+Double.valueOf(imei).longValue());
                ps.setLong(1,type.equalsIgnoreCase("FSIM")?Double.valueOf(imsi).longValue():type.equalsIgnoreCase("EQU")?Double.valueOf(endimei.substring(0, 14)).longValue():Double.valueOf(imei).longValue());
                if(!((type.equalsIgnoreCase("FSIM") || type.equalsIgnoreCase("EMST")) && iMEIStatusChangeRequest.getAction().equalsIgnoreCase("DEL")))
                {
                    ps.setLong(2,type.equalsIgnoreCase("FSIM")?Double.valueOf(imei).longValue():type.equalsIgnoreCase("EQU")?Double.valueOf(endimei.substring(0, 14)).longValue():Double.valueOf(imsi).longValue());
                }
                if(type.equalsIgnoreCase("EQU"))
                {
                    ps.setString(3,iMEIStatusChangeRequest.getStatus());
                    ps.setString(4,iMEIStatusChangeRequest.getComment());
                }       
            }
            count = ps.executeUpdate();
            conn.commit();
            log.debug("Count value is :" + count);
        } catch (Exception ex) {
            log.error("error while inserting/deleting the data in changeIMEIStatusInDB " + ex.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.error("error while closing ps in changeIMEIStatusInDB " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("error while closing conn in changeIMEIStatusInDB " + e.getMessage());
                }
            }
        }        
        return count;
    }
    
    private static String makeInputData(IMEIStatusChangeRequest iMEIStatusChangeRequest)
    {
        log.info("Inside makeInputData(): EquipmentType:"+iMEIStatusChangeRequest.getEquipmentType()+" Action:"+iMEIStatusChangeRequest.getAction());
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfTime = new SimpleDateFormat("hhmmss");
        Date date = new Date();
        String strDate = sdfDate.format(date);
        String strTime = sdfTime.format(date);
        String data="";
        
        try {
            if(iMEIStatusChangeRequest.getEquipmentType().equalsIgnoreCase("EQU"))
            {
                data=iMEIStatusChangeRequest.getAction()+":"+iMEIStatusChangeRequest.getEquipmentType()
                    +","+ iMEIStatusChangeRequest.getImei()+","+iMEIStatusChangeRequest.getImeiend().substring(8,14)
                    +","+ConvertStatus.valueOf(iMEIStatusChangeRequest.getStatus()).getStatus()+","+","+strDate
                    +",##"+iMEIStatusChangeRequest.getComment().replaceAll("\\s+", "_")+"##,"+strTime+";";
            } else if(iMEIStatusChangeRequest.getEquipmentType().equalsIgnoreCase("CLHU"))
            {
                data=iMEIStatusChangeRequest.getAction()+":"+iMEIStatusChangeRequest.getEquipmentType()
                    +","+iMEIStatusChangeRequest.getImsi()+","+iMEIStatusChangeRequest.getImei()+";";
            } else if(iMEIStatusChangeRequest.getEquipmentType().equalsIgnoreCase("EMST"))
            {
                if(iMEIStatusChangeRequest.getAction().equalsIgnoreCase("CRE")) {
                data=iMEIStatusChangeRequest.getAction()+":"+iMEIStatusChangeRequest.getEquipmentType()
                    +","+iMEIStatusChangeRequest.getImei() +","+","+iMEIStatusChangeRequest.getImsi()+";";
                } else {
                    data=iMEIStatusChangeRequest.getAction()+":"+iMEIStatusChangeRequest.getEquipmentType()
                    +","+iMEIStatusChangeRequest.getImei() +";";
                }
            } else if(iMEIStatusChangeRequest.getEquipmentType().equalsIgnoreCase("FSIM"))
            {
                if(iMEIStatusChangeRequest.getAction().equalsIgnoreCase("CRE")) {
                data=iMEIStatusChangeRequest.getAction()+":"+iMEIStatusChangeRequest.getEquipmentType()
                    +","+iMEIStatusChangeRequest.getImsi() +","+iMEIStatusChangeRequest.getImei()+";";
                } else {
                    data=iMEIStatusChangeRequest.getAction()+":"+iMEIStatusChangeRequest.getEquipmentType()
                    +","+iMEIStatusChangeRequest.getImsi() +";";
                }
            }
        } catch (Exception ex) {
            log.error("error in makeInputData() for CEIR" + ex.getMessage());
        }
        
        return data;
    }
    
    enum ConvertStatus
{
    WL("0"), BL("1"), GL("2");
    private String status;

    public String getStatus() {
        return status;
    }
    private ConvertStatus(String status)
    {
        this.status = status;
    }
}
}