/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos.db.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author lesiba
 */
@Entity
@Table(name = "tra_state")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TraState.findAll", query = "SELECT t FROM TraState t"),
    @NamedQuery(name = "TraState.findBySaleId", query = "SELECT t FROM TraState t WHERE t.saleId = :saleId"),
    @NamedQuery(name = "TraState.findByStatus", query = "SELECT t FROM TraState t WHERE t.status = :status"),
    @NamedQuery(name = "TraState.findByStartDateTime", query = "SELECT t FROM TraState t WHERE t.startDateTime = :startDateTime"),
    @NamedQuery(name = "TraState.findByEndDateTime", query = "SELECT t FROM TraState t WHERE t.endDateTime = :endDateTime")})
public class TraState implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "SALE_ID")
    private Integer saleId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2)
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "START_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDateTime;
    @Column(name = "END_DATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDateTime;

    public TraState() {
    }

    public TraState(Integer saleId) {
        this.saleId = saleId;
    }

    public TraState(Integer saleId, String status, Date startDateTime) {
        this.saleId = saleId;
        this.status = status;
        this.startDateTime = startDateTime;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Date endDateTime) {
        this.endDateTime = endDateTime;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (saleId != null ? saleId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TraState)) {
            return false;
        }
        TraState other = (TraState) object;
        if ((this.saleId == null && other.saleId != null) || (this.saleId != null && !this.saleId.equals(other.saleId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pos.db.model.TraState[ saleId=" + saleId + " ]";
    }
    
}
