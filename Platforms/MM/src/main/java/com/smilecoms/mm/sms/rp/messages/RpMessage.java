/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.sms.rp.messages;

import com.smilecoms.mm.sms.SmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public abstract class RpMessage {
    private static final Logger log = LoggerFactory.getLogger(RpAckMessage.class);
    protected MTI mti;
    protected Direction direction;
    protected short messageReference;                             /*int 0-255 - 1 octet*/
    protected int curByte = 0;
    protected int payloadOffset;
    protected byte[] rawPduData;
//    protected SmsMessage smsMessage;
    
    public static enum MTI {
        RPDATA, RPACK, RPERR, RP_SMMA, RESERVED, UNKNOWN
    };        /*first 3 bits*/

    public static enum Direction {
        incoming, outgoing
    };
    
    public abstract void deSerialise() throws RpMessageDecodeException ;
    
    public static RpMessage decode(byte[] pdu, Direction dir) throws RpMessageDecodeException {
        int offset = 0;
        MTI mti = MTI.UNKNOWN;
        RpMessage rpMessage = null;

        int x = pdu[offset++] & 0x07;
        switch (x) {
            case 0:
                mti = dir==Direction.incoming ? MTI.RPDATA : MTI.RESERVED;
                break;
            case 1:
                mti = dir == Direction.incoming ? MTI.RESERVED : MTI.RPDATA;
                break;
            case 2:
                mti = dir == Direction.incoming ? MTI.RPACK : MTI.RESERVED;
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 7:
                break;
            default:
                mti = MTI.UNKNOWN;
                log.error("Failed to decode");
                throw new RpMessageDecodeException();
        }
        
        switch (mti) {
            case RPDATA:
                rpMessage = new RpDataMessage();
                rpMessage.setRawPduData(pdu);
                rpMessage.direction = dir;
                rpMessage.mti = mti;
                rpMessage.messageReference = (short) pdu[offset++];
                break;
            case RPACK:
                rpMessage = new RpAckMessage();
                rpMessage.setRawPduData(pdu);
                rpMessage.direction = dir;
                rpMessage.mti = mti;
                rpMessage.messageReference = (short) pdu[offset++];
                break;                
            case RPERR:
                rpMessage = new RpErrorMessage();
                break;
            case RP_SMMA:
                //unsupported
                break;
            default:
                log.error("failed to decode");
                throw new RpMessageDecodeException();
        }

        rpMessage.curByte = offset;
        rpMessage.deSerialise();
        
//        log.debug("MMSIP: curbyte: [{}]", rpMessage.curByte);
        
        return rpMessage;
    }

    public byte[] getRawPduData() {
        return rawPduData;
    }

    public void setRawPduData(byte[] rawPduData) {
        this.rawPduData = rawPduData;
    }
    
    

    public MTI getMti() {
        return mti;
    }
    
    public int getMtiAsInt() {
        
        switch (this.direction) {
            case incoming: {
                switch (this.mti) {
                    case RESERVED:
                        return 1;
                    case RPACK:
                        return 2;
                    case RPDATA:
                        return 0;
                    case RPERR:
                        return 4;
                    case RP_SMMA:
                        return 6;
                    case UNKNOWN:
                    default: 
                        return -1;
                }
            }
            case outgoing: {
                switch (this.mti) {
                    case RESERVED:
                        return 0;
                    case RPACK:
                        return 3;
                    case RPDATA:
                        return 1;
                    case RPERR:
                        return 5;
                    case UNKNOWN:
                    default:
                        return -1;
                }
            }
        }
        return -1;
    }

    public Direction getDirection() {
        return direction;
    }

    public short getMessageReference() {
        return messageReference;
    }

    public void setMessageReference(short messageReference) {
        this.messageReference = messageReference;
    }
    
    @Override
    public String toString() {
        return "RP Message MR: " + getMessageReference() + ", MTI: " + this.mti + ", Direction: " + this.direction;
    }
    
    public abstract byte[] serialise();

}
