/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.cm;

import java.util.List;

/**
 *
 * @author Paul Carter-Brown
 */
public class TriggerResult {

    public int productInstanceId;
    public String triggerResultString;
    public String smsMsg;
    public String smsFrom;
    public String smsTo;
    public String emailTo;
    public String emailFrom;
    public String emailTemplate;
    public int ucSpecIdToProvision;
    public List<Integer>  ucSpecIdsToProvision;
    public String enticementKey;
    public String emailTextContentPram;
    public String emailSubjectParam;
    
    @Override
    public String toString() {
        return "TriggerResult{" + "productInstanceId=" + productInstanceId + ", triggerResultString=" + triggerResultString 
                + ", smsMsg=" + smsMsg + ", smsFrom=" + smsFrom + ", smsTo=" + smsTo + ", emailTo=" + emailTo 
                + ", emailFrom=" + emailFrom + ", emailTemplate=" + emailTemplate + ", ucSpecIdToProvision=" + ucSpecIdToProvision 
                + ", ucSpecIdsToProvision=" + ucSpecIdsToProvision + ", enticementKey=" + enticementKey 
                + ",emailTextContentPram=" + emailTextContentPram +  ",emailSubjectParam=" + emailSubjectParam + '}';
    }

    


}
