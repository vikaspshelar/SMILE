/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms.rp.types;

/**
 *
 * @author jaybeepee
 */
public class RpOriginatorAddress extends RpAddress{
    
    public RpOriginatorAddress() {
        this.addessType = RpAddress.AddressType.ORIGINATING;
        this.digits = "";
        this.length = 0;
        this.numberingPlanIdentification = NPI.UNKOWN;
        this.typeOfNumber = TON.UNKNOWN;
    }

    public RpOriginatorAddress(String fromNumber) {
        this();
        this.setAddress(fromNumber);
    }
    
    
    
}
