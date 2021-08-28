/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "promotion_code_approval")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PromotionCodeApproval.findAll", query = "SELECT p FROM PromotionCodeApproval p")})
public class PromotionCodeApproval implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "APPROVAL_HASH")
    private String approvalHash;
    @Basic(optional = false)
    @NotNull
    @Column(name = "APPROVAL_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvalDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "APPROVAL_CUSTOMER_ID")
    private int approvalCustomerId;
    @Column(name = "SALE_ID")
    private Integer saleId;
    
    public PromotionCodeApproval() {
    }

    public PromotionCodeApproval(String approvalHash) {
        this.approvalHash = approvalHash;
    }

    public PromotionCodeApproval(String approvalHash, Date approvalDateTime) {
        this.approvalHash = approvalHash;
        this.approvalDateTime = approvalDateTime;
    }

    public String getApprovalHash() {
        return approvalHash;
    }

    public void setApprovalHash(String approvalHash) {
        this.approvalHash = approvalHash;
    }

    public Date getApprovalDateTime() {
        return approvalDateTime;
    }

    public void setApprovalDateTime(Date approvalDateTime) {
        this.approvalDateTime = approvalDateTime;
    }

    public int getApprovalCustomerId() {
        return approvalCustomerId;
    }

    public void setApprovalCustomerId(int approvalCustomerId) {
        this.approvalCustomerId = approvalCustomerId;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (approvalHash != null ? approvalHash.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PromotionCodeApproval)) {
            return false;
        }
        PromotionCodeApproval other = (PromotionCodeApproval) object;
        if ((this.approvalHash == null && other.approvalHash != null) || (this.approvalHash != null && !this.approvalHash.equals(other.approvalHash))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.PromotionCodeApproval[ approvalHash=" + approvalHash + " ]";
    }
    
}
