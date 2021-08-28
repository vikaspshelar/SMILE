/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.model;

import java.math.BigDecimal;

/**
 *
 * @author rajeshkumar
 */
public class InternationalCallRate {
    
    private String country;
    private String service;
    private BigDecimal centsPerUnit;
    private String unitCreditName;
    private BigDecimal mib;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public BigDecimal getCentsPerUnit() {
        return centsPerUnit;
    }

    public void setCentsPerUnit(BigDecimal centsPerUnit) {
        this.centsPerUnit = centsPerUnit;
    }

    public String getUnitCreditName() {
        return unitCreditName;
    }

    public void setUnitCreditName(String unitCreditName) {
        this.unitCreditName = unitCreditName;
    }

    public BigDecimal getMib() {
        return mib;
    }

    public void setMib(BigDecimal mib) {
        this.mib = mib;
    }

    @Override
    public String toString() {
        return "InternationalCallRate{" + "country=" + country + ", service=" + service + ", centsPerUnit=" + centsPerUnit + ", unitCreditName=" + unitCreditName + ", mib=" + mib + '}';
    }
    
}
