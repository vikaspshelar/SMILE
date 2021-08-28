/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.sn;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sticky_note_field_config")
@NamedQueries({
    @NamedQuery(name = "StickyNoteFieldConfig.findAll", query = "SELECT s FROM StickyNoteFieldConfig s"),
    @NamedQuery(name = "StickyNoteFieldConfig.findByFieldName", query = "SELECT s FROM StickyNoteFieldConfig s WHERE s.stickyNoteFieldConfigPK.fieldName = :fieldName"),
    @NamedQuery(name = "StickyNoteFieldConfig.findByTypeName", query = "SELECT s FROM StickyNoteFieldConfig s WHERE s.stickyNoteFieldConfigPK.typeName = :typeName"),
    @NamedQuery(name = "StickyNoteFieldConfig.findByFieldType", query = "SELECT s FROM StickyNoteFieldConfig s WHERE s.fieldType = :fieldType")})
public class StickyNoteFieldConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected StickyNoteFieldConfigPK stickyNoteFieldConfigPK;
    @Basic(optional = false)
    @Column(name = "FIELD_TYPE")
    private String fieldType;
    @JoinColumn(name = "TYPE_NAME", referencedColumnName = "TYPE_NAME", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private StickyNoteTypeConfig stickyNoteTypeConfig;

    public StickyNoteFieldConfig() {
    }

    public StickyNoteFieldConfig(StickyNoteFieldConfigPK stickyNoteFieldConfigPK) {
        this.stickyNoteFieldConfigPK = stickyNoteFieldConfigPK;
    }

    public StickyNoteFieldConfig(StickyNoteFieldConfigPK stickyNoteFieldConfigPK, String fieldType) {
        this.stickyNoteFieldConfigPK = stickyNoteFieldConfigPK;
        this.fieldType = fieldType;
    }

    public StickyNoteFieldConfig(String fieldName, String typeName) {
        this.stickyNoteFieldConfigPK = new StickyNoteFieldConfigPK(fieldName, typeName);
    }

    public StickyNoteFieldConfigPK getStickyNoteFieldConfigPK() {
        return stickyNoteFieldConfigPK;
    }

    public void setStickyNoteFieldConfigPK(StickyNoteFieldConfigPK stickyNoteFieldConfigPK) {
        this.stickyNoteFieldConfigPK = stickyNoteFieldConfigPK;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public StickyNoteTypeConfig getStickyNoteTypeConfig() {
        return stickyNoteTypeConfig;
    }

    public void setStickyNoteTypeConfig(StickyNoteTypeConfig stickyNoteTypeConfig) {
        this.stickyNoteTypeConfig = stickyNoteTypeConfig;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (stickyNoteFieldConfigPK != null ? stickyNoteFieldConfigPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StickyNoteFieldConfig)) {
            return false;
        }
        StickyNoteFieldConfig other = (StickyNoteFieldConfig) object;
        if ((this.stickyNoteFieldConfigPK == null && other.stickyNoteFieldConfigPK != null) || (this.stickyNoteFieldConfigPK != null && !this.stickyNoteFieldConfigPK.equals(other.stickyNoteFieldConfigPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sn.StickyNoteFieldConfig[stickyNoteFieldConfigPK=" + stickyNoteFieldConfigPK + "]";
    }

}
