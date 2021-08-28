/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.hwf.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "hwf_process_definition")
@NamedQueries({
    @NamedQuery(name = "HwfProcessDefinition.findAll", query = "SELECT h FROM HwfProcessDefinition h")})
public class HwfProcessDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "RESOURCE_NAME")
    private String resourceName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATUS")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Column(name = "BPMN")
    private String bpmn;

    public HwfProcessDefinition() {
    }

    public HwfProcessDefinition(String resourceName) {
        this.resourceName = resourceName;
    }

    public HwfProcessDefinition(String resourceName, String status, String bpmn) {
        this.resourceName = resourceName;
        this.status = status;
        this.bpmn = bpmn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBpmn() {
        return bpmn;
    }

    public void setBpmn(String bpmn) {
        this.bpmn = bpmn;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (resourceName != null ? resourceName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof HwfProcessDefinition)) {
            return false;
        }
        HwfProcessDefinition other = (HwfProcessDefinition) object;
        if ((this.resourceName == null && other.resourceName != null) || (this.resourceName != null && !this.resourceName.equals(other.resourceName))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.hwf.db.model.HwfProcessDefinition[ resourceName=" + resourceName + " ]";
    }
    
}
