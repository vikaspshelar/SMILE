/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms;

/**
 *
 * @author jaybeepee
 */
public class SmsConstants {
    /** User data text encoding code unit size */
    public static final int ENCODING_UNKNOWN = 0;
    public static final int ENCODING_7BIT = 1;
    public static final int ENCODING_8BIT = 2;
    public static final int ENCODING_16BIT = 3;

    /** The maximum number of payload septets per message */
    public static final int MAX_USER_DATA_SEPTETS = 160;

    /**
     * The maximum number of payload septets per message if a user data header
     * is present.  This assumes the header only contains the
     * CONCATENATED_8_BIT_REFERENCE element.
     */
    public static final int MAX_USER_DATA_SEPTETS_WITH_HEADER = 153;

    /**
     * This value is not defined in global standard. Only in Korea, this is used.
     */
    public static final int ENCODING_KSC5601 = 4;

    /** The maximum number of payload bytes per message */
    public static final int MAX_USER_DATA_BYTES = 140;

    /**
     * The maximum number of payload bytes per message if a user data header
     * is present.  This assumes the header only contains the
     * CONCATENATED_8_BIT_REFERENCE element.
     */
    public static final int MAX_USER_DATA_BYTES_WITH_HEADER = 134;

    /**
     * SMS Class enumeration.
     * See TS 23.038.
     */
    public enum MessageClass{
        UNKNOWN, CLASS_0, CLASS_1, CLASS_2, CLASS_3;
    }

    /**
     * Indicates unknown format SMS message.
     * @hide pending API council approval
     */
    public static final String FORMAT_UNKNOWN = "unknown";

    /**
     * Indicates a 3GPP format SMS message.
     * @hide pending API council approval
     */
    public static final String FORMAT_3GPP = "3gpp";

    /**
     * Indicates a 3GPP2 format SMS message.
     * @hide pending API council approval
     */
    public static final String FORMAT_3GPP2 = "3gpp2";

    /* SMS message MTI's according to 23.040 */
//    public static final int SMS_MTI_DELIVER = 0;
//    public static final int SMS_MTI_DELIVER_REPORT = 0;
//    public static final int SMS_MTI_SUBMIT = 1;
//    public static final int SMS_MTI_SUBMIT_REPORT = 1;
//    public static final int SMS_MTI_STATUS_REPORT = 2;
//    public static final int SMS_MTI_COMMAND = 2;
//    public static final int SMS_MTI_RESERVED = 3;
    
}
