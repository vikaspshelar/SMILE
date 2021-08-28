/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.tz;

import com.smilecoms.commons.sca.PortInEvent;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.util.Utils;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.systor.np.commontypes.CustomerType;
import st.systor.np.commontypes.DonorReject;
import st.systor.np.commontypes.PhoneNumberRange;
import st.systor.np.commontypes.RoutingInfo;
import st.systor.np.commontypes.TechnicalFaultType;
import st.systor.np.sp.SpMessageAckType;
import st.systor.np.sp.SpPort;
import st.systor.np.sp.SpTechnicalFault;
/**
 *
 * @author paul
 */
@HandlerChain(file = "/TZhandler.xml")
@WebService(serviceName = "TZMNP", portName = "SpSoap", endpointInterface = "st.systor.np.sp.SpPort", targetNamespace = "http://np.systor.st/sp", wsdlLocation = "TZMNP.wsdl")
@Stateless
public class TZMNP implements SpPort {

    private static final Logger log = LoggerFactory.getLogger(TZMNP.class);
    public static final String CUSTOMER_TYPE_INDIVIDUAL = "individual";
    public static final String CUSTOMER_TYPE_ORGANISATION = "organisation";
    public static final String CUSTOMER_TYPE_UNKNOWN = "unknown";
    public static final String CUSTOMER_ID_TYPE_PASSPORT = "password";
    public static final String CUSTOMER_ID_TYPE_NATIONALID = "nationalid";
    public static final String REQUEST_PROCESSING_STATUS_FAILED = "FAILED";
    public static final String MNP_PORTING_DIRECTION_IN = "IN";
    public static final String MNP_PORTING_DIRECTION_OUT = "OUT";
    // @Resource
    // WebServiceContext wsctx;

    @PostConstruct
    public void startUp() {
        /**
         * Provide for a callback mechanism for our custom auth module to be
         * able to call up the classloading hierarchy into smile commons SCA
         * client
         */
        // BaseUtils.registerForPropsAvailability(this);

        // Platform.init();
        ///SCAAuthenticator auth = new SCAAuthenticatorImpl();
        /// SCALoginModule.setAuthenticator(auth);
    }

    public java.lang.String noop() throws st.systor.np.sp.SpTechnicalFault {
        return "Hello,  I am here....";
    }

    public st.systor.np.sp.SpMessageAckType handleErrorNotification(st.systor.np.commontypes.ErrorNotificationType error) throws st.systor.np.sp.SpAccessFault, st.systor.np.sp.SpTechnicalFault {
        //TODO implement this method
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    // Invoked when a Smile customer goes to another operator and request to port away from Smile 
    public st.systor.np.sp.SpMessageAckType handleNPRequest(st.systor.np.sp.NPRequestToSpType npReq) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        log.debug("TZMNP in  handleNPRequest");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = null;
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {
            response = new SpMessageAckType();
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
            pEvent.setPortingOrderId(String.valueOf(npReq.getNPOrderID()));
            pEvent.setMessageId(npReq.getMessageID());
            pEvent.setValidationMSISDN(npReq.getValidationMSISDN());
            pEvent.setSenderId(npReq.getSenderID());
            pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST);
            pEvent.setIdentityNumber(npReq.getCustomerID());
            if (npReq.getCustomerIDType() == 1) { // For Passport
                pEvent.setCustomerIdType(CUSTOMER_ID_TYPE_PASSPORT);
            } else {
                pEvent.setCustomerIdType(CUSTOMER_ID_TYPE_NATIONALID);
            }

            if (npReq.getCustomerType().equals(CustomerType.PERSONAL)) {
                pEvent.setCustomerType(CUSTOMER_TYPE_INDIVIDUAL);
            } else {
                if (npReq.getCustomerType().equals(CustomerType.BUSINESS)) {
                    pEvent.setCustomerType(CUSTOMER_TYPE_ORGANISATION);
                }
            }
            pEvent.setRecipientId(npReq.getRecipientID());
            pEvent.setDonorId(npReq.getDonorID());
            pEvent.setSubscriptionType(npReq.getSubscriptionType().value());
            pEvent.setServiceType(npReq.getServiceType().value());

            if (npReq.getEmergencyRestoreID() != null && npReq.getEmergencyRestoreID() > 0) {
                pEvent.setEmergencyRestoreId(String.valueOf(npReq.getEmergencyRestoreID()));
            }
            //Set routing information list.
            pEvent.setRoutingInfoList(mapTZMNPRoutingInfoListToSCA(npReq.getRoutingInfoList()));
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            /*if(pEvent.getProcessingStatus().equals(REQUEST_PROCESSING_STATUS_FAILED)) {
                TechnicalFaultType type = new TechnicalFaultType();
                type.setErrorCode(pEvent.getErrorCode());
                type.setDescription(pEvent.getErrorDescription());
                throw new SpTechnicalFault(pEvent.getErrorDescription(), type);
            } else { */
            response.setNPOrderID(npReq.getNPOrderID());
            
            // }
        } catch (Exception stf) {
            response = null;
            log.error("Error while handling NPRequest [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            
            throw fault;            
        }

        return response;
    }

    public st.systor.np.sp.SpMessageAckType handleNPRequestReject(st.systor.np.sp.NPRequestRejectToSpType npReqRej) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        //This message is sent from the NPCDB to the Recipient in case of SMS validation failure.
        log.debug("TZMNP in handleNPRequestReject");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
            pEvent.setPortingOrderId(String.valueOf(npReqRej.getNPOrderID()));
            pEvent.setMessageId(npReqRej.getMessageID());
            pEvent.setSenderId(npReqRej.getSenderID());
            pEvent.setMessageType(StMNPMessageTypes.NPCDB_REQUEST_REJECTED);
            pEvent.setErrorCode(npReqRej.getNpcdbRejectReason().toString());
            pEvent.setErrorDescription(npReqRej.getNpcdbRejectMessage());

            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));

            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPRequestReject [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 

    }

    public st.systor.np.sp.SpMessageAckType handleNPRequestConfirmation(st.systor.np.sp.NPRequestConfirmationToSpType npReqConf) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        log.debug("TZMNP in handleNPRequestConfirmation");
        //This request is received from NPCDB when the NP request is matched to an SMS6 (sent by customer) to confirm the 
        //porting request.
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
            pEvent.setPortingOrderId(String.valueOf(npReqConf.getNPOrderID()));
            pEvent.setMessageId(npReqConf.getMessageID());
            pEvent.setSenderId(npReqConf.getSenderID());
            pEvent.setMessageType(StMNPMessageTypes.NPCDB_REQUEST_VALIDATED_TO_SP);
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
        } catch (Throwable stf) {
            response = null;
            log.error("Error while handling NPRequestConfirmation [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
            
        } 


        return response;
    }

    public st.systor.np.sp.SpMessageAckType handleNPRequestCancel(st.systor.np.sp.NPRequestCancelToSpType npCan) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        //Cancellation of porting request is being triggered from NPCDB.
        log.debug("TZMNP in handleNPRequestCancel");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {

            

            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
            pEvent.setPortingOrderId(String.valueOf(npCan.getNPOrderID()));
            pEvent.setMessageId(npCan.getMessageID());
            pEvent.setSenderId(npCan.getSenderID());

            pEvent.setErrorDescription(npCan.getCancelMessage());
            pEvent.setErrorCode(npCan.getCancelReason().toString());

            pEvent.setMessageType(StMNPMessageTypes.NPCDB_REQUEST_CANCEL_TO_SP);
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPRequestCancel [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault =  new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 
    }

    public st.systor.np.sp.SpMessageAckType handleNPDonorReject(st.systor.np.sp.NPDonorRejectToSpType npRej) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        // Donor Rejects the porting - this happens after NPCDN confirmation.
        log.debug("TZMNP in handleNPDonorReject");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
            pEvent.setPortingOrderId(String.valueOf(npRej.getNPOrderID()));
            pEvent.setMessageId(npRej.getMessageID());
            pEvent.setSenderId(npRej.getSenderID());
            pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);

            String donorRejectList = "";

            for (DonorReject donorReject : npRej.getDonorRejectList().getDonorReject()) {
                donorRejectList += "Range: [" + donorReject.getRejectedPhoneNumberRange().getPhoneNumberStart() + ", "
                        + donorReject.getRejectedPhoneNumberRange().getPhoneNumberEnd() + ", Reject Message:"
                        + donorReject.getDonorRejectMessage() + ", Reason:" + donorReject.getDonorRejectReason() + "]\r\n";
                pEvent.setErrorCode(String.valueOf(donorReject.getDonorRejectReason()));
            }

            pEvent.setErrorDescription(donorRejectList);

            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPDonorReject [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        }
    }

    public st.systor.np.sp.SpMessageAckType handleNPDonorAccept(st.systor.np.sp.NPDonorAcceptToSpType npAcc) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        //All good - Donor accepted the porting - we must provision and activate the number on the Smile network
        // and respond with an NP Activated to the NPCDB;
        log.debug("TZMNP in handleNPDonorAccept");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        
        try {

            
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
            pEvent.setPortingOrderId(String.valueOf(npAcc.getNPOrderID()));
            pEvent.setMessageId(npAcc.getMessageID());
            pEvent.setSenderId(npAcc.getSenderID());

            pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
            pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_ACCEPT_TO_SP);
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));

            return response;

        } catch (Throwable stf) {
            log.error("Error while handling handleNPDonorAccept [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;

        } 
    }

    public st.systor.np.sp.SpMessageAckType handleNPActivated(st.systor.np.sp.NPActivatedToSpType npAct) throws st.systor.np.sp.SpAccessFault, st.systor.np.sp.SpTechnicalFault {
        // This is used on a port-out - when a Smile customer is porting out of Smile network.
        // This serves to inform Smile that the customer has been provisioned and activated on the other
        // network.               
        // Number had been activated on Recipient's network so Smile must deactivate on our network and inform the clearing house.
        log.debug("TZMNP in handleNPActivated");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        
        try {
            pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_OUT_RECIPIENT_ACTIVATED); // Donated number has been activated on Recipient Service provider.
            pEvent.setMessageId(npAct.getMessageID());
            pEvent.setPortingOrderId(String.valueOf(npAct.getNPOrderID()));
            pEvent.setSenderId(npAct.getSenderID());
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT); // NP Activated is received when Smile is acting as donor in the port.

            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));

            return response;

        } catch (Throwable stf) {
            log.error("Error while handling handleNPActivated [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 
    }

    public st.systor.np.sp.SpMessageAckType handleNPDeactivated(st.systor.np.sp.NPDeactivatedToSpType npDeact) throws st.systor.np.sp.SpAccessFault, st.systor.np.sp.SpTechnicalFault {
        //QUESTION: Not sure when will NPCDB call Smile with this message - 
        //clarity is needed from Ssystor.
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public st.systor.np.sp.SpMessageAckType handleNPExecuteBroadcast(st.systor.np.sp.NPExecuteBroadcastToSpType npExecBrcast) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {

        //Broadcast of a ported number was received ...
        // Based on the specs, Smile will receive this message on both outbound and inbound porting requests.
        log.debug("TZMNP in handleNPExecuteBroadcast");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;    
        try {
            // Check if inbound or out bound porting?
            pEvent.setPortingDirection(getPortingDirection(npExecBrcast.getRecipientID()));
            //this message to both state machines for processing.

            pEvent.setPortingOrderId(String.valueOf(npExecBrcast.getNPOrderID()));
            pEvent.setMessageId(npExecBrcast.getMessageID());
            pEvent.setSenderId(npExecBrcast.getSenderID());

            pEvent.setDonorId(npExecBrcast.getDonorID());
            pEvent.setPortingDate(npExecBrcast.getPortingTime());
            pEvent.setRecipientId(npExecBrcast.getRecipientID());

            pEvent.setRoutingInfoList(mapTZMNPRoutingInfoListToSCA(npExecBrcast.getRoutingInfoList()));

            pEvent.setMessageType(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP);
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPDonorAccept [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 
    }

    public com.smilecoms.commons.sca.RoutingInfoList mapPhoneNumberListToSCARoutingInfoList(st.systor.np.commontypes.PhoneNumberList phoneNumberList) {
        com.smilecoms.commons.sca.RoutingInfoList outRoutingInfoList = new com.smilecoms.commons.sca.RoutingInfoList();

        com.smilecoms.commons.sca.RoutingInfo scaRoutingInfo;
        com.smilecoms.commons.sca.PhoneNumberRange scaPhoneNumberRange;

        for (PhoneNumberRange phoneNumberRange : phoneNumberList.getPhoneNumberRange()) {
            scaRoutingInfo = new com.smilecoms.commons.sca.RoutingInfo();
            scaPhoneNumberRange = new com.smilecoms.commons.sca.PhoneNumberRange();
            scaPhoneNumberRange.setPhoneNumberStart(phoneNumberRange.getPhoneNumberStart());
            scaPhoneNumberRange.setPhoneNumberEnd(phoneNumberRange.getPhoneNumberEnd());

            scaRoutingInfo.setPhoneNumberRange(scaPhoneNumberRange);
            scaRoutingInfo.setRoutingNumber("0");

            outRoutingInfoList.getRoutingInfo().add(scaRoutingInfo);
        }
        return outRoutingInfoList;
    }

    public com.smilecoms.commons.sca.RoutingInfoList mapTZMNPRoutingInfoListToSCA(st.systor.np.commontypes.RoutingInfoList inRoutingInfoList) {
        com.smilecoms.commons.sca.RoutingInfoList outRoutingInfoList = new com.smilecoms.commons.sca.RoutingInfoList();

        com.smilecoms.commons.sca.RoutingInfo scaRoutingInfo;
        com.smilecoms.commons.sca.PhoneNumberRange scaPhoneNumberRange;

        for (RoutingInfo inRoutingInfo : inRoutingInfoList.getRoutingInfo()) {
            scaRoutingInfo = new com.smilecoms.commons.sca.RoutingInfo();
            scaPhoneNumberRange = new com.smilecoms.commons.sca.PhoneNumberRange();
            scaPhoneNumberRange.setPhoneNumberStart(inRoutingInfo.getPhoneNumberRange().getPhoneNumberStart());
            scaPhoneNumberRange.setPhoneNumberEnd(inRoutingInfo.getPhoneNumberRange().getPhoneNumberEnd());

            scaRoutingInfo.setPhoneNumberRange(scaPhoneNumberRange);
            scaRoutingInfo.setRoutingNumber(inRoutingInfo.getRoutingNumber());

            outRoutingInfoList.getRoutingInfo().add(scaRoutingInfo);
        }
        return outRoutingInfoList;
    }

    public st.systor.np.sp.SpMessageAckType handleNPExecuteCancel(st.systor.np.sp.NPExecuteCancelToSpType npExecCan) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        //NPC DB Keeps track of timer T5 or T13, if either timer expires before the recipient 
        // provider sends NP Activated messaged - the NPCDB sends the above NP Execute cancel to both
        // Donor and Recipient to terminame the process. Process terminates and Recipient provider can start 
        // over again by sending a new NP request.
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        
        try {
            
            pEvent.setPortingDirection(null); //handleNPExecuteCancel will be called for both inbound port and outbound port, so pass 
            //this message to both state machines for processing.
            pEvent.setPortingOrderId(String.valueOf(npExecCan.getNPOrderID()));

            pEvent.setMessageId(npExecCan.getMessageID());
            pEvent.setSenderId(npExecCan.getSenderID());

            pEvent.setErrorDescription(npExecCan.getCancelMessage());
            pEvent.setErrorCode(npExecCan.getCancelReason().toString());

            pEvent.setMessageType(StMNPMessageTypes.NPCDB_EXECUTE_CANCEL_TO_SP);
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            
            log.error("Error while handling handleNPDonorAccept [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;

        } 
    }

    public st.systor.np.sp.SpMessageAckType handleNPReturnBroadcast(st.systor.np.sp.NPReturnBroadcastToSpType npRetBrcast) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        // This message is sent by the NPCDB to Recipient Provider, Rander Holder and all other 
        // service provider to notify them about an NP Return. NP Return happens when the Recipient Provider
        // terminates a subscription of the a ported number.
        log.debug("TZMNP in handleNPReturnBroadcast");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {
            

            pEvent.setPortingOrderId(String.valueOf(npRetBrcast.getNPOrderID()));
            pEvent.setMessageId(npRetBrcast.getMessageID());
            pEvent.setSenderId(npRetBrcast.getSenderID());
            pEvent.setMessageType(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST);
            
            npRetBrcast.getRangeHolderID();
            
            pEvent.setRoutingInfoList(mapPhoneNumberListToRoutingInfoList(npRetBrcast.getPhoneNumberList(), npRetBrcast.getRangeHolderID()));
            
            pEvent.setRangeHolderId(npRetBrcast.getRangeHolderID());
            pEvent.setDonorId(npRetBrcast.getSubscriptionProviderID());
            
            String smileParticipatingID = Utils.getPropertyValueFromList("env.mnp.config", "SmileMNPParticipatingId");
            
            //if(smileParticipatingID.equals(npRetBrcast.getRangeHolderID())) { // This number ranges are coming back into smile from another operator
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
            /*} else { // A return to another operator has taken place - update routing tables.
                pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
            }*/
            
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));

            return response;
           
        } catch (Throwable stf) {
            log.error("Error while handling handleNPReturnBroadcast [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 
    }

 public static com.smilecoms.commons.sca.RoutingInfoList  mapPhoneNumberListToRoutingInfoList(st.systor.np.commontypes.PhoneNumberList phoneNumberList, String rangeHolderID) {
        
        com.smilecoms.commons.sca.RoutingInfoList outRoutingInfoList = new com.smilecoms.commons.sca.RoutingInfoList();
        
        com.smilecoms.commons.sca.RoutingInfo scaRoutingInfo;
        com.smilecoms.commons.sca.PhoneNumberRange scaPhoneNumberRange;
        
        for(st.systor.np.commontypes.PhoneNumberRange numRange : phoneNumberList.getPhoneNumberRange()) {
            scaRoutingInfo = new com.smilecoms.commons.sca.RoutingInfo();
            scaPhoneNumberRange = new com.smilecoms.commons.sca.PhoneNumberRange();
            
            scaPhoneNumberRange.setPhoneNumberEnd(numRange.getPhoneNumberEnd());
            scaPhoneNumberRange.setPhoneNumberStart(numRange.getPhoneNumberStart());
            
            if(rangeHolderID == null) {        
                scaRoutingInfo.setRoutingNumber("");
            } else {
                scaRoutingInfo.setRoutingNumber(rangeHolderID);
            }
            
            scaRoutingInfo.setPhoneNumberRange(scaPhoneNumberRange);
            
            outRoutingInfoList.getRoutingInfo().add(scaRoutingInfo);
        }
        return outRoutingInfoList;
    }

    public st.systor.np.sp.SpMessageAckType handleNPRingFenceDeny(st.systor.np.sp.NPRingFenceDenyToSpType npRingFenceDeny) throws st.systor.np.sp.SpAccessFault, st.systor.np.sp.SpTechnicalFault {
        //TODO implement this method
        log.debug("TZMNP in handleNPRingFenceDeny");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        TechnicalFaultType tft;
        try {
            

            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN); //handleNPExecuteCancel will be called for both inbound port and outbound port, so pass 
            //this message to both state machines for processing.
            pEvent.setPortingOrderId(String.valueOf(npRingFenceDeny.getNPOrderID()));

            pEvent.setMessageId(npRingFenceDeny.getMessageID());
            pEvent.setSenderId(npRingFenceDeny.getSenderID());
            pEvent.setDonorId("");
            pEvent.setRecipientId("");

            pEvent.setErrorCode(String.valueOf(npRingFenceDeny.getNpcdbRejectReason()));
            pEvent.setErrorDescription(npRingFenceDeny.getNpcdbRejectMessage());

            pEvent.setMessageType(StMNPMessageTypes.NPCDB_RING_FENCE_DENIED);

            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPRingFenceDeny [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            throw new SpTechnicalFault(stf.getMessage(), tft);
        } 
    }

    public st.systor.np.sp.SpMessageAckType handleNPRingFenceApprove(st.systor.np.sp.NPRingFenceApproveToSpType npRingFenceApprove) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        // - A ring fence request has been approved.
        log.debug("TZMNP in handleNPRingFenceApprove");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {
        
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN); //handleNPExecuteCancel will be called for both inbound port and outbound port, so pass 
            //this message to both state machines for processing.
            pEvent.setPortingOrderId(String.valueOf(npRingFenceApprove.getNPOrderID()));

            pEvent.setMessageId(npRingFenceApprove.getMessageID());
            pEvent.setSenderId(npRingFenceApprove.getSenderID());
            pEvent.setDonorId("");
            pEvent.setRecipientId("");

            pEvent.setMessageType(StMNPMessageTypes.NPCDB_RING_FENCE_APPROVED);
            pEvent.setRoutingInfoList(mapPhoneNumberListToSCARoutingInfoList(npRingFenceApprove.getPhoneNumberList()));

            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPRingFenceApprove [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 
    }

    public st.systor.np.sp.SpMessageAckType handleNPEmergencyRestoreDeny(st.systor.np.sp.NPEmergencyRestoreDenyToSpType npEmergRestDeny) throws st.systor.np.sp.SpAccessFault, st.systor.np.sp.SpTechnicalFault {
        log.debug("TZMNP in handleNPEmergencyRestoreDeny");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        try {
            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
            pEvent.setPortingOrderId(String.valueOf(npEmergRestDeny.getNPOrderID()));
            pEvent.setEmergencyRestoreId(null);

            pEvent.setMessageId(npEmergRestDeny.getMessageID());
            pEvent.setSenderId(npEmergRestDeny.getSenderID());

            pEvent.setErrorCode(String.valueOf(npEmergRestDeny.getNpcdbRejectReason()));
            pEvent.setErrorDescription(npEmergRestDeny.getNpcdbRejectMessage());

            pEvent.setMessageType(StMNPMessageTypes.NPCDB_EMERGENCY_RESTORE_DENIED);
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPEmergencyRestoreDeny [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 
    }

    public st.systor.np.sp.SpMessageAckType handleNPEmergencyRestoreApprove(st.systor.np.sp.NPEmergencyRestoreApproveToSpType npEmergRestApp) throws st.systor.np.sp.SpTechnicalFault, st.systor.np.sp.SpAccessFault {
        // - A ring fence request has been approved.
        log.debug("TZMNP in handleNPEmergencyRestoreApprove");
        PortInEvent pEvent = new PortInEvent();
        SpMessageAckType response = new SpMessageAckType();
        SpTechnicalFault fault;
        TechnicalFaultType tft;
        
        try {
            

            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
            pEvent.setPortingOrderId(String.valueOf(npEmergRestApp.getOriginalNPOrderID()));
            pEvent.setEmergencyRestoreId(String.valueOf(npEmergRestApp.getEmergencyRestoreID()));
            pEvent.setIsEmergencyRestore("Y");
            
            pEvent.setMessageId(npEmergRestApp.getMessageID());
            pEvent.setSenderId(npEmergRestApp.getSenderID());

            pEvent.setMessageType(StMNPMessageTypes.NPCDB_EMERGENCY_RESTORE_APPROVED);
            SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);

            response.setNPOrderID(Long.valueOf(pEvent.getPortingOrderId()));
            return response;
        } catch (Throwable stf) {
            log.error("Error while handling handleNPEmergencyRestoreApprove [{}]", stf.getMessage(), stf);
            String message = (stf.getMessage() != null ? stf.getMessage() : stf.toString());
            tft = new TechnicalFaultType();
            tft.setErrorCode("10111");
            tft.setDescription(message);
            fault = new SpTechnicalFault(stf.getMessage(), tft);
            throw fault;
        } 
    }

    
   
    private String getPortingDirection(String recipientId) {
        
        String mnpParticipatingID = Utils.getPropertyValueFromList("env.mnp.config", "SmileMNPParticipatingId");
        String testMnpParticipatingID = Utils.getPropertyValueFromList("env.mnp.config", "SmileTestMNPParticipatingId");
            
        if(recipientId.equals(mnpParticipatingID) || recipientId.equals(testMnpParticipatingID)) {
            return MNP_PORTING_DIRECTION_IN;
        } else {
            return MNP_PORTING_DIRECTION_OUT;
        }
        
    }

    
}

