/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf.db.model;

import com.smilecoms.pc.pcrf.IPCANSession;
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
@Table(name = "ipcan_sessions", catalog = "PCRFDB", schema = "")
public class DBIPCANSession implements Serializable, IPCANSession {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "gx_server_session_id")
    private String gxServerSessionId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "binding_identifier")
    private String bindingIdentifier;
    @Basic(optional = false)
    @NotNull
    @Column(name = "end_user_private")
    private String endUserPrivate;
    @Basic(optional = false)
    @Column(name = "called_station_id")
    private String calledStationId;
    @Basic(optional = false)
    @Column(name = "highest_priority_service_id")
    private int highestPriorityServiceId;
    @Basic(optional = false)
    @Column(name = "state")
    private String state;
    

    public DBIPCANSession(){
        
    }
            
    public DBIPCANSession(String gxServerSessionId, String bindingIdentifier, String calledStationId, String state) {
        this.gxServerSessionId = gxServerSessionId;
        this.bindingIdentifier = bindingIdentifier;
        this.calledStationId = calledStationId;
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
    public String getGxServerSessionId() {
        return gxServerSessionId;
    }

    @Override
    public void setGxServerSessionId(String gxServerSessionId) {
        this.gxServerSessionId = gxServerSessionId;
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
    public String getCalledStationId() {
        return calledStationId;
    }

    @Override
    public void setCalledStationId(String calledStationId) {
        this.calledStationId = calledStationId;
    }

    @Override
    public int getHighestPriorityServiceId() {
        return highestPriorityServiceId;
    }

    @Override
    public void setHighestPriorityServiceId(int highestPriorityServiceId) {
        this.highestPriorityServiceId = highestPriorityServiceId;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (gxServerSessionId != null ? gxServerSessionId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DBIPCANSession)) {
            return false;
        }
        DBIPCANSession other = (DBIPCANSession) object;
        if ((this.gxServerSessionId == null && other.gxServerSessionId != null) || (this.gxServerSessionId != null && !this.gxServerSessionId.equals(other.gxServerSessionId))) {
            return false;
        }
        return true;
    }

    public String getEndUserPrivate() {
        return endUserPrivate;
    }

    public void setEndUserPrivate(String endUserPrivate) {
        this.endUserPrivate = endUserPrivate;
    }
    
    @Override
    public String toString() {
        return "com.smilecoms.pc.db.model.IPCANSession[ GxServerSessionId=" + this.gxServerSessionId + " ]";
    }
}
