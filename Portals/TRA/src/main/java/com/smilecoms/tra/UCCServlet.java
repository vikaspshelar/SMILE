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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author abhilash
 * 
 */
public class UCCServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(UCCServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        log.warn("Inside doget");
        
        PrintWriter out = resp.getWriter();
        PreparedStatement ps = null;
        Connection conn = null;
        
        try {
            log.warn("Getting database connection");
            conn = getConnection();
            log.debug("Getting customer by nin");
            ps = getPreparedStatement(req, conn, "/getbynin");
            
            if (ps == null) {
                out.println(generateErrorHandlingResponse().toString());
                out.flush();
                return;
            }

            log.debug("Executing query to get basic details:");
            ResultSet rs = ps.executeQuery();
            log.debug("Marshalling result to JSON - Basic Details");
            //JSONObject obj = convertRowToJSONObject(rs);
            JSONObject obj = marshallResultSetToJSON(rs);
            ps = getPreparedStatement(req, conn, "/getcustomer");

            if (ps == null) {
                out.println(generateErrorHandlingResponse().toString());
                out.flush();
                return;
            }

            log.debug("Executing query to get MSISDN details");
            rs = ps.executeQuery();
            log.debug("Marshalling result to JSON - MSISDN details");

            resp.setContentType("application/json");
            JSONArray marshallResultSetToJSONArray = marshallResultSetToJSONArray(rs);
            
            if (marshallResultSetToJSONArray.toString().contains("status")) {
                out.print(marshallResultSetToJSONArray.toString());
            } else {
                obj.put("SIM", marshallResultSetToJSONArray);
                out.print(obj.toString());
            }
            
            log.debug("Flushing");
            out.flush();
            
        } catch (Exception e) {
            log.warn("Error: ", e);
            out.println(generateErrorHandlingResponse().toString());
            out.flush();
            return;
            
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
                log.warn("Exception while closing the statement: ", ex);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                log.warn("Exception while closing the connection: ", ex);
            }
        }
    }

    private PreparedStatement getPreparedStatement(HttpServletRequest req, Connection conn, String path) throws Exception {

        log.warn("Path is [{}]", path);
    
        List<String> queries = BaseUtils.getPropertyAsList("env.ucc.sql.queries");
        for (String line : queries) {

            String[] parts = line.split("=", 2);
            String resourcePath = parts[0];
            String sqlQuery = parts[1];

            if (path.contains(resourcePath)) {
                log.debug("SQL query mapped to the request for path [{}] is [{}]."
                        + " Now setting parameters", path, sqlQuery);
                PreparedStatement ps = conn.prepareStatement(sqlQuery);

                if (resourcePath.equals("/getbynin") || resourcePath.equals("/getcustomer")) {
                    String idNumberType = req.getParameter("nin");
                    log.debug("id is :"+idNumberType);
                    if (idNumberType == null || idNumberType.isEmpty()) {
                        return null;
                    }
                    ps.setString(1, idNumberType);
                } 
                return ps;
            }
        }
        return null;
    }

    private Connection getConnection() throws Exception {
        return JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
    }
    
    private JSONObject marshallResultSetToJSON(ResultSet rs) throws Exception {
        JSONArray jsonArray = convert(rs);
        return (JSONObject) jsonArray.get(0);
    }

    private JSONArray marshallResultSetToJSONArray(ResultSet rs) throws Exception {
        JSONArray jsonArray = convert(rs);
        return jsonArray;
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

        if (count == 0) {
            return generateErrorHandlingResponse();
        }
        return json;
    }

    private JSONArray generateErrorHandlingResponse() {
        JSONArray json = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put("status", "No Data");
        obj.put("message", "No Data for the request");
        json.put(obj);
        return json;
    }

}
