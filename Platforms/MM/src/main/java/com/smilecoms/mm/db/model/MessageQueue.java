/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "message_queue")
@NamedQueries({
    @NamedQuery(name = "MessageQueue.findAll", query = "SELECT m FROM MessageQueue m")})
public class MessageQueue implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "MESSAGE_ID")
    private String messageId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DEQUEUE_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dequeueTimestamp;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(name = "MESSAGE")
    private byte[] message;
    @Basic(optional = false)
    @NotNull
    @Column(name = "QUEUE_LABEL")
    private String queueLabel;

    public MessageQueue() {
    }

    public MessageQueue(String messageId) {
        this.messageId = messageId;
    }

    public MessageQueue(String messageId, Date dequeueTimestamp, byte[] message) {
        this.messageId = messageId;
        this.dequeueTimestamp = dequeueTimestamp;
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Date getDequeueTimestamp() {
        return dequeueTimestamp;
    }

    public void setDequeueTimestamp(Date dequeueTimestamp) {
        this.dequeueTimestamp = dequeueTimestamp;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public String getQueueLabel() {
        return queueLabel;
    }

    public void setQueueLabel(String queueLabel) {
        this.queueLabel = queueLabel;
    }

    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (messageId != null ? messageId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MessageQueue)) {
            return false;
        }
        MessageQueue other = (MessageQueue) object;
        if ((this.messageId == null && other.messageId != null) || (this.messageId != null && !this.messageId.equals(other.messageId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.db.model.MessageQueue[ messageId=" + messageId + " ]";
    }
    
}
