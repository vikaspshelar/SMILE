/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

import com.cloudhopper.smpp.SmppBindType;
import com.smilecoms.commons.util.Utils;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author paul
 */
@Entity
@Table(name = "interconnect_smsc")
public class InterconnectSMSC implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "INTERCONNECT_SMSC_ID")
    private Integer interconnectSmscId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "NAME")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Column(name = "HOST")
    private String host;
    @Basic(optional = false)
    @NotNull
    @Column(name = "HOST_MAPPING")
    private String hostMapping;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PORT")
    private int port;
    @Basic(optional = false)
    @NotNull
    @Column(name = "SYSTEM_ID")
    private String systemId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "PASSWORD")
    private String password;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONNECTION_TYPE")
    private String connectionType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONNECTION_TIMEOUT")
    private int connectionTimeout;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONNECTION_DRE_TIMEOUT")
    private int connectionDreTimeout;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONNECTION_DWM_INTERVAL")
    private int connectionDwmInterval;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CLIENT_WINDOW_SIZE")
    private int clientWindowSize;
    @Basic(optional = false)
    @NotNull
    @Column(name = "COUNTERS_ENABLED")
    private int countersEnabled;
    @Basic(optional = false)
    @NotNull
    @Column(name = "LOG_BYTES_ENABLED")
    private int logBytesEnabled;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ENABLED")
    private int enabled;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CLIENT_BIND")
    private int clientBind;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CLIENT_MONITOR_EXECUTOR_THREADS")
    private int clientMonitorExecutorThreads;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONFIG_VERSION")
    private int configVersion;
    @Basic(optional = false)
    @NotNull
    @Column(name = "INTERNAL_TRUNK_ID")
    private String internalTrunkId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RESPONSE_TIMEOUT")
    private int responseTimeout;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MIN_SUBMIT_GAP_MS")
    private int minSubmitGapMs;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MNP_PREFIX")
    private String mnpPrefix;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CONFIG")
    private String config;
    
    public static final int UDH_CONCATENATION_MODE = 1;
    public static final int TLV_CONCATENATION_MODE = 2;
    
    public InterconnectSMSC() {
    }

    public InterconnectSMSC(Integer interconnectSmscId) {
        this.interconnectSmscId = interconnectSmscId;
    }

    public InterconnectSMSC(Integer interconnectSmscId, String name, String host, int port, String systemId, String password, String connectionType, int connectionTimeout, int connectionDreTimeout, int connectionDwmInterval, int clientWindowSize, int countersEnabled, int logBytesEnabled) {
        this.interconnectSmscId = interconnectSmscId;
        this.name = name;
        this.host = host;
        this.port = port;
        this.systemId = systemId;
        this.password = password;
        this.connectionType = connectionType;
        this.connectionTimeout = connectionTimeout;
        this.connectionDreTimeout = connectionDreTimeout;
        this.connectionDwmInterval = connectionDwmInterval;
        this.clientWindowSize = clientWindowSize;
        this.countersEnabled = countersEnabled;
        this.logBytesEnabled = logBytesEnabled;
    }

    public Integer getInterconnectSmscId() {
        return interconnectSmscId;
    }

    public void setInterconnectSmscId(Integer interconnectSmscId) {
        this.interconnectSmscId = interconnectSmscId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHostMapping() {
        return hostMapping;
    }

    public void setHostMapping(String hostMapping) {
        this.hostMapping = hostMapping;
    }

    public int getClientBind() {
        return clientBind;
    }

    public void setClientBind(int clientBind) {
        this.clientBind = clientBind;
    }
    
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public SmppBindType getBindType() {
        if (this.connectionType.equalsIgnoreCase("transceiver")) {
            return SmppBindType.TRANSCEIVER;
        } else if (this.connectionType.equalsIgnoreCase("receiver")) {
            return SmppBindType.RECEIVER;
        } else if (this.connectionType.equalsIgnoreCase("transmitter")) {
            return SmppBindType.TRANSMITTER;
        } else {
            return SmppBindType.TRANSCEIVER;
        }
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getConnectionDreTimeout() {
        return connectionDreTimeout;
    }

    public void setConnectionDreTimeout(int connectionDreTimeout) {
        this.connectionDreTimeout = connectionDreTimeout;
    }

    public int getConnectionDwmInterval() {
        return connectionDwmInterval;
    }

    public void setConnectionDwmInterval(int connectionDwmInterval) {
        this.connectionDwmInterval = connectionDwmInterval;
    }

    public int getClientWindowSize() {
        return clientWindowSize;
    }

    public void setClientWindowSize(int clientWindowSize) {
        this.clientWindowSize = clientWindowSize;
    }

    public int getCountersEnabled() {
        return countersEnabled;
    }

    public void setCountersEnabled(int countersEnabled) {
        this.countersEnabled = countersEnabled;
    }

    public int getMinSubmitGapMs() {
        return minSubmitGapMs;
    }

    public void setMinSubmitGapMs(int minSubmitGapMs) {
        this.minSubmitGapMs = minSubmitGapMs;
    }

    
    public int getLogBytesEnabled() {
        return logBytesEnabled;
    }

    public void setLogBytesEnabled(int logBytesEnabled) {
        this.logBytesEnabled = logBytesEnabled;
    }

    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public int getClientMonitorExecutorThreads() {
        return clientMonitorExecutorThreads;
    }

    public void setClientMonitorExecutorThreads(int clientMonitorExecutorThreads) {
        this.clientMonitorExecutorThreads = clientMonitorExecutorThreads;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }

    public String getInternalTrunkId() {
        return internalTrunkId;
    }

    public void setInternalTrunkId(String internalTrunkId) {
        this.internalTrunkId = internalTrunkId;
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public String getMnpPrefix() {
        return mnpPrefix;
    }

    public void setMnpPrefix(String mnpPrefix) {
        this.mnpPrefix = mnpPrefix;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (interconnectSmscId != null ? interconnectSmscId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof InterconnectSMSC)) {
            return false;
        }
        InterconnectSMSC other = (InterconnectSMSC) object;
        if ((this.interconnectSmscId == null && other.interconnectSmscId != null) || (this.interconnectSmscId != null && !this.interconnectSmscId.equals(other.interconnectSmscId))) {
            return false;
        }
        if (this.configVersion != other.configVersion) {
            return false;
        }
        return true;
    }
    
    public String getPropertyFromConfig(String propName) {
        if (config == null) {
            return null;
        }
        return Utils.getValueFromCRDelimitedAVPString(config, propName);
    }
    
    public int getPropertyAsIntFromConfig(String propName) {
        if (config == null) {
            return -1;
        }
        String res = Utils.getValueFromCRDelimitedAVPString(config, propName);
        if (res == null || res.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(res);
    }
    
    public boolean getPropertyAsBooleanFromConfig(String propName) {
        if (config == null) {
            return false;
        }
        String res = Utils.getValueFromCRDelimitedAVPString(config, propName);
        if (res == null || res.isEmpty()) {
            return false;
        }
        return Boolean.valueOf(res);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id=").append(interconnectSmscId);
        sb.append(" Name=").append(name);
        sb.append(" Enabled=").append(enabled);
        sb.append(" Host=").append(host);
        sb.append(" Port=").append(port);
        sb.append(" TrunkId=").append(internalTrunkId);
        sb.append(" Version=").append(configVersion);
        return sb.toString();
    }
    
    

}
