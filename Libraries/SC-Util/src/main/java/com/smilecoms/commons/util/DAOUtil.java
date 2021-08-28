/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

import com.smilecoms.model.InternationalCallRate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rajeshkumar
 */
public class DAOUtil {

    private static final String DS = "jdbc/SmileDB";
    private static List<InternationalCallRate> internationalCallRateList = null;
    private static final Logger LOG = LoggerFactory.getLogger(DAOUtil.class);

    private static Connection getConnection(String dsName) throws Exception {
        return JPAUtils.getNonJTAConnection(dsName);
    }

    public static List<InternationalCallRate> getInternationalCallRates() {
        if (internationalCallRateList != null) {
            return internationalCallRateList;
        }
        internationalCallRateList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        String selectQuery = "select COUNTRY, SERVICE, CENTS_PER_UNIT, UNIT_CREDIT_NAME, MiB from international_rates_summary";

        try {
           conn = getConnection(DS);
           ps = conn.prepareStatement(selectQuery);
           ResultSet rs = ps.executeQuery();
            while(rs.next()){
                InternationalCallRate iCallRates = new InternationalCallRate();
                iCallRates.setCountry(rs.getString("COUNTRY"));
                iCallRates.setService(rs.getString("SERVICE"));
                iCallRates.setCentsPerUnit(rs.getBigDecimal("CENTS_PER_UNIT"));
                iCallRates.setUnitCreditName(rs.getString("UNIT_CREDIT_NAME"));
                iCallRates.setMib(rs.getBigDecimal("MiB"));
                internationalCallRateList.add(iCallRates);
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
        return internationalCallRateList;
    }
}
