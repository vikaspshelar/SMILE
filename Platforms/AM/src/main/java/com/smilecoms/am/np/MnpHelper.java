/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np;

import com.smilecoms.xml.schema.am.RoutingInfo;
import com.smilecoms.am.db.model.MnpPortRequest;
import com.smilecoms.am.db.model.MnpPortRequestPK;
import com.smilecoms.am.db.model.RingfencedNumber;
import com.smilecoms.am.db.op.DAO;
import static com.smilecoms.am.np.ng.NGPortingHelper.MAX_MSG_HEADER_ID;
import com.smilecoms.am.np.tz.TZPortInState;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceList;
import com.smilecoms.commons.sca.ProductInstanceQuery;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.ProductServiceSpecificationMapping;
import com.smilecoms.commons.sca.ProductSpecification;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.ServiceSpecification;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.direct.am.AvailableNumberRange;
import com.smilecoms.commons.sca.direct.bm.PlatformContext;
import com.smilecoms.commons.sca.direct.bm.PortingData;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PhoneNumberRange;
import com.smilecoms.commons.sca.direct.am.PlatformString;
import com.smilecoms.xml.schema.am.PortInEvent;
import com.smilecoms.xml.schema.am.RoutingInfo;
import com.smilecoms.xml.schema.am.RoutingInfoList;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class MnpHelper {
    
    private static final Logger log = LoggerFactory.getLogger(MnpHelper.class);
    
    public static Properties props = null;
    public static final String REQUEST_PROCESSING_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String REQUEST_PROCESSING_STATUS_ERROR = "ERROR";
    
    public static final String REQUEST_PROCESSING_STATUS_DONE = "DONE";
    public static final String MNP_PORTING_DIRECTION_IN = "IN";
    public static final String MNP_PORTING_DIRECTION_OUT = "OUT";
    
    public static final String MNP_CUSTOMER_TYPE_INDIVIDUAL = "individual";
    public static final String MNP_CUSTOMER_TYPE_CORPORATE = "corporate";
    
    public static void initialise() {
        props = new Properties();
        try {            
            log.warn("Here are the MNP properties as contained in env.mnp.config\n");
            log.warn(BaseUtils.getProperty("env.mnp.config"));
            props.load(BaseUtils.getPropertyAsStream("env.mnp.config"));
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.mnp.config: ", ex);
        }
    }
    
    public static String routingInformationListToString(RoutingInfoList routingInfoList) {
        StringBuilder sRoutinInfoList = new StringBuilder();
        // Convert the routing information list object to a comma delimited list.
        String newLine = "";
        
        for(RoutingInfo routingInfo : routingInfoList.getRoutingInfo()) {
            sRoutinInfoList.append(newLine).append(routingInfo.getServiceInstanceId())
                    .append(", ").append(routingInfo.getPhoneNumberRange().getPhoneNumberStart()) 
                    .append(", ").append(routingInfo.getPhoneNumberRange().getPhoneNumberEnd())  
                    .append(", ").append(routingInfo.getRoutingNumber());
            newLine = "\r\n";
       }
        
        return sRoutinInfoList.toString();
    }
    
    
            
    public static String phoneNumberRangeListToString(List<PhoneNumberRange> phoneNumberList) {
        StringBuilder strPhoneNumberList = new StringBuilder();
        // Convert the routing information list object to a comma delimited list.
        String newLine = "";
        
        log.debug("Size of phoneNumberList is: {}", phoneNumberList.size());
                
        for(PhoneNumberRange phoneRange : phoneNumberList) {
            strPhoneNumberList.append(newLine).append(phoneRange.getPhoneNumberStart()) 
                    .append(", ").append(phoneRange.getPhoneNumberEnd());
            newLine = "\r\n";
       }
        
        return strPhoneNumberList.toString();
    }


    public static RoutingInfoList parseStringToRoutingInfoList(String sRoutingInfoList)  throws Exception {
        // This function receives a comma separated list of routingInformationList and returns an object;
        RoutingInfoList routingInfoList = new RoutingInfoList();
        List<String> lstRoutingInfo = stringLinesToList(sRoutingInfoList);
        StringTokenizer routingInfoFields = null;
        
        log.debug("Size of number ranges to parse into routing information list - {}", lstRoutingInfo.size());
        for (String stRoutingInfo : lstRoutingInfo) {
            routingInfoFields = new StringTokenizer(stRoutingInfo, ",");
            
            if(routingInfoFields.countTokens() != 4) {
                throw new Exception("Invalid routing information entry, exactly four fields expected, got [" + stRoutingInfo + "].");
            }
            
            RoutingInfo rInfo = new RoutingInfo();
            rInfo.setServiceInstanceId(Integer.valueOf(routingInfoFields.nextToken()));
            PhoneNumberRange phoneNumberRange = new PhoneNumberRange();
            phoneNumberRange.setPhoneNumberStart(routingInfoFields.nextToken().trim());
            phoneNumberRange.setPhoneNumberEnd(routingInfoFields.nextToken().trim());
            rInfo.setPhoneNumberRange(phoneNumberRange);
            rInfo.setRoutingNumber(routingInfoFields.nextToken().trim());
            routingInfoList.getRoutingInfo().add(rInfo);
        }
        
        return routingInfoList;
    }
    
    public static List <PhoneNumberRange> parseStringToPhoneNumberRangeList(String numberRanges)  throws Exception {
        // This function receives a comma separated list of routingInformationList and returns an object;
        List <PhoneNumberRange> phoneNumberRangeList = new ArrayList();
        
        List<String> lstNumberRangeStrings = stringLinesToList(numberRanges);
        StringTokenizer phoneNumberRangeFields = null;
        
        log.debug("Size of number ranges to parse into routing information list - {}", lstNumberRangeStrings.size());
        for (String strPhoneNumberRange : lstNumberRangeStrings) {
            phoneNumberRangeFields = new StringTokenizer(strPhoneNumberRange, ",");
            
            if(phoneNumberRangeFields.countTokens() != 2) {
                throw new Exception("Invalid phone number range format, exactly two fields expected, got [" + strPhoneNumberRange + "].");
            }
            
            PhoneNumberRange phoneRange = new PhoneNumberRange();
            phoneRange.setPhoneNumberStart(phoneNumberRangeFields.nextToken().trim());
            phoneRange.setPhoneNumberEnd(phoneNumberRangeFields.nextToken().trim());
            
            phoneNumberRangeList.add(phoneRange);
        }   
        
        log.debug("Size of phoneNumberRangeList is: {}", phoneNumberRangeList.size());
        
        return phoneNumberRangeList;
    }
    
    private static List<String> stringLinesToList(String lines) {
            List<String> ret = new ArrayList<String>();
            if(lines == null) {
                lines = "";
            }
            
            StringTokenizer stValues = new StringTokenizer(lines, "\r\n");
            
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (val.trim().length() != 0) {
                    ret.add(val);
                }
            }
            
            return ret;
    }
    
    public static PortInEvent cloneStateContext(IPortState state) throws Exception {
        PortInEvent newPortinEvent = new PortInEvent();
        MnpPortRequest dbTransit = new MnpPortRequest();
        
        synchXMLPortRequestToDBPortRequest(state, dbTransit);
        synchDBPortRequestToPortInEvent(newPortinEvent, dbTransit);
        
        return newPortinEvent;
    }
    
    public static com.smilecoms.commons.sca.PortInEvent makeSCAPortInEvent(PortInEvent inEvent) {
           com.smilecoms.commons.sca.PortInEvent event = new com.smilecoms.commons.sca.PortInEvent();
           
           event.setAutomaticAccept(inEvent.getAutomaticAccept());
           event.setErrorDescription(inEvent.getErrorDescription());
           event.setEmergencyRestoreId(inEvent.getEmergencyRestoreId());
           event.setIsEmergencyRestore(inEvent.getIsEmergencyRestore());
           event.setErrorCode(inEvent.getErrorCode());
           event.setServiceType(inEvent.getServiceType());
           event.setCustomerProfileId(inEvent.getCustomerProfileId());
           event.setOrganisationId(inEvent.getOrganisationId());           
           event.setOrganisationName(inEvent.getOrganisationName());
           event.setOrganisationNumber(inEvent.getOrganisationNumber());
           event.setOrganisationTaxNumber(inEvent.getOrganisationTaxNumber());
           event.setCustomerType(inEvent.getCustomerType());
           event.setSubscriptionType(inEvent.getSubscriptionType());
           event.setPortingDirection(inEvent.getPortingDirection());
           event.setDonorId(inEvent.getDonorId());
           event.setPortingRejectionList(inEvent.getPortingRejectionList());
           event.setHandleManually(inEvent.getHandleManually());
           event.setNpState(inEvent.getNpState());
           event.setPortingDate(inEvent.getPortingDate());
           event.setPortingOrderId(inEvent.getPortingOrderId());
           event.setRangeHolderId(inEvent.getRangeHolderId());
           event.setRecipientId(inEvent.getRecipientId());
           event.setRoutingInfoList(mapAMRoutingInfoListToSCARoutingInfoList(inEvent.getRoutingInfoList()));            event.setRequestDatetime(inEvent.getRequestDatetime());
           event.setProcessingStatus(inEvent.getProcessingStatus());
           event.setValidationMSISDN(inEvent.getValidationMSISDN());
            
           return event;
    }
    
    public static com.smilecoms.commons.sca.RoutingInfoList mapAMRoutingInfoListToSCARoutingInfoList(RoutingInfoList inRoutingInfoList) {
        com.smilecoms.commons.sca.RoutingInfoList outRoutingInfoList = new com.smilecoms.commons.sca.RoutingInfoList();
        
        com.smilecoms.commons.sca.RoutingInfo scaRoutingInfo = null;
        com.smilecoms.commons.sca.PhoneNumberRange scaPhoneNumberRange = null;
        
        for(RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            scaRoutingInfo = new com.smilecoms.commons.sca.RoutingInfo();
            scaPhoneNumberRange = new com.smilecoms.commons.sca.PhoneNumberRange();
            scaPhoneNumberRange.setPhoneNumberStart(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            scaPhoneNumberRange.setPhoneNumberEnd(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            
            scaRoutingInfo.setPhoneNumberRange(scaPhoneNumberRange);
            scaRoutingInfo.setRoutingNumber(inRoutingInfo.getRoutingNumber());
            scaRoutingInfo.setServiceInstanceId(inRoutingInfo.getServiceInstanceId());
            outRoutingInfoList.getRoutingInfo().add(scaRoutingInfo);
        }
        return outRoutingInfoList;
    } 
    
    public static PortInEvent synchDBPortRequestToPortInEvent(PortInEvent event, MnpPortRequest dbPortInRequest) throws Exception {
        
            event.setAutomaticAccept(dbPortInRequest.getAutomaticAccept());
            
            if(event.getErrorDescription() == null || event.getErrorDescription().isEmpty()) {
                event.setErrorDescription(dbPortInRequest.getErrorDescription());
            }
            
            if(event.getEmergencyRestoreId()== null || event.getEmergencyRestoreId().isEmpty()) {
                event.setEmergencyRestoreId(dbPortInRequest.getEmergencyRestoreId());
            }
            
            if(event.getIsEmergencyRestore()== null || event.getIsEmergencyRestore().isEmpty()) {
                event.setIsEmergencyRestore(dbPortInRequest.getIsEmergencyRestore());
            }
            
            if(event.getErrorCode() == null || event.getErrorCode().isEmpty()) {
                event.setErrorCode(dbPortInRequest.getErrorCode());
            }
            
            event.setServiceType(dbPortInRequest.getServiceType());
            event.setCustomerProfileId(dbPortInRequest.getCustomerProfileId());
            
            if(event.getCustomerType() == null || event.getCustomerType().isEmpty()) {
                event.setCustomerType(dbPortInRequest.getCustomerType());
            }
            
            event.setPortingDirection(dbPortInRequest.getMnpPortRequestPK().getPortingDirection());
            event.setDonorId(dbPortInRequest.getDonorId());
            
            if(event.getPortingRejectionList() == null || event.getPortingRejectionList().isEmpty()) {
                event.setPortingRejectionList(dbPortInRequest.getPortingRejectionList());
            }
            
            event.setHandleManually(dbPortInRequest.getHandleManually());
            event.setNpState(dbPortInRequest.getNpState().getStateId());
            event.setPortingDate(Utils.getDateAsXMLGregorianCalendar(dbPortInRequest.getPortingDatetime()));
            event.setPortingOrderId(dbPortInRequest.getMnpPortRequestPK().getPortingOrderId());
            event.setRangeHolderId(dbPortInRequest.getRangeHolderId());
            event.setRecipientId(dbPortInRequest.getRecipientId());
            event.setSubscriptionType(dbPortInRequest.getSubscriptionType());
            
            event.setRoutingInfoList(dbPortInRequest.getRoutingInformationList());
            
            event.setPortRequestFormId(dbPortInRequest.getPortRequestFormId());
            event.setRingFenceIndicator(dbPortInRequest.getRingFenceIndicator());
            event.getRingFenceNumberList().addAll(dbPortInRequest.getRingFenceNumberList());
            
            event.setRequestDatetime(Utils.getDateAsXMLGregorianCalendar(dbPortInRequest.getRequestDatetime()));
            
            if(event.getProcessingStatus() == null || event.getProcessingStatus().isEmpty()) {
                event.setProcessingStatus(dbPortInRequest.getProcessingStatus());
            }
            
            event.setValidationMSISDN(dbPortInRequest.getValidationMsisdn());
            event.setOrganisationId(dbPortInRequest.getOrganisationId()); 
            event.setOrganisationName(dbPortInRequest.getOrganisationName());
            event.setOrganisationNumber(dbPortInRequest.getOrganisationNumber());
            event.setOrganisationTaxNumber(dbPortInRequest.getOrganisationTaxNumber());
           
            return event;
    }     
    
    public static boolean isEmergencyRestore(PortInEvent context) {
        return ((context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty()) || 
                (context.getIsEmergencyRestore() != null && !context.getIsEmergencyRestore().isEmpty() && context.getIsEmergencyRestore().equalsIgnoreCase("Y")));
    }
    
    public static void createEvent(String eventKey, String messageType, String  requestData, String responseData, String errorMessage) {
        log.debug("Writing event for event key {}", eventKey);
       
        String eventData = "REQUEST:\n" + requestData + "\n\nRESPONSE:\n" + ((responseData != null) ? responseData : errorMessage);
        Event event = new Event();
        event.setEventSubType("PortingEvent");
        event.setEventType("MNP");
        event.setEventKey(eventKey + "-" + messageType);
        event.setEventData(eventData);

        try {
            SCAWrapper.getAdminInstance().createEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to create event via SCA call, cause: {}", ex.toString());
        }
    }
    
    
     
    public static void synchXMLPortRequestToDBPortRequest(IPortState state, MnpPortRequest dbPortInRequest) throws Exception {
        PortInEvent portingEvent =   state.getContext();
        if(portingEvent.getPortingOrderId() != null && portingEvent.getPortingDirection() != null) {
            MnpPortRequestPK pk = new MnpPortRequestPK();
            pk.setPortingOrderId(portingEvent.getPortingOrderId());
            pk.setPortingDirection(portingEvent.getPortingDirection());
            dbPortInRequest.setMnpPortRequestPK(pk);
        }
        
        if(portingEvent.getCustomerProfileId() != null) {
            dbPortInRequest.setCustomerProfileId(portingEvent.getCustomerProfileId());
        }
        
        if(portingEvent.getOrganisationId() != null) {
            dbPortInRequest.setOrganisationId(portingEvent.getOrganisationId());
        }
        
        
        if(portingEvent.getServiceType() != null) {
            dbPortInRequest.setServiceType(portingEvent.getServiceType());
        }
        
        if(portingEvent.getValidationMSISDN() != null) {
            dbPortInRequest.setValidationMsisdn(portingEvent.getValidationMSISDN());
        }
        
        if(portingEvent.getEmergencyRestoreId() != null) {
            dbPortInRequest.setEmergencyRestoreId(portingEvent.getEmergencyRestoreId());
        }
        
        if(portingEvent.getIsEmergencyRestore() != null) {
            dbPortInRequest.setIsEmergencyRestore(portingEvent.getIsEmergencyRestore());
        }
        
        if(portingEvent.getCustomerType() != null) {
            dbPortInRequest.setCustomerType(portingEvent.getCustomerType());
        }
        
        if(portingEvent.getRecipientId() != null) {
            dbPortInRequest.setRecipientId(portingEvent.getRecipientId());
        }
        
        if(portingEvent.getDonorId() != null) {
            dbPortInRequest.setDonorId(portingEvent.getDonorId());
        }
        
        if(portingEvent.getRangeHolderId() != null) {
            dbPortInRequest.setRangeHolderId(portingEvent.getRangeHolderId());
        }
        
        if(portingEvent.getRingFenceIndicator() != null) {
            dbPortInRequest.setRingFenceIndicator(portingEvent.getRingFenceIndicator());
        }
        
        if(portingEvent.getPortRequestFormId() != null) {
            dbPortInRequest.setPortRequestFormId(portingEvent.getPortRequestFormId());
        }
        
        if(portingEvent.getHandleManually() != null) {
            dbPortInRequest.setHandleManually(portingEvent.getHandleManually());
        } 
        
        
        if(dbPortInRequest.getRequestDatetime() == null) {
            dbPortInRequest.setRequestDatetime(new Date());
        }
        
        if(portingEvent.getPortingDate() != null) {
            dbPortInRequest.setPortingDatetime(Utils.getJavaDate(portingEvent.getPortingDate()));
        }
        
        dbPortInRequest.setNpState(state);
        
        if ((portingEvent.getRoutingInfoList() != null) && (portingEvent.getRoutingInfoList().getRoutingInfo().size() > 0)) {
            dbPortInRequest.setRoutingInformationList(portingEvent.getRoutingInfoList());
        }
        
        if(dbPortInRequest.getRoutingInformationList() == null)  {
            dbPortInRequest.setRoutingInformationList(new RoutingInfoList());
        }
        
       if ((portingEvent.getRingFenceNumberList() != null) && (portingEvent.getRingFenceNumberList().size() > 0)) {
            dbPortInRequest.setRingFenceNumberList(portingEvent.getRingFenceNumberList());
       }
       
       if ((portingEvent.getReducedRoutingInfoList()!= null) && (portingEvent.getReducedRoutingInfoList().size() > 0)) {
            dbPortInRequest.setReducedRoutingInformationList(portingEvent.getReducedRoutingInfoList());
       }
        
        
       if(portingEvent.getErrorCode() != null) {
            dbPortInRequest.setErrorCode(portingEvent.getErrorCode());
        }
        
        if(portingEvent.getErrorDescription()!= null) {
            dbPortInRequest.setErrorDescription(portingEvent.getErrorDescription());
        }
        
        if(portingEvent.getProcessingStatus()!= null) {
            dbPortInRequest.setProcessingStatus(portingEvent.getProcessingStatus());
        }
        
        if(portingEvent.getSubscriptionType() != null) {
            dbPortInRequest.setSubscriptionType(portingEvent.getSubscriptionType());
        }
        
        if(portingEvent.getPortingRejectionList()!= null) {
            dbPortInRequest.setPortingRejectionList(portingEvent.getPortingRejectionList());
        }
        
        if(portingEvent.getOrganisationName()!= null) {
            dbPortInRequest.setOrganisationName(portingEvent.getOrganisationName());
        }
        
        if(portingEvent.getOrganisationNumber()!= null) {
            dbPortInRequest.setOrganisationNumber(portingEvent.getOrganisationNumber());
        }
        
        
        if(portingEvent.getOrganisationTaxNumber()!= null) {
            dbPortInRequest.setOrganisationTaxNumber(portingEvent.getOrganisationTaxNumber());
        }
        
    } 
    
     
public static void synchMnpPortRequests(MnpPortRequest dbPortInRequestToFrom, MnpPortRequest dbPortInRequestTo) throws Exception {
       
        if(dbPortInRequestToFrom.getCustomerProfileId() != 0) {
            dbPortInRequestTo.setCustomerProfileId(Integer.valueOf(dbPortInRequestToFrom.getCustomerProfileId()));
        }
        
        if(dbPortInRequestToFrom.getOrganisationId() != 0) {
            dbPortInRequestTo.setOrganisationId(Integer.valueOf(dbPortInRequestToFrom.getOrganisationId()));
        }
        
        if(dbPortInRequestToFrom.getServiceType() != null) {
            dbPortInRequestTo.setServiceType(dbPortInRequestToFrom.getServiceType());
        }
        
        if(dbPortInRequestToFrom.getValidationMsisdn()!= null) {
            dbPortInRequestTo.setValidationMsisdn(dbPortInRequestToFrom.getValidationMsisdn());
        }
        
        if(dbPortInRequestToFrom.getEmergencyRestoreId() != null) {
            dbPortInRequestTo.setEmergencyRestoreId(dbPortInRequestToFrom.getEmergencyRestoreId());
        }
        
         if(dbPortInRequestToFrom.getIsEmergencyRestore() != null) {
            dbPortInRequestTo.setIsEmergencyRestore(dbPortInRequestToFrom.getIsEmergencyRestore());
        }
        
        if(dbPortInRequestToFrom.getCustomerType() != null) {
            dbPortInRequestTo.setCustomerType(dbPortInRequestToFrom.getCustomerType());
        }
        
        if(dbPortInRequestToFrom.getRecipientId() != null) {
            dbPortInRequestTo.setRecipientId(dbPortInRequestToFrom.getRecipientId());
        }
        
        if(dbPortInRequestToFrom.getDonorId() != null) {
            dbPortInRequestTo.setDonorId(dbPortInRequestToFrom.getDonorId());
        }
        
        if(dbPortInRequestToFrom.getRangeHolderId() != null) {
            dbPortInRequestTo.setRangeHolderId(dbPortInRequestToFrom.getRangeHolderId());
        }
        
        if(dbPortInRequestToFrom.getRingFenceIndicator() != null) {
            dbPortInRequestTo.setRingFenceIndicator(dbPortInRequestToFrom.getRingFenceIndicator());
        }
        
        if(dbPortInRequestToFrom.getPortRequestFormId() != null) {
            dbPortInRequestTo.setPortRequestFormId(dbPortInRequestToFrom.getPortRequestFormId());
        }
        
        if(dbPortInRequestToFrom.getHandleManually() != null) {
            dbPortInRequestTo.setHandleManually(dbPortInRequestToFrom.getHandleManually());
        } 
        
        
        if(dbPortInRequestTo.getRequestDatetime() == null) {
            dbPortInRequestTo.setRequestDatetime(new Date());
        }
        
        if(dbPortInRequestToFrom.getPortingDatetime() != null) {
            dbPortInRequestTo.setPortingDatetime(dbPortInRequestToFrom.getPortingDatetime());
        }
        
        dbPortInRequestTo.setNpState(dbPortInRequestToFrom.getNpState());
        
        if (dbPortInRequestToFrom.getRoutingInformationList() != null) {
            dbPortInRequestTo.setRoutingInformationList(dbPortInRequestToFrom.getRoutingInformationList());
        }
        
       if ((dbPortInRequestToFrom.getRingFenceNumberList() != null) && (dbPortInRequestToFrom.getRingFenceNumberList().size() > 0)) {
            dbPortInRequestTo.setRingFenceNumberList(dbPortInRequestToFrom.getRingFenceNumberList());
       }
        
       if ((dbPortInRequestToFrom.getReducedRoutingInformationList() != null) && (dbPortInRequestToFrom.getReducedRoutingInformationList().size() > 0)) {
            dbPortInRequestTo.setReducedRoutingInformationList(dbPortInRequestToFrom.getReducedRoutingInformationList());
       }         
        
       if(dbPortInRequestToFrom.getErrorCode() != null) {
            dbPortInRequestTo.setErrorCode(dbPortInRequestToFrom.getErrorCode());
        }
        
        if(dbPortInRequestToFrom.getErrorDescription()!= null) {
            dbPortInRequestTo.setErrorDescription(dbPortInRequestToFrom.getErrorDescription());
        }
        
        if(dbPortInRequestToFrom.getProcessingStatus()!= null) {
            dbPortInRequestTo.setProcessingStatus(dbPortInRequestToFrom.getProcessingStatus());
        }
        
        if(dbPortInRequestToFrom.getSubscriptionType() != null) {
            dbPortInRequestTo.setSubscriptionType(dbPortInRequestToFrom.getSubscriptionType());
        }
        
        if(dbPortInRequestToFrom.getPortingRejectionList()!= null) {
            dbPortInRequestTo.setPortingRejectionList(dbPortInRequestToFrom.getPortingRejectionList());
        }
        
        if(dbPortInRequestToFrom.getOrganisationName()!= null) {
            dbPortInRequestTo.setOrganisationName(dbPortInRequestToFrom.getOrganisationName());
        }
        
        if(dbPortInRequestToFrom.getOrganisationNumber()!= null) {
            dbPortInRequestTo.setOrganisationNumber(dbPortInRequestToFrom.getOrganisationNumber());
        }       
        
        if(dbPortInRequestToFrom.getOrganisationTaxNumber()!= null) {
            dbPortInRequestTo.setOrganisationTaxNumber(dbPortInRequestToFrom.getOrganisationTaxNumber());
        }
        
    } 
     
    public static void formatAVPsForSendingToSCA(List<AVP> avPs, int serviceSpecificationId, boolean populatePhotoData) {
        
        if (avPs == null) {
            return;
        }
        
        for (AVP avp : avPs) {
            if (avp != null && avp.getAttribute() != null) {
                AVP avpConfig = getAVPConfig(avp.getAttribute(), serviceSpecificationId);
                avp.setInputType(avpConfig.getInputType());
                if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                    avp.setValue(Utils.getPublicIdentityForPhoneNumber(avp.getValue()));
                } 
            }
        }
    }
    
    private static AVP getAVPConfig(String attributeName, int serviceSpecificationId) {
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(serviceSpecificationId);
        for (AVP ssAVP : ss.getAVPs()) {
            if (attributeName.equals(ssAVP.getAttribute())) {
                return ssAVP;
            }
        }
        return null;
    }

    public static boolean activatePortedNumberOnSmileNetwork(PortInEvent context) throws Exception {
        // This function will be used to activate a ported number onto Smile network as soon as we 
        // receive the NPCDB_DONOR_ACCEPT_TO_SP (Donor Acceptance) message.
        log.debug("MNP In  activatePortedNumberOnSmileNetwork");
        try {
        // 1. Add the ported numbers on SmileDB's ported_number Table;
                    // AddressManager am = new AddressManager();
                    log.debug("Number of number ranges to port in = {}", context.getRoutingInfoList().getRoutingInfo().size());
                    for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                        log.debug("Adding ported number range [{}, {}] from donor network {} to the database.", new Object[]{
                                         routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                         routingInfo.getPhoneNumberRange().getPhoneNumberEnd(), 
                                         context.getDonorId()});
                        
                        PortingData portingData = new PortingData();
                        portingData.setPlatformContext(new PlatformContext());
                        portingData.getPlatformContext().setTxId(context.getPlatformContext().getTxId());
                        portingData.setStartE164(Long.parseLong(Utils.getFriendlyPhoneNumberKeepingCountryCode(routingInfo.getPhoneNumberRange().getPhoneNumberStart())));
                        portingData.setEndE164(Long.parseLong(Utils.getFriendlyPhoneNumberKeepingCountryCode(routingInfo.getPhoneNumberRange().getPhoneNumberEnd())));
                                                
                        portingData.setInterconnectPartnerCode(context.getRecipientId()); // Number is now hosted by Smile ...
                        SCAWrapper.getAdminInstance().updatePortingData_Direct(portingData);
                        
                        String impu = Utils.getPublicIdentityForPhoneNumber(routingInfo.getPhoneNumberRange().getPhoneNumberStart());
                           
                        // 2. Add the ported numbers to available_numbers table.
                        if(MnpHelper.isEmergencyRestore(context)) {
                          //Free-up the original  number so it can be reused to the new product below.
                          PlatformString numToIssue = new PlatformString();
                          numToIssue.setString(impu);
                          SCAWrapper.getAdminInstance().freeNumber_Direct(numToIssue);

                        } else {
                            AvailableNumberRange availableNumberRange = new AvailableNumberRange();
                            availableNumberRange.setOwnedByCustomerProfileId(context.getCustomerProfileId());
                            // availableNumberRange.setOwnedByOrganisationId(context.getOrganisationId());
                            com.smilecoms.commons.sca.direct.am.PhoneNumberRange phoneNumberRange = 
                                    new com.smilecoms.commons.sca.direct.am.PhoneNumberRange();
                            phoneNumberRange.setPhoneNumberEnd(routingInfo.getPhoneNumberRange().getPhoneNumberEnd());
                            phoneNumberRange.setPhoneNumberStart(routingInfo.getPhoneNumberRange().getPhoneNumberStart());
                                                        
                            availableNumberRange.setPhoneNumberRange(phoneNumberRange);
                            availableNumberRange.setPriceCents(0);
                            if(context.getCustomerType().equalsIgnoreCase(MnpHelper.MNP_CUSTOMER_TYPE_INDIVIDUAL)) {
                                availableNumberRange.setOwnedByCustomerProfileId(context.getCustomerProfileId());
                            }
                            
                            if(context.getCustomerType().equalsIgnoreCase(MnpHelper.MNP_CUSTOMER_TYPE_CORPORATE)) {
                                availableNumberRange.setOwnedByOrganisationId(context.getOrganisationId());
                            }
                            
                            // am.addAvailableNumberRange(availableNumberRange);
                            SCAWrapper.getAdminInstance().addAvailableNumberRange_Direct(availableNumberRange);
                        }
                        
                        // 3. If not a range, then modify Service Instance Here ...
                        if(routingInfo.getServiceInstanceId() == -1) {
                            log.warn("Porting of a number range ({} - {}) into Smile - added into the available_number table, no need to activate services.", routingInfo.getPhoneNumberRange().getPhoneNumberStart(), routingInfo.getPhoneNumberRange().getPhoneNumberEnd());
                        } else {
                            
                            if(MnpHelper.isEmergencyRestore(context)) {
                                // The previous service would have been deleted, so we need to add a new service instance to the product which the old service used to belong.
                                ProductInstanceQuery piQuery = new ProductInstanceQuery();
                                piQuery.setVerbosity(StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP);
                                piQuery.setServiceInstanceId(routingInfo.getServiceInstanceId());
                                ProductInstanceList piList = SCAWrapper.getAdminInstance().getProductInstances(piQuery);
                                
                                if(piList == null || piList.getNumberOfProductInstances() <= 0) {
                                    log.error("MNP Emergency Restore - no product instance found with old/delete service instance {}.", routingInfo.getServiceInstanceId());
                                } else {
                                    if(piList.getNumberOfProductInstances() > 1) { // TOo manny product instances
                                        log.error("MNP Emergency Restore - too many product instances found ({}) for old/deleted service instance ({}), do not know which one to use for emergency restore.", piList.getNumberOfProductInstances(), routingInfo.getServiceInstanceId());
                                    } else { // We have exactly 1 product, use it.
                                        
                                        ProductInstance pi = piList.getProductInstances().get(0);
                                        log.debug("MNP Emergency Restore - adding new voice service to product instance ({})", pi.getProductInstanceId());
                                        ProductOrder po = new ProductOrder();
                                        po.setProductInstanceId(pi.getProductInstanceId());
                                        po.setAction(StAction.NONE);
                                        po.setCustomerId(pi.getCustomerId());
                                        ServiceInstanceOrder siO = new ServiceInstanceOrder();
                                        siO.setAction(StAction.CREATE);
                                        ServiceInstance si = new ServiceInstance();
                                        si.setServiceSpecificationId(100);
                                        //Set AccountID
                                        if (pi.getProductServiceInstanceMappings().isEmpty()) {
                                            // there is no account so create a new one
                                            si.setAccountId(-1);
                                        } else {
                                            si.setAccountId(pi.getProductServiceInstanceMappings().get(0).getServiceInstance().getAccountId());
                                        }
                                        // AVPs
                                        /*ProductSpecification ps = NonUserSpecificCachedDataHelper.getProductSpecification(pi.getProductSpecificationId());
                                        ProductServiceSpecificationMapping pssm = null;
                                        for (ProductServiceSpecificationMapping mapping : ps.getProductServiceSpecificationMappings()) {
                                            if (mapping.getServiceSpecification().getServiceSpecificationId() == 100) {
                                                pssm = mapping;
                                                break;
                                            }
                                        }*/
                                        
                                        si.getAVPs().addAll(NonUserSpecificCachedDataHelper.getServiceSpecification(100).getAVPs());
                                        for (AVP avp : si.getAVPs()) {
                                            if (avp != null && avp.getAttribute() != null) {
                                                    if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                                                        avp.setValue(impu);
                                                    }
                                                }
                                        }
                                        
                                        po.setOrganisationId(pi.getOrganisationId());
                                        si.setCustomerId(pi.getCustomerId());
                                        si.setStatus("AC");
                                        siO.setServiceInstance(si);
                                        siO.setAction(StAction.CREATE);
                                        po.getServiceInstanceOrders().add(siO);
                                        SCAWrapper.getAdminInstance().processOrder(po);
                                    }
                                }    
                            } else { //Normal portin - not an emergency restore.
                            // -- Assign the ported number to the service instance of the customer ...
                            // -- Retrieve the service instance that was used for this porting order.
                            ServiceInstanceQuery siQuery = new ServiceInstanceQuery();
                            siQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                            siQuery.setServiceInstanceId(routingInfo.getServiceInstanceId());
                            ServiceInstanceList siList = SCAWrapper.getAdminInstance().getServiceInstances(siQuery);

                            if (siList.getNumberOfServiceInstances() < 1) { 
                                // TODO - handle missing SI when provisioning a ported number. 
                                log.error("No service instance found with sid {}", routingInfo.getServiceInstanceId());
                                throw new Exception("Service instance with sid " +  routingInfo.getServiceInstanceId() +  " does not exist.");
                            } 

                            ProductOrder pOrder = new ProductOrder();
                            pOrder.setAction(StAction.NONE);
                                                        
                            if(context.getCustomerType().equalsIgnoreCase(MnpHelper.MNP_CUSTOMER_TYPE_CORPORATE)) {
                                pOrder.setOrganisationId(context.getOrganisationId());
                            } else {
                                pOrder.setOrganisationId(0);
                            }
                                                        
                            pOrder.setCustomerId(siList.getServiceInstances().get(0).getCustomerId());

                            ServiceInstanceOrder siOrder = new ServiceInstanceOrder();
                            ServiceInstance siToChange = siList.getServiceInstances().get(0);
                            // Change Public Identity here
                            if (siToChange.getAVPs() != null) {
                                for (AVP avp : siToChange.getAVPs()) {
                                    if (avp != null && avp.getAttribute() != null) {
                                        if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                                            avp.setValue(impu);
                                        }
                                    }
                                }
                            }

                            siOrder.setAction(StAction.UPDATE);
                            pOrder.setProductInstanceId(siList.getServiceInstances().get(0).getProductInstanceId());
                            siToChange.setStatus("AC"); //Activate the service instance - in case this is an emergency restore
                            siOrder.setServiceInstance(siToChange);

                            pOrder.getServiceInstanceOrders().add(siOrder);
                            MnpHelper.formatAVPsForSendingToSCA(pOrder.getServiceInstanceOrders().get(0).getServiceInstance().getAVPs(), pOrder.getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId(), true);
                            SCAWrapper.getAdminInstance().processOrder(pOrder);
                        }
                    }
                    }
                    //All good ...
                    return true;
        } catch(Exception ex) {
            log.error("Error while attempting to activate ported number onto Smile network - ", ex);
            throw ex;
        } finally {
            log.debug("Done  activatePortedNumberOnSmileNetwork");
        }
    } 
    
    public static String getErrorMessage(Exception ex) {
        if(ex.getMessage() == null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        } 
        return ex.getMessage();
    }
    
    
    public static void saveRingFencedNumberList(EntityManager em, List<PhoneNumberRange> ringFencedNumList, String ringFenceReceipientId) {
        
        if(ringFencedNumList != null) {
            for(PhoneNumberRange phoneNumberRange: ringFencedNumList) {
                long startNumber = Long.parseLong(phoneNumberRange.getPhoneNumberStart());
                long endNumber = Long.parseLong(phoneNumberRange.getPhoneNumberEnd());
                RingfencedNumber curRfNumber = null;
                String ndd = "";
                for (long num = startNumber; num <= endNumber; num++) {
                    curRfNumber = new RingfencedNumber();
                    ndd = Utils.getFriendlyPhoneNumberKeepingCountryCode(Utils.makeNationalDirectDial(String.valueOf(num)));
                    curRfNumber.setNumber(Long.parseLong(ndd));
                    curRfNumber.setRingfenceExpiryDate(null); //Does not expire
                    curRfNumber.setRingfenceRecipientId(ringFenceReceipientId);
                    DAO.addRingfencedNumber(em, curRfNumber);
                }
            }
        }
    }
    
    
    public static void clearRingfencedNumber(EntityManager em, RingfencedNumber rfn) {
        DAO.removeRingfencedNumber(em, rfn.getNumber()); // Current logic is to delete the ringfence - to avoid the ringfence table growing beyond proportions.
    }
    
    public static RingfencedNumber doRingfenceValidation(EntityManager em,  String nddNumber, String recipientNetwork) throws Exception {
        
        RingfencedNumber rFN =  DAO.getRingfencedNumber(em, Long.parseLong(nddNumber));
        boolean isRingfenced = false;
        
        if(rFN == null) { //Not found in ringfence list.
            return null;
        } else {
            //Check if ringfence is expired.
            if(rFN.getRingfenceExpiryDate() == null) { // Ring fence is still enforced.
                isRingfenced = true;
            } else {
                // Check if we have passed the expiry date.
                if(rFN.getRingfenceExpiryDate().before(new Date())) { // If ringfence expiry date is in future, then number is in ringfence.
                    return rFN; //  Number is not ringfenced - ringdenced expired.
                } else {
                    isRingfenced = true;
                }
            }
            
            if(isRingfenced) {
                // Check Recipient network is the same and allow the port
                if(recipientNetwork.equalsIgnoreCase(rFN.getRingfenceRecipientId())) {
                    return rFN;
                } else { // Ringfence violation.
                    throw new Exception("Number " + rFN.getNumber() + "is currently in ringfence and can only be ported to network " + rFN.getRingfenceRecipientId() + "."); 
                }                    
            }
        }
        
        return null;
    } 
    
    public static List<PhoneNumberRange> reducePhoneNumberRanges(RoutingInfoList routingInfoList) {
        //First expand the number ranges to ensure the reduced ranges cover all the numbers;
        List<Long> numberList = expandPhoneNumberRanges(routingInfoList);
                
        List<PhoneNumberRange> retRanges = new ArrayList<PhoneNumberRange>();
        PhoneNumberRange curRange = null;
        if (numberList.isEmpty()) {
            return new ArrayList<PhoneNumberRange>();
        }
        // Only necessary if not already sorted
        Collections.sort(numberList);

        long start = numberList.get(0);
        long end = numberList.get(0);

        for (long rev : numberList) {
            if (rev - end > 1) {
                // break in range
                log.debug("Found PhoneNumberRange: [{}, {}]", start, end);
                curRange = new PhoneNumberRange();
                curRange.setPhoneNumberStart(Utils.getFriendlyPhoneNumberKeepingCountryCode(Utils.makeNationalDirectDial(String.valueOf(start))));
                curRange.setPhoneNumberEnd(Utils.getFriendlyPhoneNumberKeepingCountryCode(Utils.makeNationalDirectDial(String.valueOf(end))));
                retRanges.add(curRange);
                start = rev;
            }
            end = rev;
        }
        
        // Add the last range here.
        log.debug("Last PhoneNumberRange: [{}, {}]", start, end);
        curRange = new PhoneNumberRange();
        curRange.setPhoneNumberStart(Utils.getFriendlyPhoneNumberKeepingCountryCode(Utils.makeNationalDirectDial(String.valueOf(start))));
        curRange.setPhoneNumberEnd(Utils.getFriendlyPhoneNumberKeepingCountryCode(Utils.makeNationalDirectDial(String.valueOf(end))));
        retRanges.add(curRange);
        
        return  retRanges;    
    }
    
    public static List<Long> expandPhoneNumberRanges(RoutingInfoList routingInfoList) {
        
        List<Long> numbers = new ArrayList<Long>();
        if(routingInfoList.getRoutingInfo().isEmpty()){
            return numbers;
        }
        
        for (RoutingInfo routingInfo : routingInfoList.getRoutingInfo()) {
            // Check each number in the range ...
            long fromE164 = Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart());
            long toE164 = Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd());
            for (long num = fromE164; num <= toE164; num++) {
                numbers.add(num);
            }
        }        
        return numbers;
    }
}
