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
@Table(name = "product_specification_avp")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProductSpecificationAvp.findAll", query = "SELECT p FROM ProductSpecificationAvp p"),
    @NamedQuery(name = "ProductSpecificationAvp.findByProductSpecificationId", query = "SELECT p FROM ProductSpecificationAvp p WHERE p.productSpecificationAvpPK.productSpecificationId = :productSpecificationId"),
    @NamedQuery(name = "ProductSpecificationAvp.findByAttribute", query = "SELECT p FROM ProductSpecificationAvp p WHERE p.productSpecificationAvpPK.attribute = :attribute"),
    @NamedQuery(name = "ProductSpecificationAvp.findByValue", query = "SELECT p FROM ProductSpecificationAvp p WHERE p.value = :value"),
    @NamedQuery(name = "ProductSpecificationAvp.findByUserDefined", query = "SELECT p FROM ProductSpecificationAvp p WHERE p.userDefined = :userDefined"),
    @NamedQuery(name = "ProductSpecificationAvp.findByValidationRule", query = "SELECT p FROM ProductSpecificationAvp p WHERE p.validationRule = :validationRule"),
    @NamedQuery(name = "ProductSpecificationAvp.findByInputType", query = "SELECT p FROM ProductSpecificationAvp p WHERE p.inputType = :inputType"),
    @NamedQuery(name = "ProductSpecificationAvp.findByTechnicalDescription", query = "SELECT p FROM ProductSpecificationAvp p WHERE p.technicalDescription = :technicalDescription")})
public class ProductSpecificationAvp implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ProductSpecificationAvpPK productSpecificationAvpPK;
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
    @JoinColumn(name = "PRODUCT_SPECIFICATION_ID", referencedColumnName = "PRODUCT_SPECIFICATION_ID", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private ProductSpecification productSpecification;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROVISION_ROLES")
    private String provisionRoles;

    public ProductSpecificationAvp() {
    }

    public ProductSpecificationAvp(ProductSpecificationAvpPK productSpecificationAvpPK) {
        this.productSpecificationAvpPK = productSpecificationAvpPK;
    }

    public ProductSpecificationAvp(ProductSpecificationAvpPK productSpecificationAvpPK, String value, String userDefined, String validationRule, String inputType, String technicalDescription) {
        this.productSpecificationAvpPK = productSpecificationAvpPK;
        this.value = value;
        this.userDefined = userDefined;
        this.validationRule = validationRule;
        this.inputType = inputType;
        this.technicalDescription = technicalDescription;
    }

    public ProductSpecificationAvp(int productSpecificationId, String attribute) {
        this.productSpecificationAvpPK = new ProductSpecificationAvpPK(productSpecificationId, attribute);
    }

    public ProductSpecificationAvpPK getProductSpecificationAvpPK() {
        return productSpecificationAvpPK;
    }

    public void setProductSpecificationAvpPK(ProductSpecificationAvpPK productSpecificationAvpPK) {
        this.productSpecificationAvpPK = productSpecificationAvpPK;
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

    public ProductSpecification getProductSpecification() {
        return productSpecification;
    }

    public void setProductSpecification(ProductSpecification productSpecification) {
        this.productSpecification = productSpecification;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (productSpecificationAvpPK != null ? productSpecificationAvpPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductSpecificationAvp)) {
            return false;
        }
        ProductSpecificationAvp other = (ProductSpecificationAvp) object;
        if ((this.productSpecificationAvpPK == null && other.productSpecificationAvpPK != null) || (this.productSpecificationAvpPK != null && !this.productSpecificationAvpPK.equals(other.productSpecificationAvpPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.ProductSpecificationAvp[ productSpecificationAvpPK=" + productSpecificationAvpPK + " ]";
    }
}
