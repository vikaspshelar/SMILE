/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.CCAgentData;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.IncomingCCAgentCallData;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.stripes.*;
import com.smilecoms.sep.helpers.*;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.stripes.action.*;

/**
 *
 * @author Jason Penton
 */
public class CallcentreActionBean extends SmileActionBean {

    private static final String QUEUES_KEY = "env.cti.availablequeues";
    private OptionTransfer optionTransfer;
    private final static String OPTION_TRANSFER_DELIMITER = ",";

    @DefaultHandler
    public Resolution showCallCentreLogin() {
        checkPermissions(Permissions.CALL_CENTRE_PABX);
        return getDDForwardResolution("/call_centre/login.jsp");
    }

    public Resolution callCentreLogout() {
        //remove this agent from all the queues we know about
        checkPermissions(Permissions.CALL_CENTRE_PABX);

        String ccAgentExtension = getCallCentreAgentExtensionFromSession();
        if (ccAgentExtension != null) {
            CCAgentData callcentreAgentLogoutData = new CCAgentData();
            callcentreAgentLogoutData.setCCAgentExtension(ccAgentExtension);
            SCAWrapper.getUserSpecificInstance().logCCAgentOutQueues(callcentreAgentLogoutData);
            removeCallCentreAgentExtensionFromSession();
        }
        
        deleteCookie(SESSION_KEY_CCAGENT_EXTENSION);
        setPageMessage("cc.logout.succeeded");
        return getDDForwardResolution("/index.jsp");
    }

    public Resolution lookForIncomingCall() {
        checkPermissions(Permissions.CALL_CENTRE_PABX);
        String extension = getCallCentreAgentExtensionFromSession();
        if (extension == null) {
            return new StreamingResolution("text", new StringReader("nothing"));
        }
        log.debug("Agent extension is [{}]", extension);
        CCAgentData agentData = new CCAgentData();
        agentData.setCCAgentExtension(extension);
        
        IncomingCCAgentCallData incomingCall = SCAWrapper.getUserSpecificInstance().checkForNewCCAgentCall(agentData);
        return new StreamingResolution("text", new StringReader(incomingCall.getNumber()));
    }

    public Resolution callCentreLogin() {
        checkPermissions(Permissions.CALL_CENTRE_PABX);
        
        CustomerQuery custQuery = new CustomerQuery();
        custQuery.setCustomerId(getUserCustomerIdFromSession());
        custQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer sepUser  = (SCAWrapper.getUserSpecificInstance().getCustomer(custQuery));
        
        /* Retrieve values of queues to log into using OptionTransfer */
        String newRight = optionTransfer.getNewRight();
        String[] queueNames = newRight.split(OPTION_TRANSFER_DELIMITER);
        getCCQueueLoginData().getMemberQueues().addAll(Arrays.asList(queueNames));
        getCCQueueLoginData().setCCAgentName(sepUser.getFirstName() + " " + sepUser.getLastName());
        SCAWrapper.getUserSpecificInstance().logCCAgentIntoQueues(getCCQueueLoginData());
        //set the session object to let us know we are an agent logged in to receive CTI events/popups
        setCallCentreAgentExtensionInSession(getCCQueueLoginData().getCCAgentExtension());
        //We will also add a cookie which will help our CTI state remain across failover. This is beacuse not ALL
        //of the HTTP session is restored when we failover to another backend portal node.
        createCookie(SESSION_KEY_CCAGENT_EXTENSION, getCCQueueLoginData().getCCAgentExtension(), 3660*24*7);

        setPageMessage("cc.login.succeeded");
        return getDDForwardResolution("/index.jsp");
    }

    public String[] getAvailableQueues() {
        List<String> availableQueues = BaseUtils.getPropertyAsList(QUEUES_KEY);
        String[] queues = new String[availableQueues.size()];
        Iterator<String> it = availableQueues.iterator();
        int i = 0;
        while (it.hasNext()) {
            queues[i++] = it.next();
        }
        return queues;
    }

    public void setAvailableQueues(String[] availableQueues) {
    }

    public OptionTransfer getOptionTransfer() {
        return optionTransfer;
    }

    public void setOptionTransfer(OptionTransfer optionTransfer) {
        this.optionTransfer = optionTransfer;
    }
}
