/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "service_specification_avp")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ServiceSpecificationAvp.findAll", query = "SELECT s FROM ServiceSpecificationAvp s"),
    @NamedQuery(name = "ServiceSpecificationAvp.findByServiceSpecificationId", query = "SELECT s FROM ServiceSpecificationAvp s WHERE s.serviceSpecificationAvpPK.serviceSpecificationId = :serviceSpecificationId"),
    @NamedQuery(name = "ServiceSpecificationAvp.findByAttribute", query = "SELECT s FROM ServiceSpecificationAvp s WHERE s.serviceSpecificationAvpPK.attribute = :attribute"),
    @NamedQuery(name = "ServiceSpecificationAvp.findByValue", query = "SELECT s FROM ServiceSpecificationAvp s WHERE s.value = :value"),
    @NamedQuery(name = "ServiceSpecificationAvp.findByUserDefined", query = "SELECT s FROM ServiceSpecificationAvp s WHERE s.userDefined = :userDefined"),
    @NamedQuery(name = "ServiceSpecificationAvp.findByValidationRule", query = "SELECT s FROM ServiceSpecificationAvp s WHERE s.validationRule = :validationRule"),
    @NamedQuery(name = "ServiceSpecificationAvp.findByInputType", query = "SELECT s FROM ServiceSpecificationAvp s WHERE s.inputType = :inputType"),
    @NamedQuery(name = "ServiceSpecificationAvp.findByTechnicalDescription", query = "SELECT s FROM ServiceSpecificationAvp s WHERE s.technicalDescription = :technicalDescription")})
public class ServiceSpecificationAvp implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ServiceSpecificationAvpPK serviceSpecificationAvpPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "VALUE")
    private String value;
    @Basic(optional = false)
    @NotNull
    @Column(name = "USER_DEFINED")
    private String userDefined;
    @Basic(optional = false)
    @NotNull
    @Column(name = "VALIDATION_RULE")
    private String validationRule;
    @Basic(optional = false)
    @NotNull
    @Column(name = "INPUT_TYPE")
    private String inputType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TECHNICAL_DESCRIPTION")
    private String technicalDescription;
    @JoinColumn(name = "SERVICE_SPECIFICATION_ID", referencedColumnName = "SERVICE_SPECIFICATION_ID", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private ServiceSpecification serviceSpecification;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROVISION_ROLES")
    private String provisionRoles;
    
    public ServiceSpecificationAvp() {
    }

    public ServiceSpecificationAvp(ServiceSpecificationAvpPK serviceSpecificationAvpPK) {
        this.serviceSpecificationAvpPK = serviceSpecificationAvpPK;
    }

    public ServiceSpecificationAvp(ServiceSpecificationAvpPK serviceSpecificationAvpPK, String value, String userDefined, String validationRule, String inputType, String technicalDescription) {
        this.serviceSpecificationAvpPK = serviceSpecificationAvpPK;
        this.value = value;
        this.userDefined = userDefined;
        this.validationRule = validationRule;
        this.inputType = inputType;
        this.technicalDescription = technicalDescription;
    }

    public ServiceSpecificationAvp(int serviceSpecificationId, String attribute) {
        this.serviceSpecificationAvpPK = new ServiceSpecificationAvpPK(serviceSpecificationId, attribute);
    }

    public ServiceSpecificationAvpPK getServiceSpecificationAvpPK() {
        return serviceSpecificationAvpPK;
    }

    public void setServiceSpecificationAvpPK(ServiceSpecificationAvpPK serviceSpecificationAvpPK) {
        this.serviceSpecificationAvpPK = serviceSpecificationAvpPK;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getProvisionRoles() {
        return provisionRoles;
    }

    public void setProvisionRoles(String provisionRoles) {
        this.provisionRoles = provisionRoles;
    }

    public String getUserDefined() {
        return userDefined;
    }

    public void setUserDefined(String userDefined) {
        this.userDefined = userDefined;
    }

    public String getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(String validationRule) {
        this.validationRule = validationRule;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getTechnicalDescription() {
        return technicalDescription;
    }

    public void setTechnicalDescription(String technicalDescription) {
        this.technicalDescription = technicalDescription;
    }

    public ServiceSpecification getServiceSpecification() {
        return serviceSpecification;
    }

    public void setServiceSpecification(ServiceSpecification serviceSpecification) {
        this.serviceSpecification = serviceSpecification;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (serviceSpecificationAvpPK != null ? serviceSpecificationAvpPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceSpecificationAvp)) {
            return false;
        }
        ServiceSpecificationAvp other = (ServiceSpecificationAvp) object;
        if ((this.serviceSpecificationAvpPK == null && other.serviceSpecificationAvpPK != null) || (this.serviceSpecificationAvpPK != null && !this.serviceSpecificationAvpPK.equals(other.serviceSpecificationAvpPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ServiceSpecificationAvp[ serviceSpecificationAvpPK=" + serviceSpecificationAvpPK + " ]";
    }
    
}
