/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cti;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.xml.cti.CTIError;
import com.smilecoms.xml.cti.CTISoap;
import com.smilecoms.xml.schema.cti.AgentData;
import com.smilecoms.xml.schema.cti.Done;
import com.smilecoms.xml.schema.cti.IncomingCallData;
import com.smilecoms.xml.schema.cti.QueueLoginData;
import com.smilecoms.xml.schema.cti.StDone;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import org.asteriskjava.manager.action.QueueAddAction;
import org.asteriskjava.manager.action.QueueRemoveAction;
import org.asteriskjava.manager.response.ManagerResponse;

/**
 *
 * @author paul
 */
@WebService(serviceName = "CTI", portName = "CTISoap", endpointInterface = "com.smilecoms.xml.cti.CTISoap", targetNamespace = "http://xml.smilecoms.com/CTI", wsdlLocation = "CTIServiceDefinition.wsdl")
@HandlerChain(file = "/handler.xml")
@Stateless
public class CTIManager extends SmileWebService implements CTISoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;

    @Override
    public Done logCCAgentOutQueues(AgentData agentData) throws CTIError {
        setContext(agentData, wsctx);
        ManagerResponse response;
        //what we do here is log the agent out of all the queues we know about
        log.debug("Logging Agent [{}] out of queues", agentData.getCCAgentExtension());

        try {
            QueueRemoveAction qRemoveAction = new QueueRemoveAction();
            qRemoveAction.setInterface("Local/" + agentData.getCCAgentExtension() + "@from-queue/n");
            Map queueMap = new HashMap<>();
            Iterator<String> it = BaseUtils.getPropertyAsList("env.cti.availablequeues").iterator();
            String queueName;
            String queueNumber;
            String queueString;
            while (it.hasNext()) {
                queueString = it.next();
                queueName = queueString.substring(0, queueString.indexOf("<"));
                queueNumber = queueString.substring(queueString.indexOf("<") + 1, queueString.indexOf(">"));
                queueMap.put(queueNumber, queueName);
            }
            Iterator<String> keys = queueMap.keySet().iterator();
            Asterisk asterisk = Asterisk.getInstance();
            while (keys.hasNext()) {
                qRemoveAction.setQueue(keys.next());
                response = asterisk.sendAction(qRemoveAction);
                if (response != null) {
                    if (response.getResponse().compareTo("") != 1) {
                        if ((response.getResponse().compareTo("Error") == 1) && (!response.getMessage().contains("Already there"))) {
                            log.debug("Logging agent out of queue failed");
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw processError(CTIError.class, e);
        }
        return makeDone();
    }

    @Override
    public Done logCCAgentIntoQueues(QueueLoginData queueLoginData) throws CTIError {
        setContext(queueLoginData, wsctx);
        QueueAddAction qAddAction;
        ManagerResponse response;
        log.debug("Logging Agent [{}] into the following queues:", queueLoginData.getCCAgentExtension());

        try {
            Asterisk asterisk = Asterisk.getInstance();
            for (String queueName : queueLoginData.getMemberQueues()) {
                log.debug(queueName);
                qAddAction = new QueueAddAction(queueName, "Local/" + queueLoginData.getCCAgentExtension() + "@from-queue/n");
                qAddAction.setMemberName(queueLoginData.getCCAgentName());
                response = asterisk.sendAction(qAddAction);
                if (response != null) {
                    if (response.getResponse().compareTo("") != 1) {
                        if ((response.getResponse().compareTo("Error") == 1) && (!response.getMessage().contains("Already there"))) {
                            log.debug("Logging agent into queue [{}] failed", queueName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw processError(CTIError.class, e);
        }
        return makeDone();
    }

    @Override
    public IncomingCallData checkForNewCCAgentCall(AgentData agentData) throws CTIError {
        setContext(agentData, wsctx);
        int sleepCount = BaseUtils.getIntProperty("global.cti.checkforcall.blocking.secs");
        int sleepTime = 1000;
        int count = 0;

        log.debug("Checking for new call to CC Agent with SmileID [{}]", agentData.getCCAgentExtension() );
        IncomingCallData incomingCallData = new IncomingCallData();

        try {
            Asterisk asterisk = Asterisk.getInstance();
            while (count < sleepCount) {
                log.debug("Getting latest event for Agent with number [{}]", agentData.getCCAgentExtension());
                CTICoarseEvent newEvent = asterisk.pullEvent(agentData.getCCAgentExtension());
                if (newEvent != null) {
                    log.debug("Agent [{}] has a new event with CLI [{}]", agentData.getCCAgentExtension(), newEvent.getSourceID());
                    incomingCallData.setNumber(newEvent.getSourceID());
                    return incomingCallData;
                }
                log.debug("Sleeping for [{}] ms", sleepTime);
                sleep(sleepTime);
                count++;
            }
            incomingCallData.setNumber("nothing");
        } catch (Exception e) {
            throw processError(CTIError.class, e);
        }
        return incomingCallData;
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     * @return The resulting complex object
     */
    private Done makeDone() {
        Done done = new Done();
        done.setDone(StDone.TRUE);
        return done;
    }

    @Override
    public Done isUp(String isUpRequest) throws CTIError {
        log.debug("In IsUp");
        try {
            // This would return an error if cant connect to Asterisk
            if (!BaseUtils.getBooleanProperty("env.development.mode", false)) {
                // Only mark as down in prod environments. Hate the constant stack traces in dev
                Asterisk a = Asterisk.getInstance();
            }
        } catch (Exception e) {
            throw processError(CTIError.class, e);
        }
        if (!BaseUtils.isPropsAvailable()) {
            throw createError(CTIError.class, "Properties are not available so this platform will be reported as down");
        }
        log.debug("IsUp returning ok!");
        return makeDone();
    }

    private void sleep(int s) {
        try {
            Thread.sleep(s);
        } catch (Exception e) {
        }
    }
}