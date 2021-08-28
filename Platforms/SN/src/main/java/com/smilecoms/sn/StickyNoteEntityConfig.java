/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.sn;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sticky_note_entity_config")
@NamedQueries({
    @NamedQuery(name = "StickyNoteEntityConfig.findAll", query = "SELECT s FROM StickyNoteEntityConfig s"),
    @NamedQuery(name = "StickyNoteEntityConfig.findByEntityType", query = "SELECT s FROM StickyNoteEntityConfig s WHERE s.entityType = :entityType")})
public class StickyNoteEntityConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "ENTITY_TYPE")
    private String entityType;
    @JoinTable(name = "sticky_note_entity_type_mapping", joinColumns = {
        @JoinColumn(name = "ENTITY", referencedColumnName = "ENTITY_TYPE")}, inverseJoinColumns = {
        @JoinColumn(name = "TYPE_NAME", referencedColumnName = "TYPE_NAME")})
    @ManyToMany
    private Collection<StickyNoteTypeConfig> stickyNoteTypeConfigCollection;

    public StickyNoteEntityConfig() {
    }

    public StickyNoteEntityConfig(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Collection<StickyNoteTypeConfig> getStickyNoteTypeConfigCollection() {
        return stickyNoteTypeConfigCollection;
    }

    public void setStickyNoteTypeConfigCollection(Collection<StickyNoteTypeConfig> stickyNoteTypeConfigCollection) {
        this.stickyNoteTypeConfigCollection = stickyNoteTypeConfigCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (entityType != null ? entityType.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StickyNoteEntityConfig)) {
            return false;
        }
        StickyNoteEntityConfig other = (StickyNoteEntityConfig) object;
        if ((this.entityType == null && other.entityType != null) || (this.entityType != null && !this.entityType.equals(other.entityType))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sn.StickyNoteEntityConfig[entityType=" + entityType + "]";
    }

}
