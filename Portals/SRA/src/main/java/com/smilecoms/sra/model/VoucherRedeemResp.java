/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

/**
 *
 * @author rajeshkumar
 */
public class VoucherRedeemResp {
    
    private int prepaidStripId;
    private double valueInCents;
    private String status;
    private double units;
    private int usableDays;
    private int unitCreditSpecificationId;
    private String unitCreditName;
    private String generationDate;

    public int getPrepaidStripId() {
        return prepaidStripId;
    }

    public void setPrepaidStripId(int prepaidStripId) {
        this.prepaidStripId = prepaidStripId;
    }

    public double getValueInCents() {
        return valueInCents;
    }

    public void setValueInCents(double valueInCents) {
        this.valueInCents = valueInCents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(int unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public String getUnitCreditName() {
        return unitCreditName;
    }

    public void setUnitCreditName(String unitCreditName) {
        this.unitCreditName = unitCreditName;
    }

    public String getGenerationDate() {
        return generationDate;
    }

    public void setGenerationDate(String generationDate) {
        this.generationDate = generationDate;
    } 

    public double getUnits() {
        return units;
    }

    public void setUnits(double units) {
        this.units = units;
    }

    public int getUsableDays() {
        return usableDays;
    }

    public void setUsableDays(int usableDays) {
        this.usableDays = usableDays;
    }

    @Override
    public String toString() {
        return "VoucherRedeemResp{" + "prepaidStripId=" + prepaidStripId + ", valueInCents=" + valueInCents + ", status=" + status + ", units=" + units + ", usableDays=" + usableDays + ", unitCreditSpecificationId=" + unitCreditSpecificationId + ", unitCreditName=" + unitCreditName + ", generationDate=" + generationDate + '}';
    }
    
    
}
