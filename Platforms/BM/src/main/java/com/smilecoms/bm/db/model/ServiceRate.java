/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
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
@Table(name = "service_rate")
@NamedQueries({
    @NamedQuery(name = "ServiceRate.findAll", query = "SELECT s FROM ServiceRate s")})
public class ServiceRate implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "SERVICE_RATE_ID")
    private Integer serviceRateId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LEG")
    private String leg;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SERVICE_CODE")
    private String serviceCode;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FROM_PREFIX")
    private long fromPrefix;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TO_PREFIX")
    private long toPrefix;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FROM_INTERCONNECT_CENTS_PER_UNIT")
    private BigDecimal fromInterconnectCentsPerUnit;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FROM_INTERCONNECT_RATE_CURRENCY")
    private String fromInterconnectRateCurrency;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TO_INTERCONNECT_CENTS_PER_UNIT")
    private BigDecimal toInterconnectCentsPerUnit;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TO_INTERCONNECT_RATE_CURRENCY")
    private String toInterconnectRateCurrency;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RETAIL_CENTS_PER_UNIT")
    private BigDecimal retailCentsPerUnit;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATING_HINT")
    private String ratingHint;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DATE_FROM")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateFrom;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DATE_TO")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateTo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRIORITY")
    private int priority;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DESCRIPTION")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FROM_INTERCONNECT_PARTNER_ID")
    private int fromInterconnectPartnerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TO_INTERCONNECT_PARTNER_ID")
    private int toInterconnectPartnerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RATE_PLAN_MATCH")
    private String ratePlanMatch;

    public ServiceRate() {
    }

    public ServiceRate(Integer serviceRateId) {
        this.serviceRateId = serviceRateId;
    }

    public Integer getServiceRateId() {
        return serviceRateId;
    }

    public void setServiceRateId(Integer serviceRateId) {
        this.serviceRateId = serviceRateId;
    }

    public String getLeg() {
        return leg;
    }

    public void setLeg(String leg) {
        this.leg = leg;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public int getFromInterconnectPartnerId() {
        return fromInterconnectPartnerId;
    }

    public void setFromInterconnectPartnerId(int fromInterconnectPartnerId) {
        this.fromInterconnectPartnerId = fromInterconnectPartnerId;
    }

    public int getToInterconnectPartnerId() {
        return toInterconnectPartnerId;
    }

    public void setToInterconnectPartnerId(int toInterconnectPartnerId) {
        this.toInterconnectPartnerId = toInterconnectPartnerId;
    }

    public long getFromPrefix() {
        return fromPrefix;
    }

    public void setFromPrefix(long fromPrefix) {
        this.fromPrefix = fromPrefix;
    }

    public long getToPrefix() {
        return toPrefix;
    }

    public void setToPrefix(long toPrefix) {
        this.toPrefix = toPrefix;
    }

    public BigDecimal getFromInterconnectCentsPerUnit() {
        return fromInterconnectCentsPerUnit;
    }

    public void setFromInterconnectCentsPerUnit(BigDecimal fromInterconnectCentsPerUnit) {
        this.fromInterconnectCentsPerUnit = fromInterconnectCentsPerUnit;
    }

    public String getFromInterconnectRateCurrency() {
        return fromInterconnectRateCurrency;
    }

    public void setFromInterconnectRateCurrency(String fromInterconnectRateCurrency) {
        this.fromInterconnectRateCurrency = fromInterconnectRateCurrency;
    }

    public BigDecimal getToInterconnectCentsPerUnit() {
        return toInterconnectCentsPerUnit;
    }

    public void setToInterconnectCentsPerUnit(BigDecimal toInterconnectCentsPerUnit) {
        this.toInterconnectCentsPerUnit = toInterconnectCentsPerUnit;
    }

    public String getToInterconnectRateCurrency() {
        return toInterconnectRateCurrency;
    }

    public void setToInterconnectRateCurrency(String toInterconnectRateCurrency) {
        this.toInterconnectRateCurrency = toInterconnectRateCurrency;
    }

    public BigDecimal getRetailCentsPerUnit() {
        return retailCentsPerUnit;
    }

    public String getRatingHint() {
        return ratingHint;
    }

    public void setRatingHint(String ratingHint) {
        this.ratingHint = ratingHint;
    }

    public void setRetailCentsPerUnit(BigDecimal retailCentsPerUnit) {
        this.retailCentsPerUnit = retailCentsPerUnit;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRatePlanMatch() {
        return ratePlanMatch;
    }

    public void setRatePlanMatch(String ratePlanMatch) {
        this.ratePlanMatch = ratePlanMatch;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (serviceRateId != null ? serviceRateId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceRate)) {
            return false;
        }
        ServiceRate other = (ServiceRate) object;
        if ((this.serviceRateId == null && other.serviceRateId != null) || (this.serviceRateId != null && !this.serviceRateId.equals(other.serviceRateId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.ServiceRate[ serviceRateId=" + serviceRateId + " ]";
    }
    
}
