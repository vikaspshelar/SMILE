/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.db.model;

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
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author root
 */
@Entity
@Table(name = "vmailbox")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Vmailbox.findAll", query = "SELECT v FROM Vmailbox v"),
    @NamedQuery(name = "Vmailbox.findByVmailboxId", query = "SELECT v FROM Vmailbox v WHERE v.vmailboxId = :vmailboxId"),
    @NamedQuery(name = "Vmailbox.findByVmailboxName", query = "SELECT v FROM Vmailbox v WHERE v.vmailboxName = :vmailboxName")})
public class Vmailbox implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "VMAILBOX_ID")
    private Integer vmailboxId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "VMAILBOX_NAME")
    private String vmailboxName;

    public Vmailbox() {
    }

    public Vmailbox(Integer vmailboxId) {
        this.vmailboxId = vmailboxId;
    }

    public Vmailbox(Integer vmailboxId, String vmailboxName) {
        this.vmailboxId = vmailboxId;
        this.vmailboxName = vmailboxName;
    }

    public Integer getVmailboxId() {
        return vmailboxId;
    }

    public void setVmailboxId(Integer vmailboxId) {
        this.vmailboxId = vmailboxId;
    }

    public String getVmailboxName() {
        return vmailboxName;
    }

    public void setVmailboxName(String vmailboxName) {
        this.vmailboxName = vmailboxName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (vmailboxId != null ? vmailboxId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Vmailbox)) {
            return false;
        }
        Vmailbox other = (Vmailbox) object;
        if ((this.vmailboxId == null && other.vmailboxId != null) || (this.vmailboxId != null && !this.vmailboxId.equals(other.vmailboxId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.db.model.Vmailbox[ vmailboxId=" + vmailboxId + " ]";
    }
    
}
