/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Embeddable
public class SecurityGroupMembershipPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "SECURITY_GROUP_NAME")
    private String securityGroupName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;

    public SecurityGroupMembershipPK() {
    }

    public SecurityGroupMembershipPK(String securityGroupName, int customerProfileId) {
        this.securityGroupName = securityGroupName;
        this.customerProfileId = customerProfileId;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (securityGroupName != null ? securityGroupName.hashCode() : 0);
        hash += (int) customerProfileId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SecurityGroupMembershipPK)) {
            return false;
        }
        SecurityGroupMembershipPK other = (SecurityGroupMembershipPK) object;
        if ((this.securityGroupName == null && other.securityGroupName != null) || (this.securityGroupName != null && !this.securityGroupName.equals(other.securityGroupName))) {
            return false;
        }
        if (this.customerProfileId != other.customerProfileId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.SecurityGroupMembershipPK[ securityGroupName=" + securityGroupName + ", customerProfileId=" + customerProfileId + " ]";
    }
    
}
