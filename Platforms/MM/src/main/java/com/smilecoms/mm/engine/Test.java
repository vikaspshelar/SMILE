/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.engine;

import com.smilecoms.commons.base.lifecycle.BaseListener;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.plugins.offnetsms.OffnetSMSMessage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.slf4j.*;

/**
 *
 * @author paul
 */
@Singleton
@Startup
public class Test{// implements BaseListener, Runnable {
    private static final Logger log = LoggerFactory.getLogger(Test.class);
    private boolean muststop = false;
//    @PostConstruct
//    public void startUp() {
//        BaseUtils.registerForPropsAvailability(this);
//    }
//
//    @Override
//    public void propsAreReadyTrigger() {
//        Thread t = new Thread(this);
//        t.start();
//    }
//
//    @PreDestroy
//    public void shutDown() {
//        muststop = true;
//        BaseUtils.deregisterForPropsAvailability(this);
//        
//    }
//
//    @Override
//    public void propsHaveChangedTrigger() {
//    }
//
//    @Override
//    public void run() {
//        Utils.sleep(5000);
//        for (int i = 0; i < 100; i++) {
//            if (muststop) {
//                break;
//            }
//            OffnetSMSMessage m = new OffnetSMSMessage();
//            m.setFrom("1111111111");
//            List<String> d = new ArrayList<String>();
//            d.add("sip:paul@ims.smilecoms.com");
//            m.setTo(d);
//            m.setMessageText("sdhjishdakldjasidghayusgdyasgduiashdasuhdsgdyuasgduiashduiashdiasgduiasdia");
//           // DeliveryEngine.getInstance().enqueueMessage(m);
//            
//            if (i % 1000 == 0) {
//                log.warn("Enqueued [{}] messages", i);
//            }
//       }
//    }
}
