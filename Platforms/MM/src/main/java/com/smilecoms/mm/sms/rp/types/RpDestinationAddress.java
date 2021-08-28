/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms.rp.types;

/**
 *
 * @author jaybeepee
 */
public class RpDestinationAddress extends RpAddress {

    public RpDestinationAddress() {
        this.digits = "";
        this.length = 0;
        this.numberingPlanIdentification = NPI.UNKOWN;
        this.typeOfNumber = TON.UNKNOWN;
        this.addessType = RpAddress.AddressType.DESTINATION;
    }

    public RpDestinationAddress(String to) {
        this();
        setAddress(to);
    }
    
}
