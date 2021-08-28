/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author mukosi
 */
@Embeddable
public class ServiceSpecificationAvpPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_SPECIFICATION_ID")
    private int serviceSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ATTRIBUTE")
    private String attribute;

    public ServiceSpecificationAvpPK() {
    }

    public ServiceSpecificationAvpPK(int serviceSpecificationId, String attribute) {
        this.serviceSpecificationId = serviceSpecificationId;
        this.attribute = attribute;
    }

    public int getServiceSpecificationId() {
        return serviceSpecificationId;
    }

    public void setServiceSpecificationId(int serviceSpecificationId) {
        this.serviceSpecificationId = serviceSpecificationId;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) serviceSpecificationId;
        hash += (attribute != null ? attribute.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceSpecificationAvpPK)) {
            return false;
        }
        ServiceSpecificationAvpPK other = (ServiceSpecificationAvpPK) object;
        if (this.serviceSpecificationId != other.serviceSpecificationId) {
            return false;
        }
        if ((this.attribute == null && other.attribute != null) || (this.attribute != null && !this.attribute.equals(other.attribute))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ServiceSpecificationAvpPK[ serviceSpecificationId=" + serviceSpecificationId + ", attribute=" + attribute + " ]";
    }
    
}
