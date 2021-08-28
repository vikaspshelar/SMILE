/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.paymentgateway;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.util.Oauth1Utils;
import com.smilecoms.commons.util.Utils;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public abstract class PaymentGatewayManager {

    protected Logger log = LoggerFactory.getLogger(this.getClass());
    private static final Pattern PARAM_VALUE_PATTERN = Pattern.compile("<[a-zA-Z0-9_]+>");

    public abstract <T extends IPGWTransactionData> T startTransaction(Sale data, String itemDescription) throws Exception;

    public static String getStringRoundedForBank(double value) {
        String format = BaseUtils.getProperty("env.payment.gateway.decimal.format", "#.00");
        DecimalFormat dfDB = new DecimalFormat(format);
        return dfDB.format(getDoubleRoundedForBank(value));
    }

    public static double getDoubleRoundedForBank(double value) {
        double res = Utils.round(value, getBankDecimalPlaces());
        return res;
    }

    private static int getBankDecimalPlaces() {
        String format = BaseUtils.getProperty("env.payment.gateway.decimal.format", "#.00");
        int ret = format.substring(format.indexOf(".") + 1).length();
        return ret;
    }

    public static Customer getCustomer(int custId) {
        return UserSpecificCachedDataHelper.getCustomer(custId, StCustomerLookupVerbosity.CUSTOMER);
    }

    public void replaceParamValuePlaceholder(IPGWTransactionData data) {
        String urlData = data.getGatewayURLData();
        try {
            int indx = urlData.indexOf("<");
            if (indx == -1) {
                return;
            }
            log.debug("Before replacement: [{}]", urlData);
            Matcher m = PARAM_VALUE_PATTERN.matcher(urlData);
            Customer customer = getCustomer(data.getPaymentGatewaySale().getRecipientCustomerId());
            while (m.find()) {
                String parameterValueWithBrackets = m.group();
                log.debug("Property with matching pattern found: [{}]", parameterValueWithBrackets);
                String parameterValuePlaceholder = parameterValueWithBrackets.replace("<", "").replace(">", "");
                log.debug("Parameter value placeholder name is [{}]", parameterValuePlaceholder);
                if (parameterValuePlaceholder.equals("customer_email")) {
                    String parameterValue = customer.getEmailAddress();
                    log.debug("Replacing [{}] with [{}]", parameterValueWithBrackets, parameterValue);
                    urlData = urlData.replace(parameterValueWithBrackets, parameterValue);
                }
                if (parameterValuePlaceholder.equals("customer_fullname")) {
                    String parameterValue = customer.getFirstName() + " " + customer.getLastName();
                    log.debug("Replacing [{}] with [{}]", parameterValueWithBrackets, parameterValue);
                    urlData = urlData.replace(parameterValueWithBrackets, parameterValue);
                }
            }
        } catch (Exception e) {
            log.warn("Error doing parameter value replacement: {}", e);
        }
        data.setGatewayURLData(urlData);
    }

    public String getOauth1SignedURL(String url, String httpMethod, Map<String, String> resourceParams, String callBack, String propName) {
        String signedRequest = "";
        try {
            signedRequest = Oauth1Utils.getAbsoluteUrl(url, httpMethod, resourceParams, callBack, propName);
        } catch (Exception ex) {
            log.warn("Error occured trying to sign request: {}", ex);
        }
        return signedRequest;
    }

}
