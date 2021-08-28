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
public class UnitCreditInstance {

    private int unitCreditInstanceId;

    private String name;

    private String unitType;

    private Integer unitCreditSpecificationId;

    private Long accountId;

    private Double currentUnitsRemaining;

    private Double availableUnitsRemaining;

    private Integer productInstanceId;

    public int getUnitCreditInstanceId() {
        return unitCreditInstanceId;
    }

    public void setUnitCreditInstanceId(int unitCreditInstanceId) {
        this.unitCreditInstanceId = unitCreditInstanceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public Integer getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(Integer unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Double getCurrentUnitsRemaining() {
        return currentUnitsRemaining;
    }

    public void setCurrentUnitsRemaining(Double currentUnitsRemaining) {
        this.currentUnitsRemaining = currentUnitsRemaining;
    }

    public Double getAvailableUnitsRemaining() {
        return availableUnitsRemaining;
    }

    public void setAvailableUnitsRemaining(Double availableUnitsRemaining) {
        this.availableUnitsRemaining = availableUnitsRemaining;
    }

    public Integer getProductInstanceId() {
        return productInstanceId;
    }

    public void setProductInstanceId(Integer productInstanceId) {
        this.productInstanceId = productInstanceId;
    }

}
