/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import java.util.Map;

/**
 *
 * @author rajeshkumar
 */
public class UnitCredit {

    private int unitCreditSpecificationId;
    private String name;
    private String itemNumber;
    private double priceInCents;
    private int validityDays;
    private int usableDays;
    private double units;
    private String unitType;
    private String description;
    private Map<String, String> config;

    public int getUnitCreditSpecificationId() {
        return unitCreditSpecificationId;
    }

    public void setUnitCreditSpecificationId(int unitCreditSpecificationId) {
        this.unitCreditSpecificationId = unitCreditSpecificationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public double getPriceInCents() {
        return priceInCents;
    }

    public void setPriceInCents(double priceInCents) {
        this.priceInCents = priceInCents;
    }

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }

    public int getUsableDays() {
        return usableDays;
    }

    public void setUsableDays(int usableDays) {
        this.usableDays = usableDays;
    }

    public double getUnits() {
        return units;
    }

    public void setUnits(double units) {
        this.units = units;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    
}
