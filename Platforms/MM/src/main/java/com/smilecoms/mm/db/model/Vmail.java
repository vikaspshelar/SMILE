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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author root
 */
@Entity
@Table(name = "vmail")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Vmail.findAll", query = "SELECT v FROM Vmail v"),
    @NamedQuery(name = "Vmail.findByIdentifier", query = "SELECT v FROM Vmail v WHERE v.identifier = :identifier"),
    @NamedQuery(name = "Vmail.findByVmailboxId", query = "SELECT v FROM Vmail v WHERE v.vmailboxId = :vmailboxId"),
    @NamedQuery(name = "Vmail.findBySource", query = "SELECT v FROM Vmail v WHERE v.source = :source"),
    @NamedQuery(name = "Vmail.findByDestination", query = "SELECT v FROM Vmail v WHERE v.destination = :destination"),
    @NamedQuery(name = "Vmail.findBySenddate", query = "SELECT v FROM Vmail v WHERE v.senddate = :senddate"),
    @NamedQuery(name = "Vmail.findByStatus", query = "SELECT v FROM Vmail v WHERE v.status = :status"),
    @NamedQuery(name = "Vmail.findByExpires", query = "SELECT v FROM Vmail v WHERE v.expires = :expires")})
public class Vmail implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = true)
    @Column(name = "IDENTIFIER")
    private Integer identifier;
    @Column(name = "VMAILBOX_ID")
    private Integer vmailboxId;
    @Column(name = "MESSAGE_ID")
    private String messageId;
    @Column(name = "SOURCE")
    private String source;
    @Column(name = "DESTINATION")
    private String destination;
    @Column(name = "SENDDATE")
    @Temporal(TemporalType.DATE)
    private Date senddate;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "EXPIRES")
    @Temporal(TemporalType.DATE)
    private Date expires;
    @Column(name = "MEDIA_SERVER")
    private String mediaServer;

    public Vmail() {
    }

    public Vmail(Integer identifier) {
        this.identifier = identifier;
    }

    public String getMediaServer() {
        return mediaServer;
    }

    public void setMediaServer(String mediaServer) {
        this.mediaServer = mediaServer;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    public Integer getVmailboxId() {
        return vmailboxId;
    }

    public void setVmailboxId(Integer vmailboxId) {
        this.vmailboxId = vmailboxId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Date getSenddate() {
        return senddate;
    }

    public void setSenddate(Date senddate) {
        this.senddate = senddate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (identifier != null ? identifier.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Vmail)) {
            return false;
        }
        Vmail other = (Vmail) object;
        if ((this.identifier == null && other.identifier != null) || (this.identifier != null && !this.identifier.equals(other.identifier))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.db.model.Vmail[ identifier=" + identifier + " ]";
    }
    
}
