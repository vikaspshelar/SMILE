/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.sn;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sticky_note_type_config")
@NamedQueries({
    @NamedQuery(name = "StickyNoteTypeConfig.findAll", query = "SELECT s FROM StickyNoteTypeConfig s"),
    @NamedQuery(name = "StickyNoteTypeConfig.findByTypeName", query = "SELECT s FROM StickyNoteTypeConfig s WHERE s.typeName = :typeName")})
public class StickyNoteTypeConfig implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "DISPLAY_PRIORITY")
    private short displayPriority;
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "TYPE_NAME")
    private String typeName;
    @ManyToMany(mappedBy = "stickyNoteTypeConfigCollection")
    private Collection<StickyNoteEntityConfig> stickyNoteEntityConfigCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "stickyNoteTypeConfig")
    private Collection<StickyNoteFieldConfig> stickyNoteFieldConfigCollection;
    @Basic(optional = false)
    @Column(name = "ALLOWED_ROLES")
    private String allowedRoles;
    
    public StickyNoteTypeConfig() {
    }

    public StickyNoteTypeConfig(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Collection<StickyNoteEntityConfig> getStickyNoteEntityConfigCollection() {
        return stickyNoteEntityConfigCollection;
    }

    public void setStickyNoteEntityConfigCollection(Collection<StickyNoteEntityConfig> stickyNoteEntityConfigCollection) {
        this.stickyNoteEntityConfigCollection = stickyNoteEntityConfigCollection;
    }

    public Collection<StickyNoteFieldConfig> getStickyNoteFieldConfigCollection() {
        return stickyNoteFieldConfigCollection;
    }

    public void setStickyNoteFieldConfigCollection(Collection<StickyNoteFieldConfig> stickyNoteFieldConfigCollection) {
        this.stickyNoteFieldConfigCollection = stickyNoteFieldConfigCollection;
    }

    public String getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(String allowedRoles) {
        this.allowedRoles = allowedRoles;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (typeName != null ? typeName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StickyNoteTypeConfig)) {
            return false;
        }
        StickyNoteTypeConfig other = (StickyNoteTypeConfig) object;
        if ((this.typeName == null && other.typeName != null) || (this.typeName != null && !this.typeName.equals(other.typeName))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sn.StickyNoteTypeConfig[typeName=" + typeName + "]";
    }

    public short getDisplayPriority() {
        return displayPriority;
    }

    public void setDisplayPriority(short displayPriority) {
        this.displayPriority = displayPriority;
    }

}
