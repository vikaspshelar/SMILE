/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "unit_credit_service_mapping")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UnitCreditServiceMapping.findAll", query = "SELECT u FROM UnitCreditServiceMapping u")})
public class UnitCreditServiceMapping implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected UnitCreditServiceMappingPK unitCreditServiceMappingPK;

    public UnitCreditServiceMapping() {
    }

    public UnitCreditServiceMapping(UnitCreditServiceMappingPK unitCreditServiceMappingPK) {
        this.unitCreditServiceMappingPK = unitCreditServiceMappingPK;
    }

    public UnitCreditServiceMapping(int unitCreditSpecificationId, int serviceSpecificationId, int productSpecificationId) {
        this.unitCreditServiceMappingPK = new UnitCreditServiceMappingPK(unitCreditSpecificationId, serviceSpecificationId, productSpecificationId);
    }

    public UnitCreditServiceMappingPK getUnitCreditServiceMappingPK() {
        return unitCreditServiceMappingPK;
    }

    public void setUnitCreditServiceMappingPK(UnitCreditServiceMappingPK unitCreditServiceMappingPK) {
        this.unitCreditServiceMappingPK = unitCreditServiceMappingPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (unitCreditServiceMappingPK != null ? unitCreditServiceMappingPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UnitCreditServiceMapping)) {
            return false;
        }
        UnitCreditServiceMapping other = (UnitCreditServiceMapping) object;
        if ((this.unitCreditServiceMappingPK == null && other.unitCreditServiceMappingPK != null) || (this.unitCreditServiceMappingPK != null && !this.unitCreditServiceMappingPK.equals(other.unitCreditServiceMappingPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.UnitCreditServiceMapping[ unitCreditServiceMappingPK=" + unitCreditServiceMappingPK + " ]";
    }
    
}
