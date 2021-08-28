/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */
public class ChargingDetail {
    
    Long chargingTimeStamp;
    Double eventUnits;
    Double eventUnitCreditUnits;
    private static final Logger log = LoggerFactory.getLogger(ChargingDetail.class);
    
    public ChargingDetail(String sessionDetail) throws Exception {
        //1370031554000,0.0,27484.0,-27484.0
        String[] tokens = sessionDetail.split(",");
        try {
            //this is timestamp!
            this.chargingTimeStamp = Long.parseLong(tokens[0]);
            //this is event units
            this.eventUnits = Double.parseDouble(tokens[1]);
            //this is event units credit!
            this.eventUnitCreditUnits = Double.parseDouble(tokens[2]);
        } catch (Exception e) {
            log.warn("Error inititlaising ChargingDetail object for string [{}] Error: [{}]", sessionDetail, e.toString());
            throw e;
        }
    }
    
    public Long getChargingTimeStamp() {
        return chargingTimeStamp;
    }
    
    public Double getEventUnitCreditUnits() {
        return eventUnitCreditUnits;
    }
    
    public Double getEventUnits() {
        return eventUnits;
    }
}
