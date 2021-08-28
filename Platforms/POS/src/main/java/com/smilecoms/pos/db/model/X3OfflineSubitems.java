/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.math.BigDecimal;
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
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "x3_offline_subitems")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "X3OfflineSubitems.findAll", query = "SELECT x FROM X3OfflineSubitems x"),
    @NamedQuery(name = "X3OfflineSubitems.findById", query = "SELECT x FROM X3OfflineSubitems x WHERE x.id = :id"),
    @NamedQuery(name = "X3OfflineSubitems.findByKit", query = "SELECT x FROM X3OfflineSubitems x WHERE x.kit = :kit"),
    @NamedQuery(name = "X3OfflineSubitems.findByComponentItemNumber", query = "SELECT x FROM X3OfflineSubitems x WHERE x.componentItemNumber = :componentItemNumber"),
    @NamedQuery(name = "X3OfflineSubitems.findByDescription", query = "SELECT x FROM X3OfflineSubitems x WHERE x.description = :description"),
    @NamedQuery(name = "X3OfflineSubitems.findByPrice", query = "SELECT x FROM X3OfflineSubitems x WHERE x.price = :price"),
    @NamedQuery(name = "X3OfflineSubitems.findByCurrency", query = "SELECT x FROM X3OfflineSubitems x WHERE x.currency = :currency")})
public class X3OfflineSubitems implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "KIT")
    private String kit;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 200)
    @Column(name = "COMPONENT_ITEM_NUMBER")
    private String componentItemNumber;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2000)
    @Column(name = "DESCRIPTION")
    private String description;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRICE")
    private BigDecimal price;
    @Size(max = 20)
    @Column(name = "CURRENCY")
    private String currency;
    @Size(max = 50)
    @Column(name = "CATEGORY")
    private String category;

    public X3OfflineSubitems() {
    }

    public X3OfflineSubitems(Long id) {
        this.id = id;
    }

    public X3OfflineSubitems(Long id, String kit, String componentItemNumber, String description, BigDecimal price, String currency) {
        this.id = id;
        this.kit = kit;
        this.componentItemNumber = componentItemNumber;
        this.description = description;
        this.price = price;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKit() {
        return kit;
    }

    public void setKit(String kit) {
        this.kit = kit;
    }

    public String getComponentItemNumber() {
        return componentItemNumber;
    }

    public void setComponentItemNumber(String componentItemNumber) {
        this.componentItemNumber = componentItemNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof X3OfflineSubitems)) {
            return false;
        }
        X3OfflineSubitems other = (X3OfflineSubitems) object;
                
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        
        if((this.id == null && other.id == null)) {
            return ((this.getKit() != null && this.getKit().equals(other.getKit()))  &&
                    (this.getComponentItemNumber() != null && this.getComponentItemNumber().equals(other.getComponentItemNumber())) &&
                    ((this.getCurrency() == null && other.getCurrency() == null) || (this.getCurrency() != null && this.getCurrency().equals(other.getCurrency()))));
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.X3OfflineSubitems[ id=" + id + " ]";
    }
    
}
