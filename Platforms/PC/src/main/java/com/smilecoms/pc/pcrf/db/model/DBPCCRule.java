/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf.db.model;

import com.smilecoms.pc.pcrf.PCCRule;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "pcc_rules", catalog = "PCRFDB", schema = "")
public class DBPCCRule implements Serializable, PCCRule {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "pcc_rule_name")
    private String pccRuleName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "binding_identifier")
    private String bindingIdentifier;
    @Basic(optional = false)
    @Column(name = "type")
    private int type;
    

    public DBPCCRule(){
        
    }

    public DBPCCRule(String pccRuleName, String bindingIdentifier, int type) {
        this.pccRuleName = pccRuleName;
        this.bindingIdentifier = bindingIdentifier;
        this.type = type;
    }

    @Override
    public String getBindingIdentifier() {
        return bindingIdentifier;
    }

    @Override
    public void setBindingIdentifier(String bindingIdentifier) {
        this.bindingIdentifier = bindingIdentifier;
    }

    @Override
    public String getPccRuleName() {
        return pccRuleName;
    }

    @Override
    public void setPccRuleName(String pccRuleName) {
        this.pccRuleName = pccRuleName;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        String unique = this.bindingIdentifier + this.pccRuleName;
        hash += (unique != null ? unique.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DBPCCRule)) {
            return false;
        }
        DBPCCRule other = (DBPCCRule) object;
        
        if(this.bindingIdentifier.equals(other.bindingIdentifier) && this.pccRuleName.equals(other.pccRuleName)){
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pc.db.model.PCCRule[ PCCRuleName=" + this.pccRuleName + " Binding Identifier=" +this.bindingIdentifier + " ]";
    }
}
