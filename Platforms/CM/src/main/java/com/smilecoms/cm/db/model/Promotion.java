/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "promotion")
@NamedQueries({
    @NamedQuery(name = "Promotion.findAll", query = "SELECT p FROM Promotion p")})
public class Promotion implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    @Column(name = "PROMOTION_ID")
    private Integer promotionId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROMOTION_NAME")
    private String promotionName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRODUCT_SPECIFICATION_ID")
    private int productSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UNIT_CREDIT_SPECIFICATION_ID")
    private int unitCreditSpecificationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "AVAILABLE_FROM")
    @Temporal(TemporalType.TIMESTAMP)
    private Date availableFrom;
    @Basic(optional = false)
    @NotNull
    @Column(name = "AVAILABLE_TO")
    @Temporal(TemporalType.TIMESTAMP)
    private Date availableTo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ASSOCIATED_ITEM_NUMBER_REGEX")
    private String associatedItemNumberRegex;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_ID_REGEX")
    private String organisationIdRegex;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROMOTION_CODE_REGEX")
    private String promotionCodeRegex;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROVISION_ROLES")
    private String provisionRoles;
    
    
    public Promotion() {
    }

    public Promotion(Integer promotionId) {
        this.promotionId = promotionId;
    }

    public Promotion(Integer promotionId, int productSpecificationId, int unitCreditSpecificationId, Date availableFrom, Date availableTo) {
        this.promotionId = promotionId;
        this.productSpecificationId = productSpecificationId;
        this.unitCreditSpecificationId = unitCreditSpecificationId;
        this.availableFrom = availableFrom;
        this.availableTo = availableTo;
    }

    public Integer getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Integer promotionId) {
        this.promotionId = promotionId;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public int getProductSpecificationId() {
        return productSpecificationId;
    }

    public void setProductSpecificationId(int productSpecificationId) {
        this.productSpecificationId = productSpecificationId;
    }

    public int getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(int unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public Date getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(Date availableFrom) {
        this.availableFrom = availableFrom;
    }

    public Date getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(Date availableTo) {
        this.availableTo = availableTo;
    }

    public String getAssociatedItemNumberRegex() {
        return associatedItemNumberRegex;
    }

    public void setAssociatedItemNumberRegex(String associatedItemNumberRegex) {
        this.associatedItemNumberRegex = associatedItemNumberRegex;
    }

    public String getOrganisationIdRegex() {
        return organisationIdRegex;
    }

    public void setOrganisationIdRegex(String organisationIdRegex) {
        this.organisationIdRegex = organisationIdRegex;
    }

    public String getPromotionCodeRegex() {
        return promotionCodeRegex;
    }

    public void setPromotionCodeRegex(String promotionCodeRegex) {
        this.promotionCodeRegex = promotionCodeRegex;
    }

    public String getProvisionRoles() {
        return provisionRoles;
    }

    public void setProvisionRoles(String provisionRoles) {
        this.provisionRoles = provisionRoles;
    }
    
    


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (promotionId != null ? promotionId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Promotion)) {
            return false;
        }
        Promotion other = (Promotion) object;
        if ((this.promotionId == null && other.promotionId != null) || (this.promotionId != null && !this.promotionId.equals(other.promotionId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.cm.db.model.Promotion[ promotionId=" + promotionId + " ]";
    }
}
