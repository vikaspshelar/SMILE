/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.unitcredits.wrappers.IUnitCredit;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.commons.util.Utils;
import java.util.Calendar;
import java.util.List;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class EventHelper {

    private static final Logger log = LoggerFactory.getLogger(EventHelper.class.getName());

    public enum AccSubTypes {
        CREDIT, DEBIT, DEPLETED
    }

    public static void sendUnitCreditRemovedDueToConstraints(IUnitCredit uc) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending notification of Unit credit id [{}] removed due to constraints account [{}]", uc.getUnitCreditInstanceId(), uc.getAccountId());
                Stopwatch.start();
            }
            // Almost Finished
            PlatformEventManager.createEvent(
                    "BM",
                    "UNIT_CREDIT_REMOVED_DUE_TO_CONSTRAINTS",
                    String.valueOf(uc.getAccountId()),
                    uc.getUnitCreditName() + "|" + uc.getUnitCreditInstanceId() + "|" + uc.getAccountId());
            if (log.isDebugEnabled()) {
                Stopwatch.stop();
                log.debug("Sending Smile event took [{}]", Stopwatch.millisString());
            }
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

    public static void sendImeiUnknownEvent(String imeisv) {
        if (imeisv == null) {
            log.debug("imeisv is null so not sending event");
            return;
        }
        try {
            log.debug("Sending notification of Unknown IMEI [{}]", imeisv);
            PlatformEventManager.createEventAsync(
                    "BM",
                    "IMEI_UNKNOWN",
                    imeisv,
                    imeisv);
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

    //still used for static IP 30 day Jira ticket logging
    public static void sendUnitCredit30DaysFromExpiredEvent(IUnitCredit uc) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending notification of Unit credit id [{}] expires in 30 days for account [{}]", uc.getUnitCreditInstanceId(), uc.getAccountId());
                Stopwatch.start();
            }
            // Almost Finished
            PlatformEventManager.createEvent(
                    "UNIT_CREDIT_EXPIRING",
                    "30DAYS",
                    String.valueOf(uc.getAccountId()),
                    uc.getUnitCreditName() + "|" + uc.getUnitCreditInstanceId() + "|" + uc.getAccountId() + "|" + uc.getAvailableUnitsLeft().toPlainString()
                    + "|" + uc.getSpecUnits().toPlainString() + "|" + Utils.formatDateLong(uc.getExpiryDate()) + "|" + uc.getAccount().getCurrentBalanceCents().toPlainString()
                    + "|" + uc.getClass().getSimpleName()
                    + "|" + uc.getPropertyFromConfig("EventData"),
                    "UC30DAYS_" + uc.getUnitCreditInstanceId());
            if (log.isDebugEnabled()) {
                Stopwatch.stop();
                log.debug("Sending Smile event took [{}]", Stopwatch.millisString());
            }
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

    public static void sendUnitCreditEvent(IUnitCredit uc, String subType) {
        if (subType == null) {
            log.debug("Event sub type is null so not sending event");
            return;
        }
        try {
            log.debug("Sending notification of Unit credit id [{}] event sub type [{}]", uc.getUnitCreditInstanceId(), subType);
            String data = "AccId=" + uc.getAccountId() + "\r\nPIId=" + uc.getProductInstanceId() + "\r\nUCId=" + uc.getUnitCreditInstanceId();
            PlatformEventManager.createEvent(
                    "CL_UC",
                    subType,
                    String.valueOf(uc.getAccountId()),
                    data,
                    "CL_UC_" + subType + "_" + uc.getUnitCreditInstanceId());
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}] for account [{}] unitCred [{}]. Error: [{}]",subType,uc.getAccountId(), uc.getUnitCreditName(),  e.toString());
        }
    }

    public static void sendUnitCreditEvent(IUnitCredit uc, String subType, String count) {

        if (subType == null) {
            log.debug("Event sub type is null so not sending event");
            return;
        }

        try {
            log.debug("Sending notification of Unit credit id [{}] event sub type [{}]", uc.getUnitCreditInstanceId(), subType);
            String data = "AccId=" + uc.getAccountId() + "\r\nPIId=" + uc.getProductInstanceId() + "\r\nUCId=" + uc.getUnitCreditInstanceId();
            PlatformEventManager.createEvent(
                    "CL_UC",
                    subType,
                    String.valueOf(uc.getAccountId()),
                    data,
                    "CL_UC_" + subType + "_" + uc.getUnitCreditInstanceId() + "_" + uc.getUnitCreditInstanceId() + "_" + count);
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}] for account [{}] unitCred [{}]. Error: [{}]",subType,uc.getAccountId(), uc.getUnitCreditName(),  e.toString());
        }
    }

    //Used to send events for Container bundles; e.g. consolidating provisioning events
    public static void sendContainerUnitCreditEvent(List<IUnitCredit> ucList, String type, String subType) {

        if (subType == null) {
            log.debug("Event sub type is null so not sending event");
            return;
        }

        try {

            log.debug("Sending notification of container unit credit event sub type [{}]", subType);

            long accountId = 0;
            boolean isFirst = true;
            String line = "";
            String key = "";
            for (IUnitCredit uc : ucList) {
                line = "UCId=" + uc.getUnitCreditInstanceId() + "UCNAME=" + uc.getUnitCreditName() + "UCId=" + uc.getProductInstanceId() + "AccId=" + uc.getAccountId();

                key = key + uc.getUnitCreditInstanceId() + "_";
                if (!isFirst) {
                    line = line + "|";
                }
                isFirst = false;
            }

            line = line.substring(0, line.lastIndexOf("\\|"));
            key = key.substring(0, key.lastIndexOf("_"));

            PlatformEventManager.createEvent(
                    type,
                    subType,
                    String.valueOf(accountId),
                    line,
                    "CL_UC_" + subType + "_" + key);
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

    public static void sendAccountEvent(IAccount acc, AccSubTypes subType, long ahId) {
        try {
            log.debug("Sending notification of account id [{}] event sub type [{}]", acc.getAccountId(), subType);
            String data;
            if (ahId > 0) {
                data = "AccId=" + acc.getAccountId() + "\r\nAHId=" + ahId;
            } else {
                data = "AccId=" + acc.getAccountId();
            }

            Calendar eventDelay = Calendar.getInstance();
            eventDelay.add(Calendar.SECOND, 5);

            PlatformEventManager.createEvent(
                    "CL_AC",
                    subType.toString(),
                    String.valueOf(acc.getAccountId()),
                    data, eventDelay.getTime());
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

    public static void sendAccountEvent(IAccount acc, AccSubTypes subType, long ahId, String frequency) {
        try {
            log.debug("Sending notification of account id [{}] event sub type [{}]", acc.getAccountId(), subType);
            String data;
            if (ahId > 0) {
                data = "AccId=" + acc.getAccountId() + "\r\nAHId=" + ahId;
            } else {
                data = "AccId=" + acc.getAccountId();
            }

            PlatformEventManager.createEvent(
                    "CL_AC",
                    subType.toString(),
                    String.valueOf(acc.getAccountId()),
                    data, frequency);
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

    public static void sendUCSplitEvent(long srcAccId, IUnitCredit uc, long ahId) {
        try {
            log.debug("Sending notification of UC split");
            String data;
            data = "AccId=" + srcAccId + "\r\nAHId=" + ahId + "\r\nUCId=" + uc.getUnitCreditInstanceId();

            Calendar eventDelay = Calendar.getInstance();
            eventDelay.add(Calendar.SECOND, 5);

            PlatformEventManager.createEvent(
                    "CL_UC",
                    "SPLIT",
                    String.valueOf(srcAccId),
                    data, eventDelay.getTime());
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

    public static void sendTermsAndConditionsEvent(IUnitCredit uc, String termsAndConditionsResourceKey) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Sending terms and conditions event for UCI [{}]", uc.getUnitCreditInstanceId());
                Stopwatch.start();
            }
            if (uc.getProductInstanceId() > 0) {
                // Provide SI Id
                // PCB - Next Release add event subscription
                PlatformEventManager.createEvent(
                        "TERMS_AND_CONDITIONS",
                        "UC_PI",
                        String.valueOf(uc.getUnitCreditInstanceId()),
                        termsAndConditionsResourceKey + "|" + uc.getProductInstanceId());
            } else {
                // Provide account Id
                PlatformEventManager.createEvent(
                        "TERMS_AND_CONDITIONS",
                        "UC_AC",
                        String.valueOf(uc.getUnitCreditInstanceId()),
                        termsAndConditionsResourceKey + "|" + uc.getAccountId());
            }

            if (log.isDebugEnabled()) {
                Stopwatch.stop();
                log.debug("Sending Smile event took [{}]", Stopwatch.millisString());
            }
        } catch (Exception e) {
            log.warn("Error sending Smile Event: [{}]", e.toString());
        }
    }

}
