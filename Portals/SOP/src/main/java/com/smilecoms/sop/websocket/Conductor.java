/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sop.websocket;

import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class Conductor {
    private static final Logger log = LoggerFactory.getLogger(Conductor.class.getName());
    private static Conductor instance;
    CopyOnWriteArraySet<Session> wsSessions = null;
    SOPWebsocketCallcentreThread ccThread = null;

    public Conductor() {
        log.debug("Instantiating conductor singleton");
        wsSessions = new CopyOnWriteArraySet<>();
        ccThread = new SOPWebsocketCallcentreThread(this, wsSessions);
        ccThread.start();
    }
    
    public static synchronized Conductor getInstance() {
        if (instance == null) { 
            instance = new Conductor();
        }
        
        return instance;
    }
    
    public void addClient(Session session) {
        wsSessions.add(session);
    }
    
    public void removeClient(Session session) {
        wsSessions.remove(session);
    }
    
    
    
    
    
}
