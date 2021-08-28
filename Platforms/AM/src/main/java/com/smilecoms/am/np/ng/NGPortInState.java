/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.ng;

import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.InvalidStateTransitionException;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Address;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.SCAMarshaller;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.sca.direct.am.AvailableNumberRange;
import com.smilecoms.commons.sca.direct.bm.PlatformContext;
import com.smilecoms.commons.sca.direct.bm.PortingData;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PhoneNumberRange;
import com.smilecoms.xml.schema.am.PortInEvent;
import com.smilecoms.xml.schema.am.RoutingInfo;
import com.telcordia.inpac.ws.NPCWebServicePortType;
import com.telcordia.inpac.ws.ProcessNPCMsgRequest;
import com.telcordia.inpac.ws.ProcessNPCMsgResponse;
import com.telcordia.inpac.ws.binding.MessageHeaderType;
import com.telcordia.inpac.ws.binding.NPCData;
import com.telcordia.inpac.ws.binding.NPCMessageType;
import com.telcordia.inpac.ws.binding.NumberRetReqMsgType;
import com.telcordia.inpac.ws.binding.NumberRetRspMsgType;
import com.telcordia.inpac.ws.binding.PortAppReqMsgType;
import com.telcordia.inpac.ws.binding.PortDeactReqMsgType;
import com.telcordia.inpac.ws.binding.PortRevDeactReqMsgType;
import com.telcordia.inpac.ws.binding.SubscriberDataType;
import com.telcordia.inpac.ws.binding.ValidationType;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi This class will represent all the states for a port in
 * request, a port in request is when a customer request for smile to initiate a
 * request to migrate them from their current network into the Smile network.
 * this class will also code the transitions between the states.
 */
public enum NGPortInState implements IPortState {

    NP_NEW_REQUEST {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
                    
                    setContext(context);
                    
           // Send porting request to to NPCDB and obtain an Acknowledgement back.
                    // NPRequestToNpcdbType request = new NPRequestToNpcdbType();
                    // 1. Call NPCDB here
                    //NpcdbMessageAckType npcdbResponse = new   NpcdbMessageAckType();
                    // Hardcoded response ....
                    //npcdbResponse.setDonorID("01");
                    //npcdbResponse.setMessageID(300000000);
                    //npcdbResponse.setNPOrderID(123456789);
                    //npcdbResponse.setRangeHolderID("MTN");
                    //npcdbResponse.setRecipientID("02");
           // Get porting order id from NPCDB - currently set to a random number for testing purposes
           /*Random rand = new Random();
           
                     getContext().setSenderId("00");
                     getContext().setPortingOrderId(portingOrderId);
                     getContext().setDonorId(context.getDonorId());
                     getContext().setRangeHolderId(context.getRangeHolderId());
                     getContext().setRecipientId(context.getRecipientId());
                     getContext().setMessageId(context.getMessageId()); */
                    //getContext().setNpState(this.getStateId()); // This is a new request and awaiting confirmation from NPCDB.
                    getContext().setRoutingInfoList(context.getRoutingInfoList());

           // Create a new NP request in the database (mnp_port_in_request)
                    // MnpPortInRequest dbPortInRequest = DAO.createNewPortInRequest(em, request);
                    // NpcdbMessageAckType ack = tzNpcdbService.getNpcdbSoap().sendNPRequest(request); 
                    // tzPortInReq.setCurrentState(NP_REQUEST_SUBMITTED_TO_NPCDB); 
                    
                    ProcessNPCMsgRequest request = new ProcessNPCMsgRequest();

                    request.setPassword(Utils.encodeBase64(MnpHelper.props.getProperty("NpcdbWSPassword").getBytes())); // ICN Password must be Base64 encoded.
                    request.setUserID(MnpHelper.props.getProperty("NpcdbWSUsername"));

                    NPCData requestData = new NPCData();

                    MessageHeaderType header = new MessageHeaderType();

                    header.setSender(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // SML for Smile in Nigeria
                    header.setHeaderID(String.valueOf(context.getCustomerProfileId()));
                    header.setMessageID(NGPortingHelper.MSG_ID_PORTING_APPROVAL_REQUEST);
                    header.setPortType(MnpHelper.props.getProperty("NpcdbMobilePortingType"));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    header.setMsgCreateTimestamp(sdf.format(new Date()));
                    requestData.setMessageHeader(header);

                    PortAppReqMsgType portMessage = new PortAppReqMsgType();
                    // message.set
                    portMessage.setDonor(context.getDonorId());
                    portMessage.setRecipient(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // Smile's MNP participating ID
                    portMessage.setSMSAssocNumber(Utils.getFriendlyPhoneNumberKeepingCountryCode(context.getValidationMSISDN()));
                    SubscriberDataType subData = new SubscriberDataType();
                    
                    if(getContext().getCustomerType().equalsIgnoreCase("individual")) {
                        log.debug("Porting request is for individual.");
                        portMessage.setSubType(NGPortingHelper.SUB_TYPE_INDIVIDUAL); // Mandatory 0 (Individual) or 1 (Corporate)

                        subData.setSubFirstName(context.getCustomerFirstName());
                        subData.setSubLastName(context.getCustomerLastName());
                        subData.setSubGender(context.getGender());
                        
                        Customer cust = NGPortingHelper.getCustomer(context.getCustomerProfileId());
                        Address addToUse = null;
                        for (Address add : cust.getAddresses()) {
                            // Use physical address
                            if (add.getType().equalsIgnoreCase("Physical Address")) {
                                addToUse = add;
                                break;
                            }
                        }

                        if (addToUse == null) {
                            throw new Exception("No Physical Address configured for customer with id - " + context.getCustomerProfileId());
                        } else {
                            subData.setSubStateOfOrigin(addToUse.getState()); // Must get from address ...
                        }
                    }
                    
                    if(getContext().getCustomerType().equalsIgnoreCase("corporate")) {
                        log.debug("Porting request is for corporate.");
                        portMessage.setSubType(NGPortingHelper.SUB_TYPE_CORPORATE); // Mandatory 0 (Individual) or 1 (Corporate)

                        subData.setCompanyName(context.getOrganisationName());
                        subData.setCompanyRegNum(context.getOrganisationTaxNumber());
                                                
                    } 
                    
                    portMessage.setSubscriberData(subData);
                    
                    log.debug("Size of RingFenceNumberList is {} and RingFenceIndicator set to {}", context.getRingFenceNumberList().size(), context.getRingFenceIndicator());
                    // Set any attached documents.
                    log.debug("Port request contains [{}] documents, they will be attached on the request.", context.getPortRequestForms().size());
                    request.getAttachedDoc().addAll(NGPortingHelper.getPortingFormsAsAttachedDocTypeList(context.getPortRequestForms()));
                    
                    log.debug("Sending request to NPC with [{}] documents attached.", request.getAttachedDoc().size());
                    
                    List<PhoneNumberRange> reducedRanges = MnpHelper.reducePhoneNumberRanges(context.getRoutingInfoList());
                    
                    context.getReducedRoutingInfoList().addAll(reducedRanges);
                    portMessage.getNumbers().addAll(NGPortingHelper.mapXMLPhoneNumberRangeListToNPCDB(reducedRanges));
                    
                    portMessage.setSubConfirmed("Y");
                    
                    portMessage.setPortReqFormID(context.getPortRequestFormId());
                    portMessage.setRingFenceInd(context.getRingFenceIndicator());
                    
                    if(context.getRingFenceIndicator().equalsIgnoreCase("Y")) {
                        portMessage.getRingFenceNumbers().addAll(NGPortingHelper.mapXMLPhoneNumberRangeListToNPCDB(context.getRingFenceNumberList()));
                    }
                    
           // portMessage.set
                    NPCMessageType npcMessage = new NPCMessageType();
                    npcMessage.setPortApprovalReq(portMessage);

                    requestData.setNPCMessage(npcMessage);

                    Marshaller m = Utils.getJAXBMarshallerForSoap(NPCData.class);
                    StringWriter sw = new StringWriter();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                    m.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
                    m.marshal(requestData, sw);
                    request.setXmlMsg(sw.toString());

                    SCAMarshaller.logObject(request, "XML being sent to ICN:");
                                       
                    // This is used to simulate new porting request only - the request will not be submitted to NPCDB.
                    if(BaseUtils.getBooleanProperty("env.mnp.porting.request.test.mode", false)) {
                        getContext().setPortingOrderId(sdf.format(new Date()));
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                        getContext().setPortingDirection(MnpHelper.MNP_PORTING_DIRECTION_IN);
                        getContext().setMessageId(100000L);
                        
                        getContext().setErrorCode("000001");
                        getContext().getValidationErrors().add("NB: Porting order [" + getContext().getPortingOrderId() + 
                                "] inserted in the DB but is not submitted to NPCDB, MNP porting request currently running in test mode (check property env.mnp.porting.request.test.mode");
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                        
                        return initNextState(NP_REQUEST_ACCEPTED_BY_NPCDB, getContext()); 
                    }
                    
                    // Show message being sent to NPC
                    ProcessNPCMsgResponse response = new ProcessNPCMsgResponse();
                    
                    try {
                        log.debug("", Utils.marshallSoapObjectToString(request));
                        log.debug("Calling getNPCDBConnection");
                        NPCWebServicePortType connection = getNPCDBConnection();
                        
                        log.debug("Got getNPCDBConnection, now calling the processNPCMsg method");
                        
                        response = connection.processNPCMsg(request);
                        
                        log.debug("Done calling  processNPCMsg");
                        
                        SCAMarshaller.logObject(response, "XML response from ICN:");

                        // log.debug("XML Response from ICN:" + response.getResponse());
                        // Send (1001) porting approval request and wait for 1002 message back.
                        
                        NPCData npcData = NGPortingHelper.getNPCDataFromResponse(response.getResponse()); 
                                                
                        log.error("Response from ICN:" + response.getResponse());
                        
                        if(npcData.getNPCWebServiceResponse().getResponse().equals("Success")) { // 1002 (success) All good ...
                            getContext().setPortingOrderId(npcData.getMessageHeader().getTransactionID());
                            
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                            getContext().setPortingDirection(MnpHelper.MNP_PORTING_DIRECTION_IN);
                            getContext().setMessageId(Long.valueOf(npcData.getMessageHeader().getMessageID()));
                            return initNextState(NP_REQUEST_ACCEPTED_BY_NPCDB, getContext()); 
                        } else { // Error .... (1002 (failure))
                           getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                           // Add all errors to the context and return to SCA ...
                           for(ValidationType validation : npcData.getNPCWebServiceResponse().getValidation()) {
                               getContext().getValidationErrors().add(validation.getReasonCode() + " - " + validation.getReasonDesc());
                           }
                           return initNextState(NP_REQUEST_REJECTED_BY_NPCDB, getContext());
                        }
                    }  catch (Exception ex)  {
                        log.error("Error while trying to submit new porting request to NPCDB",ex);
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                        String errorMessage = MnpHelper.getErrorMessage(ex);
                        getContext().getValidationErrors().add(errorMessage);
                        getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                        getContext().setErrorDescription(errorMessage);
                        getContext().setRoutingInfoList(context.getRoutingInfoList());
                        
                        return initNextState(NP_FAILED, getContext());
                    } finally {
                        MnpHelper.createEvent(getContext().getPortingOrderId(), NGPortingHelper.MSG_ID_PORTING_APPROVAL_REQUEST,  sw.toString(), response.getResponse(), getContext().getErrorDescription());
                    }
         }
        @Override
        public String getStateId() {
            return "NP_NEW_REQUEST";
        }
    }, NP_REQUEST_REJECTED_BY_NPCDB {
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Do nothing ....
            return null;
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_REJECTED_BY_NPCDB";
        }
    } ,
    NP_REQUEST_ACCEPTED_BY_NPCDB { // 1002 Received
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context)  throws Exception {
            try {
                setContext(context);                                                                                            
                if((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_VALIDATED_TO_SP.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_CANCEL_TO_SP.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_VALIDATION_ERROR.value()))) {
                    
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_ACCEPTED_BY_NPCDB.getStateId() , context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_VALIDATION_ERROR.value())) {
                     //Handle Ring Fence Validation Errors.
                     // The porting request had ring-fencing numbers and at list one of them was rejected
                    // Just stay in this state since the handling of the porting request will continue despite the failure on Ring Fencing.
                    return  initNextState(this, getContext()); //Stay within this state and wait for the next messages.
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR.value())) {
                   getContext().setErrorDescription("Violation of Port Validation Time (1090)");
                   return  initNextState(this, getContext()); //Stay within this state.
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_VALIDATED_TO_SP.value())) {
                    // All good - np request was accepted by NPCDB and now forwarded to Donor SP.
                    // Change the state of our np request to in progress.
                    context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS); // All good (so far) - request will proceed to the next step ...
                    return initNextState(NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR, context);
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_CANCEL_TO_SP.value())) {
                   // NP REQUEST was REJECTED at NPCDB before sending to Donor - could be due timer T10 EXPIREring
                            // E.g Susbcriber did not send SMS0.
                            // Chance request status to CANCELLED and save the cancel message/reason.
                            // Process terminates...
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                            return initNextState(NP_REQUEST_CANCELLED, getContext());
                        }
                        // Do not know how to proceed with this transition...
                        throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_ACCEPTED_BY_NPCDB.getStateId(), context.getMessageType());
                    } catch (InvalidStateTransitionException ex) {
                        ex.processException();
                        throw ex;
                    }
                }

                @Override
                public String getStateId() {
                    return "NP_REQUEST_ACCEPTED_BY_NPCDB";
                }
            },
    NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR {
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            // While we are in this status - we can receive a Donor Accept or Donor Reject
            try {
                if((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_ACCEPT_TO_SP.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP.value()) || 
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_ACTIVATION_ERROR.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR.getStateId() , context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_ACTIVATION_ERROR.value())) {
                   // Donor did not respond with Message 1004 on time - so this is donor accept error. Stay in this state and 
                   // wait for a proper Donor Accept message (1004).
                   getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                   getContext().setErrorDescription("Donor Activation Error (1091)");
                   getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                   return initNextState(this, getContext());
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR.value())) {
                       // Donor did not respond with Message 1004 on time - so this is donor accept error. Stay in this state and 
                       // wait for a proper Donor Accept message (1004).
                       getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                       getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                       getContext().setErrorDescription("Violation of Port Validation Time (1090)");
                       return  initNextState(this, getContext()); //Stay within this state.
                }else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_ACCEPT_TO_SP.value())) {
                        // All good - np request was accepted by DONOR.
                                // So Smile can provision the number on the network 
                                // and then send the NpActivated message to NPCDB
                                // TODO:
                        // 1. Add the ported numbers on SmileDB's ported_number Table;
                        // AddressManager am = new AddressManager();
                        StringWriter sw = new StringWriter();
                        ProcessNPCMsgResponse response = new ProcessNPCMsgResponse();
                        String messageType = "";
                        try {
                            
                            MnpHelper.activatePortedNumberOnSmileNetwork(context);
                            
                            // If all is good, we tell the clearing house that NP is Activated and request the DONOR to deactivate.
                            // Send Message 1006 - port deactivation request.
                            NPCWebServicePortType connection = getNPCDBConnection();
                            
                            
                            ProcessNPCMsgRequest request = new ProcessNPCMsgRequest();

                            request.setPassword(Utils.encodeBase64(MnpHelper.props.getProperty("NpcdbWSPassword").getBytes())); // ICN Password must be Base64 encoded.
                            request.setUserID(MnpHelper.props.getProperty("NpcdbWSUsername"));

                            NPCData requestData = new NPCData();
                            // Do the header bits here.
                            MessageHeaderType header = new MessageHeaderType();
                            header.setSender(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // SML for Smile in Nigeria
                            header.setHeaderID(String.valueOf(context.getCustomerProfileId()));
                            header.setTransactionID(context.getPortingOrderId());
                            
                            
                            header.setPortType(MnpHelper.props.getProperty("NpcdbMobilePortingType"));
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                            header.setMsgCreateTimestamp(sdf.format(new Date()));
                            requestData.setMessageHeader(header);
                            
                            NPCMessageType npcMessage = new NPCMessageType();
                            
                            // Do the message bits here ...
                            if(MnpHelper.isEmergencyRestore(context)) {
                                messageType = NGPortingHelper.MSG_ID_PORT_REVERSAL_DEACTIVATION_REQUEST;
                                header.setMessageID(messageType); //Send 3006
                                
                                PortRevDeactReqMsgType msg = new PortRevDeactReqMsgType();
                                msg.setDonor(context.getDonorId());
                                msg.setRecipient(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // Smile's MNP participating ID
                    
                                // List<PhoneNumberRange> reducedRanges = MnpHelper.reducePhoneNumberRanges(context.getRoutingInfoList());
                    
                                msg.getNumbers().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDB(context.getRoutingInfoList()));
                                npcMessage.setPortReversalDeactReq(msg);
                            
                            } else {
                                messageType = NGPortingHelper.MSG_ID_PORT_DEACTIVATION_REQUEST;
                                header.setMessageID(messageType); //Send 1006
                            
                                List<PhoneNumberRange> reducedRanges = MnpHelper.reducePhoneNumberRanges(context.getRoutingInfoList());
                                
                                PortDeactReqMsgType portingMessage = new PortDeactReqMsgType();
                                portingMessage.setSubType(NGPortingHelper.mapCustomerType(context.getCustomerType())); // Mandatory 0 (Individual) or 1 (Corporate)
                                portingMessage.setDonor(context.getDonorId());
                                portingMessage.setRecipient(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // Smile's MNP participating ID
                                portingMessage.getNumbers().addAll(NGPortingHelper.mapXMLPhoneNumberRangeListToNPCDB(reducedRanges));
                                portingMessage.setPortReqFormID(context.getPortRequestFormId());

                                npcMessage.setPortDeactReq(portingMessage);
                            }
                            
                            requestData.setNPCMessage(npcMessage);

                            Marshaller m = Utils.getJAXBMarshallerForSoap(NPCData.class);
                            
                            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                            m.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
                            m.marshal(requestData, sw);
                            request.setXmlMsg(sw.toString());
                            // Show message being sent to NPC
                            SCAMarshaller.logObject(request, "XML being sent to ICN:");
                            
                            log.debug("", Utils.marshallSoapObjectToString(request));
                            log.debug("Done activation for port order {}, now sending message {} ", context.getPortingOrderId(),header.getMessageID());
                            
                            response = connection.processNPCMsg(request);
                            Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);

                            SCAMarshaller.logObject(response, "XML response from ICN:");
                            NPCData npcData = (NPCData) um.unmarshal(new StringReader(response.getResponse()));

                            log.error("Response from ICN:" + response.getResponse());
                            // All is good, transition into NP_ACTIVATED.
                            
                        if(npcData.getNPCWebServiceResponse().getResponse().equals("Success")) { // All good ...
                            getContext().setPortingOrderId(npcData.getMessageHeader().getTransactionID());
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                            getContext().setPortingDirection(MnpHelper.MNP_PORTING_DIRECTION_IN);
                            getContext().setMessageId(Long.valueOf(npcData.getMessageHeader().getMessageID()));
                            return initNextState(NP_ACTIVATED, context); // Awaiting DONOR_DEACT
                            
                        } else { // Error ....
                           getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                           // Add all errors to the context and return to SCA ...
                           String errorCode = "", errorDesc = "";
                           for(ValidationType validation : npcData.getNPCWebServiceResponse().getValidation()) {
                               getContext().getValidationErrors().add(validation.getReasonCode() + " - " + validation.getReasonDesc());
                               errorCode = validation.getReasonCode();
                               errorDesc +=  validation.getReasonCode() + " - " + validation.getReasonDesc() + "\r\n";
                           }
                           getContext().setErrorCode(errorCode);
                           getContext().setErrorDescription(errorDesc);
                           return initNextState(this, getContext()); // Stay in this state so error can be addressed and retried.
                        }
                            
                        }  catch (Exception ex)  {
                            log.error("Error while trying to submit message type {} to NPCDB.", NGPortingHelper.MSG_ID_PORT_DEACTIVATION_REQUEST, ex);
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                            getContext().getValidationErrors().add(ex.getMessage());
                            getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                            getContext().setErrorDescription(Utils.getStackTrace(ex));
                            getContext().setRoutingInfoList(context.getRoutingInfoList());
                            return initNextState(this, getContext()); // Stay in this state since ICN will retry any failed request.
                        } finally {
                            MnpHelper.createEvent(getContext().getPortingOrderId(), messageType,  sw.toString(), response.getResponse(), getContext().getErrorDescription());
                        }
                        
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP.value())) {
                   // NP REQUEST was REJECTED by the Donor SP therefore process terminates.
                   // Process terminates...
                   getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                   return initNextState(NP_DONOR_REJECTED, context);
                } 
                // Do not know how to proceed with this transition...
                throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR.getStateId() , context.getMessageType());    
            
            } catch (Exception ex) {
                log.error("Error while handling state {}:", this.getStateId(), ex);
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                getContext().getValidationErrors().add(ex.getMessage());
                getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                getContext().setErrorDescription(ex.getMessage());
                return initNextState(NP_FAILED, context);
            } 
            
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR";
        }
    },
    NP_REQUEST_CANCELLED {
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            log.error("Terminal state has been reached for porting request [{}]- cannot transition away from [{}] state.", context.getPortingOrderId(), getStateId());
            return initNextState(NP_REQUEST_CANCELLED, context); // Stay in this statr forever ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_CANCELLED";
        }
    },
    NP_DONOR_REJECTED {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) {
                    setContext(context);
                    return null;
                }

                @Override
                public String getStateId() {
                    return "NP_DONOR_REJECTED";
                }
    },
    NP_ACTIVATED {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
                                        
                    setContext(context);
                    // We expect a NPCDB_EXECUTE_BROADCAST_TO_SP message while in this state.
                    try {
                        if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value()) //Broadcast 1008 Received Before 1010 
                                                                 || context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_DEACTIVATED.value()) //1010
                                                                 || context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR.value()))) {
                            throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_ACTIVATED.getStateId(), context.getMessageType());
                        }
                        
                        if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR.value())) { // Donor did not respond with 1007 on time.
                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                                getContext().setErrorDescription("Port Deactivate Error (1092)");
                                return  initNextState(this, getContext()); //Stay within this state.
                        } else
                        if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_DEACTIVATED.value())) {
                            // All good update Smile Routing tables and then go to NP_DONE state.
                            getContext().setErrorDescription("Donor Deactivated");
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                            return initNextState(NP_DONE, getContext());
                        } else 
                            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                                getContext().setErrorDescription("Port Activated Broadcast (1008)");
                                return initNextState(this, getContext()); //Stay here and wait for 1010
                            }
                        
                        // If we get to this point, stay in this state, no panic.
                        return initNextState(this, getContext());
                        
                    } catch (InvalidStateTransitionException ex) {
                        ex.processException();
                        throw ex;
                    }
                        
        } 
        
        @Override
        public String getStateId() {
            return "NP_ACTIVATED";
        }
    },
    NP_DONE {
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            // Throw an error - should never try to transition further from this state.
            try {
                
                if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                    // All good stay in NP_DONE state.
                    getContext().setErrorDescription("Broadcast Received");
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    return initNextState(NP_DONE, context); 
                } else 
                if (context.getMessageType().equals(StMNPMessageTypes.SMILE_REQUEST_NUMBER_RETURN.value())) { // Handle port-out request here.
                    
                    // NB: A new port order id will be allocated and must be used for future reference to this return request.
                    
                    //First check if the quarantine period has elapsed.
                    int quarantinePeriodInDays = Integer.parseInt(MnpHelper.props.getProperty("QuarantinePeriod"));
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(Utils.getJavaDate(getContext().getPortingDate()));
                    cal.add(Calendar.DATE, quarantinePeriodInDays);
                    Date currentDate = new Date();
                    if (currentDate.before(cal.getTime())) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
                        String errorMessage = String.format("Number return not allowed during quarantine period, it can only return after %s", sdf.format(cal.getTime()));
                        log.error(errorMessage);
                        getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                        getContext().setErrorDescription(errorMessage);
                        getContext().getValidationErrors().add(NGPortingHelper.MNP_ERROR_CODE_OTHER + " - " +  errorMessage);
                        // Stay in this state until QuarantinePeriod elapsed;
                        return initNextState(NP_DONE, getContext()); 
                    }
                            
                    // Do Number  Return here ...
                    
                    ProcessNPCMsgRequest request = new ProcessNPCMsgRequest();

                    request.setPassword(Utils.encodeBase64(MnpHelper.props.getProperty("NpcdbWSPassword").getBytes())); // ICN Password must be Base64 encoded.
                    request.setUserID(MnpHelper.props.getProperty("NpcdbWSUsername"));

                    NPCData requestData = new NPCData();
                    // Do the header bits here.
                    MessageHeaderType header = new MessageHeaderType();
                    header.setSender(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // SML for Smile in Nigeria
                    header.setHeaderID(String.valueOf(context.getCustomerProfileId()));
                    
                    header.setMessageID(NGPortingHelper.MSG_ID_NUMBER_RETURN_REQUEST); //Send 2001
                    header.setPortType(MnpHelper.props.getProperty("NpcdbMobilePortingType"));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    header.setMsgCreateTimestamp(sdf.format(new Date()));
                    requestData.setMessageHeader(header);
                    // Do the message bits here ...
                    NumberRetReqMsgType numberReturnMessage = new NumberRetReqMsgType();
                    numberReturnMessage.getNumbers().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDB(context.getRoutingInfoList()));
                    
                    NPCMessageType npcMessage = new NPCMessageType();
                    npcMessage.setNumberReturnReq(numberReturnMessage);
                    requestData.setNPCMessage(npcMessage);

                    Marshaller m = Utils.getJAXBMarshallerForSoap(NPCData.class);
                    StringWriter sw = new StringWriter();
                    m.marshal(requestData, sw);
                    request.setXmlMsg(sw.toString());
                    // Show message being sent to NPC
                    SCAMarshaller.logObject(request, "XML being sent to ICN:");
                    
                    ProcessNPCMsgResponse response = new ProcessNPCMsgResponse();
                    
                    try {
                        log.debug("", Utils.marshallSoapObjectToString(request));
                        NPCWebServicePortType connection = getNPCDBConnection();
                        
                        response = connection.processNPCMsg(request);
                        Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);

                        SCAMarshaller.logObject(response, "XML response from ICN:");
                        NPCData npcData = (NPCData) um.unmarshal(new StringReader(response.getResponse()));

                        log.error("Response from ICN:" + response.getResponse());
                        // All is good, transition into NP_ACTIVATED.
                        // Go to Number Return Validation Result
                        
                        if(npcData.getNPCWebServiceResponse().getResponse().equals("Success")) { // All good ...
                            getContext().setPortingOrderId(npcData.getMessageHeader().getTransactionID());
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                            getContext().setPortingDirection(MnpHelper.MNP_PORTING_DIRECTION_IN);
                            getContext().setMessageId(Long.valueOf(npcData.getMessageHeader().getMessageID()));
                            return initNextState(NP_AWAITING_NUMBER_RETURN_VALIDATION_RESULT, context); // Awaiting Return Validation Result
                        } else { // Error ....
                           getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                           // Add all errors to the context and return to SCA ...
                           String errorCode = "", errorDesc = "";
                           for(ValidationType validation : npcData.getNPCWebServiceResponse().getValidation()) {
                               getContext().getValidationErrors().add(validation.getReasonCode() + " - " + validation.getReasonDesc());
                               errorCode = validation.getReasonCode();
                               errorDesc +=  validation.getReasonCode() + " - " + validation.getReasonDesc() + "\r\n";
                           }
                           getContext().setErrorCode(errorCode);
                           getContext().setErrorDescription(errorDesc);
                           
                           return initNextState(NP_REQUEST_REJECTED_BY_NPCDB, getContext());
                        }
                    }  catch (Exception ex)  {
                        log.error("Error while trying to submit message type {} to NPCDB.", NGPortingHelper.MSG_ID_NUMBER_RETURN_REQUEST, ex);
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                        getContext().getValidationErrors().add(ex.getMessage());
                        getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                        getContext().setErrorDescription(ex.getMessage());
                        getContext().setRoutingInfoList(context.getRoutingInfoList());
                    } finally {
                            MnpHelper.createEvent(getContext().getPortingOrderId(), NGPortingHelper.MSG_ID_NUMBER_RETURN_REQUEST,  sw.toString(), response.getResponse(), getContext().getErrorDescription());
                        }
                    
                    return initNextState(NP_AWAITING_RETURN_BROADCAST, getContext()); 
                } else {
                   throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_DONE.getStateId() , context.getMessageType());
                }
                
            } catch (Exception ex) {
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                getContext().setErrorDescription(Utils.getStackTrace(ex));
                return initNextState(NP_FAILED, getContext());
            }
        }
        
        @Override
        public String getStateId() {
            return "NP_DONE";
        }
    },
    NP_REQUEST_CANCEL_VALIDATION_OK {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) {
                    setContext(context);
                    return null;
                }

                @Override
                public String getStateId() {
                    return "NP_REQUEST_CANCEL_VALIDATION_OK";
                }
            },
     NP_AWAITING_NUMBER_RETURN_VALIDATION_RESULT { // Wait for 2002
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            
            try {
                if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_VALIDATION_OK.value())
                                                         || context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_VALIDATION_FAILED.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_AWAITING_NUMBER_RETURN_VALIDATION_RESULT.getStateId(), context.getMessageType());
                }

                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_VALIDATION_OK.value())) { // Donor did not respond with 1007 on time.
                    
                    getContext().setErrorDescription("");
                    getContext().setErrorCode("");
                    
                    return  initNextState(NP_NUMBER_RETURN_DONE, getContext()); //Stay within this state.
                } else
                if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_VALIDATION_FAILED.value())) {
                    // Go to NP_DONE - so that the errors highlighted can be fixed and the return attempted again.
                   return  initNextState(NP_DONE, getContext()); //Stay within this state.
                }

            } catch (InvalidStateTransitionException ex) {
                ex.processException();
                throw ex;
            }
            
            return null;
        }
        
        @Override
        public String getStateId() {
            return "NP_AWAITING_NUMBER_RETURN_VALIDATION_RESULT";
        }
        
     },
    NP_REQUEST_CANCEL_VALIDATION_FAILED {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) {
                    setContext(context);
                    return null;
                }

                @Override
                public String getStateId() {
                    return "NP_REQUEST_CANCEL_VALIDATION_FAILED";
                }
    },
     NP_FAILED {
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Do nothing
            // So that we return successfully to  the porting house
            log.error("ERROR: Porting Order ({}) is in NP_FAILED status, cannot transition away from this state, received messaage ({})", context.getPortingOrderId(), context.getMessageType());
            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
            
            return this; //Stay here
        } 
        
        @Override
        public String getStateId() {
            return "NP_FAILED";
        }
     },
     NP_AWAITING_RETURN_BROADCAST {
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Do nothing
            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST.value())) {
                return  initNextState(NP_NUMBER_RETURN_DONE, getContext());
            } 
            
            return this; // Stay here
        } 
        
        @Override
        public String getStateId() {
            return "NP_AWAITING_RETURN_BROADCAST";
        }
     }
    ,
     NP_NUMBER_RETURN_DONE {
        @Override
        public NGPortInState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Do nothing
            return this; //Stay here
        } 
        
        @Override
        public String getStateId() {
            return "NP_NUMBER_RETURN_DONE";
        }
     },
    NP_COMPLETED {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
                    // Throw an error - should never try to transition further from this state.
                    try {
                        throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_COMPLETED.getStateId(), context.getMessageType());
                    } catch (InvalidStateTransitionException ex) {
                        ex.processException();
                        throw ex;
                    }
                }

                @Override
                public String getStateId() {
                    return "NP_COMPLETED";
                }

            }, NP_REQUEST_PROCESS_END {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) {
                    setContext(context);
                    // Do nothing
                    return null;
                }

                @Override
                public String getStateId() {
                    return "NP_REQUEST_PROCESS_END";
                }
            }, NP_DEFAULT {
                @Override
                public NGPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
                    // Throw an error - should never try to transition further from this state.
                    try {
                        if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())
                                                               || context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_REQUEST.value())
                                                               || context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST.value()))) {
                            throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_DEFAULT.getStateId(), context.getMessageType());
                        }

                        if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_REQUEST.value())) { // Got 2003 - Our number, that was previously ported out has been returned.
                            
                            // Put it back into available numbers list so it can be given to someone else.
                            for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                                log.debug("Adding our own returned number range [{}, {}] to the available_number pool.", routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                                 routingInfo.getPhoneNumberRange().getPhoneNumberEnd());

                                // 2. Add the ported numbers to available_numbers table.
                                AvailableNumberRange availableNumberRange = new AvailableNumberRange();
                                availableNumberRange.setOwnedByCustomerProfileId(0);
                                // availableNumberRange.setOwnedByOrganisationId(context.getOrganisationId());
                                com.smilecoms.commons.sca.direct.am.PhoneNumberRange phoneNumberRange = 
                                        new com.smilecoms.commons.sca.direct.am.PhoneNumberRange();
                                phoneNumberRange.setPhoneNumberEnd(routingInfo.getPhoneNumberRange().getPhoneNumberEnd());
                                phoneNumberRange.setPhoneNumberStart(routingInfo.getPhoneNumberRange().getPhoneNumberStart());

                                availableNumberRange.setPhoneNumberRange(phoneNumberRange);
                                availableNumberRange.setPriceCents(0);

                                // am.addAvailableNumberRange(availableNumberRange);
                                SCAWrapper.getAdminInstance().addAvailableNumberRange_Direct(availableNumberRange);
                                
                            }
                            
                            //Respond with message 2004
                    
                            ProcessNPCMsgRequest request = new ProcessNPCMsgRequest();

                            request.setPassword(Utils.encodeBase64(MnpHelper.props.getProperty("NpcdbWSPassword").getBytes())); // ICN Password must be Base64 encoded.
                            request.setUserID(MnpHelper.props.getProperty("NpcdbWSUsername"));

                            NPCData requestData = new NPCData();
                            // Do the header bits here.
                            MessageHeaderType header = new MessageHeaderType();
                            header.setSender(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // SML for Smile in Nigeria
                            header.setTransactionID(context.getPortingOrderId());
                            
                            header.setHeaderID(getNewHeaderID());
                            
                            header.setMessageID(NGPortingHelper.MSG_ID_NUMBER_RETURN_RESPONSE); //Send 2004
                            header.setPortType(MnpHelper.props.getProperty("NpcdbMobilePortingType"));
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                            header.setMsgCreateTimestamp(sdf.format(new Date()));
                            requestData.setMessageHeader(header);
                            // Do the message bits here ...
                            NumberRetRspMsgType numberReturnMessage = new NumberRetRspMsgType();
                            numberReturnMessage.getNumbers().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDB(context.getRoutingInfoList()));

                            NPCMessageType npcMessage = new NPCMessageType();
                            npcMessage.setNumberReturnRsp(numberReturnMessage);
                            requestData.setNPCMessage(npcMessage);

                            Marshaller m = Utils.getJAXBMarshallerForSoap(NPCData.class);
                            StringWriter sw = new StringWriter();
                            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                            m.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
                            m.marshal(requestData, sw);
                            request.setXmlMsg(sw.toString());
                            // Show message being sent to NPC
                            SCAMarshaller.logObject(request, "XML being sent to ICN:");
                            ProcessNPCMsgResponse response = new ProcessNPCMsgResponse();
                            try {
                                log.debug("", Utils.marshallSoapObjectToString(request));
                                NPCWebServicePortType connection = getNPCDBConnection();

                                response = connection.processNPCMsg(request);
                                Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);

                                SCAMarshaller.logObject(response, "XML response from ICN:");
                                NPCData npcData = (NPCData) um.unmarshal(new StringReader(response.getResponse()));

                                log.error("Response from ICN:" + response.getResponse());
                                // All is good, transition into NP_ACTIVATED.
                                // Go to Number Return Validation Result
                                return initNextState(NP_AWAITING_RETURN_BROADCAST, context); // Awaiting DONOR_DEACT
                            }  catch (Exception ex)  {
                                log.error("Error while trying to submit message type {} to NPCDB.", NGPortingHelper.MSG_ID_NUMBER_RETURN_RESPONSE, ex);
                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                getContext().getValidationErrors().add(ex.getMessage());
                                getContext().setErrorCode("UNKNOWN");
                                getContext().setErrorDescription(ex.getMessage());
                                getContext().setRoutingInfoList(context.getRoutingInfoList());
                                
                                context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                                return initNextState(NP_NUMBER_RETURN_DONE, context); 
                            } finally {
                                MnpHelper.createEvent(getContext().getPortingOrderId(), NGPortingHelper.MSG_ID_NUMBER_RETURN_RESPONSE,  sw.toString(), response.getResponse(), getContext().getErrorDescription());
                            }
                            
                        } else
                            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST.value())) {
                                //1. A number has been returned to another operator and Smile is not involved, so update our ported_number table accorgingly.
                                for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                                log.debug("Update/add porting_data for number range [{}, {}] that was returned to its origninal block holder {}.", new Object[]{
                                                 routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                                 routingInfo.getPhoneNumberRange().getPhoneNumberEnd(), 
                                                 context.getRangeHolderId()});
                                PortingData portingData = new PortingData();
                                portingData.setPlatformContext(new PlatformContext());
                                portingData.getPlatformContext().setTxId(context.getPlatformContext().getTxId());
                                portingData.setStartE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart()));
                                portingData.setEndE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
                                portingData.setInterconnectPartnerCode(context.getRangeHolderId()); // Number is now hosted by its range holder ...
                                SCAWrapper.getAdminInstance().updatePortingData_Direct(portingData);
                                
                            }
                                return initNextState(NP_NUMBER_RETURN_DONE, context); 
                        } else    
                        if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                    // This is a porting broacast to notify Smile about a porting that took place between other operators.
                            // 0. Set customerProfileID to -1 since this is not a Smile customer.
                            context.setCustomerProfileId(-1);
                            context.setCustomerType("UNKNOWN");
                            context.setHandleManually("0");
                            context.setPortingDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
                            context.setRangeHolderId("0");

                            // 1. Update Smile's network routing tables here
                            log.debug("Updating SMILE routing tables with number portability information; senderID [{}], recipientID [{}], donorID [{}], routingInformationList [{}]",
                                    new Object[]{
                                        context.getSenderId(),
                                        context.getRecipientId(),
                                        context.getDonorId(),
                                        context.getRoutingInfoList()
                                    });
                            // TODO:  Update the routing tables here.
                            for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                                log.debug("Adding a ported number range [{}, {}] from donor network {} to {} into the ported_numbers table.", new Object[]{
                                                         routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                                         routingInfo.getPhoneNumberRange().getPhoneNumberEnd(), 
                                                         context.getDonorId(), context.getRecipientId()});
                                PortingData portingData = new PortingData();
                                portingData.setPlatformContext(new PlatformContext());
                                portingData.getPlatformContext().setTxId(context.getPlatformContext().getTxId());
                                portingData.setStartE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart()));
                                portingData.setEndE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
                                portingData.setInterconnectPartnerCode(context.getRecipientId()); // Calls to this number must now be routed to this recipient network
                                SCAWrapper.getAdminInstance().updatePortingData_Direct(portingData);
                            }
                            context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);

                            // 2. Go to NP_COMPLETED status.
                            return initNextState(NP_DONE, context);
                        } else {
                            
                        }
                        throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_ACTIVATED.getStateId(), context.getMessageType());
                    } catch (InvalidStateTransitionException ex) {
                        ex.processException();
                        throw ex;
                    }
                }

                @Override
                public String getStateId() {
                    return "NP_DEFAULT";
                }
            };

    // public abstract NPPortInState nextState(TZPortInRequest request) throws Exception;
    // NpcdbService tzNpcdbService = new NpcdbService();
    private PortInEvent context;

    public PortInEvent getContext() {
        return context;
    }

    public void setContext(PortInEvent req) {
        context = req;
        log.error("------ context:" + context);
        log.error("------ OrderId:" + context.getPortingOrderId());
        log.error("------ ErrorCode:" + context.getErrorCode());
    }

    public NGPortInState initNextState(NGPortInState nextState, PortInEvent context) {
        nextState.setContext(context);
        return nextState;
    }

    @Override
    public NPCWebServicePortType getNPCDBConnection() throws Exception { //Invokes web service at the NPC DB and bring back resutls
        return NGPortingHelper.getNPCDBConnection();
    }

    
           
    private static final Logger log = LoggerFactory.getLogger(NGPortInState.class);
    
    NGPortInState() {
        // Do nothing
    }
    
    public String getNewHeaderID() {
        long number = (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
        return String.valueOf(number);
    }
    
  }
