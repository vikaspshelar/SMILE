/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */

/*
 * This helper class is used to instantiate charging detail information
 * We do this on the front end as apposed to passing around Charging Detail objects in SCA
 * This because charging detail is heavy (many per account history object)
 * So they are passed through SCA as a zipped String
 * Then we instantiate here for easy access to internal information on the front end
 */
public class ChargingDetail {

    Long chargingTimeStamp;
    Double eventUnits;
    Double eventUnitCreditUnits;
    private static final Logger log = LoggerFactory.getLogger(ChargingDetail.class);

    public ChargingDetail(String sessionDetail) throws Exception {
        //sessionDetail parm is zipped
        //sessionDetail currently is a charging Detail String in format:
        //timestamp,eventUnits,eventUnitCreditUnits\n
        String[] tokens = sessionDetail.split(",");
        try {
            //this is timestamp!
            this.chargingTimeStamp = Long.parseLong(tokens[0]);
            //this is event units
            this.eventUnits = Double.parseDouble(tokens[1]);
            //this is event units credit!
            this.eventUnitCreditUnits = Double.parseDouble(tokens[2]);
        } catch (Exception ex) {
            log.warn("Error inititlaising ChargingDetail object for string [{}] Error: [{}]", sessionDetail, ex.toString());
            throw ex;
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
