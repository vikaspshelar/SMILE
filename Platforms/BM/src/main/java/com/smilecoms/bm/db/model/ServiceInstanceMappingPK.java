/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Embeddable
public class ServiceInstanceMappingPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_INSTANCE_ID")
    private int serviceInstanceId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDENTIFIER")
    private String identifier;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IDENTIFIER_TYPE")
    private String identifierType;

    public ServiceInstanceMappingPK() {
    }

    public ServiceInstanceMappingPK(int serviceInstanceId, String identifier, String identifierType) {
        this.serviceInstanceId = serviceInstanceId;
        this.identifier = identifier;
        this.identifierType = identifierType;
    }

    public int getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(int serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) serviceInstanceId;
        hash += (identifier != null ? identifier.hashCode() : 0);
        hash += (identifierType != null ? identifierType.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceInstanceMappingPK)) {
            return false;
        }
        ServiceInstanceMappingPK other = (ServiceInstanceMappingPK) object;
        if (this.serviceInstanceId != other.serviceInstanceId) {
            return false;
        }
        if ((this.identifier == null && other.identifier != null) || (this.identifier != null && !this.identifier.equals(other.identifier))) {
            return false;
        }
        if ((this.identifierType == null && other.identifierType != null) || (this.identifierType != null && !this.identifierType.equals(other.identifierType))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.ServiceInstanceMappingPK[ serviceInstanceId=" + serviceInstanceId + ", identifier=" + identifier + ", identifierType=" + identifierType + " ]";
    }
    
}
