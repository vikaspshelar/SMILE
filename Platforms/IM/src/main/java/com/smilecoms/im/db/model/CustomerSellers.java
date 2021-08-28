/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;


@Entity
@Table(name = "customer_sellers", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "CustomerSellers.findAll", query = "SELECT s FROM CustomerSellers s")})
public class CustomerSellers implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected CustomerSellersPK customerSellersPK;

    public CustomerSellers() {
    }

    public CustomerSellers(CustomerSellersPK customerSellersPK) {
        this.customerSellersPK = customerSellersPK;
    }

    public CustomerSellersPK getCustomerSellersPK() {
        return customerSellersPK;
    }

    public void setCustomerSellersPK(CustomerSellersPK customerSellersPK) {
        this.customerSellersPK = customerSellersPK;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.customerSellersPK);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CustomerSellers other = (CustomerSellers) obj;
        if (!Objects.equals(this.customerSellersPK, other.customerSellersPK)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CustomerSellers{" + "customerSellersPK=" + customerSellersPK + '}';
    }
    
    
    
}
