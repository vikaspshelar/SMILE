/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.websocket;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import org.slf4j.*;

/**
 *
 * @author jaybeepee
 */
@ServerEndpoint(value = "/cti")
public class CTIWebSocket{
    private static final Logger log = LoggerFactory.getLogger(CTIWebSocket.class.getName());
    Conductor conductor;
    private final HashMap<String, IncomingCallEvent> eventHistory;
    
    public CTIWebSocket() {
        conductor = Conductor.getInstance();
        eventHistory = conductor.getEventHistory();
    }
    
    
    @OnMessage
    public void processClientRequest(@PathParam("name") String key,
            String message, Session session) {
        /* clients need to receive this before they consider the connection being up */
        String p = "^(\\S*)\\|(\\d*)";      //"EXT|EPOCH_OF_LAST_CLIENT_EVENT"
        Pattern pattern = Pattern.compile(p);
        Matcher m = pattern.matcher(message);
        if (m.find()) {
            long lastClientEvent = 0;
            log.debug("Received CTI WS connection for Agent extension [{}] with last received event at [{}]", m.group(1), m.group(2));
            log.debug("CTIWebSocket: Received message from new connected websocket client [{}]", message);
            session.getUserProperties().put("extension", new String(m.group(1)));
            
            try {
                 lastClientEvent = Long.parseLong(m.group(2));
            } catch (NumberFormatException e) {
                //ignore the request but log stacktrace
                log.debug("Could not get last acknowledged event timestamp from client [{}]", e.getMessage());
                return;
            }
        
            if (eventHistory.containsKey(m.group(1))) {
                IncomingCallEvent callEvent = eventHistory.get(m.group(1));            
                if (lastClientEvent < callEvent.getEpoch()) {
                    session.getAsyncRemote().sendText(Long.toString(callEvent.getEpoch()) + ":" + m.group(1) + ":" + callEvent.getCallerID());
                }
            }
        }
    }
    
    @OnOpen
    public void onOpen(final Session session) throws Exception {
        try {
            log.debug("new websocket connection");
            conductor.addClient(session);
        } catch (Exception ex) {
            log.error("exception on onOpen: " + ex.getMessage());
        }
    }
    
    @OnClose
    public void onClose(final Session session) throws Exception {
        try {
            log.debug("closed websocket connection");
            conductor.removeClient(session);
        } catch (Exception ex) {
            log.error("exception on onClose: " + ex.getMessage());
        }    
    }
    
    @OnError
    public void onError(Throwable t) {
        log.debug("error thrown: " + t.getStackTrace());
        
    }

}
