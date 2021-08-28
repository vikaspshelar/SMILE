/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sf;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author jaybeepee
 */
@Embeddable
public class MdxStatisticPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "LOCATION")
    private String location;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STAT_NAME")
    private String statName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "STAT_TYPE")
    private String statType;

    public MdxStatisticPK() {
    }

    public MdxStatisticPK(String location, String statName, String statType) {
        this.location = location;
        this.statName = statName;
        this.statType = statType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatName() {
        return statName;
    }

    public void setStatName(String statName) {
        this.statName = statName;
    }

    public String getStatType() {
        return statType;
    }

    public void setStatType(String statType) {
        this.statType = statType;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (location != null ? location.hashCode() : 0);
        hash += (statName != null ? statName.hashCode() : 0);
        hash += (statType != null ? statType.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MdxStatisticPK)) {
            return false;
        }
        MdxStatisticPK other = (MdxStatisticPK) object;
        if ((this.location == null && other.location != null) || (this.location != null && !this.location.equals(other.location))) {
            return false;
        }
        if ((this.statName == null && other.statName != null) || (this.statName != null && !this.statName.equals(other.statName))) {
            return false;
        }
        if ((this.statType == null && other.statType != null) || (this.statType != null && !this.statType.equals(other.statType))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.sf.MdxStatisticPK[ location=" + location + ", statName=" + statName + ", statType=" + statType + " ]";
    }
    
}
