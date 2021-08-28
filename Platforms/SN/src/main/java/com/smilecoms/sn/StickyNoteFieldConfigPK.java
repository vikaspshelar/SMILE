/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.sn;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 * @author paul
 */
@Embeddable
public class StickyNoteFieldConfigPK implements Serializable {
    @Basic(optional = false)
    @Column(name = "FIELD_NAME")
    private String fieldName;
    @Basic(optional = false)
    @Column(name = "TYPE_NAME")
    private String typeName;

    public StickyNoteFieldConfigPK() {
    }

    public StickyNoteFieldConfigPK(String fieldName, String typeName) {
        this.fieldName = fieldName;
        this.typeName = typeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (fieldName != null ? fieldName.hashCode() : 0);
        hash += (typeName != null ? typeName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StickyNoteFieldConfigPK)) {
            return false;
        }
        StickyNoteFieldConfigPK other = (StickyNoteFieldConfigPK) object;
        if ((this.fieldName == null && other.fieldName != null) || (this.fieldName != null && !this.fieldName.equals(other.fieldName))) {
            return false;
        }
        if ((this.typeName == null && other.typeName != null) || (this.typeName != null && !this.typeName.equals(other.typeName))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sn.StickyNoteFieldConfigPK[fieldName=" + fieldName + ", typeName=" + typeName + "]";
    }

}
