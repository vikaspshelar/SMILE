/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */

@ServerEndpoint(value = "/sop-callcentre")
public class SopWebsocket {
    private static Logger log = LoggerFactory.getLogger(SopWebsocket.class.getName());
    Conductor conductor = Conductor.getInstance();
    
    @OnMessage
    public void processClientRequest(@PathParam("name") String key,
            String message, Session session) {
        /* clients need to receive this before they consider the connection being up */
        session.getAsyncRemote().sendText("success");
    }
    
    @OnOpen
    public void onOpen(final Session session) throws Exception {
        log.debug("new websocket connection");
        conductor.addClient(session);
    }
    
    @OnClose
    public void onClose(final Session session) throws Exception {
        log.debug("closed websocket connection");
        conductor.removeClient(session);
    }
    
}
