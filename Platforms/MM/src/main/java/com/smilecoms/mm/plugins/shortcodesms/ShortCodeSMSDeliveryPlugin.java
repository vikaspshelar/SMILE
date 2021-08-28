/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.shortcodesms;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditInstance;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.beans.CampaignBean;
import com.smilecoms.commons.sca.beans.CustomerBean;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.ProductBean;
import com.smilecoms.commons.sca.beans.ServiceBean;
import com.smilecoms.commons.util.Javassist;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.engine.BaseMessage;
import com.smilecoms.mm.engine.DeliveryEngine;
import com.smilecoms.mm.engine.DeliveryPipelinePlugin;
import com.smilecoms.mm.engine.DeliveryPluginResult;
import com.smilecoms.mm.engine.FinalDeliveryPluginResult;
import com.smilecoms.mm.utils.SMSCodec;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import javax.persistence.EntityManagerFactory;
import org.slf4j.*;

/**
 *
 * @author JBP
 */
public class ShortCodeSMSDeliveryPlugin implements DeliveryPipelinePlugin {

    private static final Logger log = LoggerFactory.getLogger(ShortCodeSMSDeliveryPlugin.class);
    private static EntityManagerFactory emf;

    @Override
    public DeliveryPluginResult processMessage(BaseMessage msg, DeliveryEngine callbackEngine) {

        ShortCodeSMSMessage scMsg = (ShortCodeSMSMessage) msg;
        String shortCode = Utils.getFriendlyPhoneNumber(scMsg.getTo()); // e.g. sip:+1111@ss.sss becomes 1111
        log.debug("Processing message to shortcode [{}]", shortCode);
        String code = BaseUtils.getProperty("env.mm.shortcode." + shortCode + ".code", "");
        boolean mustCharge = false;
        try {
            SCAWrapper.setThreadsRequestContextAsAdmin();
            if (!code.isEmpty()) {
                mustCharge = (Boolean) Javassist.runCode(
                        new Class[]{this.getClass(),
                            org.slf4j.Logger.class,
                            com.smilecoms.mm.plugins.shortcodesms.ShortCodeSMSMessage.class,
                            com.smilecoms.commons.util.Utils.class,
                            com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper.class},
                        code,
                        scMsg.getFrom(),
                        scMsg.getTo(),
                        SMSCodec.decode(scMsg.getMessage(), scMsg.getCodingScheme()),
                        new ShortCodeActionHelper(),
                        log);
            } else {
                log.debug("No shortcode [{}]", code);
            }
        } catch (Throwable e) {
            log.warn("InvocationTargetException processing shortmessage javassist", Utils.getDeepestCause(e));
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "MM", "An error occured processing shortcode code: " + Utils.getDeepestCause(e).toString() + " Stack Trace: " + Utils.getStackTrace(e));
        } finally {
            SCAWrapper.removeThreadsRequestContext();
        }

        FinalDeliveryPluginResult res = new FinalDeliveryPluginResult();
        res.setMustCharge(mustCharge);
        res.setMustRetry(false);
        res.setPluginClassName(ShortCodeSMSDeliveryPlugin.class.getName());

        return res;
    }

    @Override
    public void initialise(EntityManagerFactory emf) {
        if (emf != null) {
            ShortCodeSMSDeliveryPlugin.emf = emf;
        }
    }

    protected static EntityManagerFactory getEMF() {
        return emf;
    }

    @Override
    public void shutDown() {
    }

    @Override
    public void propertiesChanged() {
    }

    public boolean processShortCodeSMS(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {
        int onOff;
        msg = msg.toLowerCase().trim();
        if (msg.startsWith("on") || msg.startsWith("send")) {
            onOff = 1;
        } else {
            onOff = 0;
        }
        helper.setSIpURIUsersOptInLevelBit(from, 2, onOff);

        return true;
    }

    

    public boolean genericSMSCampaign(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {
        log.warn("genericSMSCampaign from: " + from + " to: " + to + " msg: " + msg);

        if (msg.toLowerCase().startsWith("y")) {
            try {
                CustomerBean cust = CustomerBean.getCustomerByPhoneNumber(from, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
                for (int i = 0; i < cust.getProducts().size(); i++) {
                    ProductBean pi = (ProductBean) cust.getProducts().get(i);
                    ServiceBean s = (ServiceBean) ((java.util.List) pi.getServices()).get(0);
                    com.smilecoms.commons.sca.PurchaseUnitCreditRequest pucr = new com.smilecoms.commons.sca.PurchaseUnitCreditRequest();
                    pucr.setProductInstanceId(Integer.valueOf(pi.getProductInstanceId()));
                    pucr.setAccountId(s.getAccountId());
                    pucr.setUnitCreditSpecificationId(267);
                    pucr.setDaysGapBetweenStart(-1);
                    pucr.setNumberToPurchase(1);
                    pucr.setUniqueId("WkEndSpecial2_" + pi.getProductInstanceId());
                    com.smilecoms.commons.sca.SCAWrapper.getAdminInstance().purchaseUnitCredit(pucr);
                    helper.sendSMS(to, from, "InternetFreedom is now yours for FREE from Smile until Monday 6am on acc. " + s.getAccountId() + " Have unlimited fun!");
                }
            } catch (Exception e) {
                log.warn("Error", e);
                helper.sendSMS(to, from, "We do not recognise your phone number: " + Utils.getFriendlyPhoneNumberKeepingCountryCode(from) + ". Apologies for the invonvenience but you cannot participate in this offer");
            }
            return true;
        }

        try {
            msg = msg.toLowerCase().trim();
            int piId = (int) Utils.getFirstNumericPartOfString(msg);
            CampaignBean cb = new CampaignBean("110", piId, Utils.getDateAsString(new Date(), "yyyyMMddHHmm"));
            cb.processSMSOptIn(msg);
            helper.sendSMS(to, from, "Your SMS has been received and is being processed. The Smile Team.");
        } catch (NumberFormatException nfe) {
            helper.sendSMS(to, from, "Invalid SMS text. Please enter the exact text as per the SMS you received.");
        } catch (Exception e) {
            log.warn("Error in genericSMSCampaign: ", e);
        }
        return true;
    }

    public boolean accountBot(String from, String to, String msg, com.smilecoms.mm.plugins.shortcodesms.ShortCodeActionHelper helper, org.slf4j.Logger log) {

        msg = msg.toLowerCase().trim();
        if (msg.startsWith("on")) {
            helper.sendSMS(to, from, "Post call notifications are now on. Reply with 'off' to turn off");
            helper.setSIpURIUsersOptInLevelBit(from, 2, 1);
            return true;
        } else if (msg.startsWith("off")) {
            helper.sendSMS(to, from, "Post call notifications are now off, Reply with 'on' to turn on");
            helper.setSIpURIUsersOptInLevelBit(from, 2, 0);
            return true;
        }

        Account acc = helper.getAccount(from);
        StringBuilder txt = new StringBuilder();
        txt.append("Available balance on account ");
        txt.append(acc.getAccountId());
        txt.append(" is ");
        txt.append(Utils.convertCentsToCurrencyLong(acc.getAvailableBalanceInCents()));
        txt.append(" and ");
        double bundleBalance = 0.00d;
        boolean hasUnlimitedBundle = false;
        for (int i = 0; i < acc.getUnitCreditInstances().size(); i = i + 1) {
            UnitCreditInstance uci = (UnitCreditInstance) acc.getUnitCreditInstances().get(i);
            UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId().intValue());
            if (!Utils.getJavaDate(uci.getStartDate()).before(new java.util.Date()) || !ucs.getUnitType().equalsIgnoreCase("byte")) {
                continue;
            }
            String displayBal = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "DisplayBalance");
            if ((displayBal == null || displayBal.equals("true"))
                    && uci.getAvailableUnitsRemaining().doubleValue() > 0.0d) {
                bundleBalance = bundleBalance + uci.getAvailableUnitsRemaining().doubleValue();
            }
            if (ucs.getWrapperClass().equals("DustUnitCredit") || ucs.getWrapperClass().equals("CorporateAccessUnitCredit") || ucs.getWrapperClass().contains("Unlimited")) {
                hasUnlimitedBundle = true;
            }
        }
        txt.append(com.smilecoms.commons.util.Utils.displayVolumeAsString(bundleBalance, "byte"));
        if (hasUnlimitedBundle) {
            txt.append(" (+ SmileUnlimited)");
        }

        helper.sendSMS(to, from, txt.toString());
        return true;
    }

    @Override
    public void sendDeliveryReport(DeliveryEngine.DeliveryReportStatus reportStatus, HashMap<String, Serializable> deliveryReportData) {
        log.debug("In sendDeliveryReport");
    }
}
