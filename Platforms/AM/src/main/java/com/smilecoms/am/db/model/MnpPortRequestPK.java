/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author mukosi
 */
@Embeddable
public class MnpPortRequestPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "PORTING_ORDER_ID")
    private String portingOrderId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 3)
    @Column(name = "PORTING_DIRECTION")
    private String portingDirection;

    public MnpPortRequestPK() {
    }

    public MnpPortRequestPK(String portingOrderId, String portingDirection) {
        this.portingOrderId = portingOrderId;
        this.portingDirection = portingDirection;
    }

    public String getPortingOrderId() {
        return portingOrderId;
    }

    public void setPortingOrderId(String portingOrderId) {
        this.portingOrderId = portingOrderId;
    }

    public String getPortingDirection() {
        return portingDirection;
    }

    public void setPortingDirection(String portingDirection) {
        this.portingDirection = portingDirection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (portingOrderId != null ? portingOrderId.hashCode() : 0);
        hash += (portingDirection != null ? portingDirection.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MnpPortRequestPK)) {
            return false;
        }
        MnpPortRequestPK other = (MnpPortRequestPK) object;
        if ((this.portingOrderId == null && other.portingOrderId != null) || (this.portingOrderId != null && !this.portingOrderId.equals(other.portingOrderId))) {
            return false;
        }
        if ((this.portingDirection == null && other.portingDirection != null) || (this.portingDirection != null && !this.portingDirection.equals(other.portingDirection))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.am.MnpPortRequestPK[ portingOrderId=" + portingOrderId + ", portingDirection=" + portingDirection + " ]";
    }
    
}
