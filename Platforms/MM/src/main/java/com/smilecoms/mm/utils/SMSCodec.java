/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.utils;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.smilecoms.commons.base.BaseUtils;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class SMSCodec {

    static final int[] GSM7CHARS = {
        0x0040, 0x00A3, 0x0024, 0x00A5, 0x00E8, 0x00E9, 0x00F9, 0x00EC,
        0x00F2, 0x00E7, 0x000A, 0x00D8, 0x00F8, 0x000D, 0x00C5, 0x00E5,
        0x0394, 0x005F, 0x03A6, 0x0393, 0x039B, 0x03A9, 0x03A0, 0x03A8,
        0x03A3, 0x0398, 0x039E, 0x00A0, 0x00C6, 0x00E6, 0x00DF, 0x00C9,
        0x0020, 0x0021, 0x0022, 0x0023, 0x00A4, 0x0025, 0x0026, 0x0027,
        0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,
        0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
        0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,
        0x00A1, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
        0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,
        0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
        0x0058, 0x0059, 0x005A, 0x00C4, 0x00D6, 0x00D1, 0x00DC, 0x00A7,
        0x00BF, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
        0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,
        0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
        0x0078, 0x0079, 0x007A, 0x00E4, 0x00F6, 0x00F1, 0x00FC, 0x00E0,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,};

    public static final byte DEFAULT_CODING_SCHEME = 0x00;
    public static final byte ASCII_CODING_SCHEME = 0x01;
    public static final byte ASCII_CODING_SCHEME_FLASH = 0x10;
    public static final byte ISO_8859_1_CODING_SCHEME = 0x03;
    public static final byte UTF_16_CODING_SCHEME = 0x08;
    public static final byte DATA_8_BIT_CODING_SCHEME = 0x04;
    // Our own byte codes for non SMPP
    public static final byte NOSMPP_UCS_2_CODING_SCHEME = 0x50;
    public static final byte NOSMPP_UTF_8_CODING_SCHEME = 0x51;

    public static final byte CLASS_2_8_BIT_DATA_CODING_SCHEME = 0x16;

    public static final byte CLASS_0_GSM_7_BIT_DATA_CODING_SCHEME = (byte) 0xf0;
    public static final byte CLASS_1_GSM_7_BIT_DATA_CODING_SCHEME = (byte) 0xf1;
    public static final byte CLASS_1B_GSM_7_BIT_DATA_CODING_SCHEME = (byte) 0xf2;
    public static final byte CLASS_1A_GSM_7_BIT_DATA_CODING_SCHEME = (byte) 0x11;
    
    public static final byte CLASS_1_GSM_7_BIT_A_DATA_CODING_SCHEME = (byte) 0x11;
    public static final byte CLASS_3_GSM_7_BIT_A_DATA_CODING_SCHEME = (byte) 0xf3;
    public static final byte CLASS_3_GSM_7_BIT_TE_SPECIFIC_DATA_CODING_SCHEME = (byte) 0x13;
    
    public static final byte CLASS_0_UCS2_DATA_CODING_SCHEME = (byte) 0x18;
    
    public static final byte NOT_DEFINED_2_DATA_CODING_SCHEME = (byte) 0x0d;

    public static final String ASCII = "ASCII";
    public static final String ISO_8859_1 = "ISO-8859-1";

    private static final Logger log = LoggerFactory.getLogger(SMSCodec.class);

    public static String decodeSmppDirect(byte[] message, String charSet) throws UnsupportedEncodingException {
        log.debug("Decoding message for SMPP with charSet [{}]", charSet);
        return CharsetUtil.decode(message, charSet);
    }

    public static String decodeSmpp(byte[] message, byte dataCodingScheme) throws UnsupportedEncodingException {
        log.debug("Decoding message for SMPP with DCS [{}]", dataCodingScheme);
        switch (dataCodingScheme) {
            case ASCII_CODING_SCHEME:
                String defaultName = BaseUtils.getProperty("env.mm.smsc.smpp.default.charset", CharsetUtil.NAME_ISO_8859_1);
                log.debug("Decoding use [{}]", defaultName);
                return CharsetUtil.decode(message, defaultName);
            case ISO_8859_1_CODING_SCHEME:
                return CharsetUtil.decode(message, CharsetUtil.NAME_ISO_8859_1);
            case UTF_16_CODING_SCHEME:
                return new String(message, "UTF-16");
            case DATA_8_BIT_CODING_SCHEME:
                String eightBitCodingName = BaseUtils.getProperty("env.mm.smsc.smpp.eight.bit.charset", CharsetUtil.NAME_GSM8);
                return CharsetUtil.decode(message, eightBitCodingName);
            default:
                return new String(message, "UTF-8");
        }
    }

    public static String decode(byte[] message, byte dataCodingScheme) throws UnsupportedEncodingException {
        switch (dataCodingScheme) {
            case 0x00:
            case 0x01:
                String defaultName = BaseUtils.getProperty("env.mm.smsc.default.charset", ISO_8859_1);
                if (CharsetUtil.map(defaultName) != null) {
                    return CharsetUtil.decode(message, defaultName);
                } else {
                    return new String(message, defaultName);
                }
            case 0x10:
                return new String(message);
            case ISO_8859_1_CODING_SCHEME:
                return CharsetUtil.decode(message, CharsetUtil.NAME_ISO_8859_1);
            case NOSMPP_UTF_8_CODING_SCHEME:
                return new String(message, "UTF-8");
            case UTF_16_CODING_SCHEME:
                return new String(message, "UTF-16");
            case NOSMPP_UCS_2_CODING_SCHEME:
                return CharsetUtil.decode(message, CharsetUtil.CHARSET_UCS_2);
            default:
                return new String(message, "UTF-8");
        }
    }

    public static byte[] encode(String message, byte dataCodingScheme) throws UnsupportedEncodingException {
        switch (dataCodingScheme) {
            case 0x00:
            case 0x10: //this will be used for post-call flash SMSs - must be encoded as ASCII
                return message.getBytes(BaseUtils.getProperty("env.mm.smsc.default.charset", ISO_8859_1));
            case 0x01:
                String defaultName = BaseUtils.getProperty("env.mm.smsc.default.charset", ISO_8859_1);
                if (CharsetUtil.map(defaultName) != null) {
                    return CharsetUtil.encode(message, defaultName);
                } else {
                    return message.getBytes(defaultName);
                }
            case ISO_8859_1_CODING_SCHEME:
                return CharsetUtil.encode(message, CharsetUtil.NAME_ISO_8859_1);
            case NOSMPP_UTF_8_CODING_SCHEME:
                return message.getBytes("UTF-8");
            case UTF_16_CODING_SCHEME:
                return message.getBytes("UTF-16");
            case NOSMPP_UCS_2_CODING_SCHEME:
                return CharsetUtil.encode(message, CharsetUtil.CHARSET_UCS_2);
            default:
                return message.getBytes("UTF-8");
        }
    }

    public static byte[] encodeSmppDirect(String message, String charSet) throws UnsupportedEncodingException {
        log.debug("Encoding message for SMPP using charSet [{}]", charSet);
        return CharsetUtil.encode(message, charSet);
    }

    public static byte[] encodeSmpp(String message, byte dataCodingScheme) throws UnsupportedEncodingException {
        log.debug("Encoding message for SMPP with [{}]", dataCodingScheme);
        switch (dataCodingScheme) {
            case 0x00:
            case ASCII_CODING_SCHEME:
                String defaultName = BaseUtils.getProperty("env.mm.smsc.smpp.default.charset", CharsetUtil.NAME_ISO_8859_1);
                log.debug("Encoding use [{}]", defaultName);
                return CharsetUtil.encode(message, defaultName);
            case UTF_16_CODING_SCHEME:
                return message.getBytes("UTF-16");
            default:
                return message.getBytes("UTF-8");
        }
    }
    
    public static int[] decode7bit(byte[] ud, int udhl, int fillBits) {
        final int[] upperBits = {
            0xFE, // 0 = B7|B6|B5|B4|B3|B2|B1
            0xFC, // 1 = B7|B6|B5|B4|B3|B2
            0xF8, // 2 = B7|B6|B5|B4|B3
            0xF0, // 3 = B7|B6|B5|B4
            0xE0, // 4 = B7|B6|B5
            0xC0, // 5 = B7|B6
            0x80 // 6 = B7 
        };

        final int[] lowerBits = {
            0x01, // 0 =                   B0
            0x03, // 1 =                B1|B0
            0x07, // 2 =             B2|B1|B0
            0x0F, // 3 =          B3|B2|B1|B0
            0x1F, // 4 =       B4|B3|B2|B1|B0
            0x3F, // 5 =    B5|B4|B3|B2|B1|B0
            0x7F // 6 = B6|B5|B4|B3|B2|B1|B0
        };

        
        final int length = ud.length - udhl;
        if (fillBits != 0) {
            int xint;
            int xintplus1;
            final int len = length - 1;
            final int cut = lowerBits[fillBits - 1];
            final int move = 8 - fillBits;
            for (int f = udhl; f < (ud.length-1); f++) {
                xint = ud[f] & 0xff;
                xintplus1 = ud[f+1] & 0xff;
                xint >>= fillBits;
                xint |= (xintplus1 & cut) << move;
                ud[f] = (byte) xint;
                log.debug("setting ud[{}] to [{}] : [{}]",new Object[]{f, ud[f], Integer.toBinaryString(ud[f])});
            }
            xint = ud[ud.length-1] & 0xff;
            xint >>= fillBits;
            ud[ud.length-1] = (byte)xint;
        }

        int udl = length * 8 / 7; // number of septets
        final int[] output = new int[udl];

        int b = 6, p = udhl;
        for (int i = 0; i < udl; i++) {
            switch (b) {
                case 7: // U0
                    output[i] = (ud[p] & upperBits[0]) >> 1;
                    break;

                case 6: // L6
                    output[i] = ud[p] & lowerBits[b];
                    break;

                default: // The rest
                    output[i] = ((ud[p] & lowerBits[b]) << (6 - b))
                            + ((ud[p - 1] & upperBits[b + 1]) >> (b + 2));
                    break;
            }

            if (--b == -1) {
                b = 7;
            } else {
                p++;
            }
        }

        return output;
    }

    public static String decodeMultiPartUserData(byte[] userData) {

        StringBuilder decoded = new StringBuilder();
        if (BaseUtils.getBooleanProperty("env.mm.new.dcs.jasongsm7", false)) {
            int udhl = userData[0] + 1;
            int fill = 7 - ((udhl * 8) % 7);
            
            log.debug("UDL length for multipart message is [{}] and fill bits is [{}] and userdata hdr len is [{}]", new Object[]{userData.length, fill, udhl});
            int[]result = decode7bit(userData, udhl, fill);
            for (int i =0; i < result.length; i++) {
                decoded.append("").append((char) GSM7CHARS[result[i]]);
            }
        } else {
            String hex = bytesToHex(userData);
            StringBuilder binary = new StringBuilder();
            int length = hex.length();
            for (int i = length; i >= 2; i -= 2) {
                String twos = hex.substring((i - 2), i);//2 characters at a time on reverse direction
                binary.append(fromHexTo8BitBinary(twos));//Convert to 8 bit binary
            }

            //remove first 49 bits (UDH and 49th bit) (actually last 49 bits as loop above reversed this!)
            char bit49 = binary.charAt(binary.length() - 49);
            binary = binary.delete(binary.length() - 49, binary.length());

            //now append then 6 0's then the 49th bit for the 7 bit alignment
            binary = binary.append("000000");
            binary = binary.append(bit49);

            int gsm7Length = GSM7CHARS.length;
            for (int i = binary.length(); i >= 7; i -= 7) {

                //need to do the following:
                String seven = binary.substring((i - 7), i);//Chop into 7 bits binary in reverse direction
                int decimalOfSeven = Integer.parseInt(seven, 2);

                if (BaseUtils.getBooleanProperty("env.mm.new.dcs.1100", false)) {
                    if (decimalOfSeven >= 0 && decimalOfSeven < GSM7CHARS.length) {
                        decoded.append("").append((char) GSM7CHARS[decimalOfSeven]);//Do translation
                    } else {
                        String msg = String.format("Have received character [%d] in message that is not is GSM7CHARs map - so will not add it to message", decimalOfSeven);
                        log.warn(msg);
                        BaseUtils.sendTrapToOpsManagement(BaseUtils.MINOR, "SMPP", msg);
                    }

                } else {
                    for (int j = 0; j < gsm7Length; j++) {
                        if (GSM7CHARS[j] == decimalOfSeven) {
                            decoded.append("").append((char) GSM7CHARS[j]);//Do translation
                        }
                    }
                }
            }
        }

        return decoded.toString();
    }

    private static String fromHexTo8BitBinary(String hex) {
        StringBuilder ret = new StringBuilder();
        String binary = Integer.toBinaryString(Integer.parseInt(hex, 16));//Convert hex to binary
        int length = 8 - binary.length();
        for (int i = 0; i < length; i++) {
            ret.append("0");//Append missing 0's
        }
        ret.append(binary);
        return ret.toString();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
