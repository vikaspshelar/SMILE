/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.selfcare;

import com.currencyfair.onesignal.OneSignal;
import com.currencyfair.onesignal.model.notification.Button;
import com.currencyfair.onesignal.model.notification.CreateNotificationResponse;
import com.currencyfair.onesignal.model.notification.NotificationRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

/**
 *
 * @author rajeshkumar
 */
public class SelfcareService {
    
    private static final String DS = "jdbc/SmileDB";
    private static final Logger LOG = LoggerFactory.getLogger(SelfcareService.class);
    private static String PROXY_HOST = null;
    private static String PROXY_PORT = null;
    private static String ONESIGNAL_URL = null;
    private static String API_KEY = null;
    private static String APP_ID = null;
    
    
    static {
        init();
    }
    private static void init(){
        ONESIGNAL_URL = BaseUtils.getSubProperty("env.app.push.notification", "AddNotificationURL");
        API_KEY = BaseUtils.getSubProperty("env.app.push.notification", "APIAuthKey");
        PROXY_HOST = BaseUtils.getSubProperty("env.app.push.notification", "ProxyHost");
        PROXY_PORT =  BaseUtils.getSubProperty("env.app.push.notification", "ProxyPort");
        APP_ID = BaseUtils.getSubProperty("env.app.push.notification", "AppID");
    }
    
    private Connection getConnection(String dsName) throws Exception {
        return JPAUtils.getNonJTAConnection(dsName);
    }
    
    public boolean AddOrUpdateAppNotification(AppNotificationReq req){
       Connection conn = null;
       PreparedStatement ps = null;
       Statement statement = null;
       String selectQuery = "select * from selfcare_user where CUSTOMER_ID = "+req.getCustomerId();

       boolean isSuccess = false;
       try {
           conn = getConnection(DS);
           ps = conn.prepareStatement(selectQuery);
           ResultSet rs = ps.executeQuery();
           String query = null;
           if(rs.next()){
               LOG.debug("updating selfcare_user record for [{}]",req.getCustomerId());
               query = "UPDATE selfcare_user SET APP_ID = '"+req.getAppId()+"' where CUSTOMER_ID = "+req.getCustomerId();
           } else {
               LOG.debug("inserting selfcare_user record for [{}]",req.getCustomerId());
               query = "INSERT INTO selfcare_user(CUSTOMER_ID,APP_ID,APP_VERSION,APP_OS) VALUES ("+
                    req.getCustomerId()+",'"+req.getAppId()+"','"+req.getAppVersion()+"','"+req.getOs()+"')";
           }
           LOG.debug("AddOrUpdateAppNotification query [{}]",query);
           statement = conn.createStatement();
           int result = statement.executeUpdate(query);
           conn.commit();
           LOG.debug("result of query [{}]",result);
           isSuccess =  result > 0;
       } catch (Exception ex){
           LOG.error("AddOrUpdateAppNotification exception :",ex);
       } finally {
           try {
               if (statement != null){
                   statement.close();
               }
               if (ps != null){
                   ps.close();
               }
               if(conn != null){
                   conn.close();
               }
           } catch(SQLException ex){
               LOG.error("error while closing connetion ",ex);
           }
       }
       return isSuccess;
   }
    
    public String getAppId(Integer customerId){
       String appId = null;
       Connection conn = null;
       PreparedStatement ps = null;
       String selectQuery = "SELECT APP_ID from selfcare_user where CUSTOMER_ID = "+customerId;
       LOG.debug("getAppId sqlQuery to execute [{}]",selectQuery);
       try {
           conn = getConnection(DS);
           ps = conn.prepareStatement(selectQuery);
           ResultSet rs = ps.executeQuery();
           while(rs.next()){
               appId = rs.getString("APP_ID");
           }
       } catch (Exception ex){
           LOG.error("getNotificationMessage exception", ex);
       } finally {
           try{
               if(ps != null){
                   ps.close();
               }
               if(conn != null) {
                   conn.close();
               }
           } catch (SQLException ex){
               
           }
       }
       return appId;
    }
    
   public List<NotificationMessage> getNotificationMessage(int customerId){
       List<NotificationMessage> msgList = new ArrayList<>();
       Connection conn = null;
       PreparedStatement ps = null;
       String selectQuery = "SELECT NOTIFICATION_ID, CUSTOMER_ID, STATUS, MESSAGE, TITLE, LAST_MODIFIED, TYPE, EXTRA_INFO from selfcare_notification_msg where CUSTOMER_ID = "+customerId+" and STATUS != 'DL'";
       LOG.debug("getNotificationMessage sqlQuery to execute [{}]",selectQuery);
       try {
           conn = getConnection(DS);
           ps = conn.prepareStatement(selectQuery);
           ResultSet rs = ps.executeQuery();
           while(rs.next()){
               NotificationMessage message = new NotificationMessage();
               message.setCustomerId(customerId);
               message.setNotificationId(rs.getString("NOTIFICATION_ID"));
               message.setStatus(NotificationMessage.Status.valueOf(rs.getString("STATUS").toString()));
               message.setMessage(rs.getString("MESSAGE"));
               message.setTitle(rs.getString("TITLE"));
               DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
               message.setDate(df.format(rs.getTimestamp("LAST_MODIFIED")));
               message.setType(rs.getString("TYPE"));
               message.setInfo(rs.getString("EXTRA_INFO"));
               msgList.add(message);
           }
       } catch (Exception ex){
           LOG.error("getNotificationMessage exception", ex);
       } finally {
           try{
               if(ps != null){
                   ps.close();
               }
               if(conn != null) {
                   conn.close();
               }
           } catch (SQLException ex){
               
           }
       }
       
       return msgList;
   }
   
   public boolean updateNotificationMessage(NotificationMessage msg){
       boolean isUpdated = false;
       Connection conn = null;
       Statement st = null;
       String updateQuery = "UPDATE selfcare_notification_msg SET STATUS = '"+msg.getStatus()+"' WHERE NOTIFICATION_ID = '"+msg.getNotificationId()+"' and CUSTOMER_ID = '"+msg.getCustomerId()+"'";
       LOG.debug("updateQuery = "+updateQuery);
       try {
           conn = getConnection(DS);
           st = conn.createStatement();
           int result = st.executeUpdate(updateQuery);
           conn.commit();
           LOG.debug("result of query [{}]",result);
           //isSuccess =  result > 0;
           isUpdated = true;
           
       } catch (Exception ex){
           LOG.error("updateNotificationMessage exception for command [{}]  [{}]",updateQuery, ex);
       } finally {
           try{
               if(st != null){
                   st.close();
               }
               if(conn != null) {
                   conn.close();
               }
           } catch (SQLException ex){
               
           }
       }
       return isUpdated;
   }
   
   /**
    * Insert the notification message to database.
    * @param msg
    * @return 
    */
   public boolean insertNotificationMessage(NotificationMessage msg){
       LOG.debug("called insertNotificationMessage");
       boolean isInserted = false;
       Connection conn = null;
       PreparedStatement st = null;
       String updateQuery = "INSERT INTO selfcare_notification_msg (NOTIFICATION_ID, CUSTOMER_ID, STATUS, MESSAGE, TITLE, TYPE, IMAGE, EXTRA_INFO) VALUES (?,?,?,?,?,?,?,?)";
       
       try {
           conn = getConnection(DS);
           st = conn.prepareStatement(updateQuery);
           st.setString(1, msg.getNotificationId());
           st.setInt(2, msg.getCustomerId());
           st.setString(3, msg.getStatus().toString());
           st.setString(4, msg.getMessage());
           st.setString(5, msg.getTitle());
           st.setString(6, msg.getType());
           st.setString(7, msg.getImage());
           st.setString(8, msg.getInfo());
           
           int result = st.executeUpdate();
           conn.commit();
           LOG.debug("result of query [{}]",result);
           isInserted =  result > 0;
       } catch (Exception ex){
           LOG.error("insertNotificationMessage exception", ex);
       } finally {
           try{
               if(st != null){
                   st.close();
               }
               if(conn != null) {
                   conn.close();
               }
           } catch (SQLException ex){
               
           }
       }
       return isInserted;
   }
   
    public String sendPushNotificationOld(int customerId, String messageBody, 
        String messageTitle, String image, String buttons, Map<String, String> data) {
        
        if (!BaseUtils.getBooleanProperty("env.selfcare.send.notifications", false)) {
            LOG.debug("Not sending push notifications because it is disabled");
            return null;
        }
        LOG.debug("inside sendPushNotification");
        String customerAppId = getAppId(customerId);
        if (customerAppId == null || customerAppId.isEmpty()) {
            LOG.debug("selfcare application is not available for customer [{}]", customerId);
            return null;
        }
        List<String> players = new ArrayList<>();
        players.add(customerAppId);
        LOG.debug("players are [{}]", players);
        String appAuthKey = BaseUtils.getSubProperty("env.app.push.notification", "APIAuthKey");
        String appId = BaseUtils.getSubProperty("env.app.push.notification", "AppID");
        LOG.debug("appAuthKey is [{}] and appId is [{}]", appAuthKey, appId);
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setAppId(appId);
        notificationRequest.setIncludePlayerIds(players);
        notificationRequest.setData(data);
        Map<String, String> content = new HashMap<>();
        content.put("en", messageBody);

        if (image != null && !image.isEmpty()) {
            notificationRequest.setBigPicture(image);
        }

        if (buttons != null && !buttons.isEmpty()) {
            //TODO: process buttons string and create button list.
            List<Button> buttonList = new ArrayList<>();
            notificationRequest.setButtons(buttonList);
        }
        Map<String, String> headings = new HashMap<>();
        headings.put("en", messageTitle);

        LOG.debug("content of notification is [{}]", content);
        notificationRequest.setContents(content);
        notificationRequest.setHeadings(headings);
        LOG.debug("sent notification request to onesignal");
        CreateNotificationResponse resp = OneSignal.createNotification(appAuthKey, notificationRequest);
        LOG.debug("notification response is [{}]", resp);
        
        NotificationMessage nm = new NotificationMessage();
        nm.setNotificationId(resp.getId());
        nm.setCustomerId(customerId);
        nm.setMessage(messageBody);
        nm.setTitle(messageTitle);
        nm.setStatus(NotificationMessage.Status.UR);
        if(data != null && !data.isEmpty()) {
            nm.setType(data.get("type"));
            nm.setInfo(data.get("info"));
        }
        nm.setImage(image);
        insertNotificationMessage(nm);
        return resp.getId();
    }
    
    public String sendPushNotification(int customerId, String messageBody, 
        String messageTitle, String image, String buttons, Map<String, String> data) {
        
        if (!BaseUtils.getBooleanProperty("env.selfcare.send.notifications", false)) {
            LOG.debug("Not sending push notifications because it is disabled");
            return null;
        }
        LOG.debug("inside sendPushNotification");
        String customerPlayerId = getAppId(customerId);
        if (customerPlayerId == null || customerPlayerId.isEmpty()) {
            LOG.debug("selfcare application is not available for customer [{}]", customerId);
            return null;
        }

        WebClient onesignalClient = null;
        String notificationId = null;
        try {
            onesignalClient = getClient(ONESIGNAL_URL);
            onesignalClient.type(MediaType.APPLICATION_JSON);
            onesignalClient.header("Authorization", "Basic "+API_KEY);
            onesignalClient.header("Accept", "application/json");
            onesignalClient.header("Content-Type", "application/json");
            StringBuilder body = new StringBuilder();
            body.append("{\"app_id\":\"").append(APP_ID).append("\"");
            body.append(",\"contents\":{\"en\":\"").append(messageBody).append("\"}");
            body.append(",\"headings\":{\"en\":\"").append(messageTitle).append("\"}");
            body.append(",\"include_player_ids\":[\"").append(customerPlayerId).append("\"]");
            if(null != data && !data.isEmpty()){
                Gson gsonObj = new Gson();
                body.append(",\"data\":").append(gsonObj.toJson(data));
            }

            body.append("}");
            LOG.debug("body of the onesignal request is [{}]",body.toString());
            Response resp = onesignalClient.post(body.toString());
            if(resp.getStatus() == 200 && resp.getEntity() != null) {
            	String result = resp.readEntity(String.class);
                LOG.debug("notification response is [{}]", result);
            	JsonParser jsonParser = new JsonParser();
            	JsonObject jsonObj = jsonParser.parse(result).getAsJsonObject();
                notificationId = jsonObj.getAsJsonPrimitive("id").toString();
            	LOG.debug("notificationId = "+notificationId);
            }
            LOG.debug("notification response is [{}]", resp);
        } catch(Exception ex){
            LOG.error("exception in send selfcare notification :",ex);
        } finally {
            if(onesignalClient != null){
                onesignalClient.close();
            }
        }
       
        if(notificationId == null || notificationId.isEmpty()){
            LOG.error("notification not sent to customer [{}]",customerId);
            return null;
        }
        NotificationMessage nm = new NotificationMessage();
        nm.setNotificationId(notificationId);
        nm.setCustomerId(customerId);
        nm.setMessage(messageBody);
        nm.setTitle(messageTitle);
        nm.setStatus(NotificationMessage.Status.UR);
        if(data != null && !data.isEmpty()) {
            nm.setType(data.get("type"));
            nm.setInfo(data.get("info"));
        }
        nm.setImage(image);
        insertNotificationMessage(nm);
        return notificationId;
    }
    
    private static WebClient getClient(String url) {
        WebClient wc = WebClient.create(url);
        HTTPConduit conduit = WebClient.getConfig(wc).getHttpConduit();

        HTTPClientPolicy policy = new HTTPClientPolicy();
        if(PROXY_HOST != null && !PROXY_HOST.isEmpty() && PROXY_PORT != null && !PROXY_PORT.isEmpty()){
            policy.setProxyServer(PROXY_HOST);
            policy.setProxyServerPort(Integer.parseInt(PROXY_PORT));
            LOG.debug("PROXY_HOST :"+PROXY_HOST+" PROXY_PORT : "+PROXY_PORT);
        }        
        policy.setConnectionTimeout(30 * 1000);
        policy.setReceiveTimeout(180 * 1000);
        policy.setAllowChunking(false);
        policy.setConnection(ConnectionType.CLOSE);

        conduit.setClient(policy);

        TLSClientParameters tlsCP = new TLSClientParameters();
        tlsCP.setDisableCNCheck(true);
        conduit.setTlsClientParameters(tlsCP);
        wc.type(MediaType.APPLICATION_JSON);

        return wc;
    }
}
