/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sabza
 */
@Entity
@Table(name = "uicc_details", catalog = "SmileDB", schema = "")
@NamedQueries({
    @NamedQuery(name = "UiccDetails.findAll", query = "SELECT c FROM UiccDetails c")})
public class UiccDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "ICCID")
    private String ICCID;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PIN1")
    private String PIN1;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PIN2")
    private String PIN2;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PUK1")
    private String PUK1;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PUK2")
    private String PUK2;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ADM1")
    private String ADM1;

    public UiccDetails() {
    }

    public UiccDetails(String ICCID) {
        this.ICCID = ICCID;
    }

    public UiccDetails(String ICCID, String PIN1, String PIN2, String PUK1, String PUK2, String ADM1) {
        this.ICCID = ICCID;
        this.PIN1 = PIN1;
        this.PIN2 = PIN2;
        this.PUK1 = PUK1;
        this.PUK2 = PUK2;
        this.ADM1 = ADM1;
    }

    public String getICCID() {
        return ICCID;
    }

    public void setICCID(String iccid) {
        this.ICCID = iccid;
    }

    public String getPIN1() {
        return PIN1;
    }

    public void setPIN1(String PIN1) {
        this.PIN1 = PIN1;
    }

    public String getPIN2() {
        return PIN2;
    }

    public void setPIN2(String PIN2) {
        this.PIN2 = PIN2;
    }

    public String getPUK1() {
        return PUK1;
    }

    public void setPUK1(String PUK1) {
        this.PUK1 = PUK1;
    }

    public String getPUK2() {
        return PUK2;
    }

    public void setPUK2(String PUK2) {
        this.PUK2 = PUK2;
    }

    public String getADM1() {
        return ADM1;
    }

    public void setADM1(String ADM1) {
        this.ADM1 = ADM1;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (ICCID != null ? ICCID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the iccid fields are not set
        if (!(object instanceof UiccDetails)) {
            return false;
        }
        UiccDetails other = (UiccDetails) object;
        if ((this.ICCID == null && other.ICCID != null) || (this.ICCID != null && !this.ICCID.equals(other.ICCID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.UiccDetails[ ICCID=" + ICCID + " ]";
    }
    
}
