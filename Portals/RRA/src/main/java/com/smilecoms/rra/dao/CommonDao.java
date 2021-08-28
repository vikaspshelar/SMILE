/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.dao;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.smilecoms.rra.model.MinorDetail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rajeshkumar
 */
@Component
public class CommonDao {
    
    private static final Logger LOG = LoggerFactory.getLogger(CommonDao.class);
    
    public Collection<MinorDetail> getMinorDetails(){
        Map<String,MinorDetail> minorDetailMap = new HashMap<>();
        String sqlQuery = BaseUtils.getProperty("env.sql.query.minordetail");
	if (sqlQuery == null || "".equals(sqlQuery.trim())) {
	    return null;
	}
        LOG.debug("executing query :" + sqlQuery);
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
	    }
	    // 1.CUSTOMER_PROFILE_ID, 2.DATE_OF_BIRTH, 3.MSISDN
	    ResultSet rs = ps.executeQuery();
	    while (rs.next()) {
	        String customerId = rs.getString(1);
                String dateofbirth = rs.getString(2);
                String mobileNo = rs.getString(3);
		MinorDetail minor = minorDetailMap.get(customerId);
                if(minor == null){
                    minor = new MinorDetail(customerId);
                }
                minor.setDob(dateofbirth);
                minor.getMobileNumbers().add(mobileNo);
                minorDetailMap.put(customerId, minor);
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
        return minorDetailMap.values();
    }
}
