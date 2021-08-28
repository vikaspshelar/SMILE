/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.model;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author bhaskarhg
 */
public class Address {
    
    protected int addressId;
    
    protected String line1;
    
    protected String line2;
    
    protected String zone;
    
    protected String town;
    
    protected String state;
    
    protected String country;
    
    protected String code;
    
    protected String type;
    
    protected boolean postalMatchesPhysical;
    
    protected int customerId;
    
    protected int organisationId;

    public int getAddressId() {
        return addressId;
    }

    public void setAddressId(int addressId) {
        this.addressId = addressId;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPostalMatchesPhysical() {
        return postalMatchesPhysical;
    }

    public void setPostalMatchesPhysical(boolean postalMatchesPhysical) {
        this.postalMatchesPhysical = postalMatchesPhysical;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }
    
}
