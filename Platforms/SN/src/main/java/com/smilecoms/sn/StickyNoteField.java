/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.sn;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "sticky_note_field")
@NamedQueries({
    @NamedQuery(name = "StickyNoteField.findAll", query = "SELECT s FROM StickyNoteField s"),
    @NamedQuery(name = "StickyNoteField.findByFieldName", query = "SELECT s FROM StickyNoteField s WHERE s.fieldName = :fieldName"),
    @NamedQuery(name = "StickyNoteField.findByFieldData", query = "SELECT s FROM StickyNoteField s WHERE s.fieldData = :fieldData"),
    @NamedQuery(name = "StickyNoteField.findByStickyNoteFieldId", query = "SELECT s FROM StickyNoteField s WHERE s.stickyNoteFieldId = :stickyNoteFieldId"),
    @NamedQuery(name = "StickyNoteField.findByFieldType", query = "SELECT s FROM StickyNoteField s WHERE s.fieldType = :fieldType")})
public class StickyNoteField implements Serializable {
    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @Column(name = "FIELD_NAME")
    private String fieldName;
    @Basic(optional = false)
    @Column(name = "FIELD_DATA")
    private String fieldData;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "STICKY_NOTE_FIELD_ID")
    private Integer stickyNoteFieldId;
    @Basic(optional = false)
    @Column(name = "FIELD_TYPE")
    private String fieldType;
    @JoinColumn(name = "STICKY_NOTE", referencedColumnName = "STICKY_NOTE_ID")
    @ManyToOne(optional = false)
    private StickyNote stickyNote;
    @Lob
    @Column(name = "DOCUMENT_DATA")
    private byte[] documentData;

    public StickyNoteField() {
    }

    public StickyNoteField(Integer stickyNoteFieldId) {
        this.stickyNoteFieldId = stickyNoteFieldId;
    }

    public StickyNoteField(Integer stickyNoteFieldId, String fieldName, String fieldData, String fieldType) {
        this.stickyNoteFieldId = stickyNoteFieldId;
        this.fieldName = fieldName;
        this.fieldData = fieldData;
        this.fieldType = fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldData() {
        return fieldData;
    }

    public void setFieldData(String fieldData) {
        this.fieldData = fieldData;
    }

    public Integer getStickyNoteFieldId() {
        return stickyNoteFieldId;
    }

    public void setStickyNoteFieldId(Integer stickyNoteFieldId) {
        this.stickyNoteFieldId = stickyNoteFieldId;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public StickyNote getStickyNote() {
        return stickyNote;
    }

    public void setStickyNote(StickyNote stickyNote) {
        this.stickyNote = stickyNote;
    }
    
    public byte[] getDocumentData() {
        return documentData;
    }

    public void setDocumentData(byte[] data) {
        this.documentData = data;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (stickyNoteFieldId != null ? stickyNoteFieldId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StickyNoteField)) {
            return false;
        }
        StickyNoteField other = (StickyNoteField) object;
        if ((this.stickyNoteFieldId == null && other.stickyNoteFieldId != null) || (this.stickyNoteFieldId != null && !this.stickyNoteFieldId.equals(other.stickyNoteFieldId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sn.StickyNoteField[stickyNoteFieldId=" + stickyNoteFieldId + "]";
    }

}
