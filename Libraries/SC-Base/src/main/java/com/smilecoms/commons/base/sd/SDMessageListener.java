package com.smilecoms.commons.base.sd;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class SDMessageListener implements MessageListener<Boolean> {
    
    private static final Logger log = LoggerFactory.getLogger(SDMessageListener.class);
     @Override
    public void onMessage(Message msg) {
        log.debug("Notified that a service has changed. Repulling services");
        ServiceDiscoveryAgent.getInstance().pullServices();
    }
    
}