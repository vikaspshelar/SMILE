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
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "authorisation_rule")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "AuthorisationRule.findAll", query = "SELECT a FROM AuthorisationRule a"),
    @NamedQuery(name = "AuthorisationRule.findByRuleId", query = "SELECT a FROM AuthorisationRule a WHERE a.ruleId = :ruleId"),
    @NamedQuery(name = "AuthorisationRule.findByRuleSetId", query = "SELECT a FROM AuthorisationRule a WHERE a.ruleSetId = :ruleSetId"),
    @NamedQuery(name = "AuthorisationRule.findByXquery", query = "SELECT a FROM AuthorisationRule a WHERE a.xquery = :xquery"),
    @NamedQuery(name = "AuthorisationRule.findByRegexMatch", query = "SELECT a FROM AuthorisationRule a WHERE a.regexMatch = :regexMatch"),
    @NamedQuery(name = "AuthorisationRule.findByDescription", query = "SELECT a FROM AuthorisationRule a WHERE a.description = :description"),
    @NamedQuery(name = "AuthorisationRule.findByCustomerProfileId", query = "SELECT a FROM AuthorisationRule a WHERE a.customerProfileId = :customerProfileId")})
public class AuthorisationRule implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "RULE_ID")
    private Integer ruleId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RULE_SET_ID")
    private int ruleSetId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "XQUERY")
    private String xquery;
    @Basic(optional = false)
    @NotNull
    @Column(name = "REGEX_MATCH")
    private String regexMatch;
    @Basic(optional = false)
    @NotNull
    @Column(name = "DESCRIPTION")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;

    public AuthorisationRule() {
    }

    public AuthorisationRule(Integer ruleId) {
        this.ruleId = ruleId;
    }

    public AuthorisationRule(Integer ruleId, int ruleSetId, String xquery, String regexMatch, String description, int customerProfileId) {
        this.ruleId = ruleId;
        this.ruleSetId = ruleSetId;
        this.xquery = xquery;
        this.regexMatch = regexMatch;
        this.description = description;
        this.customerProfileId = customerProfileId;
    }

    public Integer getRuleId() {
        return ruleId;
    }

    public void setRuleId(Integer ruleId) {
        this.ruleId = ruleId;
    }

    public int getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(int ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getXquery() {
        return xquery;
    }

    public void setXquery(String xquery) {
        this.xquery = xquery;
    }

    public String getRegexMatch() {
        return regexMatch;
    }

    public void setRegexMatch(String regexMatch) {
        this.regexMatch = regexMatch;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        hash += (ruleId != null ? ruleId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AuthorisationRule)) {
            return false;
        }
        AuthorisationRule other = (AuthorisationRule) object;
        if ((this.ruleId == null && other.ruleId != null) || (this.ruleId != null && !this.ruleId.equals(other.ruleId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.db.model.AuthorisationRule[ ruleId=" + ruleId + " ]";
    }
    
}
