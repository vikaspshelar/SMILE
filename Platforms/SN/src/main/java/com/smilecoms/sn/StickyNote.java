/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.sn;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sticky_note")
@NamedQueries({
    @NamedQuery(name = "StickyNote.findAll", query = "SELECT s FROM StickyNote s"),
    @NamedQuery(name = "StickyNote.findByTypeName", query = "SELECT s FROM StickyNote s WHERE s.typeName = :typeName"),
    @NamedQuery(name = "StickyNote.findByStickyNoteId", query = "SELECT s FROM StickyNote s WHERE s.stickyNoteId = :stickyNoteId"),
    @NamedQuery(name = "StickyNote.findByCreatedDatetime", query = "SELECT s FROM StickyNote s WHERE s.createdDatetime = :createdDatetime"),
    @NamedQuery(name = "StickyNote.findByLastModified", query = "SELECT s FROM StickyNote s WHERE s.lastModified = :lastModified"),
    @NamedQuery(name = "StickyNote.findByEntityType", query = "SELECT s FROM StickyNote s WHERE s.entityType = :entityType"),
    @NamedQuery(name = "StickyNote.findByEntityId", query = "SELECT s FROM StickyNote s WHERE s.entityId = :entityId"),
    @NamedQuery(name = "StickyNote.findByCreatedBy", query = "SELECT s FROM StickyNote s WHERE s.createdBy = :createdBy"),
    @NamedQuery(name = "StickyNote.findByLastModifiedBy", query = "SELECT s FROM StickyNote s WHERE s.lastModifiedBy = :lastModifiedBy")})
public class StickyNote implements Serializable {
    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @Column(name = "TYPE_NAME")
    private String typeName;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "STICKY_NOTE_ID")
    private Integer stickyNoteId;
    @Basic(optional = false)
    @Column(name = "CREATED_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDatetime;
    @Basic(optional = false)
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    @Basic(optional = false)
    @Column(name = "ENTITY_TYPE")
    private String entityType;
    @Basic(optional = false)
    @Column(name = "ENTITY_ID")
    private Long entityId;
    @Basic(optional = false)
    @Column(name = "CREATED_BY")
    private String createdBy;
    @Basic(optional = false)
    @Column(name = "LAST_MODIFIED_BY")
    private String lastModifiedBy;
    @Column(name = "VERSION")
    @Version
    private int version;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "stickyNote")
    private Collection<StickyNoteField> stickyNoteFieldCollection;

    public StickyNote() {
    }

    public StickyNote(Integer stickyNoteId) {
        this.stickyNoteId = stickyNoteId;
    }

    public StickyNote(Integer stickyNoteId, String typeName, Date createdDatetime, Date lastModified, String entityType, Long entityId, String createdBy, String lastModifiedBy) {
        this.stickyNoteId = stickyNoteId;
        this.typeName = typeName;
        this.createdDatetime = createdDatetime;
        this.lastModified = lastModified;
        this.entityType = entityType;
        this.entityId = entityId;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getStickyNoteId() {
        return stickyNoteId;
    }

    public void setStickyNoteId(Integer stickyNoteId) {
        this.stickyNoteId = stickyNoteId;
    }

    public int getVersion() {
        return version;
    }

    
    public Date getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Date createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Collection<StickyNoteField> getStickyNoteFieldCollection() {
        return stickyNoteFieldCollection;
    }

    public void setStickyNoteFieldCollection(Collection<StickyNoteField> stickyNoteFieldCollection) {
        this.stickyNoteFieldCollection = stickyNoteFieldCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (stickyNoteId != null ? stickyNoteId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StickyNote)) {
            return false;
        }
        StickyNote other = (StickyNote) object;
        if ((this.stickyNoteId == null && other.stickyNoteId != null) || (this.stickyNoteId != null && !this.stickyNoteId.equals(other.stickyNoteId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sn.StickyNote[stickyNoteId=" + stickyNoteId + "]";
    }

}
