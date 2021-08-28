/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.util.Utils;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author rajeshkumar
 */
public class SRAUtil {
    
     private static final Logger log = LoggerFactory.getLogger(SRAUtil.class);
     
    public static String getAuthType(String string) {
        if (isEmail(string)) {
            return "EMAIL";
        }
        try {
            string = Utils.getCleanDestination(string);
        } catch (Exception ex) {
        }

        if (isNumeric(string)) {
            return "PHONE";
        }
        return "";
    }
    public static Pattern emailPattern = Pattern.compile(BaseUtils.getProperty("env.scp.email.validation.regex",
            "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\"
            + "x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")"
            + "@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"));
    private static final Pattern phonePattern = Pattern.compile("([0-9]*)");

    public static boolean isEmail(final String string) {
        Matcher m = emailPattern.matcher(string);
        return m.matches();
    }

    public static boolean isNumeric(final String string) {
        Matcher m = phonePattern.matcher(string);
        return m.matches();
    }
    
    public static String getMesssage(String messageKey){
        Locale local = LocalisationHelper.getDefaultLocale();
        return LocalisationHelper.getLocalisedString(local, messageKey);
    }
    
    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }
    
    public static String getSmileVoiceNumber(long accountNo){
        log.debug("checking getSmileVoiceNumber for account "+accountNo);
        AccountQuery aq = new AccountQuery();
        String smileNumber = null;
        aq.setAccountId(accountNo);
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        Account acc = SCAWrapper.getAdminInstance().getAccount(aq);
        log.debug("account service instances count is "+acc.getServiceInstances().size());
        for (ServiceInstance si : acc.getServiceInstances()) {
            if(100 != si.getServiceSpecificationId()){
                continue;
            }
            smileNumber =  UserSpecificCachedDataHelper.getServiceInstancePhoneNumber(si.getServiceInstanceId());
        }
        log.debug("smile number for account "+accountNo+" is "+smileNumber);
        return smileNumber;
    }
    
    public static String getCurrentDateTime(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();  
        return dtf.format(now);
    }
    
    public static int generateOTP(){
        Random r = new Random( System.currentTimeMillis() ); 
        int randomNo = ((1 + r.nextInt(2)) * 100000 + r.nextInt(100000));
        return randomNo;
    }
}
