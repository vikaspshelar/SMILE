/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.dao;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.rra.model.RegistrationImage;
import com.smilecoms.rra.model.RegistrationImageResponse;
import com.smilecoms.rra.model.RegistrationImages;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author abhilash
 */

@Component
public class TCRADao {
    
    private static final Logger log = LoggerFactory.getLogger(TCRADao.class);
     
    public RegistrationImageResponse getImages(String custId, String iccid, String msisdn)
    {
        List<RegistrationImage> pics = new ArrayList<>();
        RegistrationImageResponse registrationImageResponse = new RegistrationImageResponse();
        PreparedStatement ps = null;
        Connection conn = null;
        ResultSet rs = null;
        
        try {
                conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
                String query=BaseUtils.getProperty("env.csis.image.customer.query", "");

                ps = conn.prepareStatement(query);
                ps.setString(1, custId);
                rs = ps.executeQuery();

                while (rs.next()) 
                {
                    RegistrationImage RI = new RegistrationImage();

                    RI.setImageType(rs.getString(1));

                    Blob blob = rs.getBlob(2);
                    byte[] bdata = blob.getBytes(1, (int) blob.length());
                    String str = Base64.getEncoder().encodeToString(bdata);
                    RI.setImageContent(str);               
                    RI.setImageFormat("JPEG");             
                    pics.add(RI);
                }

                RegistrationImages images = new RegistrationImages();
                images.setRegistrationImage(pics);

                registrationImageResponse.setIccid(iccid);
                registrationImageResponse.setMsisdn(msisdn);
                registrationImageResponse.setRegistrationImages(images);
            
            } catch (Exception ex) {
                log.error("Error occured during getImages():"+ex);
            }
            finally
            {
                if (rs != null) 
                {
                    try {
                            rs.close();
                    } catch (SQLException ex) {
                            log.error("Error closing the Result Set " + ex);
                    }
                }
                if (ps != null) 
                {
                    try {
                            ps.close();
                    } catch (SQLException ex) {
                            log.error("Error closing the prepared statement " + ex);
                    }
                }
                if (conn != null) 
                {
                    try {
                            conn.close();
                    } catch (SQLException ex) {
                            log.error("Error closing the connection " + ex);
                    }
                }
            }

       return registrationImageResponse;
    }
    
    public String getCustomerId(String id,String code)
    {
        String custID="";
        PreparedStatement ps = null;
        Connection conn = null;
        ResultSet rs = null;
        try {
                conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
                if(code.equalsIgnoreCase("MSISDN"))
                {
                    String query=BaseUtils.getProperty("env.csis.msisdn.customer.query", "");
                    ps = conn.prepareStatement(query);
                    ps.setString(1,"%"+id);
                }
                else
                {
                    String query=BaseUtils.getProperty("env.csis.iccid.customer.query", "");
                    ps = conn.prepareStatement(query);
                    ps.setString(1,id);
                }

                rs = ps.executeQuery();
                while (rs.next()) {
                    custID=rs.getString(1);
                }
            } catch (Exception ex) {
                log.error("Error occured during getCustomerId():"+ex);
            }
            finally
                {
                    if (rs != null) 
                    {
                        try {
                                rs.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the Result Set " + ex);
                        }
                    }
                    if (ps != null) 
                    {
                        try {
                                ps.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the prepared statement " + ex);
                        }
                    }
                    if (conn != null) 
                    {
                        try {
                                conn.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the connection " + ex);
                        }
                    }
                }
        
        return custID.trim();
    }
        
}
