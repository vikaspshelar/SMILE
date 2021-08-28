/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "address", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "Address.findAll", query = "SELECT a FROM Address a")})
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ADDRESS_ID")
    private Integer addressId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LINE_1")
    private String line1;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LINE_2")
    private String line2;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ZONE")
    private String zone;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TOWN")
    private String town;
    @Basic(optional = false)
    @NotNull
    @Column(name = "COUNTRY")
    private String country;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CODE")
    private String code;
    @Column(name = "CUSTOMER_PROFILE_ID")
    private Integer customerProfileId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TYPE")
    private String type;
    @Basic(optional = false)
    @NotNull
    @Column(name = "POSTAL_MATCHES_PHYSICAL")
    private String postalMatchesPhysical;
    @Column(name = "ORGANISATION_ID")
    private Integer organisationId;
    @Column(name="STATE")
    private String state;
    
    public Address() {
    }

    public Address(Integer addressId) {
        this.addressId = addressId;
    }

    public Address(Integer addressId, String line1, String line2, String zone, String town,String state, String country, String code, Integer customerProfileId, String type, String postalMatchesPhysical) {
        this.addressId = addressId;
        this.line1 = line1;
        this.line2 = line2;
        this.zone = zone;
        this.town = town;
        this.state = state;
        this.country = country;
        this.code = code;
        this.customerProfileId = customerProfileId;
        this.type = type;
        this.postalMatchesPhysical = postalMatchesPhysical;
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
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

    public Integer getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(Integer customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPostalMatchesPhysical() {
        return postalMatchesPhysical;
    }

    public void setPostalMatchesPhysical(String postalMatchesPhysical) {
        this.postalMatchesPhysical = postalMatchesPhysical;
    }

    public Integer getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(Integer organisationId) {
        this.organisationId = organisationId;
    }
    public String getState(){
        return state;
    }
    public void setState(String state){
        this.state = state;
    }
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (addressId != null ? addressId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Address)) {
            return false;
        }
        Address other = (Address) object;
        if ((this.addressId == null && other.addressId != null) || (this.addressId != null && !this.addressId.equals(other.addressId))) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.Address[ addressId=" + addressId + " ]";
    }
    
}
