/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
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
@Table(name = "sale")
@NamedQueries({
    @NamedQuery(name = "Sale.findAll", query = "SELECT s FROM Sale s")})
public class Sale implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ID")
    private Integer saleId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALES_PERSON_CUSTOMER_ID")
    private int salesPersonCustomerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALES_PERSON_ACCOUNT_ID")
    private long salesPersonAccountId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RECIPIENT_CUSTOMER_ID")
    private int recipientCustomerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RECIPIENT_ACCOUNT_ID")
    private long recipientAccountId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RECIPIENT_ORGANISATION_ID")
    private int recipientOrganisationId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date saleDateTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_TOTAL_CENTS_INCL")
    private BigDecimal saleTotalCentsIncl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_TOTAL_TAX_CENTS")
    private BigDecimal saleTotalTaxCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_TOTAL_CENTS_EXCL")
    private BigDecimal saleTotalCentsExcl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EXT_TXID")
    private String extTxid;
    @Column(name = "UNIQUE_ID")
    private String uniqueId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_TOTAL_DISCOUNT_ON_INCL_CENTS")
    private BigDecimal saleTotalDiscountOnInclCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_TOTAL_DISCOUNT_ON_EXCL_CENTS")
    private BigDecimal saleTotalDiscountOnExclCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "AMOUNT_TENDERED_CENTS")
    private BigDecimal amountTenderedCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CHANGE_CENTS")
    private BigDecimal changeCents;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TENDERED_CURRENCY")
    private String tenderedCurrency;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TENDERED_CURRENCY_EXCHANGE_RATE")
    private BigDecimal tenderedCurrencyExchangeRate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PAYMENT_METHOD")
    private String paymentMethod;
    @Column(name = "PAYMENT_TRANSACTION_DATA")
    private String paymentTransactionData;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Column(name = "PREVIOUS_STATUS")
    private String previousStatus;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_LOCATION")
    private String saleLocation;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TILL_ID")
    private String tillId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CHANNEL")
    private String channel;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_CHANNEL")
    private String organisationChannel;
    @Basic(optional = false)
    @NotNull
    @Column(name = "WAREHOUSE_ID")
    private String warehouseId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PROMOTION_CODE")
    private String promotionCode;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RECIPIENT_NAME")
    private String recipientName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ORGANISATION_NAME")
    private String organisationName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RECIPIENT_PHONE_NUMBER")
    private String recipientPhoneNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CREDIT_ACCOUNT_NUMBER")
    private String creditAccountNumber;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PURCHASE_ORDER_DATA")
    private String purchaseOrderData;
    @Basic(optional = false)
    @Lob
    @Column(name = "INVOICE_PDF")
    private byte[] invoicePDF;
    @Basic(optional = false)
    @Lob
    @Column(name = "SMALL_INVOICE_PDF")
    private byte[] smallInvoicePDF;
    @Basic(optional = false)
    @Lob
    @Column(name = "REVERSAL_PDF")
    private byte[] reversalPDF;
    @NotNull
    @Column(name = "EXTRA_INFO")
    private String extraInfo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TAX_EXEMPT")
    private String taxExempt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "WITHHOLDING_TAX_CENTS")
    private BigDecimal withholdingTaxCents;
    @Basic(optional = false)
    @Column(name = "EXPIRY_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDateTime;
    @Basic(optional = false)
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    @Column(name = "FULFILMENT_SCHEDULE_INFO")
    private String fulfilmentScheduleInfo;
    @Column(name = "FULFILMENT_LAST_CHECK_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fulfilmentLastCheckDateTime;
    @Column(name = "FULFILMENT_PAUSED_TILL_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fulfilmentPausedTillDateTime;
    @Column(name = "CONTRACT_SALE_ID")
    private Integer contractSaleId;
    @Column(name = "CONTRACT_ID")
    private Integer contractId;

    @Column(name = "PAYMENT_GATEWAY_CODE")
    private String paymentGatewayCode;
    @Column(name = "PAYMENT_GATEWAY_POLL_COUNT")
    private int paymentGatewayPollCount;
    @Column(name = "PAYMENT_GATEWAY_LAST_POLL_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paymentGatewayLastPollDate;
    @Column(name = "PAYMENT_GATEWAY_NEXT_POLL_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paymentGatewayNextPollDate;
    @Column(name = "PAYMENT_GATEWAY_RESPONSE")
    private String paymentGatewayResponse;
    @Column(name = "PAYMENT_GATEWAY_EXTRA_DATA")
    private String paymentGatewayExtraData;
    @Column(name = "CALLBACK_URL")
    private String callbackURL;
    @Column(name = "LANDING_URL")
    private String landingURL;

    @Column(name = "TRANSACTION_FEE_CENTS")
    private BigDecimal transactionFeeCents;
    @Column(name = "DELIVERY_FEE_CENTS")
    private BigDecimal deliveryFeeCents;
    @Column(name = "TRANSACTION_FEE_MODEL")
    private String transactionFeeModel;
    @Column(name = "DELIVERY_FEE_MODEL")
    private String deliveryFeeModel;
    
    //@Column(name = "GPS_COORDINATES")
    //private String gpsCoordinates;
    
    public Sale() {
    }

    public Sale(Integer saleId) {
        this.saleId = saleId;
    }

    public Sale(Integer saleId, int salesPersonCustomerId, long salesPersonAccountId, int recipientCustomerId, long recipientAccountId, int recipientOrganisationId, Date saleDateTime, BigDecimal saleTotalCentsIncl, BigDecimal saleTotalTaxCents, BigDecimal saleTotalCentsExcl, String extTxid, BigDecimal saleTotalDiscountOnInclCents, BigDecimal saleTotalDiscountOnExclCents, BigDecimal amountTenderedCents, BigDecimal changeCents, String tenderedCurrency, BigDecimal tenderedCurrencyExchangeRate, String paymentMethod, String paymentTransactionData, String status, String saleLocation, String tillId, String warehouseId, String promotionCode, String recipientName, String organisationName, String recipientPhoneNumber) {
        this.saleId = saleId;
        this.salesPersonCustomerId = salesPersonCustomerId;
        this.salesPersonAccountId = salesPersonAccountId;
        this.recipientCustomerId = recipientCustomerId;
        this.recipientAccountId = recipientAccountId;
        this.recipientOrganisationId = recipientOrganisationId;
        this.saleDateTime = saleDateTime;
        this.saleTotalCentsIncl = saleTotalCentsIncl;
        this.saleTotalTaxCents = saleTotalTaxCents;
        this.saleTotalCentsExcl = saleTotalCentsExcl;
        this.extTxid = extTxid;
        this.saleTotalDiscountOnInclCents = saleTotalDiscountOnInclCents;
        this.saleTotalDiscountOnExclCents = saleTotalDiscountOnExclCents;
        this.amountTenderedCents = amountTenderedCents;
        this.changeCents = changeCents;
        this.tenderedCurrency = tenderedCurrency;
        this.tenderedCurrencyExchangeRate = tenderedCurrencyExchangeRate;
        this.paymentMethod = paymentMethod;
        this.paymentTransactionData = paymentTransactionData;
        this.status = status;
        this.saleLocation = saleLocation;
        this.tillId = tillId;
        this.warehouseId = warehouseId;
        this.promotionCode = promotionCode;
        this.recipientName = recipientName;
        this.organisationName = organisationName;
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public String getGpsCoordinates() {
       return ""; // gpsCoordinates;
    }

    public void setGpsCoordinates(String gpsCoordinates) {
       //this.gpsCoordinates = gpsCoordinates;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public BigDecimal getWithholdingTaxCents() {
        return withholdingTaxCents;
    }

    public void setWithholdingTaxCents(BigDecimal withholdingTaxCents) {
        this.withholdingTaxCents = withholdingTaxCents;
    }

    public String getTaxExempt() {
        return taxExempt;
    }

    public void setTaxExempt(String taxExempt) {
        this.taxExempt = taxExempt;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public int getSalesPersonCustomerId() {
        return salesPersonCustomerId;
    }

    public void setSalesPersonCustomerId(int salesPersonCustomerId) {
        this.salesPersonCustomerId = salesPersonCustomerId;
    }

    public long getSalesPersonAccountId() {
        return salesPersonAccountId;
    }

    public void setSalesPersonAccountId(long salesPersonAccountId) {
        this.salesPersonAccountId = salesPersonAccountId;
    }

    public int getRecipientCustomerId() {
        return recipientCustomerId;
    }

    public void setRecipientCustomerId(int recipientCustomerId) {
        this.recipientCustomerId = recipientCustomerId;
    }

    public long getRecipientAccountId() {
        return recipientAccountId;
    }

    public void setRecipientAccountId(long recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public int getRecipientOrganisationId() {
        return recipientOrganisationId;
    }

    public void setRecipientOrganisationId(int recipientOrganisationId) {
        this.recipientOrganisationId = recipientOrganisationId;
    }

    public Date getSaleDateTime() {
        return saleDateTime;
    }

    public void setSaleDateTime(Date saleDateTime) {
        this.saleDateTime = saleDateTime;
    }

    public BigDecimal getSaleTotalCentsIncl() {
        return saleTotalCentsIncl;
    }

    public void setSaleTotalCentsIncl(BigDecimal saleTotalCentsIncl) {
        this.saleTotalCentsIncl = saleTotalCentsIncl;
    }

    public BigDecimal getSaleTotalTaxCents() {
        return saleTotalTaxCents;
    }

    public void setSaleTotalTaxCents(BigDecimal saleTotalTaxCents) {
        this.saleTotalTaxCents = saleTotalTaxCents;
    }

    public BigDecimal getSaleTotalCentsExcl() {
        return saleTotalCentsExcl;
    }

    public void setSaleTotalCentsExcl(BigDecimal saleTotalCentsExcl) {
        this.saleTotalCentsExcl = saleTotalCentsExcl;
    }

    public String getExtTxid() {
        return extTxid;
    }

    public void setExtTxid(String extTxid) {
        this.extTxid = extTxid;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public BigDecimal getSaleTotalDiscountOnInclCents() {
        return saleTotalDiscountOnInclCents;
    }

    public void setSaleTotalDiscountOnInclCents(BigDecimal saleTotalDiscountOnInclCents) {
        this.saleTotalDiscountOnInclCents = saleTotalDiscountOnInclCents;
    }

    public BigDecimal getSaleTotalDiscountOnExclCents() {
        return saleTotalDiscountOnExclCents;
    }

    public void setSaleTotalDiscountOnExclCents(BigDecimal saleTotalDiscountOnExclCents) {
        this.saleTotalDiscountOnExclCents = saleTotalDiscountOnExclCents;
    }

    public BigDecimal getAmountTenderedCents() {
        return amountTenderedCents;
    }

    public void setAmountTenderedCents(BigDecimal amountTenderedCents) {
        this.amountTenderedCents = amountTenderedCents;
    }

    public BigDecimal getChangeCents() {
        return changeCents;
    }

    public void setChangeCents(BigDecimal changeCents) {
        this.changeCents = changeCents;
    }

    public String getTenderedCurrency() {
        return tenderedCurrency;
    }

    public void setTenderedCurrency(String tenderedCurrency) {
        this.tenderedCurrency = tenderedCurrency;
    }

    public BigDecimal getTenderedCurrencyExchangeRate() {
        return tenderedCurrencyExchangeRate;
    }

    public void setTenderedCurrencyExchangeRate(BigDecimal tenderedCurrencyExchangeRate) {
        this.tenderedCurrencyExchangeRate = tenderedCurrencyExchangeRate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentTransactionData() {
        return paymentTransactionData;
    }

    public void setPaymentTransactionData(String paymentTransactionData) {
        this.paymentTransactionData = paymentTransactionData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status != null && !status.equals(this.status)) {
            setPreviousStatus(this.status);
        }
        this.status = status;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getSaleLocation() {
        return saleLocation;
    }

    public void setSaleLocation(String saleLocation) {
        this.saleLocation = saleLocation;
    }

    public String getTillId() {
        return tillId;
    }

    public void setTillId(String tillId) {
        this.tillId = tillId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOrganisationChannel() {
        return organisationChannel;
    }

    public void setOrganisationChannel(String organisationChannel) {
        this.organisationChannel = organisationChannel;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(String recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public String getCreditAccountNumber() {
        return creditAccountNumber;
    }

    public void setCreditAccountNumber(String creditAccountNumber) {
        this.creditAccountNumber = creditAccountNumber;
    }

    public String getPurchaseOrderData() {
        return purchaseOrderData;
    }

    public void setPurchaseOrderData(String purchaseOrderData) {
        this.purchaseOrderData = purchaseOrderData;
    }

    public byte[] getInvoicePDF() {
        return invoicePDF;
    }

    public void setInvoicePDF(byte[] invoicePDF) {
        this.invoicePDF = invoicePDF;
    }

    public byte[] getSmallInvoicePDF() {
        return smallInvoicePDF;
    }

    public void setSmallInvoicePDF(byte[] smallInvoicePDF) {
        this.smallInvoicePDF = smallInvoicePDF;
    }

    public byte[] getReversalPDF() {
        return reversalPDF;
    }

    public void setReversalPDF(byte[] reversalPDF) {
        this.reversalPDF = reversalPDF;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public Date getExpiryDateTime() {
        return expiryDateTime;
    }

    public void setExpiryDateTime(Date expiryDateTime) {
        this.expiryDateTime = expiryDateTime;
    }

    public String getFulfilmentScheduleInfo() {
        return fulfilmentScheduleInfo;
    }

    public void setFulfilmentScheduleInfo(String fulfilmentScheduleInfo) {
        this.fulfilmentScheduleInfo = fulfilmentScheduleInfo;
    }

    public Date getFulfilmentLastCheckDateTime() {
        return fulfilmentLastCheckDateTime;
    }

    public void setFulfilmentLastCheckDateTime(Date fulfilmentLastCheckDateTime) {
        this.fulfilmentLastCheckDateTime = fulfilmentLastCheckDateTime;
    }

    public Date getFulfilmentPausedTillDateTime() {
        return fulfilmentPausedTillDateTime;
    }

    public void setFulfilmentPausedTillDateTime(Date fulfilmentPausedTillDateTime) {
        this.fulfilmentPausedTillDateTime = fulfilmentPausedTillDateTime;
    }

    public Integer getContractSaleId() {
        return contractSaleId;
    }

    public void setContractSaleId(Integer contractSaleId) {
        this.contractSaleId = contractSaleId;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public String getPaymentGatewayCode() {
        return paymentGatewayCode;
    }

    public void setPaymentGatewayCode(String paymentGatewayCode) {
        this.paymentGatewayCode = paymentGatewayCode;
    }

    public int getPaymentGatewayPollCount() {
        return paymentGatewayPollCount;
    }

    public void setPaymentGatewayPollCount(int paymentGatewayPollCount) {
        this.paymentGatewayPollCount = paymentGatewayPollCount;
    }

    public Date getPaymentGatewayLastPollDate() {
        return paymentGatewayLastPollDate;
    }

    public void setPaymentGatewayLastPollDate(Date paymentGatewayLastPollDate) {
        this.paymentGatewayLastPollDate = paymentGatewayLastPollDate;
    }

    public Date getPaymentGatewayNextPollDate() {
        return paymentGatewayNextPollDate;
    }

    public void setPaymentGatewayNextPollDate(Date paymentGatewayNextPollDate) {
        this.paymentGatewayNextPollDate = paymentGatewayNextPollDate;
    }

    public String getPaymentGatewayResponse() {
        return paymentGatewayResponse;
    }

    public void setPaymentGatewayResponse(String paymentGatewayResponse) {
        this.paymentGatewayResponse = paymentGatewayResponse;
    }

    public String getPaymentGatewayExtraData() {
        return paymentGatewayExtraData;
    }

    public void setPaymentGatewayExtraData(String paymentGatewayExtraData) {
        this.paymentGatewayExtraData = paymentGatewayExtraData;
    }

    public String getCallbackURL() {
        return callbackURL;
    }

    public void setCallbackURL(String callbackURL) {
        this.callbackURL = callbackURL;
    }

    public String getLandingURL() {
        return landingURL;
    }

    public void setLandingURL(String landingURL) {
        this.landingURL = landingURL;
    }

    public BigDecimal getTransactionFeeCents() {
        return transactionFeeCents;
    }

    public void setTransactionFeeCents(BigDecimal transactionFeeCents) {
        this.transactionFeeCents = transactionFeeCents;
    }

    public BigDecimal getDeliveryFeeCents() {
        return deliveryFeeCents;
    }

    public void setDeliveryFeeCents(BigDecimal deliveryFeeCents) {
        this.deliveryFeeCents = deliveryFeeCents;
    }

    public String getTransactionFeeModel() {
        return transactionFeeModel;
    }

    public void setTransactionFeeModel(String transactionFeeModel) {
        this.transactionFeeModel = transactionFeeModel;
    }

    public String getDeliveryFeeModel() {
        return deliveryFeeModel;
    }

    public void setDeliveryFeeModel(String deliveryFeeModel) {
        this.deliveryFeeModel = deliveryFeeModel;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (saleId != null ? saleId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Sale)) {
            return false;
        }
        Sale other = (Sale) object;
        if ((this.saleId == null && other.saleId != null) || (this.saleId != null && !this.saleId.equals(other.saleId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.Sale[ saleId=" + saleId + " ]";
    }

}
