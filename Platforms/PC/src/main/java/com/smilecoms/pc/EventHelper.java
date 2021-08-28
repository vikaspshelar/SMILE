/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.PlatformEventManager;
import java.util.List;
import org.slf4j.*;

/**
 *
 * @author richard
 */
public class EventHelper {

    private static final Logger log = LoggerFactory.getLogger(EventHelper.class.getName());

    public static void sendRequestIPCANSessionTermination(String endUserPrivate, String calledStationId) {
        try {
            PlatformEventManager.createEvent("PC", "IPCANSessionTermination", endUserPrivate, endUserPrivate + "|" + calledStationId);
        } catch (Exception e) {
            log.warn("Error sending Smile Event for request for IPCAN Session Termination: [{}]", e.toString());
        }
    }

    public static void sendRequestMMEPurge(String endUserPrivate) {
        try {
            List<String> mmeList = BaseUtils.getPropertyAsList("global.hss.mme.peers");
            //global.hss.mme.peers if a list of HSS peers each in format $MME_DESTINATION_HOST|$MME_DESTINATION_REALM
            for (String mme : mmeList) {
                String[] mmePeer = mme.split("\\|");
                log.debug("MME Destination Host: [{}]", mmePeer[0]);
                log.debug("MME Destination Realm: [{}]", mmePeer[1]);
                String eventData = endUserPrivate + "|" + mmePeer[0] + "|" + mmePeer[1];
                PlatformEventManager.createEvent("PC", "MMEPurge", endUserPrivate, eventData);
            }
            
        } catch (Exception e) {
            log.warn("Error sending Smile Event for request for MME Purge: [{}]", e.toString());
        }
    }
}
