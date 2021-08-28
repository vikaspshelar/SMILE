/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.action;

import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.stripes.SmileActionBean;
import java.io.StringReader;
import java.util.Random;
import javax.servlet.http.HttpSession;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;


public class OTPActionBean extends SmileActionBean {         
    
    @DefaultHandler 
    public Resolution submitOTP() {
        HttpSession session = getContext().getRequest().getSession();          
        
        
        if(!verifiedOTP(session.getAttribute("systemOtp").toString(), getOtp())) {
                session.setAttribute("otpConfirmed","false");
                setConfirmed(false);
                String msg = "<p style='color:red; font-weight:900; font-size:12px'>Invalid OneTimePin Supplied</p>";
                return new StreamingResolution("text", new StringReader(msg));  
            
        } else {
            session.removeAttribute("systemOtp"); 
            session.setAttribute("otpConfirmed","true");
            setConfirmed(true);           
            return new StreamingResolution("text", new StringReader("confirmed"));  
        }
        
    }
    
    
    public Resolution requestAnotherOtp() {    
    //Set session otp
    HttpSession session = getContext().getRequest().getSession();          
    
        if(session.getAttribute("sendOtpTo").toString().length()>0) {
            session.removeAttribute("systemOtp");  
            session.setAttribute("otpConfirmed","false");
            generateOtp();               

            String msg = "<span style='font-weight:900; font-size:12px'>New OneTimePin Sent to ***" + session.getAttribute("sendOtpTo").toString().substring(session.getAttribute("sendOtpTo").toString().length()-4)  + " </span>";
            return new StreamingResolution("text", new StringReader(msg));
        } else {
            setConfirmed(false);
            String msg = "<span style='font-weight:900' color='red'>Something went wrong. Please restart transaction from beginning...</span>";
            return new StreamingResolution("text", new StringReader(msg));
        }
    }
}
