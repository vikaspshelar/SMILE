/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.db.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "impi", catalog = "hss_db", schema = "")
@NamedQueries({
    @NamedQuery(name = "Impi.findAll", query = "SELECT i FROM Impi i")})
public class Impi implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Column(name = "identity")
    private String identity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "k")
    private String k;
    @Basic(optional = false)
    @NotNull
    @Column(name = "public_k")
    private String public_k;
    @Basic(optional = false)
    @NotNull
    @Column(name = "auth_scheme")
    private int authScheme;
    @Basic(optional = false)
    @NotNull
    @Column(name = "default_auth_scheme")
    private int defaultAuthScheme;
    @Basic(optional = false)
    @NotNull
    @Column(name = "amf")
    private String amf;
    @Basic(optional = false)
    @NotNull
    @Column(name = "op")
    private String op;
    @Basic(optional = false)
    @NotNull
    @Column(name = "sqn")
    private String sqn;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ip")
    private String ip;
    @Basic(optional = false)
    @NotNull
    @Column(name = "sim_locked_imei_list")
    private String simLockedImeiList;
    @Basic(optional = false)
    @NotNull
    @Column(name = "line_identifier")
    private String lineIdentifier;
    @Column(name = "iccid")
    private String iccid;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "impi")
    private Collection<ImpiImpu> impiImpuCollection;
    @OneToMany(mappedBy = "impi")
    private Collection<ImpiApn> impiApnCollection;
    @JoinColumn(name = "id_imsu", referencedColumnName = "id")
    @ManyToOne
    private Imsu imsu;
    @Basic(optional = false)
    @Column(name = "ossbss_reference_id")
    private String OSSBSSReferenceId;
    @Column(name = "status")
    private String status;
    @Column(name = "info")
    private String info;

    public Impi() {
    }

    public Impi(Integer id) {
        this.id = id;
    }

    public Impi(Integer id, String identity, String k, int authScheme, int defaultAuthScheme, String amf, String op, String sqn, String ip, String lineIdentifier) {
        this.id = id;
        this.identity = identity;
        this.k = k;
        this.authScheme = authScheme;
        this.defaultAuthScheme = defaultAuthScheme;
        this.amf = amf;
        this.op = op;
        this.sqn = sqn;
        this.ip = ip;
        this.lineIdentifier = lineIdentifier;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getK() {
        return k;
    }

    public void setK(String k) {
        this.k = k;
    }

    public String getPublicK() {
        return public_k;
    }

    public void setPublicK(String public_k) {
        this.public_k = public_k;
    }

    public int getAuthScheme() {
        return authScheme;
    }

    public void setAuthScheme(int authScheme) {
        this.authScheme = authScheme;
    }

    public int getDefaultAuthScheme() {
        return defaultAuthScheme;
    }

    public void setDefaultAuthScheme(int defaultAuthScheme) {
        this.defaultAuthScheme = defaultAuthScheme;
    }

    public String getAmf() {
        return amf;
    }

    public void setAmf(String amf) {
        this.amf = amf;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getSqn() {
        return sqn;
    }

    public void setSqn(String sqn) {
        this.sqn = sqn;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSimLockedImeiList() {
        return simLockedImeiList;
    }

    public void setSimLockedImeiList(String simLockedImeiList) {
        this.simLockedImeiList = simLockedImeiList;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getRegionalSubscriptionZoneCodes() {
        String regionalSubscriptionZoneCodes = "";
        String info = getInfo();

        if (info != null && !info.isEmpty()) {
            String lines[] = info.split("\\r?\\n");
            for (String line : lines) {
                if (line.startsWith("RegionalSubscriptionZoneCodes")) {
                    String avp[] = line.split("=");
                    if (avp.length == 2) {
                        regionalSubscriptionZoneCodes = avp[1];
                    }
                }
            }
        }

        return regionalSubscriptionZoneCodes;
    }

    public void setRegionalSubscriptionZoneCodes(String regionalSubscriptionZoneCodes) {

        String info = getInfo();
        boolean replaced = false;
        StringBuilder infoStringBuilder = new StringBuilder();

        if (info != null && !info.isEmpty()) {
            String lines[] = info.split("\\r?\\n");
            for (int a = 0; a < lines.length; a++) {
                if (lines[a].startsWith("RegionalSubscriptionZoneCodes")) {
                    if (regionalSubscriptionZoneCodes.isEmpty()) {
                        lines[a] = "";
                    } else {
                        lines[a] = "RegionalSubscriptionZoneCodes=" + regionalSubscriptionZoneCodes;
                    }
                    replaced = true;
                }
            }
            for (String line : lines) {
                if (!line.isEmpty()) {
                    infoStringBuilder.append(line);
                    infoStringBuilder.append("\n");
                }
            }
        }

        if (!replaced && !regionalSubscriptionZoneCodes.isEmpty()) {
            String tmp = "RegionalSubscriptionZoneCodes=" + regionalSubscriptionZoneCodes;
            infoStringBuilder.append(tmp);
        }
        setInfo(infoStringBuilder.toString());
    }

    public String getLineIdentifier() {
        return lineIdentifier;
    }

    public void setLineIdentifier(String lineIdentifier) {
        this.lineIdentifier = lineIdentifier;
    }

    public Collection<ImpiImpu> getImpiImpuCollection() {
        return impiImpuCollection;
    }

    public void setImpiImpuCollection(Collection<ImpiImpu> impiImpuCollection) {
        this.impiImpuCollection = impiImpuCollection;
    }

    public Collection<ImpiApn> getImpiApnCollection() {
        return impiApnCollection;
    }

    public void setImpiApnCollection(Collection<ImpiApn> impiApnCollection) {
        this.impiApnCollection = impiApnCollection;
    }

    public Imsu getImsu() {
        return imsu;
    }

    public void setImsu(Imsu imsu) {
        this.imsu = imsu;
    }

    public String getOSSBSSReferenceId() {
        return OSSBSSReferenceId;
    }

    public void setOSSBSSReferenceId(String OSSBSSReferenceId) {
        this.OSSBSSReferenceId = OSSBSSReferenceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        if (!(object instanceof Impi)) {
            return false;
        }
        Impi other = (Impi) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.im.model.Impi[ id=" + id + " ]";
    }

    public static int generateAuthScheme(boolean akav1, boolean akav2, boolean md5, boolean digest,
            boolean sip_digest, boolean http_digest, boolean early, boolean nass_bundle, boolean all) {

        if (all) {
            return 255;
        } else {
            int result = 0;

            if (akav1) {
                result |= 1;
            }
            if (akav2) {
                result |= 2;
            }
            if (md5) {
                result |= 4;
            }
            if (digest) {
                result |= 8;
            }
            if (http_digest) {
                result |= 16;
            }
            if (early) {
                result |= 32;
            }
            if (nass_bundle) {
                result |= 64;
            }
            if (sip_digest) {
                result |= 128;
            }
            return result;
        }
    }
}
