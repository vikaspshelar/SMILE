/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "service_instance_mapping")
@NamedQueries({
    @NamedQuery(name = "ServiceInstanceMapping.findAll", query = "SELECT s FROM ServiceInstanceMapping s")})
public class ServiceInstanceMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ServiceInstanceMappingPK serviceInstanceMappingPK;

    public ServiceInstanceMapping() {
    }

    public ServiceInstanceMapping(ServiceInstanceMappingPK serviceInstanceMappingPK) {
        this.serviceInstanceMappingPK = serviceInstanceMappingPK;
    }

    public ServiceInstanceMapping(int serviceInstanceId, String identifier, String identifierType) {
        this.serviceInstanceMappingPK = new ServiceInstanceMappingPK(serviceInstanceId, identifier, identifierType);
    }

    public ServiceInstanceMappingPK getServiceInstanceMappingPK() {
        return serviceInstanceMappingPK;
    }

    public void setServiceInstanceMappingPK(ServiceInstanceMappingPK serviceInstanceMappingPK) {
        this.serviceInstanceMappingPK = serviceInstanceMappingPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (serviceInstanceMappingPK != null ? serviceInstanceMappingPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceInstanceMapping)) {
            return false;
        }
        ServiceInstanceMapping other = (ServiceInstanceMapping) object;
        if ((this.serviceInstanceMappingPK == null && other.serviceInstanceMappingPK != null) || (this.serviceInstanceMappingPK != null && !this.serviceInstanceMappingPK.equals(other.serviceInstanceMappingPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.ServiceInstanceMapping[ serviceInstanceMappingPK=" + serviceInstanceMappingPK + " ]";
    }
    
}
