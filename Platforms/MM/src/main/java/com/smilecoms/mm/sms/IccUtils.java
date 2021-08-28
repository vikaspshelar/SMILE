/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class IccUtils {
    private static final Logger log = LoggerFactory.getLogger(IccUtils.class);
    
    /**
     * Decodes a GSM-style BCD byte, returning an int ranging from 0-99.
     *
     * In GSM land, the least significant BCD digit is stored in the most
     * significant nibble.
     *
     * Out-of-range digits are treated as 0 for the sake of the time stamp,
     * because of this:
     *
     * TS 23.040 section 9.2.3.11 "if the MS receives a non-integer value in the
     * SCTS, it shall assume the digit is set to 0 but shall store the entire
     * field exactly as received"
     */
    public static int gsmBcdByteToInt(byte b) {
        int ret = 0;

        // treat out-of-range BCD values as 0
        if ((b & 0xf0) <= 0x90) {
            ret = (b >> 4) & 0xf;
        }

        if ((b & 0x0f) <= 0x09) {
            ret += (b & 0xf) * 10;
        }

        return ret;
    }

    public static byte gsmIntToBcdByte(int i) {
        byte retByte = 0x00;
        int digit;

        String number = Integer.toString(i);

        if (number.length() <= 2) {
            for (int j = 0; j < number.length(); j++) {
                digit = number.charAt(j);
                if (j == 0) {
                    if ((digit & 0xf0) <= 0x90) {
                        retByte = (byte) (((byte) (digit) >> 4) & 0xf);
                    }
                } else {
                    if ((digit & 0x0f) <= 0x09) {
                        retByte = (byte) (retByte | (byte) (((digit << 4) & 0xf)));
                    }
                }
            }
        } else {
            log.warn("number too big");
        }

        return retByte;
    }

    public static byte[] DecToBCDArray(long num) {
        int digits = 0;
        
        if (num == 0) {
            byte bcd[] = new byte[1];
            bcd[0]=0;
            return bcd;
        }

        long temp = num;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }

        int byteLen = digits % 2 == 0 ? digits / 2 : (digits + 1) / 2;
        boolean isOdd = digits % 2 != 0;

        byte bcd[] = new byte[byteLen];

        for (int i = 0; i < digits; i++) {
            byte tmp = (byte) (num % 10);

            if (i == digits - 1 && isOdd) {
                bcd[i / 2] = tmp;
            } else if (i % 2 == 0) {
                bcd[i / 2] = tmp;
            } else {
                byte foo = (byte) (tmp << 4);
                bcd[i / 2] |= foo;
            }
           num /= 10;
        }

        for (int i = 0; i < byteLen / 2; i++) {
            byte tmp = bcd[i];
            bcd[i] = bcd[byteLen - i - 1];
            bcd[byteLen - i - 1] = tmp;
        }

        for (int i=0; i< bcd.length; i++) {
            bcd[i] = (byte)(((bcd[i]&0xf0)>>4) | ((bcd[i]&0x0f)<<4));
        }
        return bcd;
    }
}
