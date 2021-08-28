/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cti;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.platform.Platform;
import com.smilecoms.cti.CTICoarseEvent.EventID;
import java.util.Date;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.PingThread;
import org.asteriskjava.manager.action.ManagerAction;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.NewStateEvent;
import org.asteriskjava.manager.response.ManagerResponse;
import org.slf4j.*;

/**
 *
 * @author paul
 */
@Singleton
@Startup
@Local({BaseListener.class, ManagerEventListener.class})
public class Asterisk implements ManagerEventListener, BaseListener {

    private static final Logger log = LoggerFactory.getLogger(Asterisk.class);
    private static Asterisk myInstance;
    private PingThread pingThread;
    private ManagerConnection managerConnection;

//    public static Asterisk getInstance() {
//        return myInstance;
//    }
    @PostConstruct
    public void startUp() {
        log.debug("In startup...");
        Platform.init();
        BaseUtils.registerForPropsAvailability(this);
        myInstance = this;
    }

    @Override
    public void propsAreReadyTrigger() {
        try {
            this.verifyConnections();
        } catch (Exception e) {
            log.error("Unable to initialise Asterisk Manager interface for CTI");
        }
    }

    @PreDestroy
    public void shutDown() {
        log.debug("In cleanup...");
        try {
            if (this.pingThread != null) {
                this.pingThread.die();
                this.pingThread = null;
            }
            if (this.managerConnection != null) {
                this.managerConnection.logoff();
                this.managerConnection = null;
            }
        } catch (Exception e) {
            log.warn("Error cleaning up CTI connection to asterisk: ", e);
        }
    }

    public static Asterisk getInstance() throws AsteriskException {
        if (myInstance == null) {
            myInstance = new Asterisk();
        }
        try {
            myInstance.verifyConnections();
        } catch (Exception e) {
            throw new AsteriskException(e);
        }
        log.debug("Returning an instance of the singleton Asterisk class to the caller for their use");
        return myInstance;
    }

    public CTICoarseEvent pullEvent(String id) {
        CTICoarseEvent ret = CacheHelper.removeAndGetFromLocalCache(id, CTICoarseEvent.class);
        log.debug("Returning a non null event for id [{}]", id);
        return ret;
    }

    @Override
    public void onManagerEvent(ManagerEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Received Manager Event of type [{}]", event);
        }
        if (event instanceof NewStateEvent) {
            NewStateEvent newStateEvent = (NewStateEvent) event;
            if (log.isDebugEnabled()) {
                log.debug("New state is [{}]", newStateEvent.getChannelStateDesc());
            }
            if (newStateEvent.getChannelStateDesc().equalsIgnoreCase("Ringing")) {
//                if (newStateEvent.getCallerIdName().contains("queue")) {
                String channel = newStateEvent.getChannel();
                if (channel.contains("@")) { //we only care about freepbx extensions
                    String device = channel.substring(channel.indexOf("/") + 1, channel.indexOf("@"));
                    log.debug("we have a new queue call being terminated on an agents device [" + device + "]");
                    //create a new CG event and pop it in the map
                    CTICoarseEvent cgEvent = new CTICoarseEvent();
                    cgEvent.setEventID(EventID.SCREENPOP);
                    cgEvent.setTargetID(device);
                    cgEvent.setSourceID(newStateEvent.getConnectedlinenum());
                    Date d = new Date();
                    cgEvent.setTimeStamp(new Date());
                    CacheHelper.putInLocalCache(device, cgEvent, 60);
                }
//                }
            }
        }
    }

    // reconnects to asterisk if anything is wrong
    private void verifyConnections() throws Exception {
        log.debug("In verifyConnections...");
        if (managerConnection == null || managerConnection.getState() != ManagerConnectionState.CONNECTED) {
            log.debug("verifyConnections has detected that it is not connected to Asterisk and will try and connect");
            connectToAsterisk();
        }
    }

    public ManagerResponse sendAction(ManagerAction action) throws AsteriskException {
        //make sure we are still connected to the Asterisk API interface
        log.debug("In sendAction. Action is " + action.toString());
        ManagerResponse response = null;
        try {
            response = managerConnection.sendAction(action, BaseUtils.getIntProperty("global.cti.managertxtimeout"));
        } catch (Exception ex) {
            throw new AsteriskException(ex.toString());
        }
        log.debug("Action successfully sent to Asterisk");
        return response;
    }

    private void connectToAsterisk() throws Exception {
        log.debug("In connectToAsterisk...");
        // Clean up if there are residual connections or ping threads before trying to connect again
        //this.myInstance.cleanUp();
        //get properties for manager connection
        String asteriskHost = BaseUtils.getProperty("env.cti.asteriskhost", "");
        if (asteriskHost.isEmpty()) {
            log.debug("env.cti.asteriskhost is blank so we wont connect to asterisk");
            return;
        }
        String asteriskIPOrHost = null;
        try {
            asteriskIPOrHost = BaseUtils.doForwardLookup(asteriskHost);
            log.debug("Resolved host [{}] to IP [{}]. Using IP in case host entry in DNS has changed and java is caching the old one", asteriskHost, asteriskIPOrHost);
        } catch (Exception e) {
            log.warn("Error doing asterisk host forward lookup", e);
        }
        if (asteriskIPOrHost == null || asteriskIPOrHost.isEmpty()) {
            asteriskIPOrHost = asteriskHost;
        }
        String asteriskUsername = BaseUtils.getProperty("env.cti.asteriskusername");
        String asteriskPassword = BaseUtils.getProperty("env.cti.asteriskpassword");
        int asteriskPort = BaseUtils.getIntProperty("global.cti.asteriskport");
        int ctiPingThreadInterval = BaseUtils.getIntProperty("global.cti.pingthreadinterval");
        int ctiPingThreadReponseTimeout = BaseUtils.getIntProperty("global.cti.pingthreadresponsetimeout");

        if (this.pingThread != null) {
            try {
                this.pingThread.die();
            } catch (Exception e) {
                log.warn("Error killing ping thread", e);
            }
        }
        //before we continue, lets make sure our existing manager connection is nuked.
        if (this.managerConnection != null) {
            try {
                this.managerConnection.logoff();
            } catch (Exception e) {
                log.warn("Error logging off manager connection", e);
            }
        }

        //this must be the first time we are here - we need to connect with new manager connection
        //connect to manager API interface
        log.debug("Trying to connect to Asterisk API using host [" + asteriskIPOrHost + ":" + asteriskPort + "] with uname [" + asteriskUsername + "] and password [" + asteriskPassword + "]");
        ManagerConnectionFactory factory = new ManagerConnectionFactory(asteriskIPOrHost, asteriskPort, asteriskUsername, asteriskPassword);
        managerConnection = factory.createManagerConnection();

        // FROM JAVADOCS:If you set this property to a non zero value be sure to also use a PingThread or somthing similar to make sure there is some network traffic, 
        //otherwise you will encounter lots of unexpected reconnects. The read timeout should be at least twice the interval set for the PingThread. 
        managerConnection.setSocketReadTimeout(ctiPingThreadReponseTimeout * 2);
        managerConnection.setSocketTimeout(5000);
        managerConnection.login();
        managerConnection.addEventListener(this);
        //start the ping thread to keep the manager API connection alive
        pingThread = new PingThread(managerConnection);
        pingThread.setInterval(ctiPingThreadInterval);
        pingThread.setTimeout(ctiPingThreadReponseTimeout);
        pingThread.start();
        log.debug("Successfully finished connecting to Asterisk");
    }

    @Override
    public void propsHaveChangedTrigger() {
    }
}
