/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms.rp.types;

import com.smilecoms.commons.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class RpAddress {
    private static final Logger log = LoggerFactory.getLogger(RpAddress.class);

    public enum TON {

        UNKNOWN(0), INTERNATIONAL(1), NATIONAL(2), NETWORK_SPECIFIC(3), SUBSCRIBER(4), ALPHANUMERIC(5), ABBREVIATED(6), RESERVED(7);
        private int value;

        private TON(int value) {
            this.value = value;
        }
    }

    public enum NPI {

        UNKOWN(0), ISDN(1), X121(3), TELEX(4), NATIONAL(8), PRIVATE(9), ERMES(10), RESERVED(16);
        private int value;

        private NPI(int value) {
            this.value = value;
        }
    }

    public enum AddressType { ORIGINATING, DESTINATION, SMSC};

    protected int length;
    protected TON typeOfNumber;
    protected NPI numberingPlanIdentification;
    protected AddressType addessType ;
    protected String digits = "";

    @Override
    public String toString() {
        return "Type:" + addessType + ",TON:" + typeOfNumber +",NPI:" + numberingPlanIdentification + ",digits:" + digits;
    }
    
    public int deserialise(byte[] bytes, int offset) {
        int oneByte;
        
        this.length = bytes[offset++];
        
        if (length == 0) {
            log.warn("originator address is empty");
            return 1;
        }
        
        int len = this.addessType==AddressType.SMSC?(length-1):((length/2) + (length%2));
        
        int ton = ((int)bytes[offset] & 0x70) >> 4;
        int npi = (int)bytes[offset] & 0x0F;
        
        this.typeOfNumber = TON.values()[ton];
        this.numberingPlanIdentification = NPI.values()[npi];
        
        offset++;
        for (int i = 0; i < len; i++, offset++) {
            byte myByte = (byte)((bytes[offset] << 4) | (bytes[offset] >> 4));
            oneByte = myByte;
            
            if ((oneByte & 0x0F) == 0x0F) { /* ends with 'F'? */
                oneByte = ((oneByte & 0xF0) >> 4);
                digits = digits.concat(Integer.toHexString(oneByte));
            } else {
                String hex = Integer.toHexString(oneByte);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                digits = digits.concat(hex);
            }
        }

        return length + 1;
    }
    
    public byte[] serialise() {
        byte[] retBytes;
        
        /* byte for ext (1bit), TON (3), NPI(4) */
        byte newByte = (byte)0x80;  //set ext to 1 - first bit (MSB)
        
        newByte = (byte)(newByte | (this.typeOfNumber.value << 4));
        newByte = (byte)(newByte | this.numberingPlanIdentification.value);
//        log.debug("byte of address is [{}] - ton[{}], npi[{}]", Integer.toHexString(newByte), this.typeOfNumber.value, this.numberingPlanIdentification.value);
        
        byte[] addressBytes = Utils.numberToBcd(digits);
        if (digits == null || (digits.length() == 0)) {
            this.length = 0;
            retBytes = new byte[1/*length byte*/];
            retBytes[0] = (byte)length;
        } else {
            this.length = 1/*TON/NPI byte*/ + addressBytes.length;
            retBytes = new byte[this.length + 1/*length byte*/];
            retBytes[0] = (byte)length;
            retBytes[1] = newByte;
        
            /* populate BCD if the number */
            for (int i=2, j=0; j < addressBytes.length; j++, i++) {
                retBytes[i] = addressBytes[j];
            }
        }

        return retBytes;
    }
    
    public TON getTypeOfNumber() {
        return typeOfNumber;
    }

    public NPI getNumberingPlanIdentification() {
        return numberingPlanIdentification;
    }

    public AddressType getAddessType() {
        return addessType;
    }

    public String getDigits() {
        return digits;
    }
    
    /* currently we only support E.164 International number formats */
    public void setAddress(String number) {
        this.digits = number;
        this.typeOfNumber = TON.INTERNATIONAL;
        this.numberingPlanIdentification = NPI.ISDN;
    }

}
