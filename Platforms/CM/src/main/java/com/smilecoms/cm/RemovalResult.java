/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.cm;

/**
 *
 * @author Paul Carter-Brown
 */
public class RemovalResult {

    public int productInstanceId;
    public String smsMsg;
    public String smsFrom;
    public String smsTo;
    public String emailTo;
    public String emailFrom;
    public String emailTemplate;
    public String emailTextContentPram;
    public String emailSubjectParam;
    
    @Override
    public String toString() {
        return "EnrolResult{" + "productInstanceId=" + productInstanceId + ", smsMsg=" + smsMsg + 
                ", smsFrom=" + smsFrom + ", smsTo=" + smsTo 
                + ", emailTo=" + emailTo + ", emailFrom=" + emailFrom + ", emailTemplate" + emailTemplate 
                + "emailTextContentPram=" + emailTextContentPram +  ", emailSubjectParam=" + emailSubjectParam + "}";
    }
    
    
}
