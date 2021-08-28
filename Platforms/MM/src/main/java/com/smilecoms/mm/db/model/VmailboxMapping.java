/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "vmailbox_mapping")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "VmailboxMapping.findAll", query = "SELECT v FROM VmailboxMapping v"),
    @NamedQuery(name = "VmailboxMapping.findByImpu", query = "SELECT v FROM VmailboxMapping v WHERE v.impu = :impu"),
    @NamedQuery(name = "VmailboxMapping.findByVmailboxId", query = "SELECT v FROM VmailboxMapping v WHERE v.vmailboxId = :vmailboxId")})
public class VmailboxMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "IMPU")
    private String impu;
    @Basic(optional = false)
    @NotNull
    @Column(name = "VMAILBOX_ID")
    private int vmailboxId;

    public VmailboxMapping() {
    }

    public VmailboxMapping(String impu) {
        this.impu = impu;
    }

    public VmailboxMapping(String impu, int vmailboxId) {
        this.impu = impu;
        this.vmailboxId = vmailboxId;
    }

    public String getImpu() {
        return impu;
    }

    public void setImpu(String impu) {
        this.impu = impu;
    }

    public int getVmailboxId() {
        return vmailboxId;
    }

    public void setVmailboxId(int vmailboxId) {
        this.vmailboxId = vmailboxId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (impu != null ? impu.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof VmailboxMapping)) {
            return false;
        }
        VmailboxMapping other = (VmailboxMapping) object;
        if ((this.impu == null && other.impu != null) || (this.impu != null && !this.impu.equals(other.impu))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.db.model.VmailboxMapping[ impu=" + impu + " ]";
    }
    
}
