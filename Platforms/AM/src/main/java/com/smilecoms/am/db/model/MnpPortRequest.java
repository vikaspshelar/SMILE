/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.db.model;

import com.smilecoms.xml.schema.am.RoutingInfoList;
import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.am.np.PortInEventHandler;
import com.smilecoms.xml.schema.am.PhoneNumberRange;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author mukosi
 */
@Entity
@Table(name = "mnp_port_request")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "MnpPortRequest.findAll", query = "SELECT m FROM MnpPortRequest m"),
    @NamedQuery(name = "MnpPortRequest.findByPortingOrderId", query = "SELECT m FROM MnpPortRequest m WHERE m.mnpPortRequestPK.portingOrderId = :portingOrderId"),
    @NamedQuery(name = "MnpPortRequest.findByPortingDirection", query = "SELECT m FROM MnpPortRequest m WHERE m.mnpPortRequestPK.portingDirection = :portingDirection"),
    @NamedQuery(name = "MnpPortRequest.findByCustomerProfileId", query = "SELECT m FROM MnpPortRequest m WHERE m.customerProfileId = :customerProfileId"),
    @NamedQuery(name = "MnpPortRequest.findByNpState", query = "SELECT m FROM MnpPortRequest m WHERE m.npState = :npState"),
    @NamedQuery(name = "MnpPortRequest.findByNpMessageId", query = "SELECT m FROM MnpPortRequest m WHERE m.npMessageId = :npMessageId"),
    @NamedQuery(name = "MnpPortRequest.findByLastModified", query = "SELECT m FROM MnpPortRequest m WHERE m.lastModified = :lastModified"),
    @NamedQuery(name = "MnpPortRequest.findByRequestDatetime", query = "SELECT m FROM MnpPortRequest m WHERE m.requestDatetime = :requestDatetime"),
    @NamedQuery(name = "MnpPortRequest.findByPortingDatetime", query = "SELECT m FROM MnpPortRequest m WHERE m.portingDatetime = :portingDatetime"),
    @NamedQuery(name = "MnpPortRequest.findByRoutingInformationList", query = "SELECT m FROM MnpPortRequest m WHERE m.routingInformationList = :routingInformationList"),
    @NamedQuery(name = "MnpPortRequest.findByPhoneNumberList", query = "SELECT m FROM MnpPortRequest m WHERE m.phoneNumberList = :phoneNumberList"),
    @NamedQuery(name = "MnpPortRequest.findByValidationMsisdn", query = "SELECT m FROM MnpPortRequest m WHERE m.validationMsisdn = :validationMsisdn"),
    @NamedQuery(name = "MnpPortRequest.findByHandleManually", query = "SELECT m FROM MnpPortRequest m WHERE m.handleManually = :handleManually"),
    @NamedQuery(name = "MnpPortRequest.findBySubscriptionType", query = "SELECT m FROM MnpPortRequest m WHERE m.subscriptionType = :subscriptionType"),
    @NamedQuery(name = "MnpPortRequest.findByCustomerType", query = "SELECT m FROM MnpPortRequest m WHERE m.customerType = :customerType"),
    @NamedQuery(name = "MnpPortRequest.findByDonorId", query = "SELECT m FROM MnpPortRequest m WHERE m.donorId = :donorId"),
    @NamedQuery(name = "MnpPortRequest.findByRecipientId", query = "SELECT m FROM MnpPortRequest m WHERE m.recipientId = :recipientId"),
    @NamedQuery(name = "MnpPortRequest.findByRangeHolderId", query = "SELECT m FROM MnpPortRequest m WHERE m.rangeHolderId = :rangeHolderId"),
    @NamedQuery(name = "MnpPortRequest.findByPortingRejectionList", query = "SELECT m FROM MnpPortRequest m WHERE m.portingRejectionList = :portingRejectionList"),
    @NamedQuery(name = "MnpPortRequest.findByProcessingStatus", query = "SELECT m FROM MnpPortRequest m WHERE m.processingStatus = :processingStatus"),
    @NamedQuery(name = "MnpPortRequest.findByErrorDescription", query = "SELECT m FROM MnpPortRequest m WHERE m.errorDescription = :errorDescription"),
    @NamedQuery(name = "MnpPortRequest.findByErrorCode", query = "SELECT m FROM MnpPortRequest m WHERE m.errorCode = :errorCode"),
    @NamedQuery(name = "MnpPortRequest.findByEmergencyRestoreId", query = "SELECT m FROM MnpPortRequest m WHERE m.emergencyRestoreId = :emergencyRestoreId"),
    @NamedQuery(name = "MnpPortRequest.findByBlockOrderId", query = "SELECT m FROM MnpPortRequest m WHERE m.blockOrderId = :blockOrderId"),
    @NamedQuery(name = "MnpPortRequest.findByBlockOrderCount", query = "SELECT m FROM MnpPortRequest m WHERE m.blockOrderCount = :blockOrderCount"),
    @NamedQuery(name = "MnpPortRequest.findByAutomaticAccept", query = "SELECT m FROM MnpPortRequest m WHERE m.automaticAccept = :automaticAccept"),
    @NamedQuery(name = "MnpPortRequest.findBySubscriptionProviderId", query = "SELECT m FROM MnpPortRequest m WHERE m.subscriptionProviderId = :subscriptionProviderId")})
public class MnpPortRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected MnpPortRequestPK mnpPortRequestPK;
    @Column(name = "CUSTOMER_PROFILE_ID")
    private int customerProfileId;
    @Column(name = "ORGANISATION_ID")
    private int organisationId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "NP_STATE")
    private String npState;
    @Column(name = "NP_MESSAGE_ID")
    private BigInteger npMessageId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "REQUEST_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestDatetime;
    @Column(name = "PORTING_DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date portingDatetime;
    @Column(name = "ROUTING_INFORMATION_LIST")
    private String routingInformationList;
    @Column(name = "REDUCED_ROUTING_INFORMATION_LIST")
    private String reducedRoutingInformationList;
    @Size(max = 1000)
    @Column(name = "PHONE_NUMBER_LIST")
    private String phoneNumberList;
    @Size(max = 20)
    @Column(name = "VALIDATION_MSISDN")
    private String validationMsisdn;
    @Size(max = 10)
    @Column(name = "SERVICE_TYPE")
    private String serviceType;
    @Size(max = 5)
    @Column(name = "HANDLE_MANUALLY")
    private String handleManually;
    @Size(max = 8)
    @Column(name = "SUBSCRIPTION_TYPE")
    private String subscriptionType;
    @Size(max = 10)
    @Column(name = "CUSTOMER_TYPE")
    private String customerType;
    @Size(max = 20)
    @Column(name = "DONOR_ID")
    private String donorId;
    @Size(max = 20)
    @Column(name = "RECIPIENT_ID")
    private String recipientId;
    @Size(max = 10)
    @Column(name = "RANGE_HOLDER_ID")
    private String rangeHolderId;
    @Size(max = 2000)
    @Column(name = "PORTING_REJECTION_LIST")
    private String portingRejectionList;
    @Size(max = 20)
    @Column(name = "PROCESSING_STATUS")
    private String processingStatus;
    @Column(name = "ERROR_DESCRIPTION")
    private String errorDescription;
    @Size(max = 50)
    @Column(name = "ERROR_CODE")
    private String errorCode;
    @Column(name = "EMERGENCY_RESTORE_ID")
    private String emergencyRestoreId;
    @Size(max = 14)
    @Column(name = "IS_EMERGENCY_RESTORE")
    private String isEmergencyRestore;
    @Size(max = 1)
    @Column(name = "BLOCK_ORDER_ID")
    private String blockOrderId;
    @Column(name = "BLOCK_ORDER_COUNT")
    private Integer blockOrderCount;
    @Column(name = "AUTOMATIC_ACCEPT")
    private Integer automaticAccept;
    @Column(name = "SUBSCRIPTION_PROVIDER_ID")
    private Integer subscriptionProviderId;
    @Column(name = "PORT_REQUEST_FORM_ID")
    private String portRequestFormId;
    @Column(name = "RING_FENCE_NUMBER_LIST")
    private String ringFenceNumberList;
    @Column(name = "RING_FENCE_INDICATOR")
    private String ringFenceIndicator;
    @Column(name = "LAST_MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;
    
    @Size(max = 200)
    @Column(name = "ORGANISATION_NAME")
    private String organisationName;
        
    @Size(max = 50)
    @Column(name = "ORGANISATION_NUMBER")
    private String organisationNumber;
    
    @Size(max = 50)
    @Column(name = "ORGANISATION_TAX_NUMBER")
    private String organisationTaxNumber;
    
    public MnpPortRequest() {
    }

    public MnpPortRequest(MnpPortRequestPK mnpPortRequestPK) {
        this.mnpPortRequestPK = mnpPortRequestPK;
    }

    public MnpPortRequest(MnpPortRequestPK mnpPortRequestPK, int customerProfileId, String npState, Date requestDatetime, String routingInformationList) {
        this.mnpPortRequestPK = mnpPortRequestPK;
        this.customerProfileId = customerProfileId;
        this.npState = npState;
        this.requestDatetime = requestDatetime;
        this.routingInformationList = routingInformationList;
    }

    public MnpPortRequest(String portingOrderId, String portingDirection) {
        this.mnpPortRequestPK = new MnpPortRequestPK(portingOrderId, portingDirection);
    }

    public MnpPortRequestPK getMnpPortRequestPK() {
        return mnpPortRequestPK;
    }

    public void setMnpPortRequestPK(MnpPortRequestPK mnpPortRequestPK) {
        this.mnpPortRequestPK = mnpPortRequestPK;
    }

    public int getCustomerProfileId() {
        return customerProfileId;
    }

    public void setCustomerProfileId(int customerProfileId) {
        this.customerProfileId = customerProfileId;
    }

    public String getPortRequestFormId() {
        return portRequestFormId;
    }

    public void setPortRequestFormId(String portRequestFormId) {
        this.portRequestFormId = portRequestFormId;
    }
        
    public String getRingFenceIndicator() {
        return ringFenceIndicator;
    }

    public void setRingFenceIndicator(String ringFenceIndicator) {
        this.ringFenceIndicator = ringFenceIndicator;
    }
    
    
   public IPortState getNpState() throws Exception {
        if(getMnpPortRequestPK().getPortingDirection().equals(MnpHelper.MNP_PORTING_DIRECTION_IN)) {
            return PortInEventHandler.getPortInStateMachine().getState(npState);
        } else
        if(getMnpPortRequestPK().getPortingDirection().equals(MnpHelper.MNP_PORTING_DIRECTION_OUT)) {
            return PortInEventHandler.getPortOutStateMachine().getState(npState);
        } else {
            throw new Exception("Invalid porting state " + npState);
        }
    }

public void setNpState(IPortState portingState) {
        this.npState = portingState.getStateId();
    }


    public BigInteger getNpMessageId() {
        return npMessageId;
    }

    public void setNpMessageId(BigInteger npMessageId) {
        this.npMessageId = npMessageId;
    }

    public Date getRequestDatetime() {
        return requestDatetime;
    }

    public void setRequestDatetime(Date requestDatetime) {
        this.requestDatetime = requestDatetime;
    }

    public Date getPortingDatetime() {
        return portingDatetime;
    }

    public void setPortingDatetime(Date portingDatetime) {
        this.portingDatetime = portingDatetime;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lasModified) {
        this.lastModified = lasModified;
    }

    
    public RoutingInfoList getRoutingInformationList() throws Exception {
        return MnpHelper.parseStringToRoutingInfoList(routingInformationList);
    }

    public void setRoutingInformationList(RoutingInfoList routingInformationList) {
        this.routingInformationList =  MnpHelper.routingInformationListToString(routingInformationList);
    }

    public List <PhoneNumberRange> getRingFenceNumberList() throws Exception {
        return MnpHelper.parseStringToPhoneNumberRangeList(ringFenceNumberList);
    }

    public void setRingFenceNumberList(List <PhoneNumberRange> ringFenceNumberList) {
        this.ringFenceNumberList =  MnpHelper.phoneNumberRangeListToString(ringFenceNumberList);
    }
    
    public List <PhoneNumberRange> getReducedRoutingInformationList() throws Exception {
        return MnpHelper.parseStringToPhoneNumberRangeList(reducedRoutingInformationList);
    }

    public void setReducedRoutingInformationList(List <PhoneNumberRange> reducedList) {
        this.reducedRoutingInformationList =  MnpHelper.phoneNumberRangeListToString(reducedList);
    }


    public String getPhoneNumberList() {
        return phoneNumberList;
    }

    public void setPhoneNumberList(String phoneNumberList) {
        this.phoneNumberList = phoneNumberList;
    }

    public String getValidationMsisdn() {
        return validationMsisdn;
    }

    public void setValidationMsisdn(String validationMsisdn) {
        this.validationMsisdn = validationMsisdn;
    }

    public String getHandleManually() {
        return handleManually;
    }

    public void setHandleManually(String handleManually) {
        this.handleManually = handleManually;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRangeHolderId() {
        return rangeHolderId;
    }

    public void setRangeHolderId(String rangeHolderId) {
        this.rangeHolderId = rangeHolderId;
    }

    public String getPortingRejectionList() {
        return portingRejectionList;
    }

    public void setPortingRejectionList(String portingRejectionList) {
        this.portingRejectionList = portingRejectionList;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getEmergencyRestoreId() {
        return emergencyRestoreId;
    }

    public void setEmergencyRestoreId(String emergencyRestoreId) {
        this.emergencyRestoreId = emergencyRestoreId;
    }
   
    public String getIsEmergencyRestore() {
        return isEmergencyRestore;
    }

    public void setIsEmergencyRestore(String emergencyRestore) {
        this.isEmergencyRestore = emergencyRestore;
    }
   
    
    public String getBlockOrderId() {
        return blockOrderId;
    }

    public void setBlockOrderId(String blockOrderId) {
        this.blockOrderId = blockOrderId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Integer getBlockOrderCount() {
        return blockOrderCount;
    }

    public void setBlockOrderCount(Integer blockOrderCount) {
        this.blockOrderCount = blockOrderCount;
    }

    public Integer getAutomaticAccept() {
        return automaticAccept;
    }

    public void setAutomaticAccept(Integer automaticAccept) {
        this.automaticAccept = automaticAccept;
    }

    public Integer getSubscriptionProviderId() {
        return subscriptionProviderId;
    }

    public void setSubscriptionProviderId(Integer subscriptionProviderId) {
        this.subscriptionProviderId = subscriptionProviderId;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getOrganisationNumber() {
        return organisationNumber;
    }

    public void setOrganisationNumber(String organisationNumber) {
        this.organisationNumber = organisationNumber;
    }

    public String getOrganisationTaxNumber() {
        return organisationTaxNumber;
    }

    public void setOrganisationTaxNumber(String organisationTaxNumber) {
        this.organisationTaxNumber = organisationTaxNumber;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (mnpPortRequestPK != null ? mnpPortRequestPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MnpPortRequest)) {
            return false;
        }
        MnpPortRequest other = (MnpPortRequest) object;
        if ((this.mnpPortRequestPK == null && other.mnpPortRequestPK != null) || (this.mnpPortRequestPK != null && !this.mnpPortRequestPK.equals(other.mnpPortRequestPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.smilecoms.am.MnpPortRequest[ mnpPortRequestPK=" + mnpPortRequestPK + " ]";
    }
    
}
