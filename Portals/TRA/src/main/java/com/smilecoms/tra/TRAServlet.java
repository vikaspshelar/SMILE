/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tra;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class TRAServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(TRAServlet.class);
   
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.debug("In doget [{}]", req.getPathInfo());
        PrintWriter out = resp.getWriter();
        PreparedStatement ps = null;
        Connection conn = null;
        try {
            log.debug("Getting database connection");
            conn = getConnection();
            ps = getPreparedStatement(req, conn);
            
            if(ps == null) {
                out.println(generateErrorHandlingResponse().toString());
                out.flush();
                return;
            }
            
            log.debug("Executing query");
            ResultSet rs = ps.executeQuery();
            log.debug("Marshalling result to JSON");
            String json = marshallResultSetToJSON(rs);
            resp.setContentType("application/json");
            
            out.print(json);
            log.debug("Flushing");
            out.flush();
        } catch (Exception e) {
            log.warn("Error: ", e);
            // resp.sendError(500, e.toString());
            out.println(generateErrorHandlingResponse().toString()); // TRA requested we send this on all errors.
            out.flush();
            return;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }

    }

    private PreparedStatement getPreparedStatement(HttpServletRequest req, Connection conn) throws Exception {
        String path = "";
        String transDate = "";
        String transId = "";
        
        List<String> queries = BaseUtils.getPropertyAsList("env.tra.sql.queries");
        for (String line : queries) {
            
            path = req.getPathInfo();
            
            String[] parts = line.split("=", 2);
            String match = parts[0];
            String query = parts[1];
            
            boolean getTotalsPerDay = false;
            boolean isForReconcilliations = false;
            boolean isForMissingTransactionsStepOne = false;
            boolean isForMissingTransactionsStepTwo = false;
            
            
            String limit =  req.getParameter("limit");
            log.debug("TRA:Liming coming in:" + limit);
            
            if(limit == null || limit.isEmpty()) {
                //Treat as total  per day.
                getTotalsPerDay = true;
            }
            
            if(path.equals("/recon")) {
                isForReconcilliations = true;
                String channelType = req.getParameter("channeltype");
                transDate = req.getParameter("transdate");
                
                if(channelType == null || channelType.isEmpty()) {
                    return null;
                } else {
                    path = path.concat("/" + channelType);
                }
                                
                log.debug("TRA: Going to pull recons/missing transactions for [{}] date [{}]", path, transDate);
            } else 
                if(path.equals("/gettransactions")) { // Missing transactions step 1
                    
                    String channelType = req.getParameter("channeltype");
                    transDate = req.getParameter("transdate");
                    isForMissingTransactionsStepOne = true;
                    
                    if(channelType == null || channelType.isEmpty()) {
                        return null;
                    } 
                    
                   path = path.concat("/" + channelType);
                                     
                   log.debug("TRA: Going to pull recon transactions for [{}] transaction id [{}]", path, transId);                   
            } else 
                if (path.equals("/missingtrans")) { // Missing transactions step 2
                    String channelType = req.getParameter("channeltype");
                    transId = req.getParameter("transid");
                    isForMissingTransactionsStepTwo = true;
                    
                    if(channelType == null || channelType.isEmpty()) {
                        return null;
                    }  
                    
                    path = path.concat("/" + channelType); 
                    
                    log.error("TRA: Going to pull missing transactions for [{}] date [{}], transaction id [{}]", path, transDate, transId);
          }
            
            
            if (path.contains(match)) {
                log.debug("SQL query mapped to the request is [{}]. Now setting parameters", query);
                PreparedStatement ps = conn.prepareStatement(query);
                
                String [] parameters = path.split("/");
                   
                for (int i = 0; i < parameters.length; i++) {
                    log.error("TRA: parameters[" +  i + "] = "  + parameters[i]);
                }
                
                if(isForReconcilliations || isForMissingTransactionsStepOne) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date reconDate = sdf.parse(transDate);
                    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
                    
                    ps.setString(1, sdf2.format(reconDate));
                    ps.setString(2, sdf2.format(reconDate));
                    
                    log.debug("TRA:Passing recon date = " + sdf2.format(reconDate));
                    
                } else
                    if(isForMissingTransactionsStepTwo == true) {
                        ps.setDouble(1, Double.parseDouble(transId));  
                        log.error("TRA:Passing transid date = " + Double.parseDouble(transId));
                } else {
                    // Extract the last two  parameters
                    SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
                    java.util.Date fromDate = sdf.parse(parameters[parameters.length - 2]);
                    String transactionIdToStartFrom = parameters[parameters.length - 1];
                    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
                    //FromDate (MM-yyyy)
                    ps.setString(1, sdf2.format(fromDate));
                    log.debug("TRA:Passing Date FROM = " + sdf2.format(fromDate));

                    // Restrict the search into one month.
                    Calendar cc = Calendar.getInstance();
                    cc.setTime(fromDate);
                    cc.add(Calendar.MONTH, 1);
                    java.util.Date endDate = cc.getTime();

                    ps.setString(2, sdf2.format(endDate));
                    log.debug("TRA:Passing Date TO = " + sdf2.format(endDate));

                    if(!getTotalsPerDay) {
                        //TransactionID
                        ps.setDouble(3, Double.parseDouble(transactionIdToStartFrom));
                        log.debug("TRA:Passing TransactionID = " + Double.parseDouble(transactionIdToStartFrom));
                        ps.setLong(4, Long.parseLong(limit));
                        log.debug("TRA:Passing Limit = " + Long.parseLong(limit));
                    } else { // getTotalsPerDay = true
                        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd");
                        java.util.Date txFromDate = sdf3.parse(transactionIdToStartFrom);

                        //Restrict totals transaction are
                        Calendar cc2 = Calendar.getInstance();
                        cc2.setTime(txFromDate);
                        cc2.add(Calendar.DAY_OF_MONTH, 1);

                        log.debug("TRA:Passing Date = " + sdf2.format(endDate));
                        java.util.Date txEndDate = cc2.getTime();

                        // TRA are requesting totals for a specific date
                        ps.setString(3, sdf3.format(txFromDate));
                        log.debug("TRA:Transaction Start Date = " +  sdf3.format(txFromDate));
                        // Set end date for daily transactions
                        ps.setString(4, sdf3.format(txEndDate));
                        log.debug("TRA:Transaction End Date = " + sdf3.format(txEndDate));
                    }
                }
               /* for (int i = 1; i <= (count - 1); i++) {
                    String param = getRequestPathParamAtPositionAfterString(path, match, i);
                    ps.setString(i, param);
                    log.debug("Set [{}]:[{}]", i, param);
                }*/
               
                return ps;
            }
        }

        // throw new Exception("Unknown transaction type :" + path);
        return null;
    }

    private String marshallResultSetToJSON(ResultSet rs) throws Exception {
        JSONArray jsonArray = convert(rs);
        return jsonArray.toString();
    }

    private Connection getConnection() throws Exception {
        return JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
        // return JPAUtils.getNonJTAConnection("jdbc/SmileDB");
    }

    public JSONArray convert(ResultSet rs) throws SQLException, JSONException {

        JSONArray json = new JSONArray();
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        int count = 0;
        
        while (rs.next()) {
            JSONObject obj = new JSONObject();

            for (int i = 1; i < numColumns + 1; i++) {
                String column_name = rsmd.getColumnLabel(i);

                switch (rsmd.getColumnType(i)) {
                    case java.sql.Types.ARRAY:
                        obj.put(column_name, rs.getArray(i));
                        break;
                    case java.sql.Types.BIGINT:
                        obj.put(column_name, rs.getLong(i));
                        break;
                    case java.sql.Types.BOOLEAN:
                        obj.put(column_name, rs.getBoolean(i));
                        break;
                    case java.sql.Types.BLOB:
                        obj.put(column_name, rs.getBlob(i));
                        break;
                    case java.sql.Types.DOUBLE:
                        obj.put(column_name, rs.getDouble(i));
                        break;
                    case java.sql.Types.FLOAT:
                        obj.put(column_name, rs.getFloat(i));
                        break;
                    case java.sql.Types.INTEGER:
                        obj.put(column_name, rs.getInt(i));
                        break;
                    case java.sql.Types.NVARCHAR:
                        obj.put(column_name, rs.getNString(i));
                        break;
                    case java.sql.Types.VARCHAR:
                        obj.put(column_name, rs.getString(i));
                        break;
                    case java.sql.Types.TINYINT:
                        obj.put(column_name, rs.getInt(i));
                        break;
                    case java.sql.Types.SMALLINT:
                        obj.put(column_name, rs.getInt(i));
                        break;
                    case java.sql.Types.DATE:
                        obj.put(column_name, rs.getDate(i));
                        break;
                    case java.sql.Types.TIMESTAMP:
                        obj.put(column_name, rs.getTimestamp(i));
                        break;
                    default:
                        obj.put(column_name, rs.getObject(i));
                        break;
                }
            }

            json.put(obj);
            count++;
        }

        if(count == 0) {
            return generateErrorHandlingResponse();
        }    
        return json;
    }

    private String getRequestPathParamAtPositionAfterString(String path, String after, int i) {
        String pathAfter = path.substring(path.indexOf(after) + after.length() + 1);
        return pathAfter.split("/")[i - 1];
    }
    
    private JSONArray generateErrorHandlingResponse() {
               JSONArray json = new JSONArray(); 
               JSONObject obj = new JSONObject();
               // As requested by TRA - Return {"status" : "No Data", "message" : "No Data for the requested date."} if no records where found.
               obj.put("status", "No Data");
               obj.put("message", "No Data for the requested date.");
               json.put(obj);
               return json;
    }

}
