/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "return_replacement")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ReturnReplacement.findAll", query = "SELECT r FROM ReturnReplacement r"),
    @NamedQuery(name = "ReturnReplacement.findByReturnReplacementId", query = "SELECT r FROM ReturnReplacement r WHERE r.returnReplacementId = :returnReplacementId"),
    @NamedQuery(name = "ReturnReplacement.findByCreatedDateTime", query = "SELECT r FROM ReturnReplacement r WHERE r.createdDateTime = :createdDateTime"),
    @NamedQuery(name = "ReturnReplacement.findByLastModified", query = "SELECT r FROM ReturnReplacement r WHERE r.lastModified = :lastModified"),
    @NamedQuery(name = "ReturnReplacement.findByCreatedByCustomerProfileId", query = "SELECT r FROM ReturnReplacement r WHERE r.createdByCustomerProfileId = :createdByCustomerProfileId"),
    @NamedQuery(name = "ReturnReplacement.findByReplacementItemNumber", query = "SELECT r FROM ReturnReplacement r WHERE r.replacementItemNumber = :replacementItemNumber"),
    @NamedQuery(name = "ReturnReplacement.findByReplacementSerialNumber", query = "SELECT r FROM ReturnReplacement r WHERE r.replacementSerialNumber = :replacementSerialNumber"),
    @NamedQuery(name = "ReturnReplacement.findByLocation", query = "SELECT r FROM ReturnReplacement r WHERE r.location = :location"),
    @NamedQuery(name = "ReturnReplacement.findByReasonCode", query = "SELECT r FROM ReturnReplacement r WHERE r.reasonCode = :reasonCode"),
    @NamedQuery(name = "ReturnReplacement.findByDescription", query = "SELECT r FROM ReturnReplacement r WHERE r.description = :description")})
public class ReturnReplacement implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "RETURN_REPLACEMENT_ID")
    private Integer returnReplacementId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDateTime;
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREATED_BY_CUSTOMER_PROFILE_ID")
    private int createdByCustomerProfileId;
    @Size(max = 20)
    @Column(name = "REPLACEMENT_ITEM_NUMBER")
    private String replacementItemNumber;
    @Size(max = 200)
    @Column(name = "REPLACEMENT_SERIAL_NUMBER")
    private String replacementSerialNumber;
    @Size(max = 200)
    @Column(name = "RETURNED_SERIAL_NUMBER")
    private String returnedSerialNumber;
    @Size(max = 20)
    @Column(name = "RETURNED_ITEM_NUMBER")
    private String returnedItemNumber;
    @Size(max = 200)
    @Column(name = "LOCATION")
    private String location;
    @Size(max = 100)
    @Column(name = "REASON_CODE")
    private String reasonCode;
    @Size(max = 2000)
    @Column(name = "DESCRIPTION")
    private String description;
    
    @Size(max = 2000)
    @Column(name = "REPLACEMENT_ITEM_DESCRIPTION")
    private String replacementItemDescription;
            
    @Column(name = "SALE_ROW_ID")
    private int saleRowId;
    
    @Basic(optional = false)
    @Lob
    @Column(name = "RETURN_REPLACEMENT_PDF")
    private byte[] returnReplacementPDF;
    
    @Column(name = "PARENT_RETURN_REPLACEMENT_ID")
    private Integer parentReturnReplacementId;
    
    public int getSaleRowId() {
        return this.saleRowId;
    }

    public void setSaleRowId(int id) {
        this.saleRowId = id;
    }
    
    public Integer getParentReturnReplacementId() {
        return parentReturnReplacementId;
    }
    

    public void setParentReturnReplacementId(Integer parentReturnReplacementId) {
        this.parentReturnReplacementId = parentReturnReplacementId;
    }

    
    public ReturnReplacement() {
    }

    public ReturnReplacement(Integer returnReplacementId) {
        this.returnReplacementId = returnReplacementId;
    }

    public ReturnReplacement(Integer returnReplacementId, Date createdDateTime, int createdByCustomerProfileId) {
        this.returnReplacementId = returnReplacementId;
        this.createdDateTime = createdDateTime;
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public Integer getReturnReplacementId() {
        return returnReplacementId;
    }

    public void setReturnReplacementId(Integer returnReplacementId) {
        this.returnReplacementId = returnReplacementId;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public int getCreatedByCustomerProfileId() {
        return createdByCustomerProfileId;
    }

    public void setCreatedByCustomerProfileId(int createdByCustomerProfileId) {
        this.createdByCustomerProfileId = createdByCustomerProfileId;
    }

    public String getReplacementItemNumber() {
        return replacementItemNumber;
    }

    public void setReplacementItemNumber(String replacementItemNumber) {
        this.replacementItemNumber = replacementItemNumber;
    }

    public String getReplacementSerialNumber() {
        return replacementSerialNumber;
    }

    public void setReplacementSerialNumber(String replacementSerialNumber) {
        this.replacementSerialNumber = replacementSerialNumber;
    }

    public String getReturnedSerialNumber() {
        return returnedSerialNumber;
    }

    public void setReturnedSerialNumber(String returnedSerialNumber) {
        this.returnedSerialNumber = returnedSerialNumber;
    }
    
    public String getReturnedItemNumber() {
        return returnedItemNumber;
    }

    public void setReturnedItemNumber(String returnedItemNumber) {
        this.returnedItemNumber = returnedItemNumber;
    }
    
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    
    public String getReplacementItemDescription() {
        return replacementItemDescription;
    }

    public void setReplacementItemDescription(String replacementItemDescription) {
        this.replacementItemDescription = replacementItemDescription;
    }
    
    public byte[] getReturnReplacementPDF() {
        return returnReplacementPDF;
    }

    public void setReturnReplacementPDF(byte[] returnReplacementPDF) {
        this.returnReplacementPDF = returnReplacementPDF;
    }
    
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (returnReplacementId != null ? returnReplacementId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ReturnReplacement)) {
            return false;
        }
        ReturnReplacement other = (ReturnReplacement) object;
        if ((this.returnReplacementId == null && other.returnReplacementId != null) || (this.returnReplacementId != null && !this.returnReplacementId.equals(other.returnReplacementId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.ReturnReplacement[ returnReplacementId=" + returnReplacementId + " ]";
    }
    
}
