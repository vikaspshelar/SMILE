/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms.rp.messages;
import com.smilecoms.mm.sms.SmsMessage;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class RpAckMessage extends RpMessage {
    private static final Logger log = LoggerFactory.getLogger(RpAckMessage.class);
    private SmsMessage smsMessage;
    
    @Override
    public void deSerialise() {
        log.debug("MMSIP: deserialise RP-ACK for message reference [{}]", messageReference);
        curByte++;  //Information element ID = 0x41/65
        int smsMessageLen = 0;
        if (curByte < rawPduData.length) {
            smsMessageLen = rawPduData[curByte++];
        }
        log.debug("MMSIP: deserialise RP-ACK with SMS message of length [{}]", smsMessageLen);
        if (smsMessageLen > 0) {
            smsMessage = SmsMessage.createFromPdu(rawPduData, curByte);
        }
    }

    public byte[] serialise() {
        int i = 0;
        List<Byte> byteArray = new ArrayList<Byte>();
        
        byteArray.add(new Byte((byte)3));
        byteArray.add(new Byte((byte)messageReference));
        byteArray.add(new Byte((byte)0x41));  //information element for user data
        
        
        byte[] userData = smsMessage.serialise();
        byteArray.add(new Byte((byte)userData.length));
        
        byte[] retBytes = new byte[byteArray.size() + userData.length];
        for (i=0 ; i < byteArray.size(); i++) {
            retBytes[i] = byteArray.get(i);
        }
        
        for (int j=0; j < userData.length; j++) {
            retBytes[i++] = userData[j];
        }

        return retBytes;
    }
    public void setSmsMessage(SmsMessage smsSubmitReportMessage) {
        this.smsMessage = smsSubmitReportMessage;
    }
    
    
}
