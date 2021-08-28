/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.ng;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.PhoneNumberRange;
import com.smilecoms.commons.sca.Photograph;
import com.smilecoms.commons.sca.PortInEvent;
import com.smilecoms.commons.sca.RoutingInfo;
import com.smilecoms.commons.sca.RoutingInfoList;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.util.Utils;
import com.telcordia.inpac.ws.AttachedDocType;
import com.telcordia.inpac.ws.NPCWebServicePortType;
import com.telcordia.inpac.ws.binding.NPCData;
import com.telcordia.inpac.ws.ProcessNPCMsgResponse;
import com.telcordia.inpac.ws.binding.MultiNbrRejectReasonType;
import com.telcordia.inpac.ws.binding.MultiReqRejectReasonType;
import com.telcordia.inpac.ws.binding.NumberListType;
import com.telcordia.inpac.ws.binding.NumberListWithFlagReasonType;
import com.telcordia.inpac.ws.binding.NumberListWithFlagType;
import com.telcordia.inpac.ws.binding.NumberRetBroadcastMsgType;
import com.telcordia.inpac.ws.binding.NumberRetReqBlockHolderMsgType;
import com.telcordia.inpac.ws.binding.NumberRetValResultMsgType;
import com.telcordia.inpac.ws.binding.PartValidationType;
import com.telcordia.inpac.ws.binding.PartWebServiceRespType;
import com.telcordia.inpac.ws.binding.PortActBroadcastMsgType;
import com.telcordia.inpac.ws.binding.PortActErrorMsgType;
import com.telcordia.inpac.ws.binding.PortAppReqDonorMsgType;
import com.telcordia.inpac.ws.binding.PortAppRspErrorMsgType;
import com.telcordia.inpac.ws.binding.PortAppRspRecipientMsgType;
import com.telcordia.inpac.ws.binding.PortDeactErrorMsgType;
import com.telcordia.inpac.ws.binding.PortDeactReqDonorMsgType;
import com.telcordia.inpac.ws.binding.PortDeactRspRecipientMsgType;
import com.telcordia.inpac.ws.binding.PortOrdValResultMsgType;
import com.telcordia.inpac.ws.binding.PortRevActBroadcastMsgType;
import com.telcordia.inpac.ws.binding.PortRevActErrorMsgType;
import com.telcordia.inpac.ws.binding.PortRevDeactErrorMsgType;
import com.telcordia.inpac.ws.binding.PortRevDeactReqDonorMsgType;
import com.telcordia.inpac.ws.binding.PortRevDeactRspRecipientMsgType;
import com.telcordia.inpac.ws.binding.PortRevReqDonorMsgType;
import com.telcordia.inpac.ws.binding.PortRevRspErrorMsgType;
import com.telcordia.inpac.ws.binding.PortRevRspRecipientMsgType;
import com.telcordia.inpac.ws.binding.PortRevValResultMsgType;
import com.telcordia.inpac.ws.binding.RingFenceValErrorMsgType;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.BindingType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author mukosi
 */
@HandlerChain(file = "/NGhandler.xml")
@WebService(serviceName = "NGMNP", portName = "NPCWebServiceHttpSoap12Endpoint", endpointInterface = "com.telcordia.inpac.ws.NPCWebServicePortType", targetNamespace = "http://ws.inpac.telcordia.com", wsdlLocation = "NGMNP.wsdl")
@BindingType(value = "http://java.sun.com/xml/ns/jaxws/2003/05/soap/bindings/HTTP/")
// @DeclareRoles("Customer")
@Stateless
public class NGMNP implements NPCWebServicePortType {

    private static final Logger log = LoggerFactory.getLogger(NGMNP.class);
    private static DocumentBuilderFactory mDocFactory = null;
    private static DocumentBuilder mDocBuilder = null;
    private static XPathFactory factory = null;
    private static XPath xPath = null;
    private static Properties props = null;
    public static final String MNP_PORTING_DIRECTION_IN = "IN";
    public static final String MNP_PORTING_DIRECTION_OUT = "OUT";
    public static final String REQUEST_PROCESSING_STATUS_SUCCESS = "Success";
    public static final String REQUEST_PROCESSING_STATUS_ERROR = "Error";
    
    public static final String MNP_SERVICE_NOT_ACTIVE_ERROR_CODE = "OPR1001"; // The service has been suspended under instructions from the customer
    public static final String MNP_INVALID_CUSTOMER_ERROR_CODE = "OPR1005"; // No longer with this customer. The number being ported is ceased and is in quarantine/being reallocated."
    public static final String MNP_INVALID_NUMBER_ERROR_CODE = "OPR1004";
    public static final String MNP_ERROR_CODE_OTHER = "OPR1006";

    // @RolesAllowed("Customer")
    public com.telcordia.inpac.ws.ProcessNPCMsgResponse processNPCMsg(com.telcordia.inpac.ws.ProcessNPCMsgRequest parameters) {
        
        String messageId = "";
        String transactionId = "";
        NPCData npcData = null;
        ProcessNPCMsgResponse response = new ProcessNPCMsgResponse();
        
        try {
            
            init();
            //TODO implement this method
            // throw new UnsupportedOperationException("Not implemented yet.");
            // parameters.
            log.error("NGMNP Received Packet with the following parameters:[" +
                    "\nparameters.getPassword(): " + new String(Utils.decodeBase64(parameters.getPassword())) + 
                    "\nparameters.getUserID():" + parameters.getUserID() + 
                    "\nparameters.getXmlMsg():" + parameters.getXmlMsg() +
                    "\nparameters.getAttachedDoc().size()):" + parameters.getAttachedDoc().size());

            //Get the JAXB Context for MNP Nigeria
            Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);
            npcData = (NPCData) um.unmarshal(new StringReader(parameters.getXmlMsg())); 
            
            PortInEvent pEvent = new PortInEvent();
            response = new ProcessNPCMsgResponse();

            messageId = npcData.getMessageHeader().getMessageID();
            String senderId  = npcData.getMessageHeader().getSender();
            transactionId = npcData.getMessageHeader().getTransactionID();
            
            
            pEvent.setPortingOrderId(transactionId);
            
            try {
            switch (messageId) {
                
                case "3002" : { // Repatriation Port Order Validation Result
                    
                    PortRevValResultMsgType npcMessage = npcData.getNPCMessage().getPortReversalValidationResult();
                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    String requestAcceptedYN = npcMessage.getRequestAcceptFlag();
                    switch (requestAcceptedYN) {

                        case "Y": { // Subscriber has sent SMS 0 - Validation SMS
                            //Port Order Validation Result Accepted Message from NPC to Recipient
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            // pEvent.setRoutingInfoList(mapNGMNPPortedNumbersToSCARoutingInfoList(document));
                            // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_REQUEST_VALIDATED_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response  = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                        }
                        case "N": // Rejection
                            // Port Order Validation Result Rejected Message from NPC to Recipient
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            // pEvent.setRoutingInfoList(mapNGMNPPortedNumbersToSCARoutingInfoList(npcData));
                            // pEvent.setDonorRejectList(mapNGMNPDonorRejectionList(document));
                            // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                            pEvent.setPortingRejectionList(mapNGMNPNumbersWithFlagReasonToSCAPortingRejectionList(npcMessage.getNumbersWithFlagReason()));
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_REQUEST_CANCEL_TO_SP);
                            String requestRejectReasons = "";
                            if(npcMessage != null) {
                                for(MultiReqRejectReasonType reason: npcMessage.getRequestRejectReasons()) {
                                    requestRejectReasons = requestRejectReasons + reason.getRequestRejectReason() + "\n";
                                    pEvent.setErrorCode(reason.getRequestRejectReason());
                                }
                            }   pEvent.setErrorDescription(requestRejectReasons);
                            // pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                        default:
                            break;
                    }
                    response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "0", REQUEST_PROCESSING_STATUS_SUCCESS);
                    return response;
                }
                case "1002" : // Port Order Validation Result 
                    // Message 1002 – Port Oder Validation Response
                    // If the porting request included numbers to be ring-fenced; note that rejection of the port order validation request will be a sufficient condition for the Recipient to know that the numbers will not be ring fenced; NPC will not send Message 1093 in this case.
                                        
                    PortOrdValResultMsgType npcMessage = npcData.getNPCMessage().getPortOrderValidationResult();
                    
                    pEvent.setMessageId(Long.valueOf(messageId));

                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);

                    String requestAcceptedYN = npcMessage.getRequestAcceptFlag();
                    switch (requestAcceptedYN) {

                        case "Y": { // Subscriber has sent SMS 0 - Validation SMS
                            //Port Order Validation Result Accepted Message from NPC to Recipient
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            // pEvent.setRoutingInfoList(mapNGMNPPortedNumbersToSCARoutingInfoList(document));
                            // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_REQUEST_VALIDATED_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                            }
                        case "N": {// Rejection
                            // Port Order Validation Result Rejected Message from NPC to Recipient
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            // pEvent.setRoutingInfoList(mapNGMNPPortedNumbersToSCARoutingInfoList(npcData));
                            // pEvent.setDonorRejectList(mapNGMNPDonorRejectionList(document));
                            // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                            pEvent.setPortingRejectionList(mapNGMNPNumbersWithFlagReasonToSCAPortingRejectionList(npcMessage.getNumbersWithFlagReason()));
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_REQUEST_CANCEL_TO_SP);
                            String requestRejectReasons = "";
                            if(npcMessage != null) {
                                for(MultiReqRejectReasonType reason: npcMessage.getRequestRejectReasons()) {
                                    requestRejectReasons = requestRejectReasons + reason.getRequestRejectReason() + "\n";
                                    pEvent.setErrorCode(reason.getRequestRejectReason());
                                }
                            }   pEvent.setErrorDescription(requestRejectReasons);
                            // pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                        }
                        default:
                            break;
                    }
                    response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "0", REQUEST_PROCESSING_STATUS_SUCCESS);
                    return response;
                case "3003" : {  // Port Reversal Request
                    pEvent.setIsEmergencyRestore("Y"); // This will be used as an indication for Emergency Repatriation
                    PortRevReqDonorMsgType message = npcData.getNPCMessage().getPortReversalReqDonor();
                        /* MessageID = 1003 will arrive from NPCDB to kick-off a porting out request - i.e. when a subscriber want to port away from Smile */ 
                        String donorId = message.getDonor();
                        if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) {
                        
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            pEvent.setDonorId(donorId);
                            pEvent.setRecipientId(message.getRecipient());
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST);
                            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                            
                            pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;    
                            
                        } else {
                            log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                        }
                        
                    response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "0", REQUEST_PROCESSING_STATUS_SUCCESS); 
                    return response;
                }
                case "1003": { // Porting Approval Request
                    
                        PortAppReqDonorMsgType message = npcData.getNPCMessage().getPortApprovalReqDonor();
                        /* MessageID = 1003 will arrive from NPCDB to kick-off a porting out request - i.e. when a subscriber want to port away from Smile */ 
                        String donorId = message.getDonor();
                        if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) {
                        
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            pEvent.setDonorId(donorId);
                            pEvent.setRecipientId(message.getRecipient());
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST);
                            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                            pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                            pEvent.setCustomerType(mapCustomerType(message.getSubType()));
                            pEvent.setRingFenceIndicator(message.getRingFenceInd());
                            pEvent.setPortRequestFormId(message.getPortReqFormID());
                            
                            // Request contains numbers to  be ring-fenced.
                            if(message.getRingFenceNumbers().size() > 0) {
                                pEvent.getRingFenceNumberList().addAll(mapNPCDBNumberListToPhoneNumberRangeList(message.getRingFenceNumbers()));
                            }
                            //Check if request came with some documents and load them.
                            if(parameters.getAttachedDoc().size()  > 0) {
                               pEvent.getPortRequestForms().addAll(getAttachedDocTypeListAsPhotographList(parameters.getAttachedDoc()));
                            }
                            
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response= generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;    
                            
                        } else {
                            log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                        }
                                           
                    response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "0", REQUEST_PROCESSING_STATUS_SUCCESS);
                    return response;
                }
                case "3090" : { // Emergency Repatriation Request Exception Case 2
                   PortRevRspErrorMsgType  message = npcData.getNPCMessage().getPortReversalRspError();

                    /* Port Request Exception Case 4: is the case where Message 1004 – Porting Approval Response from the Donor does not arrive by expiration of the Port Validation Time. */ 
                    String donorId = message.getDonor();
                    if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) { // Smile is Donor (NPC will log a timer violation for the Donor.)
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                    } else { //Smile is recipient.
                        log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    }

                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setSenderId(senderId);
                    pEvent.setDonorId(donorId);
                    pEvent.setRecipientId(message.getRecipient());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR);

                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "1090": { //Porting Approval Response Error (Not Sent within set timeout)

                    PortAppRspErrorMsgType  message = npcData.getNPCMessage().getPortApprovalRspError();

                    /* Port Request Exception Case 4: is the case where Message 1004 – Porting Approval Response from the Donor does not arrive by expiration of the Port Validation Time. */ 
                    String donorId = message.getDonor();
                    if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) { // Smile is Donor (NPC will log a timer violation for the Donor.)
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                    } else { //Smile is recipient.
                        log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    }

                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setSenderId(senderId);
                    pEvent.setDonorId(donorId);
                    pEvent.setRecipientId(message.getRecipient());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR);

                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "3091" : { // Emergency Repatriation Activation Exception Case 1
                    PortRevActErrorMsgType message = npcData.getNPCMessage().getPortReversalActError();
                    /* Port Request Exception Case 4: is the case where Message 1004 – Porting Approval Response from the Donor does not arrive by expiration of the Port Validation Time. */ 
                    String donorId = message.getDonor();
                    if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) { // Smile is Donor (NPC will log a timer violation for the Donor.)
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                    } else { //Smile is recipient.
                        log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    }

                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setSenderId(senderId);
                    pEvent.setDonorId(donorId);
                    pEvent.setRecipientId(message.getRecipient());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_ACTIVATION_ERROR);

                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "1091": {// Message 1091 – Port Activation Error
                    
                        PortActErrorMsgType message = npcData.getNPCMessage().getPortActError();
                        /* Port Request Exception Case 4: is the case where Message 1004 – Porting Approval Response from the Donor does not arrive by expiration of the Port Validation Time. */ 
                        String donorId = message.getDonor();
                        if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) { // Smile is Donor (NPC will log a timer violation for the Donor.)
                            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                        } else { //Smile is recipient.
                            log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                        }
                        
                        pEvent.setMessageId(Long.valueOf(messageId));
                        pEvent.setSenderId(senderId);
                        pEvent.setDonorId(donorId);
                        pEvent.setRecipientId(message.getRecipient());
                        pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_ACTIVATION_ERROR);
                        
                        pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                        PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                        response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                        return response;
                }
                case "3092" : { // Message 3092 – Reversal Deactivation Error = Emergency Repatriation Activation Exception Case 2
                    PortRevDeactErrorMsgType message = npcData.getNPCMessage().getPortReversalDeactError();
                    /* Port Request Exception Case 4: is the case where Message 1004 – Porting Approval Response from the Donor does not arrive by expiration of the Port Validation Time. */ 
                    String donorId = message.getDonor();
                    if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) { // Smile is Donor (NPC will log a timer violation for the Donor.)
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                    } else { //Smile is recipient.
                        log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    }

                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setSenderId(senderId);
                    pEvent.setDonorId(donorId);
                    pEvent.setRecipientId(message.getRecipient());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR);

                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }     
                case "1092": { // Message 1092 – Port Deactivate Error to the Recipient and Donor
                    PortDeactErrorMsgType message = npcData.getNPCMessage().getPortDeactError();
                    /* Port Request Exception Case 4: is the case where Message 1004 – Porting Approval Response from the Donor does not arrive by expiration of the Port Validation Time. */ 
                    String donorId = message.getDonor();
                    if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) { // Smile is Donor (NPC will log a timer violation for the Donor.)
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                    } else { //Smile is recipient.
                        log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    }

                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setSenderId(senderId);
                    pEvent.setDonorId(donorId);
                    pEvent.setRecipientId(message.getRecipient());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR);

                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "1093": { // 3.3.7.2 Ring Fence Exception Case 1 (RingFenceValidationError)
                             // Message ID: 1093 – Ring Fence Validation Error Message from NPC to Recipient
                    // Ring Fence Exception Case 1: is the case where one or more of the numbers which the Recipient specified for ring fencing has failed Initial Validation
                    // or Case 2 - One or more of ring fencing numbers is not registered in the Subscriber Information Database or that there is a mismatch with the information in the request.
                    // or Case 3 - is the case where one or more of the numbers which the Recipient specified for ring fencing is currently ring fenced.
                    
                    /* Port Request Exception Case 4: is the case where Message 1004 – Porting Approval Response from the Donor does not arrive by expiration of the Port Validation Time. */ 
                    RingFenceValErrorMsgType message = npcData.getNPCMessage().getRingFenceValidationError();
                     
                        String donorId = message.getDonor();
                        if(donorId.equals(props.getProperty("SmileMNPParticipatingId"))) { // Smile is Donor (NPC will log a timer violation for the Donor.)
                            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                        } else { //Smile is recipient.
                            log.error("Port out request [{}] received but not meant for Smile, received donor Id = [{}]", transactionId, donorId);
                            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                        }
                                                
                        pEvent.setMessageId(Long.valueOf(messageId));
                        pEvent.setSenderId(senderId);
                        pEvent.setDonorId(donorId);
                        pEvent.setRecipientId(message.getRecipient());
                        pEvent.setMessageType(StMNPMessageTypes.NPCDB_RING_FENCE_VALIDATION_ERROR);
                        pEvent.setPortingRejectionList(mapNGMNPNumbersWithFlagReasonToSCAPortingRejectionList(message.getNumbersWithFlagReason()));
                        String requestRejectReasons = "";
                        if(message != null) {
                            for(MultiReqRejectReasonType reason: message.getRequestRejectReasons()) {
                                requestRejectReasons = requestRejectReasons + reason.getRequestRejectReason() + "\n";
                                pEvent.setErrorCode(reason.getRequestRejectReason());
                            }
                        }   
                        pEvent.setErrorDescription(requestRejectReasons);
                        pEvent.setPortRequestFormId(message.getPortReqFormID());
                        
                        PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                        response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                        return response;
                                
                }
                case "3005" : { // Port Reversal Response
                    PortRevRspRecipientMsgType message = npcData.getNPCMessage().getPortReversalRspRecipient();
                    /* 1005 – Porting Approval Response NPC sends this to Smile to indicate DONOR accept/reject*/
                    
                    String requestAcceptedByDonorYN = message.getRequestAcceptFlag();
                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    switch (requestAcceptedByDonorYN) {
                        case "Y": {
                            //Good port has been accepted by Donor - Activate number on our network!
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            pEvent.setRoutingInfoList(mapNGMNPNumbersWithFlagToSCARoutingInfoList(message.getNumbersWithFlag()));
                            // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_ACCEPT_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                        }
                        case "N": {
                            // Port Order Validation Result Rejected Message from NPC to Recipient
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);                               
                            pEvent.setPortingRejectionList(mapNGMNPNumbersWithFlagToSCAPortingRejectionList(message.getNumbersWithFlag()));
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);
                            String requestRejectReasons = "";
                            if(npcData.getNPCMessage().getPortOrderValidationResult() != null) {
                                for(MultiReqRejectReasonType reason: npcData.getNPCMessage().getPortOrderValidationResult().getRequestRejectReasons()) {
                                    requestRejectReasons = requestRejectReasons + reason.getRequestRejectReason() + "\n";
                                }
                            }   
                            pEvent.setErrorCode(requestRejectReasons);
                            // pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                        }
                        default:
                            break;
                        }        
                        response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "0", REQUEST_PROCESSING_STATUS_SUCCESS);
                        return response;
                }
                case "1005": {// Porting Approval Response
                    PortAppRspRecipientMsgType message = npcData.getNPCMessage().getPortApprovalRspRecipient();
                    /*1005 – Porting Approval Response NPC sends this to Smile to indicate DONOR accept/reject*/
                    
                    String requestAcceptedByDonorYN = message.getRequestAcceptFlag();
                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    switch (requestAcceptedByDonorYN) {
                        case "Y": {
                            //Good port has been accepted by Donor - Activate number on our network!
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);
                            pEvent.setRoutingInfoList(mapNGMNPPortedNumbersToSCARoutingInfoList(message.getNumbersWithFlagReason()));
                            // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_ACCEPT_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                        }
                        case "N": {
                            // Port Order Validation Result Rejected Message from NPC to Recipient
                            pEvent.setMessageId(Long.valueOf(messageId));
                            pEvent.setSenderId(senderId);                               
                            pEvent.setPortingRejectionList(mapNGMNPNumbersWithFlagReasonToSCAPortingRejectionList(message.getNumbersWithFlagReason()));
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);
                            String requestRejectReasons = "";
                            if(npcData.getNPCMessage().getPortOrderValidationResult() != null) {
                                for(MultiReqRejectReasonType reason: npcData.getNPCMessage().getPortOrderValidationResult().getRequestRejectReasons()) {
                                    requestRejectReasons = requestRejectReasons + reason.getRequestRejectReason() + "\n";
                                }
                            }   
                            pEvent.setErrorCode(requestRejectReasons);
                            // pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                        }
                        default:
                            break;
                        }              
                        response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "0", REQUEST_PROCESSING_STATUS_SUCCESS);
                        return response;
                }
                case "3010" : {// Reversal Deactivation Response
                    PortRevDeactRspRecipientMsgType message = npcData.getNPCMessage().getPortReversalDeactRspRecipient();
                    
                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    //Good port has been accepted by Donor - Activate number on our network!
                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setSenderId(senderId);
                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_DEACTIVATED);
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "1010": { // Port Deactivation Response
                    PortDeactRspRecipientMsgType message = npcData.getNPCMessage().getPortDeactRspRecipient();
                   
                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    //Good port has been accepted by Donor - Activate number on our network!
                    pEvent.setMessageId(Long.valueOf(messageId));
                    pEvent.setSenderId(senderId);
                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_DEACTIVATED);
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "3007" : // Port Reversal Deactivation Request
                            { //TODO: Port Deactivation Request
                        PortRevDeactReqDonorMsgType message = npcData.getNPCMessage().getPortReversalDeactReqDonor();
                        
                        // This message can is receive when a user is porting out from Smile
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);

                        //Good port has been accepted by Donor - Activate number on our network!
                        pEvent.setMessageId(Long.valueOf(messageId));
                        pEvent.setSenderId(senderId);

                        pEvent.setDonorId(message.getDonor());
                        pEvent.setRecipientId(message.getRecipient());

                        pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                        pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_DEACTIVATION_REQUEST);
                        PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                        response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                        return response;
                }
                case "1007": { //TODO: Port Deactivation Request
                        PortDeactReqDonorMsgType message = npcData.getNPCMessage().getPortDeactReqDonor();
                        // This message can is receive when a user is porting out from Smile
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);

                        //Good port has been accepted by Donor - Activate number on our network!
                        pEvent.setMessageId(Long.valueOf(messageId));
                        pEvent.setSenderId(senderId);

                        pEvent.setDonorId(message.getDonor());
                        pEvent.setRecipientId(message.getRecipient());

                        pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                        pEvent.setMessageType(StMNPMessageTypes.NPCDB_PORT_DEACTIVATION_REQUEST);
                        PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                        response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                        return response;
                }
                case "3008" : { // Port Reversal Activated Broadcast Request
                           PortRevActBroadcastMsgType message =  npcData.getNPCMessage().getPortReversalActBroadcast();    
                            pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN); 
                            pEvent.setMessageType(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP);
                            pEvent.setDonorId(message.getDonor());
                            pEvent.setRecipientId(message.getRecipient());

                            pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                            PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                            response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                            return response;
                }
                case "1008":  { //Port Activated Broadcast - will be received when a portation has taken place betweeen other operators and not involving Smile.
                    PortActBroadcastMsgType message =  npcData.getNPCMessage().getPortActBroadcast();    
                    
                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN); 
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP);
                    pEvent.setDonorId(message.getDonor());
                    if(message.getDonor().equalsIgnoreCase(props.getProperty("SmileMNPParticipatingId"))) { // A Port Activate Broadcast was received for a port request 
                        // on a number that ported away from Smile
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_OUT);
                    } else {
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                    }
                    
                    pEvent.setRecipientId(message.getRecipient());

                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "2002":  {// Number Return Validation Result.
                        NumberRetValResultMsgType message = npcData.getNPCMessage().getNumberReturnValidationResult();
                        //Check if it is a good validation
                        String requestAcceptFlag = message.getRequestAcceptFlag();
                        pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN);
                        switch (requestAcceptFlag) {
                            case "Y": {
                                //Good, number return has been accepted  - flag the same on our side.
                                pEvent.setMessageId(Long.valueOf(messageId));
                                pEvent.setSenderId(senderId);
                                pEvent.setRoutingInfoList(mapNGMNPPortedNumbersToSCARoutingInfoList(message.getNumbersWithFlagReason()));
                                // pEvent.setAutomaticAccept(npAcc.getAutomaticAccept());
                                pEvent.setMessageType(StMNPMessageTypes.NPCDB_NUMBER_RETURN_VALIDATION_OK);
                                PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                                response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                                return response;
                        }
                            case "N": {
                                // Port Order Validation Result Rejected Message from NPC to Recipient
                                pEvent.setMessageId(Long.valueOf(messageId));
                                pEvent.setSenderId(senderId);                               
                                pEvent.setPortingRejectionList(mapNGMNPNumbersWithFlagReasonToSCAPortingRejectionList(message.getNumbersWithFlagReason()));
                                pEvent.setMessageType(StMNPMessageTypes.NPCDB_NUMBER_RETURN_VALIDATION_FAILED);
                                String requestRejectReasons = "";
                                if(npcData.getNPCMessage().getNumberReturnValidationResult() != null) {
                                    for(MultiReqRejectReasonType reason: message.getRequestRejectReasons()) {
                                        requestRejectReasons = requestRejectReasons + reason.getRequestRejectReason() + "\n";
                                    }
                                }   
                                pEvent.setErrorCode(requestRejectReasons);
                                pEvent.setErrorDescription(requestRejectReasons);
                                // pEvent.setMessageType(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP);
                                PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                                response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                                return response;
                        }
                            default:
                                break;
                            }
                        
                            response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "0", REQUEST_PROCESSING_STATUS_SUCCESS);
                            return response;
                }
                case "2003": { // Number Return Request - Our number are coming back
                    NumberRetReqBlockHolderMsgType message = npcData.getNPCMessage().getNumberReturnReqBlockHolder();
                     pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN); 
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_NUMBER_RETURN_REQUEST);
                    pEvent.setDonorId("");
                    pEvent.setRecipientId("");
                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                }
                case "2005": { // Number Return Broadcast
                    NumberRetBroadcastMsgType message = npcData.getNPCMessage().getNumberReturnBroadcast();
                    pEvent.setPortingDirection(MNP_PORTING_DIRECTION_IN); 
                    pEvent.setRangeHolderId(npcData.getNPCMessage().getNumberReturnBroadcast().getBlockHolder());
                    pEvent.setMessageType(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST);
                    pEvent.setDonorId(npcData.getNPCMessage().getNumberReturnBroadcast().getBlockHolder());
                    pEvent.setRecipientId(npcData.getNPCMessage().getNumberReturnBroadcast().getLastRecipient());
                    pEvent.setRoutingInfoList(mapNumberListTypeToSCARoutingInfoList(message.getNumbers()));
                    PortInEvent outcome = SCAWrapper.getAdminInstance().handlePortInEvent(pEvent);
                    response = generateAcknowledgement(npcData, outcome.getProcessingStatus(), outcome.getErrorCode(), outcome.getErrorDescription());
                    return response;
                      
                }
                default:
                        String errorMessage = String.format("NGMNP - Do not know how to handle the reveived message id %s with transaction id %s and senderId %s.", 
                                messageId, transactionId, senderId);
                        log.error(errorMessage);
                        response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_SUCCESS, "", errorMessage); // So that ICN do not keep trying this message
                        return response;
            }
            } catch (Exception ex) {
                log.error("Error while processing message type {}.", messageId, ex);
                response = generateAcknowledgement(npcData, REQUEST_PROCESSING_STATUS_ERROR, "OPR1006", ex.getMessage());
                return response;
            }
            
        } catch(Exception ex) {
            log.error("Error while trying to handle NP request" + ex.getMessage(), ex);
        } finally {
            //Ensure the event is loged
            createEvent(transactionId , messageId, parameters.getXmlMsg(), response.getResponse());
        }
        
        return null;
    }
    
    private static RoutingInfoList mapNumberListTypeToSCARoutingInfoList(List<NumberListType>  numberList) {
        RoutingInfoList routingInfoList = new RoutingInfoList();
        try {
            
            RoutingInfo routingInfo;
            PhoneNumberRange phoneNumberRange;
            
            for (NumberListType number : numberList) {
                routingInfo = new RoutingInfo();
                phoneNumberRange = new PhoneNumberRange();
                phoneNumberRange.setPhoneNumberStart(number.getStartNumber());
                phoneNumberRange.setPhoneNumberEnd(number.getEndNumber());
                routingInfo.setPhoneNumberRange(phoneNumberRange);
                routingInfo.setRoutingNumber("-");
                routingInfoList.getRoutingInfo().add(routingInfo);
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return routingInfoList;
    }
    
    private static RoutingInfoList mapNGMNPPortedNumbersToSCARoutingInfoList(List<NumberListWithFlagReasonType>  numberListWithFlagReasons) {
        RoutingInfoList routingInfoList = new RoutingInfoList();
        try {
            
            RoutingInfo routingInfo;
            PhoneNumberRange phoneNumberRange;
                   
            String rangeAcceptedYN = null;
            // String startNumber= null;
            // String endNumber= null;
                
            for (NumberListWithFlagReasonType number : numberListWithFlagReasons) {
                rangeAcceptedYN = number.getNumberAcceptFlag();
                routingInfo = new RoutingInfo();
                phoneNumberRange = new PhoneNumberRange();
                phoneNumberRange.setPhoneNumberStart(number.getStartNumber());
                phoneNumberRange.setPhoneNumberEnd(number.getEndNumber());
                routingInfo.setPhoneNumberRange(phoneNumberRange);
                routingInfo.setRoutingNumber(props.getProperty("SmileNetworkRoutingNumber"));
                routingInfoList.getRoutingInfo().add(routingInfo);
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return routingInfoList;
    }
    
   private static RoutingInfoList mapNGMNPNumbersWithFlagToSCARoutingInfoList(List<NumberListWithFlagType>  numberListWithFlag) {
        RoutingInfoList routingInfoList = new RoutingInfoList();
        try {
            
            RoutingInfo routingInfo;
            PhoneNumberRange phoneNumberRange;
                   
            String rangeAcceptedYN = null;
            
            for (NumberListWithFlagType number : numberListWithFlag) {
                rangeAcceptedYN = number.getNumberAcceptFlag();
                routingInfo = new RoutingInfo();
                phoneNumberRange = new PhoneNumberRange();
                phoneNumberRange.setPhoneNumberStart(number.getStartNumber());
                phoneNumberRange.setPhoneNumberEnd(number.getEndNumber());
                routingInfo.setPhoneNumberRange(phoneNumberRange);
                routingInfo.setRoutingNumber(props.getProperty("SmileNetworkRoutingNumber"));
                routingInfoList.getRoutingInfo().add(routingInfo);
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return routingInfoList;
    }
    
    private com.telcordia.inpac.ws.ProcessNPCMsgResponse generateAcknowledgement(NPCData npcDataReq, String status, String errorCode, String errorDesc) throws Exception {
        
        ProcessNPCMsgResponse rsp = new ProcessNPCMsgResponse();
        NPCData npcDataRsp = new NPCData();
        
        npcDataRsp.setMessageHeader(npcDataReq.getMessageHeader());
        npcDataRsp.getMessageHeader().setSender(props.getProperty("SmileMNPParticipatingId"));
        PartWebServiceRespType partWebServiceResp = new PartWebServiceRespType();
                
        if(status != null && status.equalsIgnoreCase(REQUEST_PROCESSING_STATUS_ERROR))  {
            PartValidationType t = new PartValidationType();
            t.setPartRespReasonCode(getNPCErrorCode(errorCode)); //Normalise error codes to what NPC understands.
            t.setPartRespReasonDesc(errorDesc);
            partWebServiceResp.setPartValidation(t);
            partWebServiceResp.setResponse(REQUEST_PROCESSING_STATUS_ERROR);
        } else  { //All is good
            partWebServiceResp.setResponse(REQUEST_PROCESSING_STATUS_SUCCESS);
        }
        npcDataRsp.setPartWebServiceResponse(partWebServiceResp);
        
        Marshaller m = Utils.getJAXBMarshallerForSoap(NPCData.class);
        StringWriter sw = new StringWriter();
        m.marshal(npcDataRsp, sw);
        rsp.setResponse(sw.toString());
        
        return rsp;
    }
    
    
    private static String mapNGMNPNumbersWithFlagToSCAPortingRejectionList(List<NumberListWithFlagType> npcNumbersWithFlagReasonList) {
        StringBuilder numbersWithFlagReasonList = new StringBuilder();
        try {
            
            RoutingInfo routingInfo;
            PhoneNumberRange phoneNumberRange;
                    
            String rangeAcceptedYN = null;
            // String startNumber= null;
            // String endNumber= null;
            String newLine = "";
            
            for (NumberListWithFlagType number : npcNumbersWithFlagReasonList) {
                rangeAcceptedYN = number.getNumberAcceptFlag();
                numbersWithFlagReasonList.append(newLine).append(number.getStartNumber()).append(",").append(number.getEndNumber()).append(",").append(rangeAcceptedYN);
                newLine = "\r\n";
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return numbersWithFlagReasonList.toString();
    }
    
    private static String mapNGMNPNumbersWithFlagReasonToSCAPortingRejectionList(List<NumberListWithFlagReasonType> npcNumbersWithFlagReasonList) {
        StringBuilder numbersWithFlagReasonList = new StringBuilder();
        try {
            
            RoutingInfo routingInfo;
            PhoneNumberRange phoneNumberRange;
                    
            String rangeAcceptedYN = null;
            // String startNumber= null;
            // String endNumber= null;
            String newLine = "";
            
            for (NumberListWithFlagReasonType number : npcNumbersWithFlagReasonList) {
                rangeAcceptedYN = number.getNumberAcceptFlag();
                numbersWithFlagReasonList.append(newLine).append(number.getStartNumber()).append(",").append(number.getEndNumber()).append(",").append(rangeAcceptedYN);
                String numberRejectReasons = "";
                
                if(rangeAcceptedYN.equals("N")) { // This range was rejected ...
                    for (MultiNbrRejectReasonType reason: number.getNumberRejectReasons()) {
                        numberRejectReasons += reason.getNumberRejectReason() + " ";
                    }
                }
                if(numberRejectReasons.trim().length() > 0) {
                    numbersWithFlagReasonList.append(",").append(numberRejectReasons.trim());
                }
                newLine = "\r\n";
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return numbersWithFlagReasonList.toString();
    }
    
    public static List <PhoneNumberRange>  mapNPCDBNumberListToPhoneNumberRangeList(List <NumberListType> numberList) {
        
        List <PhoneNumberRange> phoneNumberRangeList = new ArrayList();
        
        PhoneNumberRange currentNumberRangeEntry = null;
                
        for(NumberListType number : numberList) {
            currentNumberRangeEntry = new PhoneNumberRange();
            currentNumberRangeEntry.setPhoneNumberStart(number.getStartNumber());
            currentNumberRangeEntry.setPhoneNumberEnd(number.getEndNumber());
            
            phoneNumberRangeList.add(currentNumberRangeEntry);
        }
        return phoneNumberRangeList;
    }
    
    public static List <Photograph> getAttachedDocTypeListAsPhotographList(List <AttachedDocType> portingForms) throws Exception {
            List <Photograph> attachedDocs = new ArrayList();
            for (AttachedDocType doc : portingForms) {
                Photograph p = new Photograph(); 
                p.setPhotoGuid(Utils.getUUID());
                p.setData(Utils.encodeBase64(doc.getDocumentFile()));
                if(doc.getDocumentType() == null || doc.getDocumentName().equalsIgnoreCase("PRF")) {
                    p.setPhotoType("portrequestform");
                } else {
                    p.setPhotoType(doc.getDocumentName());
                }
                attachedDocs.add(p);
            }            
            return attachedDocs;
    }
   
    public static String getNPCErrorCode(String errorCode) {
        //Making sure we are returning an error code that is allowed by NPC
        if(errorCode != null && (errorCode.equals(MNP_SERVICE_NOT_ACTIVE_ERROR_CODE) ||
               errorCode.equals(MNP_INVALID_CUSTOMER_ERROR_CODE)   ||
               errorCode.equals(MNP_INVALID_NUMBER_ERROR_CODE))) {     
                return errorCode;   
        }
        return MNP_ERROR_CODE_OTHER;
   }
   
     public static void createEvent(String eventKey, String messageType, String  requestData, String responseData) {
        log.debug("Writing event for event key {}", eventKey);
       
        String eventData = "REQUEST:\n" + requestData + "\n\nRESPONSE:\n" + responseData;
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

   
    
    /*
    private static com.smilecoms.commons.sca.RoutingInfoList mapNGMNPPortedNumbersToSCARoutingInfoList(Document doc) {
        com.smilecoms.commons.sca.RoutingInfoList outRoutingInfoList = new com.smilecoms.commons.sca.RoutingInfoList();
        
        try {
            com.smilecoms.commons.sca.RoutingInfo scaRoutingInfo;
            com.smilecoms.commons.sca.PhoneNumberRange scaPhoneNumberRange;
        
            XPathExpression xpathExp =
                xPath.compile("/NPCData/NPCMessage/PortOrderValidationResult/NumbersWithFlagReason");
            NodeList nodes = (NodeList) xpathExp.evaluate(doc, XPathConstants.NODESET);
            
            for (int i = 0; i < nodes.getLength(); i++) {
                xpathExp = xPath.compile("/StartNumber/text()");
                String startNumber= (String) xpathExp.evaluate(nodes.item(i), XPathConstants.STRING);
                xpathExp = xPath.compile("/EndNumber/text()");
                String endNumber= (String) xpathExp.evaluate(nodes.item(i), XPathConstants.STRING);
                
                scaRoutingInfo = new com.smilecoms.commons.sca.RoutingInfo();
                scaPhoneNumberRange = new com.smilecoms.commons.sca.PhoneNumberRange();
                scaPhoneNumberRange.setPhoneNumberStart(startNumber);
                scaPhoneNumberRange.setPhoneNumberEnd(endNumber);

                scaRoutingInfo.setPhoneNumberRange(scaPhoneNumberRange);
                scaRoutingInfo.setRoutingNumber(props.getProperty("SmileNetworkRoutingNumber"));
                outRoutingInfoList.getRoutingInfo().add(scaRoutingInfo);
                // list.add(nodes.item(i).getNodeValue());
            }
        } catch (Exception e) {
            log.warn("Error: ", e);
            
        }
        return outRoutingInfoList;
    } */
    
    private String mapCustomerType(String custType) throws Exception {
    
        if(custType == null || custType.isEmpty()) {
            throw new Exception("Customer type not supplied, do not know how to proceed!");
        } else
            if(custType.equalsIgnoreCase("0")) {
                return "individual";
            } else
                if(custType.equalsIgnoreCase("1")) {
                    return "corporate";
                } else {
                    throw new Exception("Invalid customer type supplied [" +  custType + "].");
                }
    } 
    
    private void init() throws Exception {

        try {
            if(mDocFactory == null) {
                mDocFactory = DocumentBuilderFactory.newInstance();
            }
            
            if(mDocBuilder == null) {
                mDocBuilder = mDocFactory.newDocumentBuilder();
            }
            
            if(factory == null) {
                factory = XPathFactory.newInstance();
            }
            
            if(xPath == null) {
                xPath = factory.newXPath();
            }
        } catch (Exception ex) {
            throw new Exception(ex);
        }
        
        if (props == null) {
            
            props = new Properties();

            try {
                log.warn("Loading MNP Web service Properties as contained in env.mnp.config \n");
                log.warn(BaseUtils.getProperty("env.mnp.config"));
                props.load(BaseUtils.getPropertyAsStream("env.mnp.config"));
            } catch (Exception ex) {
                log.error("Failed to load properties from env.mnp.config: ", ex);
            }
        }
    }
}
