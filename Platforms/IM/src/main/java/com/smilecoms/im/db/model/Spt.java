/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "spt", catalog = "hss_db", schema = "")
public class Spt implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "condition_negated")
    private int conditionNegated;
    @Basic(optional = false)
    @NotNull
    @Column(name = "grp")
    private int grp;
    @Basic(optional = false)
    @NotNull
    @Column(name = "type")
    private int type;
    @Column(name = "requesturi")
    private String requesturi;
    @Column(name = "method")
    private String method;
    @Column(name = "header")
    private String header;
    @Column(name = "header_content")
    private String headerContent;
    @Column(name = "session_case")
    private Integer sessionCase;
    @Column(name = "sdp_line")
    private String sdpLine;
    @Column(name = "sdp_line_content")
    private String sdpLineContent;
    @Column(name = "registration_type")
    private Integer registrationType;
    @JoinColumn(name = "id_tp", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Tp idTp;

    public Spt() {
    }

    public Spt(Integer id) {
        this.id = id;
    }

    public Spt(Integer id, int conditionNegated, int grp, int type) {
        this.id = id;
        this.conditionNegated = conditionNegated;
        this.grp = grp;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getConditionNegated() {
        return conditionNegated;
    }

    public void setConditionNegated(int conditionNegated) {
        this.conditionNegated = conditionNegated;
    }

    public int getGrp() {
        return grp;
    }

    public void setGrp(int grp) {
        this.grp = grp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getRequesturi() {
        return requesturi;
    }

    public void setRequesturi(String requesturi) {
        this.requesturi = requesturi;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getHeaderContent() {
        return headerContent;
    }

    public void setHeaderContent(String headerContent) {
        this.headerContent = headerContent;
    }

    public Integer getSessionCase() {
        return sessionCase;
    }

    public void setSessionCase(Integer sessionCase) {
        this.sessionCase = sessionCase;
    }

    public String getSdpLine() {
        return sdpLine;
    }

    public void setSdpLine(String sdpLine) {
        this.sdpLine = sdpLine;
    }

    public String getSdpLineContent() {
        return sdpLineContent;
    }

    public void setSdpLineContent(String sdpLineContent) {
        this.sdpLineContent = sdpLineContent;
    }

    public Integer getRegistrationType() {
        return registrationType;
    }

    public void setRegistrationType(Integer registrationType) {
        this.registrationType = registrationType;
    }

    public Tp getIdTp() {
        return idTp;
    }

    public void setIdTp(Tp idTp) {
        this.idTp = idTp;
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
        if (!(object instanceof Spt)) {
            return false;
        }
        Spt other = (Spt) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.Spt[ id=" + id + " ]";
    }
    
}
