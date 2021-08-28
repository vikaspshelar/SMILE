/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers.radius;

import java.math.BigDecimal;

/**
 *
 * @author paul
 */
public class RadiusAVP {

    
    
    public enum DATA_TYPE {
        text, string, address, integer, time, ipaddr, date
    }
    
    private DATA_TYPE type;
    private String value;
    
    public RadiusAVP() {
    }
    
    public String getType() {
        return type.toString();
    }
    
    public String getValue() {
        return value;
    }

    public void setType(DATA_TYPE type) {
        this.type = type;
    }

//    public void setValue(String value) {
//        this.value = value;
//    }
    
    public void setValue(String[] value) {
        this.value = value[0];
    }
    
    public int getIntValue() {
        return Integer.parseInt(getValue());
    }
    
    public long getLongValue() {
        return Long.parseLong(getValue());
    }
    
    public BigDecimal getBigDecimalValue() {
        return new BigDecimal(getValue());
    }

    public RadiusAVP(DATA_TYPE type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Type: " + type + " Value: " + value;
    }
    
    
    
}
