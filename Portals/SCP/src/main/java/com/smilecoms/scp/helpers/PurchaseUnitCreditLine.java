package com.smilecoms.scp.helpers;

import com.smilecoms.commons.sca.UnitCreditSpecification;
import javax.xml.datatype.XMLGregorianCalendar;

public class PurchaseUnitCreditLine {

    private long accountId;
    private long paidByAccountId;
    private int unitCreditSpecificationId;
    private String unitCreditName;
    private String itemNumber;
    private Integer productInstanceId;
    private int numberToPurchase;
    private int daysGapBetweenStart;
    private XMLGregorianCalendar startDate;
    private String uniqueId;
    private String channel;
    private String paymentMethod;
    private String creditAccountNumber;
    private String info;
    private String promotionCode;
    private UnitCreditSpecification unitCreditSpecification;

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long value) {
        this.accountId = value;
    }

    public UnitCreditSpecification getUnitCreditSpecification() {
        return unitCreditSpecification;
    }

    public void setUnitCreditSpecification(UnitCreditSpecification unitCreditSpecification) {
        this.unitCreditSpecification = unitCreditSpecification;
    }

    public long getPaidByAccountId() {
        return paidByAccountId;
    }

    public void setPaidByAccountId(long value) {
        this.paidByAccountId = value;
    }

    public int getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(int value) {
        this.unitCreditSpecificationId = value;
    }

    public String getUnitCreditName() {
        return unitCreditName;
    }

    public void setUnitCreditName(String value) {
        this.unitCreditName = value;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String value) {
        this.itemNumber = value;
    }

    public Integer getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(Integer value) {
        this.productInstanceId = value;
    }

    public int getNumberToPurchase() {
        return numberToPurchase;
    }

    public void setNumberToPurchase(int value) {
        this.numberToPurchase = value;
    }

    public int getDaysGapBetweenStart() {
        return daysGapBetweenStart;
    }

    public void setDaysGapBetweenStart(int value) {
        this.daysGapBetweenStart = value;
    }

    public XMLGregorianCalendar getStartDate() {
        return startDate;
    }

    public void setStartDate(XMLGregorianCalendar value) {
        this.startDate = value;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String value) {
        this.uniqueId = value;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String value) {
        this.channel = value;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String value) {
        this.paymentMethod = value;
    }

    public String getCreditAccountNumber() {
        return creditAccountNumber;
    }

    public void setCreditAccountNumber(String value) {
        this.creditAccountNumber = value;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String value) {
        this.info = value;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String value) {
        this.promotionCode = value;
    }

}
