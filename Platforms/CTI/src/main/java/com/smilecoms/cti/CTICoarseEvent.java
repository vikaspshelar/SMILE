/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.cti;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
/**
 *
 * @author Jason Penton
 */
public class CTICoarseEvent implements Externalizable {
    public enum EventID {
        SCREENPOP, HANGUP;
    }
    private Date timeStamp;
    private String targetID;
    private String sourceID;
    private EventID eventID;

    public CTICoarseEvent() {
        
    }

    public EventID getEventID() {
        return eventID;
    }

    public void setEventID(EventID eventID) {
        this.eventID = eventID;
    }

    public String getSourceID() {
        return sourceID;
    }

    public void setSourceID(String sourceID) {
        this.sourceID = sourceID;
    }

    public String getTargetID() {
        return targetID;
    }

    public void setTargetID(String targetID) {
        this.targetID = targetID;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
    
    public void writeExternal(ObjectOutput arg0) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void readExternal(ObjectInput arg0) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
