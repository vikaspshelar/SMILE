/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "interconnect_smsc_bind")
public class InterconnectSMSCBind implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "INTERCONNECT_SMSC_BIND_ID")
    private Integer interconnectSmscBindId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "INTERCONNECT_SMSC_ID")
    private int interconnectSmscId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LOCAL_HOST")
    private String localHost;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LAST_CHECK_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastCheckDatetime;

    public InterconnectSMSCBind() {
    }

    public InterconnectSMSCBind(Integer interconnectSmscBindId) {
        this.interconnectSmscBindId = interconnectSmscBindId;
    }

    public InterconnectSMSCBind(Integer interconnectSmscBindId, int interconnectSmscId, String localHost, Date lastCheckDatetime) {
        this.interconnectSmscBindId = interconnectSmscBindId;
        this.interconnectSmscId = interconnectSmscId;
        this.localHost = localHost;
        this.lastCheckDatetime = lastCheckDatetime;
    }

    public Integer getInterconnectSmscBindId() {
        return interconnectSmscBindId;
    }

    public void setInterconnectSmscBindId(Integer interconnectSmscBindId) {
        this.interconnectSmscBindId = interconnectSmscBindId;
    }

    public int getInterconnectSmscId() {
        return interconnectSmscId;
    }

    public void setInterconnectSmscId(int interconnectSmscId) {
        this.interconnectSmscId = interconnectSmscId;
    }

    public String getLocalHost() {
        return localHost;
    }

    public void setLocalHost(String localHost) {
        this.localHost = localHost;
    }

    public Date getLastCheckDatetime() {
        return lastCheckDatetime;
    }

    public void setLastCheckDatetime(Date lastCheckDatetime) {
        this.lastCheckDatetime = lastCheckDatetime;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (interconnectSmscBindId != null ? interconnectSmscBindId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof InterconnectSMSCBind)) {
            return false;
        }
        InterconnectSMSCBind other = (InterconnectSMSCBind) object;
        if ((this.interconnectSmscBindId == null && other.interconnectSmscBindId != null) || (this.interconnectSmscBindId != null && !this.interconnectSmscBindId.equals(other.interconnectSmscBindId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.plugins.offnetsms.smsc.InterconnectSmscBind[ interconnectSmscBindId=" + interconnectSmscBindId + " ]";
    }
    
}
