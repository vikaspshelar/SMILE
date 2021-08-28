/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf.db.model;

import com.smilecoms.pc.pcrf.AFSession;
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
@Table(name = "af_sessions", catalog = "PCRFDB", schema = "")
public class DBAFSession implements Serializable, AFSession {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "rx_server_session_id")
    private String rxServerSessionId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "binding_identifier")
    private String bindingIdentifier;
    @Basic(optional = false)
    @Column(name = "type")
    private int type;
    @Basic(optional = false)
    @Column(name = "state")
    private String state;

    public DBAFSession(){
        
    }

    public DBAFSession(String rxServerSessionId, String bindingIdentifier, int type, String state) {
        this.rxServerSessionId = rxServerSessionId;
        this.bindingIdentifier = bindingIdentifier;
        this.type = type;
        this.state = state;
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
    public String getRxServerSessionId() {
        return rxServerSessionId;
    }

    @Override
    public void setRxServerSessionId(String rxServerSessionId) {
        this.rxServerSessionId = rxServerSessionId;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
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
        hash += (rxServerSessionId != null ? rxServerSessionId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DBAFSession)) {
            return false;
        }
        DBAFSession other = (DBAFSession) object;
        if ((this.rxServerSessionId == null && other.rxServerSessionId != null) || (this.rxServerSessionId != null && !this.rxServerSessionId.equals(other.rxServerSessionId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.pc.db.model.AFSession[ RxServerSessionId=" + this.rxServerSessionId + " ]";
    }
}
