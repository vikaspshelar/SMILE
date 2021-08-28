/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "security_group_membership", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "SecurityGroupMembership.findAll", query = "SELECT s FROM SecurityGroupMembership s")})
public class SecurityGroupMembership implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SecurityGroupMembershipPK securityGroupMembershipPK;

    public SecurityGroupMembership() {
    }

    public SecurityGroupMembership(SecurityGroupMembershipPK securityGroupMembershipPK) {
        this.securityGroupMembershipPK = securityGroupMembershipPK;
    }

    public SecurityGroupMembership(String securityGroupName, int customerProfileId) {
        this.securityGroupMembershipPK = new SecurityGroupMembershipPK(securityGroupName, customerProfileId);
    }

    public SecurityGroupMembershipPK getSecurityGroupMembershipPK() {
        return securityGroupMembershipPK;
    }

    public void setSecurityGroupMembershipPK(SecurityGroupMembershipPK securityGroupMembershipPK) {
        this.securityGroupMembershipPK = securityGroupMembershipPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (securityGroupMembershipPK != null ? securityGroupMembershipPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof SecurityGroupMembership)) {
            return false;
        }
        SecurityGroupMembership other = (SecurityGroupMembership) object;
        if ((this.securityGroupMembershipPK == null && other.securityGroupMembershipPK != null) || (this.securityGroupMembershipPK != null && !this.securityGroupMembershipPK.equals(other.securityGroupMembershipPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.SecurityGroupMembership[ securityGroupMembershipPK=" + securityGroupMembershipPK + " ]";
    }
    
}
