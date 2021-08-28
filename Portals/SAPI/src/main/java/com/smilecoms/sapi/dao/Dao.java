/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.dao;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.sapi.model.CustomerDetails;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author bhaskarhg
 */
@Component
public class Dao {
    
    private static final Logger LOG = LoggerFactory.getLogger(Dao.class);
    
    public Collection<CustomerDetails> getVisaExpiryCustomers(int days) {
        Map<String, CustomerDetails> customerDetailsMap = new HashMap<>();
        String sqlQuery = BaseUtils.getProperty("env.sql.query.customer.visa.details");
        if (sqlQuery == null || "".equals(sqlQuery.trim())) {
            return null;
        }
        LOG.info("executing query :" + sqlQuery);
        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            if (conn == null) {
                return null;
            }
            ps = conn.prepareStatement(sqlQuery);

            if (ps == null) {
                return null;
            }else {
            ps.setInt(1, days);
            }
           
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String customerId = rs.getString(1);
                String kycStatus = rs.getString(2);
                String productStatus = rs.getString(3);
                String expiryDate = rs.getString(4);
                CustomerDetails customer = customerDetailsMap.get(customerId);
                if (customer == null) {
                    customer = new CustomerDetails(customerId);
                }
                customer.setKycStatus(kycStatus);
                customer.setProductStatus(productStatus);
                customer.setExpiryDate(expiryDate);
                customerDetailsMap.put(customerId, customer);
            }

        } catch (Exception ex) {
            LOG.error("error while getUsersData " + ex.getMessage());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return customerDetailsMap.values();
    }   
}
