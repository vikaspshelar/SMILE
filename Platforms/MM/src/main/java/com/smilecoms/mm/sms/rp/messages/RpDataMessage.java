/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms.rp.messages;

import com.smilecoms.commons.util.Utils;
import com.smilecoms.mm.plugins.onnetsms.OnnetSMSDeliveryPlugin;
import com.smilecoms.mm.sms.SmsMessage;
import com.smilecoms.mm.sms.rp.types.RpDestinationAddress;
import com.smilecoms.mm.sms.rp.types.RpOriginatorAddress;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class RpDataMessage extends RpMessage {
    private static final Logger log = LoggerFactory.getLogger(RpMessage.class);

    private RpOriginatorAddress orignatorAddress;               /*1-12octets*/
    private RpDestinationAddress destinationAddress;            /*1 octet*/
    private int userDataOffset;                                 /* offset into byte array for userdat encapsulated within RP PDU */
    private SmsMessage smsMessage;                              /* SMS TPDU within RP-DATA message */

    public RpDataMessage() {
        this.mti = MTI.RPDATA;
        this.direction = Direction.incoming;    /*assume incoming by default*/
        this.messageReference = (short) Utils.getRandomNumber(1, 254);
    }
    
    public RpDataMessage(Direction dir, String from, String to) {
        this();
        this.direction = dir;
        
        if (dir == Direction.outgoing) {
            orignatorAddress = new RpOriginatorAddress(OnnetSMSDeliveryPlugin.getMyScE164());
            destinationAddress = new RpDestinationAddress("");
        } else {
            destinationAddress = new RpDestinationAddress(to);
            orignatorAddress = new RpOriginatorAddress("");   //blank address
        }
    }

    public void deSerialise() throws RpMessageDecodeException {
        orignatorAddress = new RpOriginatorAddress();
        this.curByte += orignatorAddress.deserialise(this.rawPduData, this.curByte);
        destinationAddress = new RpDestinationAddress();
        this.curByte += destinationAddress.deserialise(this.rawPduData, this.curByte);

        int len = (int) (this.rawPduData[curByte] & 0xFF);   //have to mask off signedness - rem. int is signed so anything > 128 will have leading ffffff
        this.userDataOffset = curByte + 1;
        
        if (len > 0) {
            smsMessage = SmsMessage.createFromPdu(this.rawPduData, userDataOffset);
        } else {
            log.warn("no SMS message in RP-DATA message");
        }
    }

    public RpOriginatorAddress getOrignatorAddress() {
        return orignatorAddress;
    }

    public RpDestinationAddress getDestinationAddress() {
        return destinationAddress;
    }

    public SmsMessage getSmsMessage() {
        return smsMessage;
    }

    public RpAckMessage createAck() {
        
        RpAckMessage rpAckMessage = new RpAckMessage();
        
        rpAckMessage.messageReference = this.messageReference;
        rpAckMessage.mti = MTI.RPACK;
        
        if (this.direction == Direction.incoming) {
            rpAckMessage.direction = Direction.outgoing;
        } else {
            rpAckMessage.direction = Direction.incoming;
        }
        
        return rpAckMessage;
    }
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public byte[] serialise() { 
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        
        try {
            os.write((byte)getMtiAsInt());
            os.write((byte)this.messageReference);
            if (orignatorAddress != null) {
                os.write(this.orignatorAddress.serialise());
            }
            if (destinationAddress != null) {
                os.write(this.destinationAddress.serialise());
            }
            
            if (this.smsMessage != null) {
                log.debug("MMSIP: serialising SMS message within RP-DATA");
//                byte[] smsBytes = hexStringToByteArray("400C815265260000007FF6418081213541003502700000301116210505B001400EA7A6B841E452659D01D132DD734A3FA084197A58D94322DD6E7B50F75B3447E586F59D3583F2D5");
//                byte[] smsBytes = hexStringToByteArray("D135820283810607815265260000008B26510C815265260000007FF6A800000000000013027100000E0AB00140000000004C0000039000");
//                byte[] smsBytes = hexStringToByteArray("000C815265260000007FF6000000000000003502700000301116210505B0014084E5A1308164C702AED86A4D6332A53234CC33D3D72425D24D4B52B794E6AB933E4130579EEACE28");
                
//                byte[] smsBytes = hexStringToByteArray("C00A8121436587097FF6410142414494023502700000301116210505B000104D9B7A66C72AD14A8C379A7A02FD1E2C3CFA734E42749FA694482880F83E67984F4E0C45585F7B56");
//eng. sample                  byte[] smsBytes = hexStringToByteArray("400A8121436587097FF6410182411043003502700000301116210505B000107EF43C09D80101D4756CF7D5965C72860E2067F8A7C7B824407ED070E55ECF39D52AA81A95F40653");

//                byte[] smsBytes = hexStringToByteArray("440A8121436587097FF6410182510353003502700000301116210505B00140DA2FF43973CEB806555F8E44CEB2F764F2BD4C4E2B4B806BA2E94982609275A71B52610A3DF24FFF");
//                byte[] smsBytes = hexStringToByteArray("400A8121436587097FF6411171414463003502700000301116210505B00010C0CB1C58AFA93D60E0478B9DF31E6A33883971A4A4BBBDD97A913327010B1A056671CC2B5EC6135D");
                        //A0C2000057D1550202838106068121436587090B
//80C2000059D157020283810607815265260000000B48400C815265260000007FF6418071412392003502700000301116010101B00140CB1893B2B274F2E084831B06BAFC41967C9E369FEEB54C400A1A851644621C894CA9EEE78B7A26F2
                byte[] smsBytes;
                
                if (OnnetSMSDeliveryPlugin.otaTest.equalsIgnoreCase("true")){
                    smsBytes = hexStringToByteArray(OnnetSMSDeliveryPlugin.otaString);
                    log.warn("Sending Binary SMS in OTATest mode - see property env.mm.ipsmgw.otatest");
                } else {
                    smsBytes = smsMessage.serialise();
                }
                os.write(smsBytes.length);
                os.write(smsBytes);
            } else {
                log.warn("MMSIP: SMS message inside RpDataMessage is NULL");
            }
        } catch (IOException ex) {
            log.error("MMSIP: failed to serialise RP-DATA {{}]", ex.getLocalizedMessage());
            log.warn("Error: ", ex);
            return null;
        }
        return os.toByteArray();
    }

    public void setSmsMessage(SmsMessage smsMessage) {
        this.smsMessage= smsMessage;
    }
    
}
