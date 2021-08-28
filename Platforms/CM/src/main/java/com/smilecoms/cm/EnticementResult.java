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
public class EnticementResult {

    public int productInstanceId;
    public String enticementResultString;
    public String smsMsg;
    public String smsFrom;
    public String smsTo;
    public String triggerKey;
    public String emailTo;
    public String emailFrom;
    public String emailTemplate;
    public String emailTextContentPram;
    public String emailSubjectParam;
    
    @Override
    public String toString() {
        return "EnticementResult{" + "productInstanceId=" + productInstanceId + ", enticementResultString=" + enticementResultString 
                + ", smsMsg=" + smsMsg + ", smsFrom=" + smsFrom + ", smsTo=" + smsTo + ", triggerKey=" + triggerKey + ", emailTo=" 
                + emailTo + ", emailFrom=" + emailFrom + ", emailTemplate=" + emailTemplate 
                + "emailTextContentPram=" + emailTextContentPram + ",emailSubjectParam=" + emailSubjectParam + '}';
    }

}
