/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.sca.SalesQuery;
import com.smilecoms.commons.sca.StSaleLookupVerbosity;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public final class SaleBean extends BaseBean {

    private static final Logger log = LoggerFactory.getLogger(SaleBean.class);
    private final com.smilecoms.commons.sca.Sale scaSale;

    public SaleBean() {
        log.debug("In constructor of SaleBean");
        scaSale = new Sale();
    }

    public static SaleBean getSaleById(int saleId) {
        log.debug("Getting Sale by Id [{}]", saleId);
        return new SaleBean(SCAWrapper.getUserSpecificInstance().getSale(saleId, StSaleLookupVerbosity.SALE_LINES));
    }
    
    public static SaleBean getSaleByLineId(int saleLineId) {
        log.debug("Getting Sale by Line Id [{}]", saleLineId);
        SalesQuery s = new SalesQuery();
        s.setSaleLineId(saleLineId);
        s.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
        List<Sale> sales = SCAWrapper.getUserSpecificInstance().getSales(s).getSales();
        checkForNoResult(sales);
        return new SaleBean(sales.get(0));
    }

    public static SaleBean getSaleByPurchaseOrder(String purchaseOrder) {
        log.debug("Getting Sale by purchase order [{}]", purchaseOrder);
        SalesQuery s = new SalesQuery();
        s.setPurchaseOrderData(purchaseOrder);
        s.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
        List<Sale> sales = SCAWrapper.getUserSpecificInstance().getSales(s).getSales();
        checkForNoResult(sales);
        return new SaleBean(sales.get(0));
    }

    public static List<SaleBean> getSalesByCustomerProfileId(int customerProfileId, Date dateFrom, Date dateTo) {
        log.debug("Getting Sales by customer profile id [{}]", customerProfileId);
        SalesQuery s = new SalesQuery();
        s.setRecipientCustomerId(customerProfileId);
        s.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
        s.setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        s.setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        List<Sale> sales = SCAWrapper.getUserSpecificInstance().getSales(s).getSales();
        return wrap(sales);
    }

    public static List<SaleBean> getSalesBySerialNumber(String serialNumber) {
        log.debug("Getting Sales by seral number [{}]", serialNumber);
        SalesQuery s = new SalesQuery();
        s.setSerialNumber(serialNumber);
        s.setVerbosity(StSaleLookupVerbosity.SALE_LINES);
        List<Sale> sales = SCAWrapper.getUserSpecificInstance().getSales(s).getSales();
        return wrap(sales);
    }

    static List<SaleBean> wrap(List<Sale> scaSales) {
        List<SaleBean> sales = new ArrayList<>();
        for (Sale sale : scaSales) {
            sales.add(new SaleBean(sale));
        }
        return sales;
    }

    public static SaleBean processSale(SaleBean sale) {
        Sale scaSale = new Sale();
        scaSale.setPaymentMethod(sale.getPaymentMethod());
        scaSale.setSalesPersonCustomerId(sale.getSalesPersonCustomerId());
        scaSale.setRecipientCustomerId(sale.getRecipientCustomerId());
        scaSale.setRecipientAccountId(sale.getRecipientAccountId());
        scaSale.setSalesPersonAccountId(sale.getSalesPersonAccountId());
        scaSale.setRecipientOrganisationId(sale.getRecipientOrganisationId());
        scaSale.setAmountTenderedCents(sale.getAmountTenderedCents());
        scaSale.setTillId(sale.getTillId());
        scaSale.setSaleTotalCentsIncl(sale.getSaleTotalCentsIncl());
        scaSale.setChannel(sale.getChannel());
        scaSale.setWarehouseId(sale.getWarehouseId());
        scaSale.setPromotionCode(sale.getPromotionCode());
        scaSale.setPaymentTransactionData(sale.getPaymentTransactionData());
        scaSale.setPurchaseOrderData(sale.getPurchaseOrderData());
        scaSale.setCreditAccountNumber(sale.getCreditAccountNumber());
        scaSale.setTaxExempt(sale.isTaxExempt());
        scaSale.setTenderedCurrency(sale.getTenderedCurrency());
        scaSale.setExtraInfo(sale.getExtraInfo());
        scaSale.setWithholdingTaxRate(sale.getWithholdingTaxRate());
        scaSale.getSaleLines().addAll(sale.getSaleLines());
        scaSale.setCallbackURL(sale.getCallbackURL());
        scaSale.setPaymentGatewayCode(sale.getPaymentGatewayCode());
        scaSale.setLandingURL(sale.getLandingURL());
        return new SaleBean(SCAWrapper.getUserSpecificInstance().processSale(scaSale));
    }
    
    public SaleBean(com.smilecoms.commons.sca.Sale scaSale) {
        this.scaSale = scaSale;
    }

    @XmlElement
    public int getSaleId() {
        return scaSale.getSaleId();
    }

    @XmlElement
    public long getSaleDate() {
        return Utils.getJavaDate(scaSale.getSaleDate()).getTime();
    }

    @XmlElement
    public int getRecipientCustomerId() {
        return scaSale.getRecipientCustomerId();
    }

    @XmlElement
    public String getPaymentMethod() {
        return scaSale.getPaymentMethod();
    }
    
    @XmlElement
    public String getTenderedCurrency() {
        return scaSale.getTenderedCurrency();
    }

    @XmlElement
    public double getSaleTotalCentsIncl() {
        return scaSale.getSaleTotalCentsIncl();
    }
    
    @XmlElement
    public double getWithholdingTaxRate() {
        return scaSale.getWithholdingTaxRate();
    }
    
    @XmlElement
    public boolean isTaxExempt() {
        return scaSale.isTaxExempt();
    }

    @XmlElement
    public String getStatus() {
        return scaSale.getStatus();
    }
    
    @XmlElement
    public String getWarehouseId() {
        return scaSale.getWarehouseId();
    }
    
    @XmlElement
    public String getExtraInfo() {
        return scaSale.getExtraInfo();
    }
    
    @XmlElement
    public String getPromotionCode() {
        return scaSale.getPromotionCode();
    }
    
    @XmlElement
    public String getPaymentTransactionData() {
        return scaSale.getPaymentTransactionData();
    }

    @XmlElement
    public String getChannel() {
        return scaSale.getChannel();
    }
    
    @XmlElement
    public String getTillId() {
        return scaSale.getTillId();
    }

    @XmlElement
    public String getCreditAccountNumber() {
        return scaSale.getCreditAccountNumber();
    }

    @XmlElement
    public String getOrganisationName() {
        return scaSale.getOrganisationName();
    }

    @XmlElement
    public String getPurchaseOrderData() {
        return scaSale.getPurchaseOrderData();
    }

    @XmlElement
    public long getRecipientAccountId() {
        return scaSale.getRecipientAccountId();
    }

    @XmlElement
    public int getRecipientOrganisationId() {
        return scaSale.getRecipientOrganisationId();
    }
    
    @XmlElement
    public int getSalesPersonCustomerId() {
        return scaSale.getSalesPersonCustomerId();
    }
    
    @XmlElement
    public long getSalesPersonAccountId() {
        return scaSale.getSalesPersonAccountId();
    }

    @XmlElement
    public double getSaleTotalTaxCents() {
        return scaSale.getSaleTotalTaxCents();
    }
    
    @XmlElement
    public double getAmountTenderedCents() {
        return scaSale.getAmountTenderedCents();
    }
    
    @XmlElement
    public String getExtTxId() {
        return scaSale.getExtTxId();
    }

    @XmlElement
    public List<SaleLine> getSaleLines() {
        return scaSale.getSaleLines();
    }
    
    public void setSalesPersonCustomerId(int value) {
        scaSale.setSalesPersonCustomerId(value);
    }

    public void setSalesPersonAccountId(long value) {
        scaSale.setSalesPersonAccountId(value);
    }

    public void setRecipientCustomerId(int value) {
        scaSale.setRecipientCustomerId(value);
    }

    public void setRecipientAccountId(long value) {
        scaSale.setRecipientAccountId(value);
    }

    public void setExpiryDate(Date value) {
        scaSale.setExpiryDate(Utils.getDateAsXMLGregorianCalendar(value));
    }

    public void setSaleTotalCentsIncl(double value) {
        scaSale.setSaleTotalCentsIncl(value);
    }

    public void setRecipientOrganisationId(int value) {
        scaSale.setRecipientOrganisationId(value);
    }

    public void setAmountTenderedCents(double value) {
        scaSale.setAmountTenderedCents(value);
    }

    public void setChangeCents(double value) {
        scaSale.setChangeCents(value);
    }

    public void setTenderedCurrency(String value) {
        scaSale.setTenderedCurrency(value);
    }

    public void setTenderedCurrencyExchangeRate(double value) {
        scaSale.setTenderedCurrencyExchangeRate(value);
    }

    public void setPaymentMethod(String value) {
        scaSale.setPaymentMethod(value);
    }

    public void setPaymentTransactionData(String value) {
        scaSale.setPaymentTransactionData(value);
    }

    public void setStatus(String value) {
        scaSale.setStatus(value);
    }

    public void setTillId(String value) {
        scaSale.setTillId(value);
    }

    public void setChannel(String value) {
        scaSale.setChannel(value);
    }

    public void setOrganisationChannel(String value) {
        scaSale.setOrganisationChannel(value);
    }

    public void setWarehouseId(String value) {
        scaSale.setWarehouseId(value);
    }

    public void setPromotionCode(String value) {
        scaSale.setPromotionCode(value);
    }

    public void setRecipientName(String value) {
        scaSale.setRecipientName(value);
    }

    public void setOrganisationName(String value) {
        scaSale.setOrganisationName(value);
    }

    public void setRecipientPhoneNumber(String value) {
        scaSale.setRecipientPhoneNumber(value);
    }
    
    @XmlElement
    public String getRecipientPhoneNumber(){
        return scaSale.getRecipientPhoneNumber();
    }

    public void setPurchaseOrderData(String value) {
        scaSale.setPurchaseOrderData(value);
    }

    public void setCreditAccountNumber(String value) {
        scaSale.setCreditAccountNumber(value);
    }

    public void setExtraInfo(String value) {
        scaSale.setExtraInfo(value);
    }

    public void setTaxExempt(boolean value) {
        scaSale.setTaxExempt(value);
    }

    public void setWithholdingTaxRate(double value) {
        scaSale.setWithholdingTaxRate(value);
    }
    
    public void setExtTxId(String value) {
        scaSale.setExtTxId(value);
    }

    public void setFulfilmentScheduleInfo(String value) {
        scaSale.setFulfilmentScheduleInfo(value);
    }

    public Integer getContractSaleId() {
        return scaSale.getContractSaleId();
    }

    public void setContractSaleId(Integer value) {
        scaSale.setContractSaleId(value);
    }

    public Integer getContractId() {
        return scaSale.getContractId();
    }

    public void setContractId(Integer value) {
        scaSale.setContractId(value);
    }

    public String getPaymentGatewayCode() {
        return scaSale.getPaymentGatewayCode();
    }

    public void setPaymentGatewayCode(String value) {
        scaSale.setPaymentGatewayCode(value);
    }

    public Integer getPaymentGatewayPollCount() {
        return scaSale.getPaymentGatewayPollCount();
    }

    public void setPaymentGatewayPollCount(Integer value) {
        scaSale.setPaymentGatewayPollCount(value);
    }

    @XmlElement
    public long getPaymentGatewayLastPollDate() {
        return scaSale.getPaymentGatewayLastPollDate() == null ? 0 : Utils.getJavaDate(scaSale.getPaymentGatewayLastPollDate()).getTime();
    }

    public void setPaymentGatewayLastPollDate(Date value) {
        scaSale.setPaymentGatewayLastPollDate(Utils.getDateAsXMLGregorianCalendar(value));
    }

    @XmlElement
    public long getPaymentGatewayNextPollDate() {
        return scaSale.getPaymentGatewayNextPollDate() == null ? 0 : Utils.getJavaDate(scaSale.getPaymentGatewayNextPollDate()).getTime();
    }

    public void setPaymentGatewayNextPollDate(Date value) {
        scaSale.setPaymentGatewayNextPollDate(Utils.getDateAsXMLGregorianCalendar(value));
    }

    @XmlElement
    public String getPaymentGatewayResponse() {
        return scaSale.getPaymentGatewayResponse();
    }

    public void setPaymentGatewayResponse(String value) {
        scaSale.setPaymentGatewayResponse(value);
    }

    @XmlElement
    public String getPaymentGatewayExtraData() {
        return scaSale.getPaymentGatewayExtraData();
    }

    public void setPaymentGatewayExtraData(String value) {
        scaSale.setPaymentGatewayExtraData(value);
    }

    @XmlElement
    public String getPaymentGatewayURL() {
        return scaSale.getPaymentGatewayURL();
    }

    public void setPaymentGatewayURL(String value) {
        scaSale.setPaymentGatewayURL(value);
    }

    @XmlElement
    public String getPaymentGatewayURLData() {
        return scaSale.getPaymentGatewayURLData();
    }

    public void setPaymentGatewayURLData(String value) {
        scaSale.setPaymentGatewayURLData(value);
    }

    public String getInvoicePDFBase64() {
        return scaSale.getInvoicePDFBase64();
    }

    public String getReversalPDFBase64() {
        return scaSale.getReversalPDFBase64();
    }

    @XmlElement
    public String getCallbackURL() {
        return scaSale.getCallbackURL();
    }

    public void setCallbackURL(String value) {
        scaSale.setCallbackURL(value);
    }

    @XmlElement
    public String getLandingURL() {
        return scaSale.getLandingURL();
    }

    public void setLandingURL(String value) {
        scaSale.setLandingURL(value);
    }

}
