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

/**
 *
 * @author paul
 */
@Entity
@Table(name = "account_history")
@NamedQueries({
    @NamedQuery(name = "AccountHistory.findAll", query = "SELECT b FROM AccountHistory b")})
public class AccountHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Long id;
    @Column(name = "SOURCE")
    private String source;
    @Column(name = "DESTINATION")
    private String destination;
    @Column(name = "START_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;
    @Column(name = "END_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;
    @Column(name = "EXT_TXID")
    private String extTxid;
    @Column(name = "ACCOUNT_CENTS")
    private BigDecimal accountCents;
    @Column(name = "SOURCE_DEVICE")
    private String sourceDevice;
    @Column(name = "TOTAL_UNITS")
    private BigDecimal totalUnits;
    @Column(name = "TERM_CODE")
    private String termCode;
    @Column(name = "TRANSACTION_TYPE")
    private String transactionType;
    @Column(name = "ACCOUNT_BALANCE_REMAINING")
    private BigDecimal accountBalanceRemaining;
    @Column(name = "DESCRIPTION")
    private String description;
    @Column(name = "STATUS")
    private String status;
    @Column(name = "ACCOUNT_ID")
    private long accountId;
    @Column(name = "SERVICE_INSTANCE_ID")
    private int serviceInstanceId;
    @Column(name = "UNIT_CREDIT_UNITS")
    private BigDecimal unitCreditUnits;
    @Column(name = "UNIT_CREDIT_BASELINE_UNITS")
    private BigDecimal unitCreditBaselineUnits;
    @Column(name = "REVENUE_CENTS")
    private BigDecimal revenueCents;
    @Column(name = "UNEARNED_REVENUE_CENTS")
    private BigDecimal unearnedRevenueCents;
    @Column(name = "FREE_REVENUE_CENTS")
    private BigDecimal freeRevenueCents;
    @Column(name = "LOCATION")
    private String location;
    @Column(name = "IP_ADDRESS")
    private String ipAddress;
    @Column(name = "SERVICE_INSTANCE_IDENTIFIER")
    private String serviceInstanceIdentifier;
    @Column(name = "INCOMING_TRUNK")
    private String incomingTrunk;
    @Column(name = "OUTGOING_TRUNK")
    private String outgoingTrunk;
    @Column(name = "LEG")
    private String leg;
    @Column(name = "RATE_GROUP")
    private String rateGroup;
    @Column(name = "INFO")
    private String info;
    
    public AccountHistory() {
    }

    public AccountHistory(Long id) {
        this.id = id;
    }

    public String getServiceInstanceIdentifier() {
        return serviceInstanceIdentifier;
    }

    public void setServiceInstanceIdentifier(String serviceInstanceIdentifier) {
        this.serviceInstanceIdentifier = serviceInstanceIdentifier;
    }

    public String getIPAddress() {
        return ipAddress;
    }

    public void setIPAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIncomingTrunk() {
        return incomingTrunk;
    }

    public void setIncomingTrunk(String incomingTrunk) {
        this.incomingTrunk = incomingTrunk;
    }

    public String getLeg() {
        return leg;
    }

    public void setLeg(String leg) {
        this.leg = leg;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
    
    public String getRateGroup() {
        return rateGroup;
    }

    public void setRateGroup(String rateGroup) {
        this.rateGroup = rateGroup;
    }
    
    public String getOutgoingTrunk() {
        return outgoingTrunk;
    }

    public void setOutgoingTrunk(String outgoingTrunk) {
        this.outgoingTrunk = outgoingTrunk;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    
    public BigDecimal getRevenueCents() {
        return revenueCents;
    }

    public void setRevenueCents(BigDecimal revenueCents) {
        this.revenueCents = revenueCents;
    }

    public BigDecimal getUnearnedRevenueCents() {
        return unearnedRevenueCents;
    }

    public void setUnearnedRevenueCents(BigDecimal unearnedRevenueCents) {
        this.unearnedRevenueCents = unearnedRevenueCents;
    }

    public BigDecimal getFreeRevenueCents() {
        return freeRevenueCents;
    }

    public void setFreeRevenueCents(BigDecimal freeRevenueCents) {
        this.freeRevenueCents = freeRevenueCents;
    }
    
    public int getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(int serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public BigDecimal getAccountBalanceRemaining() {
        return accountBalanceRemaining;
    }

    public void setAccountBalanceRemaining(BigDecimal accountBalanceRemaining) {
        this.accountBalanceRemaining = accountBalanceRemaining;
    }

    public BigDecimal getAccountCents() {
        return accountCents;
    }

    public void setAccountCents(BigDecimal accountCents) {
        this.accountCents = accountCents;
    }

    public BigDecimal getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(BigDecimal totalUnits) {
        this.totalUnits = totalUnits;
    }


    public BigDecimal getUnitCreditUnits() {
        return unitCreditUnits;
    }

    public void setUnitCreditUnits(BigDecimal unitCreditUnits) {
        this.unitCreditUnits = unitCreditUnits;
    }

    public BigDecimal getUnitCreditBaselineUnits() {
        return unitCreditBaselineUnits;
    }

    public void setUnitCreditBaselineUnits(BigDecimal unitCreditBaselineUnits) {
        this.unitCreditBaselineUnits = unitCreditBaselineUnits;
    }
    
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    

    public String getExtTxId() {
        return extTxid;
    }

    public void setExtTxId(String extTxid) {
        this.extTxid = extTxid;
    }



    public String getSourceDevice() {
        return sourceDevice;
    }

    public void setSourceDevice(String sourceDevice) {
        this.sourceDevice = sourceDevice;
    }

    public String getTermCode() {
        return termCode;
    }

    public void setTermCode(String termCode) {
        if (termCode.length() > 200) {
            termCode = termCode.substring(0, 200);
        }
        this.termCode = termCode;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AccountHistory)) {
            return false;
        }
        AccountHistory other = (AccountHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.bm.db.model.AccountHistory[ id=" + id + " ]";
    }
    
}
