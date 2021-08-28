/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "customer_role",  catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "CustomerRole.findAll", query = "SELECT c FROM CustomerRole c")})
public class CustomerRole implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CustomerRolePK customerRolePK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ROLE")
    private String role;

    public CustomerRole() {
    }

    public CustomerRole(CustomerRolePK customerRolePK) {
        this.customerRolePK = customerRolePK;
    }

    public CustomerRole(CustomerRolePK customerRolePK, String role) {
        this.customerRolePK = customerRolePK;
        this.role = role;
    }

    public CustomerRole(int customerProfileId, int organisationId) {
        this.customerRolePK = new CustomerRolePK(customerProfileId, organisationId);
    }

    public CustomerRolePK getCustomerRolePK() {
        return customerRolePK;
    }

    public void setCustomerRolePK(CustomerRolePK customerRolePK) {
        this.customerRolePK = customerRolePK;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (customerRolePK != null ? customerRolePK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomerRole)) {
            return false;
        }
        CustomerRole other = (CustomerRole) object;
        if ((this.customerRolePK == null && other.customerRolePK != null) || (this.customerRolePK != null && !this.customerRolePK.equals(other.customerRolePK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.CustomerRole[ customerRolePK=" + customerRolePK + " ]";
    }
    
}
