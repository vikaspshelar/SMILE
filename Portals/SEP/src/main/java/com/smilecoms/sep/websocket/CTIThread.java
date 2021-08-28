/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.websocket;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.Session;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.NewStateEvent;
import org.slf4j.*;

/**
 *
 * @author jaybeepee
 */
public class CTIThread extends Thread implements ManagerEventListener, BaseListener {

    private static final Logger log = LoggerFactory.getLogger(CTIThread.class.getName());
    private final CopyOnWriteArraySet<Session> sessionList;
    ManagerConnectionFactory factory;
    public boolean mustRun = true;
    public boolean propsReady = false;
    public boolean propsChanged = false;
    private ManagerConnection managerConnection;
    private String managerHost = "";
    private String managerUserName = "";
    private String managerPassword = "";
    private final HashMap<String, IncomingCallEvent> eventHistory;

    public CTIThread(CopyOnWriteArraySet<Session> sessions, HashMap<String, IncomingCallEvent> eventHistory) {
        log.warn("Creating CTI WebSocket App instance");
        sessionList = sessions;
        this.eventHistory = eventHistory;
    }

    private void connectToAsterisk() {
        Boolean connected = false;

        while (mustRun && !connected) {
            try {
                log.debug("CTIThread: connecting to Asterisk");
                managerHost = BaseUtils.getProperty("env.cti.asteriskhost", "");
                if (managerHost.isEmpty()) {
                    log.debug("env.cti.asteriskhost is blank so we wont connect to asterisk");
                    return;
                }
                managerUserName = BaseUtils.getProperty("env.cti.asteriskusername");
                managerPassword = BaseUtils.getProperty("env.cti.asteriskpassword");

                factory = new ManagerConnectionFactory(managerHost, managerUserName, managerPassword);
                managerConnection = factory.createManagerConnection();
                //                managerConnection.setSocketReadTimeout(5000);
                managerConnection.setSocketTimeout(10000);
                managerConnection.login();
                managerConnection.addEventListener(this);
                connected = true;
                log.debug("CTIThread: connected to Asterisk Manager");
            } catch (Exception e) {
                log.debug("CTIThread: Failed to connect to Asterisk " + e);
            }

            if (!connected) {
                log.debug("CTIThread: not connected...sleeping for 10 seconds before retry");
                try {
                    sleep(10000);
                } catch (Exception e) {
                    log.warn("CTIThread: failed to sleep");
                }
            }
        }
    }

    private void reconnectToAsterisk() {
        log.debug("CTIThread: Disconnecting from Asterisk....");

        if ((managerConnection != null)
                && ((managerConnection.getState() == ManagerConnectionState.CONNECTED)
                || (managerConnection.getState() == ManagerConnectionState.RECONNECTING))) {
            try {
                managerConnection.logoff();
            } catch (Exception e) {
                log.warn("CTIThread: failed to log off " + e);
            }
        }

        try {
            managerConnection.removeEventListener(this);
            managerConnection = null;
        } catch (Exception e) {
            log.warn("CTIThread: failed to remove listener on re-connect");
        }

        connectToAsterisk();
    }

    @Override
    public void run() {
        try {
            BaseUtils.registerForPropsAvailability(this);
            BaseUtils.registerForPropsChanges(this);
            while (!propsReady) {
                log.debug("properties not yet ready, will try again in 10 seconds");
                sleep(10000);
            }
        } catch (Exception e) {
            log.warn("Failed to register for properties....trying again in 10 seconds");
        }

        connectToAsterisk();

        //we can assume we are connected here and can continue.
        while (mustRun) {
            log.debug("CTIThread running");
            if (propsChanged) {
                reconnectToAsterisk();
                propsChanged = false;
            }

            //we don't do anything else here except keep the thread alive and check for changed props
            try {
                sleep(30000);
            } catch (Exception e) {
                log.warn("Exception trying to sleep " + e);
            }
        }

        try {
            if (managerConnection != null) {
                managerConnection.removeEventListener(this);
                managerConnection.logoff();
            }
        } catch (Exception e) {
            log.warn("CTIThread: failed to shutdown CTIThread cleanly..." + e);
        }

        log.debug("CTIThread shutting down");

    }

    @Override
    public void propsAreReadyTrigger() {
        propsReady = true;
    }

    @Override
    public void propsHaveChangedTrigger() {
        log.debug("CTIThread: Properties have changed trigger");
        String newManagerHost = BaseUtils.getProperty("env.callcentre.asterisk.manager.host");
        String newManagerUserName = BaseUtils.getProperty("env.callcentre.asterisk.manager.username");
        String newManagerPassword = BaseUtils.getProperty("env.callcentre.asterisk.manager.password");

        if ((!managerHost.equals(newManagerHost)) || (!managerUserName.equals(newManagerUserName)) || (!managerPassword.equals(newManagerPassword))) {
            log.debug("CTIThread: Properties have changed");
            //we need to reconnect
            propsChanged = true;
        }
    }

    @Override
    public void onManagerEvent(ManagerEvent event) {
        NewStateEvent newStateEvent;
        if (event instanceof NewStateEvent) {
            newStateEvent = (NewStateEvent) event;
            log.debug("New state for device [" + newStateEvent.getChannel() + "] " + newStateEvent.getChannelStateDesc());
            if (newStateEvent.getChannelStateDesc().equalsIgnoreCase("Ringing")) {
                String channel = newStateEvent.getChannel();
                String device = channel;
                if (device.contains("@")) { //we only care about freepbx extensions
                    device = channel.substring(channel.indexOf("/") + 1, channel.indexOf("@"));
                } else {
                    device = channel.substring(channel.indexOf("/") + 1, channel.indexOf("-"));
                }
                log.debug("we have a new queue call being terminated on an agents device [" + device + "] CallerNumber is ["
                        + newStateEvent.getConnectedlinenum() + "] and Name is [" + newStateEvent.getCallerIdName() + "]");


                IncomingCallEvent callEvent =
                        new IncomingCallEvent(System.currentTimeMillis() / 1000, device, newStateEvent.getConnectedlinenum());
                eventHistory.put(device, callEvent);

                for (Session session : sessionList) {
                    try {
                        if (session.getUserProperties().get("extension") != null && ((String)session.getUserProperties().get("extension")).equals(device)) {
                            log.debug("socket has extension listener for extension " + session.getUserProperties().get("extension"));
                            session.getAsyncRemote().sendText(Long.toString(callEvent.getEpoch()) + ":" + device + ":" + callEvent.getCallerID());
                        }
                    } catch (Exception ex) {
                        log.debug("Failed to write to websocket - it was probably closed: " + ex.getMessage());
                    }
                }
            }
        }

    }

    public void shutDown() {
        log.debug("CTIThread: Shutdown called");
        this.mustRun = false;
    }
}
