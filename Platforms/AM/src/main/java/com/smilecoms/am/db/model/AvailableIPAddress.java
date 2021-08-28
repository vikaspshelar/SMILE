/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.am.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "available_ip_address")
public class AvailableIPAddress implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "IP_ADDRESS", nullable = false)
    private String ipAddress;
    @Column(name = "ISSUED", nullable = false)
    private int issued;
    @Column(name = "ISSUED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date issuedDateTime;
    @Column(name = "RELEASED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date releasedDateTime;

    public AvailableIPAddress() {
    }

    public AvailableIPAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public AvailableIPAddress(String ipAddress, int issued) {
        this.ipAddress = ipAddress;
        this.issued = issued;
    }

    public String getIPAddress() {
        return ipAddress;
    }

    public void setIPAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getIssued() {
        return issued;
    }

    
    public void setIssued(int issued) {
        this.issued = issued;
    }

    public Date getIssuedDateTime() {
        return issuedDateTime;
    }

    public void setIssuedDateTime(Date issuedDateTime) {
        this.issuedDateTime = issuedDateTime;
    }

    public Date getReleasedDateTime() {
        return releasedDateTime;
    }

    public void setReleasedDateTime(Date releasedDateTime) {
        this.releasedDateTime = releasedDateTime;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (ipAddress != null ? ipAddress.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AvailableIPAddress)) {
            return false;
        }
        AvailableIPAddress other = (AvailableIPAddress) object;
        if ((this.ipAddress == null && other.ipAddress != null) || (this.ipAddress != null && !this.ipAddress.equals(other.ipAddress))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.nm.AvailableIPAddress[ipAddress=" + ipAddress + "]";
    }

}
