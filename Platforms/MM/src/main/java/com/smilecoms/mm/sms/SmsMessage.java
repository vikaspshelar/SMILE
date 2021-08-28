/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import static com.smilecoms.mm.sms.SmsConstants.ENCODING_UNKNOWN;
import static com.smilecoms.mm.sms.SmsConstants.ENCODING_16BIT;
import static com.smilecoms.mm.sms.SmsConstants.ENCODING_7BIT;
import static com.smilecoms.mm.sms.SmsConstants.ENCODING_8BIT;
import static com.smilecoms.mm.sms.SmsConstants.ENCODING_KSC5601;
import com.smilecoms.mm.sms.SmsConstants.MessageClass;
import com.smilecoms.mm.utils.SMSCodec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class SmsMessage {

    private static final Logger log = LoggerFactory.getLogger(SmsMessage.class);
    private MessageClass messageClass;
    private MessageType messageType;
    private MessageDirection messageDirection;
    private int mPart;
    private int mParts;
    private int mMessageID;

    public enum MessageType {

        SMS_DELIVER, SMS_DELIVER_REPORT, SMS_STATUSREPORT, SMS_COMMAND, SMS_SUBMIT, SMS_SUBMITREPORT
    };

    public enum MessageDirection {

        INCOMING, OUTGOING
    };

    private SmsMessage() {
        //default to incoming SMS-SUBMIT
        this(MessageDirection.INCOMING, MessageType.SMS_SUBMIT, "", null);
    }

    public SmsMessage(MessageDirection messageDirection, MessageType messageType, String recipientNumber, Date msgDate) {
        this.messageDirection = messageDirection;
        this.messageType = messageType;
        this.mScts = (msgDate == null ? new Date() : msgDate);
        if (recipientNumber != null) {
            this.mRecipientAddress = new GsmSmsAddress(recipientNumber);
        }
        setMti();
    }

    private void setMti() {

        switch (messageDirection) {
            case INCOMING: {
                switch (messageType) {
                    case SMS_COMMAND:
                        mMti = 2;
                        break;
                    case SMS_DELIVER:
                        mMti = 0;
                        break;
                    case SMS_SUBMIT:
                        mMti = 1;
                        break;
                    case SMS_DELIVER_REPORT:
                    case SMS_STATUSREPORT:
                    case SMS_SUBMITREPORT:
                    default:
                        mMti = -1;
                }
            }
            case OUTGOING: {
                switch (messageType) {

                    case SMS_DELIVER:
                        mMti = 0;
                        break;
                    case SMS_STATUSREPORT:
                        mMti = 2;
                        break;
                    case SMS_SUBMITREPORT:
                        mMti = 1;
                        break;
                    case SMS_DELIVER_REPORT:
                    case SMS_COMMAND:
                    case SMS_SUBMIT:
                    default:
                        mMti = -1;
                }
            }
        }
    }

    /**
     * {
     *
     * @hide} The address of the SMSC. May be null
     */
    protected String mScAddress;
    /**
     * TP-Message-Type-Indicator 9.2.3
     */
    private int mMti;
    /**
     * TP-Message-Reference - Message Reference of sent message. @hide
     */
    public int mMessageRef;

    /**
     * TP-Parameter-Indicator - octet indicating which headers are present DCS,
     * etc.
     */
    private int mParameterIndicator;

    /**
     * indicates if userdata is SMS only or also contains header
     */
    private int mUserDataHeaderIndicator;

    /**
     * current Service Centre (SC) Timestamp SCTS
     */
    private Date mScts;

    /**
     * TP-Protocol-Identifier (TP-PID)
     */
    private int mProtocolIdentifier;
    // TP-Data-Coding-Scheme
    // see TS 23.038
    private int mDataCodingScheme;

    public int getmDataCodingScheme() {
        return mDataCodingScheme;
    }

    public void setmDataCodingScheme(int mDataCodingScheme) {
        log.debug("Setting smsmessage data coding scheme to [{}]", mDataCodingScheme);
        this.mDataCodingScheme = mDataCodingScheme;
    }
    // TP-Reply-Path
    // e.g. 23.040 9.2.2.1
    private boolean mReplyPathPresent = false;
    /**
     * The address of the receiver.
     */
    private GsmSmsAddress mRecipientAddress;
    /**
     * TP-Status - status of a previously submitted SMS. This field applies to
     * SMS-STATUS-REPORT messages. 0 indicates success; see TS 23.040, 9.2.3.15
     * for description of other possible values.
     */
    private int mStatus;
    /**
     * TP-Status - status of a previously submitted SMS. This field is true iff
     * the message is a SMS-STATUS-REPORT message.
     */
    private final boolean mIsStatusReportMessage = false;

    /**
     * {
     *
     * @hide} The raw bytes for the user data section of the message
     */
    protected byte[] mUserData;

    /**
     * {
     *
     * @hide}
     */
    protected SmsHeader mUserDataHeader;

    // "Message Waiting Indication Group"
    // 23.038 Section 4
    /**
     * {
     *
     * @hide}
     */
    protected boolean mIsMwi;

    /**
     * {
     *
     * @hide}
     */
    protected boolean mMwiSense;

    /**
     * {
     *
     * @hide}
     */
    protected boolean mMwiDontStore;

    /**
     * {
     *
     * @hide} The message body as a string. May be null if the message isn't
     * text
     */
    protected String mMessageBody;

    /**
     * Create an SmsMessage from a raw PDU.
     */
    public static SmsMessage createFromPdu(byte[] pdu, int offset) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(pdu, offset);
            return msg;
        } catch (RuntimeException ex) {
            log.error("SMS PDU parsing failed: " + ex);
            log.warn("Error: ", ex);
            return null;
        } catch (OutOfMemoryError e) {
            log.error("SMS PDU parsing failed with out of memory: " + e);
            return null;
        }
    }
    private int mValidityPeriodLength;

    /**
     * TS 27.005 3.1, &lt;pdu&gt; definition "In the case of SMS: 3GPP TS 24.011
     * [6] SC address followed by 3GPP TS 23.040 [3] TPDU in hexadecimal format:
     * ME/TA converts each octet of TP data unit into two IRA character long hex
     * number (e.g. octet with integer value 42 is presented to TE as two
     * characters 2A (IRA 50 and 65))" ...in the case of cell broadcast,
     * something else...
     */
    private void parsePdu(byte[] pdu, int offset) {
        PduParser p = new PduParser(pdu, offset);

        if (mScAddress != null) {
            log.debug("MMSIP: SMS SC address: [{}]", mScAddress);
        }

        // TP-Message-Type-Indicator
        // 9.2.3
        int firstByte = p.getByte();

        log.debug("MMSIP: first byte is [{}]", firstByte);

        mMti = firstByte & 0x3;
        log.debug("MMSIP: MTI is [{}]", mMti);
        switch (mMti) {
            // TP-Message-Type-Indicator
            // 9.2.3
            case 0:
            case 3: //GSM 03.40 9.2.3.1: MTI == 3 is Reserved.
                //This should be processed in the same way as MTI == 0 (Deliver)
//                parseSmsDeliver(p, firstByte);
                break;
            case 1:
                parseSmsSubmit(p, firstByte);
                break;
            case 2:
//                parseSmsStatusReport(p, firstByte);
                break;
            default:
                // TODO(mkf) the rest of these
                throw new RuntimeException("Unsupported message type");
        }
    }
    /**
     * GSM 7 bit table
     */
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
    
    static final int[] GSM7CHARS_DEFAULT = {
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, 0x000C, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, 0x005E, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        0x007B, 0x007D, -1, -1, -1, -1, -1, 0x005C,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, 0x005B, 0x007E, 0x005D, -1,
        0x007C, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, 0x20AC, -1, -1,
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
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
    };
    
    public byte[] encodeGSM7Alphabet(byte[] ascci) {
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int length = ascci.length;
        int gsm7Length = GSM7CHARS.length;
        for (int i = 0; i < length; i++) {
            
            int c = (int)ascci[i];
//            System.out.println("processing " + (c & 0xff));
            
            //check if it's a special char
            switch (c) {
                
            }
            for (int j = 0; j < gsm7Length; j++) {
                
                if ((char) GSM7CHARS[j] == (char)(c & 0xff)) {
//                    System.out.println("matched " + c + " at pos " + j);
                    int num = GSM7CHARS[j];
                    System.out.println(num);
                    sb.append(j);
                    os.write(j);
                    break;
                }
                if ((char) GSM7CHARS_DEFAULT[j] == (char)(c & 0xff)) {
//                    System.out.println("matched special" + c + " at pos " + j);
                    int num = GSM7CHARS_DEFAULT[j];
//                    System.out.println(num);
                    os.write(0x1b); //escape char
                    os.write(j);
                    break;
                }
            }
        }
        return os.toByteArray();
    }
    

    private byte[] serialiseSmsDeliverMessage() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int udLength = 0;
        byte[] messageBytes;
        
        log.debug("serialising VOLTE message with DCS [{}] and messageBytes [{}]", new Object[] {this.getmDataCodingScheme(), this.mUserData});
        
        //if we are going to send UCS 2 then we need to reencode to UCS2
        if (this.getmDataCodingScheme() != 0 && this.getmDataCodingScheme() != 16) {
            try {
                log.debug("going to re-encode as UCS2 as DCS is [{}] and current bytes are [{}]", new Object[]{this.getmDataCodingScheme(), this.mUserData});
                this.mUserData = SMSCodec.encode(new String(this.mUserData), SMSCodec.NOSMPP_UCS_2_CODING_SCHEME);
                log.debug("after reencode bytes are [{}]", this.mUserData);
            } catch (UnsupportedEncodingException ex) {
                log.error("unable to encode string into UCS2");
                log.warn("Error: ", ex);
            }
        }
        udLength = this.getmUserData().length;
        
        try {
            if (messageDirection != messageDirection.OUTGOING) {
                log.debug("MMSIP: serialising of incoming messages is not supported - direction is [{}]", messageDirection);
                return null;
            }

            int myByte = mMti;
            if (mUserDataHeaderIndicator == 1) {
                log.debug("Setting userdata header for concatenated message");
                myByte |= 1 << 6;
                if (this.getmDataCodingScheme() == 0 || this.getmDataCodingScheme() == 16)
                    udLength = 7 /*7 heptets of header*/;
                else 
                    udLength = 6;
            }
            myByte |= 1 << 2; /* MMS  - no more messages waiting */

            os.write((byte) myByte);
            os.write(mRecipientAddress.serialise());
            os.write((byte) mParameterIndicator);
            os.write((byte) mDataCodingScheme);
            /*add SCTS*/
            Calendar cal = Calendar.getInstance();
            cal.setTime(mScts);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.YEAR) % 1000)[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.MONTH) + 1)[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.DAY_OF_MONTH))[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.HOUR_OF_DAY))[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.MINUTE))[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.SECOND))[0]);
            os.write(IccUtils.DecToBCDArray(8)[0]);

            messageBytes = this.getmUserData();
            
            if (mUserDataHeaderIndicator == 1) {
                if (this.getmDataCodingScheme() == 0 || this.getmDataCodingScheme() == 16){ 
                    messageBytes = encodeGSM7Alphabet(messageBytes);
                    udLength += messageBytes.length; //in this coding scheme length should be header plus the number of chars (ie 7 bit chars .... not bytes)
                    byte[] newMessageBytes = new byte[messageBytes.length + 1];
                    newMessageBytes[0] = 0x0;
                    System.arraycopy(messageBytes, 0, newMessageBytes, 1, messageBytes.length);
                    messageBytes = newMessageBytes;
                    messageBytes = PduBitPacker.PackBytes(messageBytes, mUserDataHeaderIndicator);
                } else {
                    udLength += messageBytes.length;
                }
                os.write((byte) udLength);
                os.write((byte) 0x05);
                os.write((byte) 0x00);
                os.write((byte) 0x03);
                os.write((byte) this.mMessageID);
                os.write((byte) this.mParts);
                os.write((byte) this.mPart);
            } else {
                if (this.getmDataCodingScheme() == 0 || this.getmDataCodingScheme() == 16){
                    messageBytes = encodeGSM7Alphabet(messageBytes);
                    udLength = messageBytes.length;
                    messageBytes = PduBitPacker.PackBytes(messageBytes, mUserDataHeaderIndicator);
                } else {
                    udLength = messageBytes.length;
                }
                os.write((byte) udLength);
            }
            //If the TP User Data is coded using the GSM 7 bit default alphabet, the TP User Data Length field gives an integer representation of the number of septets within the TP User Data field to follow.
            os.write(messageBytes);
        } catch (IOException ex) {
            log.error("MMSIP: failed to serialise SmsMessage " + ex.getLocalizedMessage());
            log.warn("Error: ", ex);
        } catch (Exception ex) {
            log.error("MMSIP: failed to serialise SmsMessage " + ex.getLocalizedMessage());
            log.warn("Error: ", ex);
        }

        return os.toByteArray();
    }

    public byte[] serialise() {

        if (messageType == messageType.SMS_DELIVER) {
            return serialiseSmsDeliverMessage();
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            if (messageDirection != messageDirection.OUTGOING) {
                log.debug("MMSIP: serialising of incoming messages is not supported - direction is [{}]", messageDirection);
                return null;
            }

            int myByte = mMti;
            if (mUserDataHeaderIndicator == 1) {
                myByte |= 1 << 4;
            }
            os.write((byte) myByte);
            if (messageType != messageType.SMS_SUBMITREPORT) {
                os.write((byte) mMessageRef);
            }
            os.write((byte) mParameterIndicator);

            /*add SCTS*/
            Calendar cal = Calendar.getInstance();
            cal.setTime(mScts);

            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.YEAR) % 1000)[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.MONTH) + 1)[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.DAY_OF_MONTH))[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.HOUR_OF_DAY))[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.MINUTE))[0]);
            os.write(IccUtils.DecToBCDArray(cal.get(Calendar.SECOND))[0]);
            os.write(IccUtils.DecToBCDArray(8)[0]);
        } catch (Exception ex) {
            log.error("MMSIP: failed to serialise SmsMessage " + ex.getLocalizedMessage());
            log.warn("Error: ", ex);
        }

        return os.toByteArray();
    }

    public void setText(String messageText, int part, int parts, int messageID) {

        if (parts > 1) {
            log.debug("setting message part [{}] of [{}] with messageID [{}]", new Object[]{part, parts, messageID});
            this.mUserDataHeaderIndicator = 1;
            this.mPart = part;
            this.mParts = parts;
            this.mMessageID = messageID;
        }

        this.mMessageBody = messageText;
    }
    
    public void setMessageInfo(int part, int parts, int messageID) {
        if (parts > 1) {
            log.debug("setting message part [{}] of [{}] with messageID [{}]", new Object[]{part, parts, messageID});
            this.mUserDataHeaderIndicator = 1;
            this.mPart = part;
            this.mParts = parts;
            this.mMessageID = messageID;
        }
    }

    private static class PduParser {
        byte mPdu[];
        int mCur;
        SmsHeader mUserDataHeader;
        byte[] mUserData;
        int mUserDataSeptetPadding;

        PduParser(byte[] pdu, int offset) {
            mPdu = pdu;
            mCur = offset;
            mUserDataSeptetPadding = 0;
        }

        /**
         * Parse and return the SC address prepended to SMS messages coming via
         * the TS 27.005 / AT interface. Returns null on invalid address
         */
        String getSCAddress() {
            int len;
            String ret;

            // length of SC Address
            len = getByte();

            if (len == 0) {
                // no SC address
                log.warn("MMSIP: No SC Address");
                ret = null;
            } else {
                // SC address
                try {
                    ret = PhoneNumberUtils
                            .calledPartyBCDToString(mPdu, mCur, len);
                } catch (RuntimeException tr) {
                    ret = null;
                }
            }

            mCur += len;

            return ret;
        }

        /**
         * returns non-sign-extended byte value
         */
        int getByte() {
            return mPdu[mCur++] & 0xff;
        }

        /**
         * Any address except the SC address (eg, originating address) See TS
         * 23.040 9.1.2.5
         */
        GsmSmsAddress getAddress() {
            GsmSmsAddress ret;

            // "The Address-Length field is an integer representation of
            // the number field, i.e. excludes any semi-octet containing only
            // fill bits."
            // The TOA field is not included as part of this
            int addressLength = mPdu[mCur] & 0xff;
            int lengthBytes = 2 + (addressLength + 1) / 2;

            try {
                ret = new GsmSmsAddress(mPdu, mCur, lengthBytes);
            } catch (ParseException e) {
                ret = null;
                //This is caught by createFromPdu(byte[] pdu)
                throw new RuntimeException(e.getMessage());
            }

            mCur += lengthBytes;

            return ret;
        }

        /**
         * Parses an SC timestamp and returns a currentTimeMillis()-style
         * timestamp
         */
        long getSCTimestampMillis() {
            // TP-Service-Centre-Time-Stamp
            int year = IccUtils.gsmBcdByteToInt(mPdu[mCur++]);
            int month = IccUtils.gsmBcdByteToInt(mPdu[mCur++]);
            int day = IccUtils.gsmBcdByteToInt(mPdu[mCur++]);
            int hour = IccUtils.gsmBcdByteToInt(mPdu[mCur++]);
            int minute = IccUtils.gsmBcdByteToInt(mPdu[mCur++]);
            int second = IccUtils.gsmBcdByteToInt(mPdu[mCur++]);

            // For the timezone, the most significant bit of the
            // least significant nibble is the sign byte
            // (meaning the max range of this field is 79 quarter-hours,
            // which is more than enough)
            byte tzByte = mPdu[mCur++];

            // Mask out sign bit.
            int timezoneOffset = IccUtils.gsmBcdByteToInt((byte) (tzByte & (~0x08)));

            timezoneOffset = ((tzByte & 0x08) == 0) ? timezoneOffset : -timezoneOffset;

            Time time = new Time(Time.TIMEZONE_UTC);

            // It's 2006.  Should I really support years < 2000?
            time.year = year >= 90 ? year + 1900 : year + 2000;
            time.month = month - 1;
            time.monthDay = day;
            time.hour = hour;
            time.minute = minute;
            time.second = second;

            // Timezone offset is in quarter hours.
            return time.toMillis(true) - (timezoneOffset * 15 * 60 * 1000);
        }

        /**
         * Pulls the user data out of the PDU, and separates the payload from
         * the header if there is one.
         *
         * @param hasUserDataHeader true if there is a user data header
         * @param dataInSeptets true if the data payload is in septets instead
         * of octets
         * @return the number of septets or octets in the user data payload
         */
        int constructUserData(boolean hasUserDataHeader, boolean dataInSeptets) {
            int offset = mCur;
            int userDataLength = mPdu[offset++] & 0xff;
            int headerSeptets = 0;
            int userDataHeaderLength = 0;

            if (hasUserDataHeader) {
                userDataHeaderLength = mPdu[offset++] & 0xff;

                byte[] udh = new byte[userDataHeaderLength];
                System.arraycopy(mPdu, offset, udh, 0, userDataHeaderLength);
                mUserDataHeader = SmsHeader.fromByteArray(udh);
                offset += userDataHeaderLength;

                int headerBits = (userDataHeaderLength + 1) * 8;
                headerSeptets = headerBits / 7;
                headerSeptets += (headerBits % 7) > 0 ? 1 : 0;
                mUserDataSeptetPadding = (headerSeptets * 7) - headerBits;
            }

            int bufferLen;
            if (dataInSeptets) {
                /*
                 * Here we just create the user data length to be the remainder of
                 * the pdu minus the user data header, since userDataLength means
                 * the number of uncompressed septets.
                 */
                bufferLen = mPdu.length - offset;
            } else {
                /*
                 * userDataLength is the count of octets, so just subtract the
                 * user data header.
                 */
                bufferLen = userDataLength - (hasUserDataHeader ? (userDataHeaderLength + 1) : 0);
                if (bufferLen < 0) {
                    bufferLen = 0;
                }
            }

            mUserData = new byte[bufferLen];
            System.arraycopy(mPdu, offset, mUserData, 0, mUserData.length);
            mCur = offset;

            if (dataInSeptets) {
                // Return the number of septets
                int count = userDataLength - headerSeptets;
                // If count < 0, return 0 (means UDL was probably incorrect)
                return count < 0 ? 0 : count;
            } else {
                // Return the number of octets
                return mUserData.length;
            }
        }

        /**
         * Returns the user data payload, not including the headers
         *
         * @return the user data payload, not including the headers
         */
        byte[] getUserData() {
            return mUserData;
        }

        /**
         * Returns an object representing the user data headers
         *
         * {
         *
         * @hide}
         */
        SmsHeader getUserDataHeader() {
            return mUserDataHeader;
        }

        /**
         * Interprets the user data payload as packed GSM 7bit characters, and
         * decodes them into a String.
         *
         * @param septetCount the number of septets in the user data payload
         * @return a String with the decoded characters
         */
        String getUserDataGSM7Bit(int septetCount, int languageTable,
                int languageShiftTable) {
            String ret;

            ret = GsmAlphabet.gsm7BitPackedToString(mPdu, mCur, septetCount,
                    mUserDataSeptetPadding, languageTable, languageShiftTable);

            mCur += (septetCount * 7) / 8;

            return ret;
        }

        /**
         * Interprets the user data payload as UCS2 characters, and decodes them
         * into a String.
         *
         * @param byteCount the number of bytes in the user data payload
         * @return a String with the decoded characters
         */
        String getUserDataUCS2(int byteCount) {
            String ret;

            try {
                ret = new String(mPdu, mCur, byteCount, "utf-16");
            } catch (UnsupportedEncodingException ex) {
                ret = "";
            }

            mCur += byteCount;
            return ret;
        }

        /**
         * Interprets the user data payload as KSC-5601 characters, and decodes
         * them into a String.
         *
         * @param byteCount the number of bytes in the user data payload
         * @return a String with the decoded characters
         */
        String getUserDataKSC5601(int byteCount) {
            String ret;

            try {
                ret = new String(mPdu, mCur, byteCount, "KSC5601");
            } catch (UnsupportedEncodingException ex) {
                ret = "";
            }

            mCur += byteCount;
            return ret;
        }

        boolean moreDataPresent() {
            return (mPdu.length > mCur);
        }
    }

    /**
     * Parses a SMS-SUBMIT message.
     *
     * @param p A PduParser, cued past the first byte.
     * @param firstByte The first byte of the PDU, which contains MTI, etc.
     */
    private void parseSmsSubmit(PduParser p, int firstByte) {
        mReplyPathPresent = (firstByte & 0x80) == 0x80;

        // TP-MR (TP-Message Reference)
        mMessageRef = p.getByte();
        log.debug("MMSIP: messageRef byte is [{}]", mMessageRef);

        mRecipientAddress = p.getAddress();

        if (mRecipientAddress != null) {
            log.debug("MMSIP: SMS recipient address: [{}]", mRecipientAddress.address);
        }

        // TP-Protocol-Identifier (TP-PID)
        // TS 23.040 9.2.3.9
        mProtocolIdentifier = p.getByte();
        log.debug("MMSIP: protocol identifier byte is [{}]", mProtocolIdentifier);

        // TP-Data-Coding-Scheme
        // see TS 23.038
        mDataCodingScheme = p.getByte();

        log.debug("MMSIP: SMS TP-PID: [{}], data coding scheme DCS: [{}]", new Object[]{mProtocolIdentifier, String.format("%02X", mDataCodingScheme)});

        // TP-Validity-Period-Format
        int validityPeriodLength = 0;
        int validityPeriodFormat = ((firstByte >> 3) & 0x3);
        if (0x0 == validityPeriodFormat) /* 00, TP-VP field not present*/ {
            validityPeriodLength = 0;
        } else if (0x2 == validityPeriodFormat) /* 10, TP-VP: relative format*/ {
            validityPeriodLength = 1;
        } else /* other case, 11 or 01, TP-VP: absolute or enhanced format*/ {
            validityPeriodLength = 7;
        }
        this.mValidityPeriodLength = validityPeriodLength;

        // TP-Validity-Period is not used on phone, so just ignore it for now.
        while (validityPeriodLength-- > 0) {
            p.getByte();
        }

        boolean hasUserDataHeader = (firstByte & 0x40) == 0x40;

        parseUserData(p, hasUserDataHeader);
    }

    /**
     * Parses the User Data of an SMS.
     *
     * @param p The current PduParser.
     * @param hasUserDataHeader Indicates whether a header is present in the
     * User Data.
     */
    private void parseUserData(PduParser p, boolean hasUserDataHeader) {
        boolean hasMessageClass = false;
        boolean userDataCompressed = false;

        int encodingType = ENCODING_UNKNOWN;

        // Look up the data encoding scheme
        if ((mDataCodingScheme & 0x80) == 0) {
            userDataCompressed = (0 != (mDataCodingScheme & 0x20));
            hasMessageClass = (0 != (mDataCodingScheme & 0x10));

            if (userDataCompressed) {
//                Rlog.w(LOG_TAG, "4 - Unsupported SMS data coding scheme "
//                        + "(compression) " + (mDataCodingScheme & 0xff));
            } else {
                switch ((mDataCodingScheme >> 2) & 0x3) {
                    case 0: // GSM 7 bit default alphabet
                        encodingType = ENCODING_7BIT;
                        break;

                    case 2: // UCS 2 (16bit)
                        encodingType = ENCODING_16BIT;
                        break;

                    case 1: // 8 bit data
                    case 3: // reserved
//                    Rlog.w(LOG_TAG, "1 - Unsupported SMS data coding scheme "
//                            + (mDataCodingScheme & 0xff));
                        encodingType = ENCODING_8BIT;
                        break;
                }
            }
        } else if ((mDataCodingScheme & 0xf0) == 0xf0) {
            hasMessageClass = true;
            userDataCompressed = false;

            if (0 == (mDataCodingScheme & 0x04)) {
                // GSM 7 bit default alphabet
                encodingType = ENCODING_7BIT;
            } else {
                // 8 bit data
                encodingType = ENCODING_8BIT;
            }
        } else if ((mDataCodingScheme & 0xF0) == 0xC0
                || (mDataCodingScheme & 0xF0) == 0xD0
                || (mDataCodingScheme & 0xF0) == 0xE0) {
            // 3GPP TS 23.038 V7.0.0 (2006-03) section 4

            // 0xC0 == 7 bit, don't store
            // 0xD0 == 7 bit, store
            // 0xE0 == UCS-2, store
            if ((mDataCodingScheme & 0xF0) == 0xE0) {
                encodingType = ENCODING_16BIT;
            } else {
                encodingType = ENCODING_7BIT;
            }

            userDataCompressed = false;
            boolean active = ((mDataCodingScheme & 0x08) == 0x08);

            // bit 0x04 reserved
            if ((mDataCodingScheme & 0x03) == 0x00) {
                mIsMwi = true;
                mMwiSense = active;
                mMwiDontStore = ((mDataCodingScheme & 0xF0) == 0xC0);
            } else {
                mIsMwi = false;

//                Rlog.w(LOG_TAG, "MWI for fax, email, or other "
//                        + (mDataCodingScheme & 0xff));
            }
        } else if ((mDataCodingScheme & 0xC0) == 0x80) {
            // 3GPP TS 23.038 V7.0.0 (2006-03) section 4
            // 0x80..0xBF == Reserved coding groups
            if (mDataCodingScheme == 0x84) {
                // This value used for KSC5601 by carriers in Korea.
                encodingType = ENCODING_KSC5601;
            } else {
//                Rlog.w(LOG_TAG, "5 - Unsupported SMS data coding scheme "
//                        + (mDataCodingScheme & 0xff));
            }
        } else {
//            Rlog.w(LOG_TAG, "3 - Unsupported SMS data coding scheme "
//                    + (mDataCodingScheme & 0xff));
        }

        // set both the user data and the user data header.
        int count = p.constructUserData(hasUserDataHeader,
                encodingType == ENCODING_7BIT);
        this.mUserData = p.getUserData();
        this.mUserDataHeader = p.getUserDataHeader();

        switch (encodingType) {
            case ENCODING_UNKNOWN:
            case ENCODING_8BIT:
                mMessageBody = null;
                break;

            case ENCODING_7BIT:
                mMessageBody = p.getUserDataGSM7Bit(count,
                        hasUserDataHeader ? mUserDataHeader.languageTable : 0,
                        hasUserDataHeader ? mUserDataHeader.languageShiftTable : 0);
                break;

            case ENCODING_16BIT:
                mMessageBody = p.getUserDataUCS2(count);
                break;

            case ENCODING_KSC5601:
                mMessageBody = p.getUserDataKSC5601(count);
                break;
        }

        if (mMessageBody != null) {
            parseMessageBody();
        }

        if (!hasMessageClass) {
            messageClass = MessageClass.UNKNOWN;
        } else {
            switch (mDataCodingScheme & 0x3) {
                case 0:
                    messageClass = MessageClass.CLASS_0;
                    break;
                case 1:
                    messageClass = MessageClass.CLASS_1;
                    break;
                case 2:
                    messageClass = MessageClass.CLASS_2;
                    break;
                case 3:
                    messageClass = MessageClass.CLASS_3;
                    break;
            }
        }
    }

    protected void parseMessageBody() {
        // originatingAddress could be null if this message is from a status
        // report.
//        if (mOriginatingAddress != null && mOriginatingAddress.couldBeEmailGateway()) {
//            extractEmailAddressFromMessageBody();
//        }
    }

    public int getmMti() {
        return mMti;
    }

//    public void setmMti(int mMti) {
//        this.mMti = mMti;
//    }
    public int getmMessageRef() {
        return mMessageRef;
    }

    public void setmMessageRef(int mMessageRef) {
        this.mMessageRef = mMessageRef;
    }

    public int getmProtocolIdentifier() {
        return mProtocolIdentifier;
    }

    public void setmProtocolIdentifier(int mProtocolIdentifier) {
        this.mProtocolIdentifier = mProtocolIdentifier;
    }

    public byte[] getmUserData() {
        return mUserData;
    }

    public void setmUserData(byte[] mUserData) {
        
        this.mUserData = mUserData;
    }

    public SmsHeader getmUserDataHeader() {
        return mUserDataHeader;
    }

    public void setmUserDataHeader(SmsHeader mUserDataHeader) {
        this.mUserDataHeader = mUserDataHeader;
    }

    public int getmValidityPeriodLength() {
        return mValidityPeriodLength;
    }

    public void setmValidityPeriodLength(int mValidityPeriodLength) {
        this.mValidityPeriodLength = mValidityPeriodLength;
    }

    public int getmParameterIndicator() {
        return mParameterIndicator;
    }

    public void setmParameterIndicator(int mParameterIndicator) {
        this.mParameterIndicator = mParameterIndicator;
    }

    public int getmUserDataHeaderIndicator() {
        return mUserDataHeaderIndicator;
    }

    public void setmUserDataHeaderIndicator(int mUserDataHeaderIndicator) {
        this.mUserDataHeaderIndicator = mUserDataHeaderIndicator;
    }

    public Date getmScts() {
        return mScts;
    }

    public GsmSmsAddress getmRecipientAddress() {
        return mRecipientAddress;
    }

    public String getMessageBody() {
        return mMessageBody;
    }

    @Override
    public String toString() {
        return "SMS Message\n*********\n"
                + "reference: " + this.mMessageRef + "\n"
                + "Destination Address: " + this.mRecipientAddress.getAddressString() + "\n"
                + "Protocol Identifier: " + this.mProtocolIdentifier + "\n"
                + "Data Coding Scheme: " + this.mDataCodingScheme + "\n"
                + "Validity Period Length: " + this.mValidityPeriodLength + "\n"
                + "User data length: " + this.mUserData.length + "\n"
                + "User data: " + this.mMessageBody + "\n"
                + "************";

    }
    
    public static byte[] convertUnicode2GSM(String msg) {
        byte[] data = new byte[msg.length()];

        for (int i = 0; i < msg.length(); i++) {
            switch (msg.charAt(i)) {
                case '@':
                    data[i] = 0x00;
                    break;
                case '$':
                    data[i] = 0x02;
                    break;
                case '\n':
                    data[i] = 0x0A;
                    break;
                case '\r':
                    data[i] = 0x0D;
                    break;
                case '_':
                    data[i] = 0x11;
                    break;
                case 'ß':
                    data[i] = 0x1E;
                    break;
                case ' ':
                    data[i] = 0x20;
                    break;
                case '!':
                    data[i] = 0x21;
                    break;
                case '\"':
                    data[i] = 0x22;
                    break;
                case '#':
                    data[i] = 0x23;
                    break;
                case '%':
                    data[i] = 0x25;
                    break;
                case '&':
                    data[i] = 0x26;
                    break;
                case '\'':
                    data[i] = 0x27;
                    break;
                case '(':
                    data[i] = 0x28;
                    break;
                case ')':
                    data[i] = 0x29;
                    break;
                case '*':
                    data[i] = 0x2A;
                    break;
                case '+':
                    data[i] = 0x2B;
                    break;
                case ',':
                    data[i] = 0x2C;
                    break;
                case '-':
                    data[i] = 0x2D;
                    break;
                case '.':
                    data[i] = 0x2E;
                    break;
                case '/':
                    data[i] = 0x2F;
                    break;
                case '0':
                    data[i] = 0x30;
                    break;
                case '1':
                    data[i] = 0x31;
                    break;
                case '2':
                    data[i] = 0x32;
                    break;
                case '3':
                    data[i] = 0x33;
                    break;
                case '4':
                    data[i] = 0x34;
                    break;
                case '5':
                    data[i] = 0x35;
                    break;
                case '6':
                    data[i] = 0x36;
                    break;
                case '7':
                    data[i] = 0x37;
                    break;
                case '8':
                    data[i] = 0x38;
                    break;
                case '9':
                    data[i] = 0x39;
                    break;
                case ':':
                    data[i] = 0x3A;
                    break;
                case ';':
                    data[i] = 0x3B;
                    break;
                case '<':
                    data[i] = 0x3C;
                    break;
                case '=':
                    data[i] = 0x3D;
                    break;
                case '>':
                    data[i] = 0x3E;
                    break;
                case '?':
                    data[i] = 0x3F;
                    break;
                case 'A':
                    data[i] = 0x41;
                    break;
                case 'B':
                    data[i] = 0x42;
                    break;
                case 'C':
                    data[i] = 0x43;
                    break;
                case 'D':
                    data[i] = 0x44;
                    break;
                case 'E':
                    data[i] = 0x45;
                    break;
                case 'F':
                    data[i] = 0x46;
                    break;
                case 'G':
                    data[i] = 0x47;
                    break;
                case 'H':
                    data[i] = 0x48;
                    break;
                case 'I':
                    data[i] = 0x49;
                    break;
                case 'J':
                    data[i] = 0x4A;
                    break;
                case 'K':
                    data[i] = 0x4B;
                    break;
                case 'L':
                    data[i] = 0x4C;
                    break;
                case 'M':
                    data[i] = 0x4D;
                    break;
                case 'N':
                    data[i] = 0x4E;
                    break;
                case 'O':
                    data[i] = 0x4F;
                    break;
                case 'P':
                    data[i] = 0x50;
                    break;
                case 'Q':
                    data[i] = 0x51;
                    break;
                case 'R':
                    data[i] = 0x52;
                    break;
                case 'S':
                    data[i] = 0x53;
                    break;
                case 'T':
                    data[i] = 0x54;
                    break;
                case 'U':
                    data[i] = 0x55;
                    break;
                case 'V':
                    data[i] = 0x56;
                    break;
                case 'W':
                    data[i] = 0x57;
                    break;
                case 'X':
                    data[i] = 0x58;
                    break;
                case 'Y':
                    data[i] = 0x59;
                    break;
                case 'Z':
                    data[i] = 0x5A;
                    break;
                case 'Ä':
                    data[i] = 0x5B;
                    break;
                case 'Ö':
                    data[i] = 0x5C;
                    break;
                case 'Ü':
                    data[i] = 0x5E;
                    break;
                case '§':
                    data[i] = 0x5F;
                    break;
                case 'a':
                    data[i] = 0x61;
                    break;
                case 'b':
                    data[i] = 0x62;
                    break;
                case 'c':
                    data[i] = 0x63;
                    break;
                case 'd':
                    data[i] = 0x64;
                    break;
                case 'e':
                    data[i] = 0x65;
                    break;
                case 'f':
                    data[i] = 0x66;
                    break;
                case 'g':
                    data[i] = 0x67;
                    break;
                case 'h':
                    data[i] = 0x68;
                    break;
                case 'i':
                    data[i] = 0x69;
                    break;
                case 'j':
                    data[i] = 0x6A;
                    break;
                case 'k':
                    data[i] = 0x6B;
                    break;
                case 'l':
                    data[i] = 0x6C;
                    break;
                case 'm':
                    data[i] = 0x6D;
                    break;
                case 'n':
                    data[i] = 0x6E;
                    break;
                case 'o':
                    data[i] = 0x6F;
                    break;
                case 'p':
                    data[i] = 0x70;
                    break;
                case 'q':
                    data[i] = 0x71;
                    break;
                case 'r':
                    data[i] = 0x72;
                    break;
                case 's':
                    data[i] = 0x73;
                    break;
                case 't':
                    data[i] = 0x74;
                    break;
                case 'u':
                    data[i] = 0x75;
                    break;
                case 'v':
                    data[i] = 0x76;
                    break;
                case 'w':
                    data[i] = 0x77;
                    break;
                case 'x':
                    data[i] = 0x78;
                    break;
                case 'y':
                    data[i] = 0x79;
                    break;
                case 'z':
                    data[i] = 0x7A;
                    break;
                case 'ä':
                    data[i] = 0x7B;
                    break;
                case 'ö':
                    data[i] = 0x7C;
                    break;
                case 'ü':
                    data[i] = 0x7E;
                    break;
                default:
                    data[i] = 0x3F;
                    break; // '?'
            }
        }
        return data;
    }
    
    public static char convertGSM2Unicode(int b) {
        char c;

        if ((b >= 0x41) && (b <= 0x5A)) {    // character is between "A" and "Z"
            c = (char) b;
            return c;
        }  // if
        if ((b >= 0x61) && (b <= 0x7A)) {    // character is between "a" and "z"
            c = (char) b;
            return c;
        }  // if
        if ((b >= 0x30) && (b <= 0x39)) {    // character is between "0" and "9"
            c = (char) b;
            return c;
        }  // if

        switch (b) {
            case 0x00:
                c = '@';
                break;
            case 0x02:
                c = '$';
                break;
            case 0x0A:
                c = '\n';
                break;
            case 0x0D:
                c = '\r';
                break;
            case 0x11:
                c = '_';
                break;
            case 0x1E:
                c = 'ß';
                break;
            case 0x20:
                c = ' ';
                break;
            case 0x21:
                c = '!';
                break;
            case 0x22:
                c = '\"';
                break;
            case 0x23:
                c = '#';
                break;
            case 0x25:
                c = '%';
                break;
            case 0x26:
                c = '&';
                break;
            case 0x27:
                c = '\'';
                break;
            case 0x28:
                c = '(';
                break;
            case 0x29:
                c = ')';
                break;
            case 0x2A:
                c = '*';
                break;
            case 0x2B:
                c = '+';
                break;
            case 0x2C:
                c = ',';
                break;
            case 0x2D:
                c = '-';
                break;
            case 0x2E:
                c = '.';
                break;
            case 0x2F:
                c = '/';
                break;
            case 0x3A:
                c = ':';
                break;
            case 0x3B:
                c = ';';
                break;
            case 0x3C:
                c = '<';
                break;
            case 0x3D:
                c = '=';
                break;
            case 0x3E:
                c = '>';
                break;
            case 0x3F:
                c = '?';
                break;
            case 0x5B:
                c = 'Ä';
                break;
            case 0x5C:
                c = 'Ö';
                break;
            case 0x5E:
                c = 'Ü';
                break;
            case 0x5F:
                c = '§';
                break;
            case 0x7B:
                c = 'ä';
                break;
            case 0x7C:
                c = 'ö';
                break;
            case 0x7E:
                c = 'ü';
                break;
            default:
                c = '?';
                break;
        }  // switch
        
        return c;
    }


}
