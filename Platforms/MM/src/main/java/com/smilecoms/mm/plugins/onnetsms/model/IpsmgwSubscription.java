/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms.model;

import com.smilecoms.mm.plugins.onnetsms.PresentityLock;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
@Entity
@Table(name = "ipsmgw_subscription")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "IpsmgwSubscription.findAll", query = "SELECT i FROM IpsmgwSubscription i")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findById", query = "SELECT i FROM IpsmgwSubscription i WHERE i.id = :id")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByPresentityUri", query = "SELECT i FROM IpsmgwSubscription i WHERE i.presentityUri = :presentityUri")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByPresentityUriAndScscf", query = "SELECT i FROM IpsmgwSubscription i WHERE i.presentityUri = :presentityUri AND i.scscfUri = :scscfUri")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByPresentityUriAndCallId", query = "SELECT i FROM IpsmgwSubscription i WHERE i.presentityUri = :presentityUri AND i.callId = :callId")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByWatcherUri", query = "SELECT i FROM IpsmgwSubscription i WHERE i.watcherUri = :watcherUri")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByScscfUri", query = "SELECT i FROM IpsmgwSubscription i WHERE i.scscfUri = :scscfUri")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByState", query = "SELECT i FROM IpsmgwSubscription i WHERE i.state = :state")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByExpires", query = "SELECT i FROM IpsmgwSubscription i WHERE i.expires = :expires")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByCallId", query = "SELECT i FROM IpsmgwSubscription i WHERE i.callId = :callId")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByFromTag", query = "SELECT i FROM IpsmgwSubscription i WHERE i.fromTag = :fromTag")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByToTag", query = "SELECT i FROM IpsmgwSubscription i WHERE i.toTag = :toTag")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByDialogId", query = "SELECT i FROM IpsmgwSubscription i WHERE i.dialogId = :dialogId")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByCseq", query = "SELECT i FROM IpsmgwSubscription i WHERE i.cseq = :cseq")
    ,
    @NamedQuery(name = "IpsmgwSubscription.findByRetries", query = "SELECT i FROM IpsmgwSubscription i WHERE i.retries = :retries")})
public class IpsmgwSubscription implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(IpsmgwSubscription.class);
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Long id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PRESENTITY_URI")
    private String presentityUri;
    @Basic(optional = false)
    @NotNull
    @Column(name = "WATCHER_URI")
    private String watcherUri;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SCSCF_URI")
    private String scscfUri;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STATE")
    private String state;
    @Column(name = "EXPIRES")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expires;
    @Column(name = "CALL_ID")
    private String callId;
    @Column(name = "FROM_TAG")
    private String fromTag;
    @Column(name = "TO_TAG")
    private String toTag;
    @Column(name = "DIALOG_ID")
    private String dialogId;
    @Column(name = "CSEQ")
    private Integer cseq;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RETRIES")
    private int retries;
    @JoinTable(name = "ipsmgw_subscription_impu", joinColumns = {
        @JoinColumn(name = "SUBSCRIPTION_ID", referencedColumnName = "ID")}, inverseJoinColumns = {
        @JoinColumn(name = "IMPU_ID", referencedColumnName = "ID")})
    @ManyToMany
    private Collection<IpsmgwImpu> ipsmgwImpuCollection;

    private transient PresentityLock lock;
    
    public void setLock(PresentityLock lock) {
        this.lock = lock;
    }
    
    public void unlock() {
        lock.unlock();
    }
    
    public int getNextCSeq() {
        this.cseq++;

        return cseq;
    }

    public void resetRetries() {
        this.setRetries(0);
    }

    public void reset() {
        setStateEnum(State.init);
        this.callId = null;
        this.toTag = null;
        this.cseq = 0;
        this.retries = 0;
        this.expires = null;
        this.dialogId = null;
    }

    public IpsmgwImpu getAssociatedImpu(EntityManager em, String aor) {
        for (IpsmgwImpu impu : this.getIpsmgwImpuCollection()) {
            if (impu.getUri().equalsIgnoreCase(aor)) {
//                em.lock(impu, LockModeType.PESSIMISTIC_READ);
                return impu;
            }
        }
        return null;
    }

    public void addImpuIfDoesntExist(IpsmgwImpu impu) {
        for (IpsmgwImpu i : this.getIpsmgwImpuCollection()) {
            if (i.equals(impu)) {
                return;
            }
        }

        this.getIpsmgwImpuCollection().add(impu);
    }

    public enum State {
        init, pending, subscribing, active, failed, inactive, unsubscribe, unsubscribing, resubscribing
    };

    public IpsmgwSubscription() {
        this.cseq = 0;
        this.retries = 0;
        this.ipsmgwImpuCollection = new ArrayList<>();
    }

    public IpsmgwSubscription(Long id) {
        this();
        this.id = id;
    }

    public IpsmgwSubscription(Long id, String presentityUri, String watcherUri, String scscfUri, String state, Date expires, String callId, String fromTag, int cseq) {
        this();
        this.id = id;
        this.presentityUri = presentityUri;
        this.watcherUri = watcherUri;
        this.scscfUri = scscfUri;
        this.state = state;
        this.expires = expires;
        this.callId = callId;
        this.fromTag = fromTag;
        this.cseq = cseq;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPresentityUri() {
        return presentityUri;
    }

    public void setPresentityUri(String presentityUri) {
        this.presentityUri = presentityUri;
    }

    public String getWatcherUri() {
        return watcherUri;
    }

    public void setWatcherUri(String watcherUri) {
        this.watcherUri = watcherUri;
    }

    public String getScscfUri() {
        return scscfUri;
    }

    public void setScscfUri(String scscfUri) {
        this.scscfUri = scscfUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        log.debug("Setting state for subscription to [{}] to [{}]", new Object[]{this.getPresentityUri(), state});
        this.state = state;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getFromTag() {
        return fromTag;
    }

    public void setFromTag(String fromTag) {
        this.fromTag = fromTag;
    }

    public String getToTag() {
        return toTag;
    }

    public void setToTag(String toTag) {
        this.toTag = toTag;
    }

    public String getDialogId() {
        return dialogId;
    }

    public void setDialogId(String dialogId) {
        this.dialogId = dialogId;
    }

    public Integer getCseq() {
        return cseq;
    }

    public void setCseq(Integer cseq) {
        this.cseq = cseq;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    @XmlTransient
    public Collection<IpsmgwImpu> getIpsmgwImpuCollection() {
        return ipsmgwImpuCollection;
    }

    public void setIpsmgwImpuCollection(Collection<IpsmgwImpu> ipsmgwImpuCollection) {
        this.ipsmgwImpuCollection = ipsmgwImpuCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof IpsmgwSubscription)) {
            return false;
        }
        IpsmgwSubscription other = (IpsmgwSubscription) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.mm.plugins.onnetsms.model.IpsmgwSubscription[ id=" + id + " ]";
    }

    public State getStateEnum() {
        return State.valueOf(this.getState());
    }

    public void setStateEnum(State state) {
        log.debug("Setting state for subscription to [{}] to [{}]", new Object[]{this.getPresentityUri(), state.name()});
        this.setState(state.name());
    }

    public synchronized void incrementRetries() {
        setRetries(this.retries + 1);
    }

}
