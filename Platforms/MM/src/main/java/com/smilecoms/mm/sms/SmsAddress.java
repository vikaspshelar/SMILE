/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms;

/**
 *
 * @author jaybeepee
 */
public abstract class SmsAddress {
    // From TS 23.040 9.1.2.5 and TS 24.008 table 10.5.118
    // and C.S0005-D table 2.7.1.3.2.4-2

    public static final int NPI_ISDN = 1;
    public static final int NPI_NATIONAL = 8;
    
    public static final int TON_UNKNOWN = 0;
    public static final int TON_INTERNATIONAL = 1;
    public static final int TON_NATIONAL = 2;
    public static final int TON_NETWORK = 3;
    public static final int TON_SUBSCRIBER = 4;
    public static final int TON_ALPHANUMERIC = 5;
    public static final int TON_ABBREVIATED = 6;
    public int ton;
    public int npi;
    public String address;
    public byte[] origBytes;

    /**
     * Returns the address of the SMS message in String form or null if
     * unavailable
     */
    public String getAddressString() {
        return address;
    }

    /**
     * Returns true if this is an alphanumeric address
     */
    public boolean isAlphanumeric() {
        return ton == TON_ALPHANUMERIC;
    }

    /**
     * Returns true if this is a network address
     */
    public boolean isNetworkSpecific() {
        return ton == TON_NETWORK;
    }

    public boolean couldBeEmailGateway() {
        // Some carriers seems to send email gateway messages in this form:
        // from: an UNKNOWN TON, 3 or 4 digits long, beginning with a 5
        // PID: 0x00, Data coding scheme 0x03
        // So we just attempt to treat any message from an address length <= 4
        // as an email gateway

        return address.length() <= 4;
    }
}
