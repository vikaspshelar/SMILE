/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "x3_request_state")
@NamedQueries({
    @NamedQuery(name = "X3RequestState.findAll", query = "SELECT x FROM X3RequestState x")})
public class X3RequestState implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected X3RequestStatePK x3RequestStatePK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "START_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDateTime;
    @Column(name = "END_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Column(name = "RESULT_CODE")
    private Integer resultCode;
    @Column(name = "RESULT_XML")
    private String resultXml;
    @Column(name = "MESSAGES")
    private String messages;
    @Column(name = "X3_RECORD_ID")
    private String x3RecordId;

    public X3RequestState() {
    }

    public X3RequestState(X3RequestStatePK x3RequestStatePK) {
        this.x3RequestStatePK = x3RequestStatePK;
    }

    public X3RequestState(X3RequestStatePK x3RequestStatePK, Date startDateTime, String status) {
        this.x3RequestStatePK = x3RequestStatePK;
        this.startDateTime = startDateTime;
        this.status = status;
    }


    public X3RequestStatePK getX3RequestStatePK() {
        return x3RequestStatePK;
    }

    public void setX3RequestStatePK(X3RequestStatePK x3RequestStatePK) {
        this.x3RequestStatePK = x3RequestStatePK;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public void setResultCode(Integer resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultXml() {
        return resultXml;
    }

    public void setResultXml(String resultXml) {
        this.resultXml = resultXml;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public String getX3RecordId() {
        return x3RecordId;
    }

    public void setX3RecordId(String x3RecordId) {
        this.x3RecordId = x3RecordId;
    }

    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (x3RequestStatePK != null ? x3RequestStatePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof X3RequestState)) {
            return false;
        }
        X3RequestState other = (X3RequestState) object;
        if ((this.x3RequestStatePK == null && other.x3RequestStatePK != null) || (this.x3RequestStatePK != null && !this.x3RequestStatePK.equals(other.x3RequestStatePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.X3RequestState[ x3RequestStatePK=" + x3RequestStatePK + " ]";
    }
    
}
