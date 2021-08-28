/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np;

/**
 *
 * @author mukosi
 */
public class OutgoingEventHandler {
    
    private OutgoingEventHandler() {
    }
    
    public static OutgoingEventHandler getInstance() {
        return OutGoingEventHandlerHolder.INSTANCE;
    }
    
    private static class OutGoingEventHandlerHolder {

        private static final OutgoingEventHandler INSTANCE = new OutgoingEventHandler();
    }
}
