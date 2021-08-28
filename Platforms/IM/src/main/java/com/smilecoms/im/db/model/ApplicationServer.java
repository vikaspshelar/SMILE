/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "application_server", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "ApplicationServer.findAll", query = "SELECT a FROM ApplicationServer a")})
public class ApplicationServer implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @NotNull
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Column(name = "server_name")
    private String serverName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "default_handling")
    private int defaultHandling;
    @Basic(optional = false)
    @NotNull
    @Column(name = "service_info")
    private String serviceInfo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "diameter_address")
    private String diameterAddress;
    @Basic(optional = false)
    @NotNull
    @Column(name = "rep_data_size_limit")
    private int repDataSizeLimit;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr")
    private short udr;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pur")
    private short pur;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr")
    private short snr;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_rep_data")
    private short udrRepData;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_impu")
    private short udrImpu;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_ims_user_state")
    private short udrImsUserState;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_scscf_name")
    private short udrScscfName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_ifc")
    private short udrIfc;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_location")
    private short udrLocation;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_user_state")
    private short udrUserState;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_charging_info")
    private short udrChargingInfo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_msisdn")
    private short udrMsisdn;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_psi_activation")
    private short udrPsiActivation;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_dsai")
    private short udrDsai;
    @Basic(optional = false)
    @NotNull
    @Column(name = "udr_aliases_rep_data")
    private short udrAliasesRepData;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pur_rep_data")
    private short purRepData;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pur_psi_activation")
    private short purPsiActivation;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pur_dsai")
    private short purDsai;
    @Basic(optional = false)
    @NotNull
    @Column(name = "pur_aliases_rep_data")
    private short purAliasesRepData;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_rep_data")
    private short snrRepData;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_impu")
    private short snrImpu;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_ims_user_state")
    private short snrImsUserState;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_scscf_name")
    private short snrScscfName;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_ifc")
    private short snrIfc;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_psi_activation")
    private short snrPsiActivation;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_dsai")
    private short snrDsai;
    @Basic(optional = false)
    @NotNull
    @Column(name = "snr_aliases_rep_data")
    private short snrAliasesRepData;
    @OneToMany(mappedBy = "applicationServer")
    private Collection<Ifc> ifcCollection;

    public ApplicationServer() {
    }

    public ApplicationServer(Integer id) {
        this.id = id;
    }

    public ApplicationServer(Integer id, String name, String serverName, int defaultHandling, String serviceInfo, String diameterAddress, int repDataSizeLimit, short udr, short pur, short snr, short udrRepData, short udrImpu, short udrImsUserState, short udrScscfName, short udrIfc, short udrLocation, short udrUserState, short udrChargingInfo, short udrMsisdn, short udrPsiActivation, short udrDsai, short udrAliasesRepData, short purRepData, short purPsiActivation, short purDsai, short purAliasesRepData, short snrRepData, short snrImpu, short snrImsUserState, short snrScscfName, short snrIfc, short snrPsiActivation, short snrDsai, short snrAliasesRepData) {
        this.id = id;
        this.name = name;
        this.serverName = serverName;
        this.defaultHandling = defaultHandling;
        this.serviceInfo = serviceInfo;
        this.diameterAddress = diameterAddress;
        this.repDataSizeLimit = repDataSizeLimit;
        this.udr = udr;
        this.pur = pur;
        this.snr = snr;
        this.udrRepData = udrRepData;
        this.udrImpu = udrImpu;
        this.udrImsUserState = udrImsUserState;
        this.udrScscfName = udrScscfName;
        this.udrIfc = udrIfc;
        this.udrLocation = udrLocation;
        this.udrUserState = udrUserState;
        this.udrChargingInfo = udrChargingInfo;
        this.udrMsisdn = udrMsisdn;
        this.udrPsiActivation = udrPsiActivation;
        this.udrDsai = udrDsai;
        this.udrAliasesRepData = udrAliasesRepData;
        this.purRepData = purRepData;
        this.purPsiActivation = purPsiActivation;
        this.purDsai = purDsai;
        this.purAliasesRepData = purAliasesRepData;
        this.snrRepData = snrRepData;
        this.snrImpu = snrImpu;
        this.snrImsUserState = snrImsUserState;
        this.snrScscfName = snrScscfName;
        this.snrIfc = snrIfc;
        this.snrPsiActivation = snrPsiActivation;
        this.snrDsai = snrDsai;
        this.snrAliasesRepData = snrAliasesRepData;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getDefaultHandling() {
        return defaultHandling;
    }

    public void setDefaultHandling(int defaultHandling) {
        this.defaultHandling = defaultHandling;
    }

    public String getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(String serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public String getDiameterAddress() {
        return diameterAddress;
    }

    public void setDiameterAddress(String diameterAddress) {
        this.diameterAddress = diameterAddress;
    }

    public int getRepDataSizeLimit() {
        return repDataSizeLimit;
    }

    public void setRepDataSizeLimit(int repDataSizeLimit) {
        this.repDataSizeLimit = repDataSizeLimit;
    }

    public short getUdr() {
        return udr;
    }

    public void setUdr(short udr) {
        this.udr = udr;
    }

    public short getPur() {
        return pur;
    }

    public void setPur(short pur) {
        this.pur = pur;
    }

    public short getSnr() {
        return snr;
    }

    public void setSnr(short snr) {
        this.snr = snr;
    }

    public short getUdrRepData() {
        return udrRepData;
    }

    public void setUdrRepData(short udrRepData) {
        this.udrRepData = udrRepData;
    }

    public short getUdrImpu() {
        return udrImpu;
    }

    public void setUdrImpu(short udrImpu) {
        this.udrImpu = udrImpu;
    }

    public short getUdrImsUserState() {
        return udrImsUserState;
    }

    public void setUdrImsUserState(short udrImsUserState) {
        this.udrImsUserState = udrImsUserState;
    }

    public short getUdrScscfName() {
        return udrScscfName;
    }

    public void setUdrScscfName(short udrScscfName) {
        this.udrScscfName = udrScscfName;
    }

    public short getUdrIfc() {
        return udrIfc;
    }

    public void setUdrIfc(short udrIfc) {
        this.udrIfc = udrIfc;
    }

    public short getUdrLocation() {
        return udrLocation;
    }

    public void setUdrLocation(short udrLocation) {
        this.udrLocation = udrLocation;
    }

    public short getUdrUserState() {
        return udrUserState;
    }

    public void setUdrUserState(short udrUserState) {
        this.udrUserState = udrUserState;
    }

    public short getUdrChargingInfo() {
        return udrChargingInfo;
    }

    public void setUdrChargingInfo(short udrChargingInfo) {
        this.udrChargingInfo = udrChargingInfo;
    }

    public short getUdrMsisdn() {
        return udrMsisdn;
    }

    public void setUdrMsisdn(short udrMsisdn) {
        this.udrMsisdn = udrMsisdn;
    }

    public short getUdrPsiActivation() {
        return udrPsiActivation;
    }

    public void setUdrPsiActivation(short udrPsiActivation) {
        this.udrPsiActivation = udrPsiActivation;
    }

    public short getUdrDsai() {
        return udrDsai;
    }

    public void setUdrDsai(short udrDsai) {
        this.udrDsai = udrDsai;
    }

    public short getUdrAliasesRepData() {
        return udrAliasesRepData;
    }

    public void setUdrAliasesRepData(short udrAliasesRepData) {
        this.udrAliasesRepData = udrAliasesRepData;
    }

    public short getPurRepData() {
        return purRepData;
    }

    public void setPurRepData(short purRepData) {
        this.purRepData = purRepData;
    }

    public short getPurPsiActivation() {
        return purPsiActivation;
    }

    public void setPurPsiActivation(short purPsiActivation) {
        this.purPsiActivation = purPsiActivation;
    }

    public short getPurDsai() {
        return purDsai;
    }

    public void setPurDsai(short purDsai) {
        this.purDsai = purDsai;
    }

    public short getPurAliasesRepData() {
        return purAliasesRepData;
    }

    public void setPurAliasesRepData(short purAliasesRepData) {
        this.purAliasesRepData = purAliasesRepData;
    }

    public short getSnrRepData() {
        return snrRepData;
    }

    public void setSnrRepData(short snrRepData) {
        this.snrRepData = snrRepData;
    }

    public short getSnrImpu() {
        return snrImpu;
    }

    public void setSnrImpu(short snrImpu) {
        this.snrImpu = snrImpu;
    }

    public short getSnrImsUserState() {
        return snrImsUserState;
    }

    public void setSnrImsUserState(short snrImsUserState) {
        this.snrImsUserState = snrImsUserState;
    }

    public short getSnrScscfName() {
        return snrScscfName;
    }

    public void setSnrScscfName(short snrScscfName) {
        this.snrScscfName = snrScscfName;
    }

    public short getSnrIfc() {
        return snrIfc;
    }

    public void setSnrIfc(short snrIfc) {
        this.snrIfc = snrIfc;
    }

    public short getSnrPsiActivation() {
        return snrPsiActivation;
    }

    public void setSnrPsiActivation(short snrPsiActivation) {
        this.snrPsiActivation = snrPsiActivation;
    }

    public short getSnrDsai() {
        return snrDsai;
    }

    public void setSnrDsai(short snrDsai) {
        this.snrDsai = snrDsai;
    }

    public short getSnrAliasesRepData() {
        return snrAliasesRepData;
    }

    public void setSnrAliasesRepData(short snrAliasesRepData) {
        this.snrAliasesRepData = snrAliasesRepData;
    }

    public Collection<Ifc> getIfcCollection() {
        return ifcCollection;
    }

    public void setIfcCollection(Collection<Ifc> ifcCollection) {
        this.ifcCollection = ifcCollection;
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
        if (!(object instanceof ApplicationServer)) {
            return false;
        }
        ApplicationServer other = (ApplicationServer) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.ApplicationServer[ id=" + id + " ]";
    }
    
}
