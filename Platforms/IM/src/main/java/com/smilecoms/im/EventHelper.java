/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.im.db.op.IMDAO;
import com.smilecoms.xml.schema.im.Customer;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class EventHelper {

    private static final Logger log = LoggerFactory.getLogger(EventHelper.class.getName());

    public static void sendCustomerAuthenticateAttempt(String ipAddress, String passFail, String SSOIdentity, int customerId) {
        try {
            PlatformEventManager.createEvent("AUTH", passFail, String.valueOf(customerId), SSOIdentity + "|" + ipAddress + "|" + customerId);
        } catch (Exception e) {
            log.warn("Error sending Smile Event for customer authentication: [{}]", e.toString());
        }
    }

    public static void sendCustomerPasswordChange(int customerProfileId) {
        try {
            PlatformEventManager.createEvent("IM", "PasswordChange", String.valueOf(customerProfileId), String.valueOf(customerProfileId));
        } catch (Exception e) {
            log.warn("Error sending Smile Event for customer password change: [{}]", e.toString());
        }
    }

    static void sendCustomerDetailChangeOld(int customerProfileId, Customer modifiedCustomer, String organisationRoles) {
        try {

            /*
         7 = Marketing Emails + Notification Emails + Post Call SMS
         6 = Marketing Emails + Post Call SMS
         5 = Marketing Emails + Notification Emails
         4 = Marketing Emails
         3 = Notification Emails + Post Call SMS
         2 = Post Call SMS
         1 = Notification Emails
         0 = Nothing
             */
            String optInLevelString = "";

            if ((modifiedCustomer.getOptInLevel() & 1) == 1) {
                optInLevelString = "Notification Emails + ";
            }
            if ((modifiedCustomer.getOptInLevel() & 2) == 2) {
                optInLevelString = optInLevelString + "Post Call SMS + ";
            }
            if ((modifiedCustomer.getOptInLevel() & 4) == 4) {
                optInLevelString = optInLevelString + "Marketing Emails + ";
            }
            if (!optInLevelString.isEmpty()) {
                optInLevelString = optInLevelString.substring(0, optInLevelString.length() - 3);
            }

            PlatformEventManager.createEvent("IM", "CustomerDetailChange", String.valueOf(customerProfileId),
                    String.valueOf(customerProfileId)
                    + "|" + modifiedCustomer.getFirstName()
                    + "|" + modifiedCustomer.getLastName()
                    + "|" + modifiedCustomer.getSSOIdentity()
                    + "|" + modifiedCustomer.getEmailAddress()
                    + "|" + modifiedCustomer.getAlternativeContact1()
                    + "|" + modifiedCustomer.getMothersMaidenName()
                    + "|" + modifiedCustomer.getIdentityNumber()
                    + "|" + organisationRoles
                    + "|" + optInLevelString
            );
        } catch (Exception e) {
            log.warn("Error sending Smile Event for customer email change: [{}]", e.toString());
        }
    }

    public static void sendUserStateChange(String impu, int imsUserState, EntityManager em) {

        if (imsUserState == 1) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Sending notification of public user identity [{}] state change to [{}]", impu, getStringUserState(imsUserState));
                    Stopwatch.start();
                }
                if (imsUserState == IMPU_user_state_Registered && !BaseUtils.getBooleanProperty("env.im.write.imsuser.state.registered.events", true)) {
                    log.debug("Wrting events for state change [{}] is disabled, user requesting change [{}]", getStringUserState(imsUserState), impu);
                    return;
                }
                if (!IMDAO.pendingIMSSubscriptionsExists(em, impu)) {
                    log.debug("There are NO pending event subscriptions for impu [{}], not going to write event", impu);
                    return;
                }
                PlatformEventManager.createEventAsync("IMS_USER_STATE", getStringUserState(imsUserState), impu, impu);
                if (log.isDebugEnabled()) {
                    Stopwatch.stop();
                    log.debug("Sending Smile event took [{}]", Stopwatch.millisString());
                }
            } catch (Exception e) {
                log.warn("Error sending Smile Event: [{}]", e.toString());
            }
        } else {
            log.debug("No need to notify of a state change != 1");
        }
    }

    private static String getStringUserState(int state) {
        switch (state) {
            case IMPU_user_state_Not_Registered:
                return "NOT_REGISTERED";
            case IMPU_user_state_Registered:
                return "REGISTERED";
            case IMPU_user_state_Unregistered:
                return "UNREGISTERED";
            case IMPU_user_state_Auth_Pending:
                return "AUTH_PENDING";
            default:
                return "UNKNOWN";
        }
    }
    // IMPU_user_state
    public static final short IMPU_user_state_Not_Registered = 0;
    public static final short IMPU_user_state_Registered = 1;
    public static final short IMPU_user_state_Unregistered = 2;
    public static final short IMPU_user_state_Auth_Pending = 3;

}
