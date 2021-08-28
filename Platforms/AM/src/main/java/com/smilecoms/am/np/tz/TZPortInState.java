/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.tz;

import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.InvalidStateTransitionException;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.sca.direct.am.AvailableNumberRange;
import com.smilecoms.commons.sca.direct.bm.PlatformContext;
import com.smilecoms.commons.sca.direct.bm.PortingData;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PortInEvent;
import com.smilecoms.xml.schema.am.RoutingInfo;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.systor.np.commontypes.CustomerType;
import st.systor.np.commontypes.ServiceType;
import st.systor.np.commontypes.SubscriptionType;
import st.systor.np.npcdb.NPActivatedToNpcdbType;
import st.systor.np.npcdb.NPRequestToNpcdbType;
import st.systor.np.npcdb.NPReturnCancelToNpcdbType;
import st.systor.np.npcdb.NPReturnToNpcdbType;
import st.systor.np.npcdb.NPRingFenceRequestToNpcdbType;
import st.systor.np.npcdb.NpcdbAccessFault;
import st.systor.np.npcdb.NpcdbMessageAckType;
import st.systor.np.npcdb.NpcdbPort;
import st.systor.np.npcdb.NpcdbReject;
import st.systor.np.npcdb.NpcdbService;

/**
 *
 * @author mukosi
 This class will represent all the states for a port in request,
 a port in request is when a customer request for smile to initiate a request to migrate them from their
 * current network into the Smile network. 
 * this class will also code the transitions between the states.
 */

public enum TZPortInState implements IPortState {
    
    NP_NEW_REQUEST  {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
           log.debug("TZMNP in state NP_NEW_REQUEST");
                      
           setContext(context);
           
           // Send porting request to to NPCDB and obtain an Acknowledgement back.
           NPRequestToNpcdbType request = new NPRequestToNpcdbType();
           request.setSenderID(getSenderId(context));
           request.setValidationMSISDN(context.getValidationMSISDN());
            
           if(context.getHandleManually() != null && context.getHandleManually().toLowerCase().equals("true")) {
                request.setHandleManually(1);
           }
           
           if(!TZPortingHelper.isEmergencyRestore(context)) { // Date of birth is optional.
                request.setDateOfBirth(context.getDateOfBirth());
           }
           
           request.setCustomerName(context.getCustomerFirstName() + " " + context.getCustomerLastName());
           
           if(context.getCustomerIdType().toLowerCase().equals("passport")) {
               /* 1 = passport number.
                  2 = civil ID */
                request.setCustomerIDType(1);
           } else { // 2 = civil ID
                request.setCustomerIDType(2);
           }
           request.setCustomerID(String.valueOf(context.getIdentityNumber()));
           
           if(context.getServiceType().toLowerCase().equals("mobile")) {
                request.setServiceType(ServiceType.MOBILE);
           } 
           if(context.getServiceType().toLowerCase().equals("fixed")) {
                request.setServiceType(ServiceType.FIXED); 
           } 
           
           if(context.getCustomerType().toLowerCase().equals(TZPortingHelper.CUSTOMER_TYPE_INDIVIDUAL)) {
                request.setCustomerType(CustomerType.PERSONAL);
           } else
               if(context.getCustomerType().toLowerCase().equals(TZPortingHelper.CUSTOMER_TYPE_ORGANISATION)) {
                request.setCustomerType(CustomerType.BUSINESS);
           }
           
           if(context.getSubscriptionType().toLowerCase().equals("prepaid")) {
                request.setSubscriptionType(SubscriptionType.PREPAID);
           } 
           if(context.getSubscriptionType().toLowerCase().equals("postpaid")) {
                request.setSubscriptionType(SubscriptionType.POSTPAID);
           }
           
           if(context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty()) {
               request.setEmergencyRestoreID(Long.parseLong(context.getEmergencyRestoreId()));
           }
               
           request.setRoutingInfoList(TZPortingHelper.mapXMLRoutingInformationListToNPCDB(context.getRoutingInfoList()));
           
           NpcdbMessageAckType npcdbResponse = null;
           
           // portingTime? (it's optional according to specs)
           try {
               
               if(BaseUtils.getBooleanProperty("env.mnp.porting.request.test.mode", false)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
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
               
                 npcdbResponse = getNPCDBConnection().sendNPRequest(request);
           
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);

                getContext().setPortingOrderId(String.valueOf(npcdbResponse.getNPOrderID()));
                getContext().setDonorId(context.getDonorId());
                getContext().setRangeHolderId(context.getRangeHolderId());
                getContext().setRecipientId(context.getRecipientId());
                getContext().setMessageId(context.getMessageId());
                getContext().setPortingDirection(MnpHelper.MNP_PORTING_DIRECTION_IN);
                //getContext().setNpState(this.getStateId()); // This is a new request and awaiting confirmation from NPCDB.
                getContext().setRoutingInfoList(context.getRoutingInfoList());

                // Create a new NP request in the database (mnp_port_in_request)
                // MnpPortInRequest dbPortInRequest = DAO.createNewPortInRequest(em, request);
                // NpcdbMessageAckType ack = tzNpcdbService.getNpcdbSoap().sendNPRequest(request); 
                // tzPortInReq.setCurrentState(NP_REQUEST_SUBMITTED_TO_NPCDB); 
                
                return initNextState(NP_REQUEST_ACCEPTED_BY_NPCDB, getContext()); 
           } catch (Exception ex)  {
               
               log.error("Error while submitting new porting request: ", Utils.getStackTrace(ex));
               
               getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
               
               if(ex instanceof NpcdbReject) {
                    NpcdbReject nr = (NpcdbReject)  ex;
                    getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                    getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                    getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else
               if(ex instanceof NpcdbAccessFault) {
                   NpcdbAccessFault nr = (NpcdbAccessFault)  ex;
                   getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                   getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                   getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else {
                   getContext().getValidationErrors().add(ex.getMessage());
                   getContext().setErrorCode("UNKNOWN");
                   getContext().setErrorDescription(ex.getMessage());
               }
               getContext().setRoutingInfoList(context.getRoutingInfoList());
           } finally {
               MnpHelper.createEvent(getContext().getPortingOrderId(), "NPRequestToNpcdbType",  Utils.marshallSoapObjectToString(request), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
           }
           log.debug("TZMNP done state NP_NEW_REQUEST");
           return initNextState(NP_REQUEST_REJECTED_BY_NPCDB, getContext());
        }
        @Override
        public String getStateId() {
            return "NP_NEW_REQUEST";
        }
    }, NP_REQUEST_REJECTED_BY_NPCDB {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            // Do nothing ....
            return null;
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_REJECTED_BY_NPCDB";
        }
    } ,
    NP_REQUEST_ACCEPTED_BY_NPCDB {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context)  throws Exception {
            try {
                setContext(context);                                                                                            
                if((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_VALIDATED_TO_SP.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_CANCEL_TO_SP.value())    ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_REJECTED.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_ACCEPTED_BY_NPCDB.getStateId() , context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_REJECTED.value())) {
                    // Request has been rejected at NPCDB due to SMS validation issues, store the reason/errror code and go to NP_REQUEST_REJECTED_BY_NPCDB
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                    return initNextState(NP_REQUEST_REJECTED_BY_NPCDB, getContext());    
                } else    
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_VALIDATED_TO_SP.value())) {
                    // All good - np request was accepted by NPCDB and now forwarded to Donor SP.
                    // Change the state of our np request to in progress.
                    return initNextState(NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR, context);
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_REQUEST_CANCEL_TO_SP.value())) {
                   // NP REQUEST was REJECTED at NPCDB before sending to Donor - could be due timer T10 EXPIREring.     
                   // Chance request status to CANCELLED and save the cancel message/reason.
                   // Process terminates...
                   getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                   return initNextState(NP_CANCELLED, getContext());
                } 
                // Do not know how to proceed with this transition...
                throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_ACCEPTED_BY_NPCDB.getStateId() , context.getMessageType());    
            } catch (InvalidStateTransitionException ex) {
                ex.processException();
                throw ex;
            }
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_ACCEPTED_BY_NPCDB";
        }
    }, NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            // While we are in this status - we can receive a Donor Accept or Donor Reject
            setContext(context);
            NpcdbMessageAckType npcdbResponse = null;
            NPActivatedToNpcdbType npActivated = null;
            
            try {
                if((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_ACCEPT_TO_SP.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_CANCEL_TO_SP.value()) ||
                                                           context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR.getStateId() , context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_CANCEL_TO_SP.value())) {
                    // For some reason, Smile failed to respond with NPActivated (T5/T13 expired) and so NPCDB is sending the NPExecuteCancel message to abandon the port.
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                    return initNextState(NP_CANCELLED, context);
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_ACCEPT_TO_SP.value())) {
                    // All good - np request was accepted by DONOR.
                    // So Smile can provision the number on the network 
                    // and then send the NpActivated message to NPCDB
                    // TODO:
                    MnpHelper.activatePortedNumberOnSmileNetwork(context);
                    // If all is good, we tell the clearing house that NP is Activated.
                    npActivated = new NPActivatedToNpcdbType();
                    npActivated.setNPOrderID(Long.valueOf(getContext().getPortingOrderId()));
                    npActivated.setSenderID(getSenderId(context));

                    npcdbResponse = getNPCDBConnection().sendNPActivated(npActivated);

                    // 4. Return with NpActivated to NPCDB
                    return initNextState(NP_ACTIVATED, context);
                        //  ...
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP.value())) {
                   // NP REQUEST was REJECTED by the Donor SP therefore process terminatesn.
                   // Process terminates...
                   getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                   return initNextState(NP_DONOR_REJECTED, context);
                } 
                // Do not know how to proceed with this transition...
                throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR.getStateId() , context.getMessageType());    
            } catch (Exception ex) {
               log.error("Error while handling NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR: ", Utils.getStackTrace(ex));
               setContextErrorCodeAndDescription(ex, getContext());
               getContext().setRoutingInfoList(context.getRoutingInfoList());
            } finally {
               MnpHelper.createEvent(getContext().getPortingOrderId(), "NPActivatedToNpcdbType",  Utils.marshallSoapObjectToString(npActivated), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
           }
           return initNextState(NP_FAILED, context);
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_VALIDATED_AND_SUBMITTED_TO_DONOR";
        }
    }, NP_FAILED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            // Do nothing
            return this; //Stay here
        } 
        
        @Override
        public String getStateId() {
            return "NP_FAILED";
        }
   },
    NP_CANCELLED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            return null;
        } 
        
        @Override
        public String getStateId() {
            return "NP_CANCELLED";
        }
    },
    NP_DONOR_REJECTED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
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
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            // We expect a NPCDB_EXECUTE_BROADCAST_TO_SP message while in this state.
            
            log.debug("TZPortInState - Entering state: {}, Received Message Type: {}, PortingOrderId: {}", new Object[]{ this.getStateId(), context.getMessageType(), context.getPortingOrderId()});
            try {
                if((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_ACTIVATED.getStateId() , context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                    // An NPExecuteBroacast is received on a number that has already been ported to us.
                    // So far there is nothing to do here - just transition to NP_DONE state.
                    context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    return initNextState(NP_DONE, context);
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_DONOR_REJECT_TO_SP.value())) {
                   // NP REQUEST was REJECTED by the Donor SP therefore process terminatesn.
                   // Process terminates...
                   return initNextState(NP_DONOR_REJECTED, context);
                } 
                // Do not know how to proceed with this transition...
                throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_ACTIVATED.getStateId() , context.getMessageType());    
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
    NP_REQUEST_CANCEL_VALIDATION_OK {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            return null;
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_CANCEL_VALIDATION_OK";
        }
    },
    NP_REQUEST_CANCEL_VALIDATION_FAILED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            return null;
        } 
        
        @Override
        public String getStateId() {
            return "NP_REQUEST_CANCEL_VALIDATION_FAILED";
        }
    },NP_RING_FENCE_APPROVED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception{
            setContext(context);
            return null;
        } 
        
        @Override
        public String getStateId() {
            return "NP_RING_FENCE_APPROVED";
        }
    },NP_RING_FENCE_DENIED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception{
            setContext(context);
            return null;
        } 
        
        @Override
        public String getStateId() {
            return "NP_RING_FENCE_DENIED";
        }
    },
    NP_DONE {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            NPReturnToNpcdbType npReturn = null;
            NpcdbMessageAckType npcdbResponse = null;
            
            // Throw an error - should never try to transition further from this state.
            try {
                
                if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                    return initNextState(NP_DONE, context); 
                } else 
                     if (context.getMessageType().equals(StMNPMessageTypes.SMILE_REQUEST_NUMBER_RETURN.value())) { // Handle port-out request here.
                    // NB: A new port order id will be allocated and must be used for future reference to this return request.
                    npReturn = new NPReturnToNpcdbType();
                    npReturn.setSenderID(MnpHelper.props.getProperty("SmileMNPParticipatingId"));
                    npReturn.setPhoneNumberList(TZPortingHelper.mapXMLRoutingInformationListToNPCDBPhoneNumberList(context.getRoutingInfoList()));

                    npcdbResponse = getNPCDBConnection().sendNPReturn(npReturn);
                    
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                    getContext().setPortingOrderId(String.valueOf(npcdbResponse.getNPOrderID()));
                    getContext().setRangeHolderId(npcdbResponse.getRangeHolderID());
                    
                    return initNextState(NP_AWAITING_RETURN_BROADCAST, getContext()); 
                } else {
                   throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_DONE.getStateId() , context.getMessageType());
                }
                
            } catch (Exception ex) {
               log.error("Error while handling NP_DONE: ", Utils.getStackTrace(ex));
               
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);

                if(ex instanceof NpcdbReject) {
                     NpcdbReject nr = (NpcdbReject)  ex;
                     getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                     getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                     getContext().setErrorDescription(nr.getFaultInfo().getDescription());
                } else
                if(ex instanceof NpcdbAccessFault) {
                    NpcdbAccessFault nr = (NpcdbAccessFault)  ex;
                    getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                    getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                    getContext().setErrorDescription(nr.getFaultInfo().getDescription());
                } else {
                    getContext().getValidationErrors().add(ex.getMessage());
                    getContext().setErrorCode("UNKNOWN");
                    getContext().setErrorDescription(ex.getMessage());
                }
                getContext().setRoutingInfoList(context.getRoutingInfoList());
            } finally {
               MnpHelper.createEvent(getContext().getPortingOrderId(), "NPReturnToNpcdbType",  Utils.marshallSoapObjectToString(npReturn), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
            }
           
            return initNextState(this, getContext()); // Stay in here so the return can be tried again.
        } 
        
        @Override
        public String getStateId() {
            return "NP_DONE";
        }
   }, NP_RING_FENCE_REQUEST_SUBMITTED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            // A ring fence request can either be APPROVED or REJECTED by the Clearing House.
            try {
                if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_APPROVED.value())
                                                       ||   context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_DENIED.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_RING_FENCE_REQUEST_SUBMITTED.getStateId(), context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_APPROVED.value())) {
                    // Ringfence has been accepted. Add the ringfenced numbers to available numbers and assign them to this customer.
                    
                    //Save ringfence numbers, if specified here.
                    MnpHelper.saveRingFencedNumberList(em, context.getRingFenceNumberList(), context.getRecipientId());
                        
                    /*
                    for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                        log.debug("Adding a ring-fenced number range [{}, {}] and reserving it for customer id {}.", new Object[]{
                                         routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                         routingInfo.getPhoneNumberRange().getPhoneNumberEnd(), 
                                         context.getCustomerProfileId()});
                        
                        // 2. Add the ported numbers to available_numbers table.
                        AvailableNumberRange availableNumberRange = new AvailableNumberRange();
                        availableNumberRange.setOwnedByCustomerProfileId(context.getCustomerProfileId());
                        // availableNumberRange.setOwnedByOrganisationId(context.getOrganisationId());
                        com.smilecoms.commons.sca.direct.am.PhoneNumberRange phoneNumberRange = 
                                new com.smilecoms.commons.sca.direct.am.PhoneNumberRange();
                        phoneNumberRange.setPhoneNumberEnd(routingInfo.getPhoneNumberRange().getPhoneNumberEnd());
                        phoneNumberRange.setPhoneNumberStart(routingInfo.getPhoneNumberRange().getPhoneNumberStart());
                        
                        availableNumberRange.setPhoneNumberRange(phoneNumberRange);
                        availableNumberRange.setPriceCents(0);
                        
                        // am.addAvailableNumberRange(availableNumberRange);
                        SCAWrapper.getAdminInstance().addAvailableNumberRange_Direct(availableNumberRange);
                        
                    }*/
                    
                    context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    return initNextState(NP_RING_FENCE_APPROVED, context);
                } else
                    if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_DENIED.value())) {
                        // Nothing really to-do ... just update the statuses
                        context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                        return initNextState(NP_RING_FENCE_DENIED, context);    
                 }
                return this;
            } catch (Exception ex)  {
               
               getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
               
               if(ex instanceof NpcdbReject) {
                    NpcdbReject nr = (NpcdbReject)  ex;
                    getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                    getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                    getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else
               if(ex instanceof NpcdbAccessFault) {
                   NpcdbAccessFault nr = (NpcdbAccessFault)  ex;
                   getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                   getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                   getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else {
                   getContext().getValidationErrors().add(ex.getMessage());
                   getContext().setErrorCode("UNKNOWN");
                   getContext().setErrorDescription(ex.getMessage());
               }
               getContext().setRoutingInfoList(context.getRoutingInfoList());
           }
           return null; 
        } 
        
        @Override
        public String getStateId() {
            return "NP_RING_FENCE_REQUEST_SUBMITTED";
        }
   }, NP_AWAITING_RETURN_BROADCAST {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            NpcdbMessageAckType npcdbResponse = null;
            NPReturnCancelToNpcdbType npCancelReturn = null;        
            try {
                // Terminal state, no transition beyond this state ...
                if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.SMILE_CANCEL_NUMBER_RETURN.value())
                                                         || context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST.value()))) {
                            throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_AWAITING_RETURN_BROADCAST.getStateId(), context.getMessageType());
                    }
                
                if(context.getMessageType().equals(StMNPMessageTypes.SMILE_CANCEL_NUMBER_RETURN.value())) {
                    //Initiate a cancel return to Clearing House.
                    npCancelReturn = new NPReturnCancelToNpcdbType();
                    npCancelReturn.setNPOrderID(Long.valueOf(context.getPortingOrderId()));
                    npCancelReturn.setSenderID(MnpHelper.props.getProperty("SmileMNPParticipatingId"));
                    
                    npcdbResponse = getNPCDBConnectionAsDonor(getContext()).sendNPReturnCancel(npCancelReturn);
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    getContext().setPortingOrderId(String.valueOf(npcdbResponse.getNPOrderID()));
                    getContext().setRangeHolderId(npcdbResponse.getRangeHolderID());
                    
                    return initNextState(NP_NUMBER_RETURN_CANCELLED, getContext()); 
                } else 
                    if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST.value())) {
                        // - Handle number return here... If we get to this state, this is the number that was returned by Smile back to the original rangeholder.
                        // - The service must already be in a deactivated/deleted state - a return to range holder cannot be initiated if the service is in AC status.
                        // - So all we need to do is to update ported_number to indicate the interconnect partner where the phone number is currently sitting.
                        for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                            log.debug("Update returned number range [{}, {}] from Smile Network back to range holder {} into the ported_numbers table.", new Object[]{
                                                     routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                                     routingInfo.getPhoneNumberRange().getPhoneNumberEnd(), 
                                                     context.getRangeHolderId()});
                            PortingData portingData = new PortingData();
                            portingData.setPlatformContext(new PlatformContext());
                            portingData.getPlatformContext().setTxId(context.getPlatformContext().getTxId());
                            portingData.setStartE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart()));
                            portingData.setEndE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
                            portingData.setInterconnectPartnerCode(context.getRangeHolderId()); // Calls to this number must now be routed to this recipient network
                            SCAWrapper.getAdminInstance().updatePortingData_Direct(portingData);
                        }
                        return initNextState(NP_NUMBER_RETURN_DONE, getContext());
                    }
            } catch (Exception ex) {
                
               log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), Utils.getStackTrace(ex));
               
               
               getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
               
               if(ex instanceof NpcdbReject) {
                    NpcdbReject nr = (NpcdbReject)  ex;
                    getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                    getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                    getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else
               if(ex instanceof NpcdbAccessFault) {
                   NpcdbAccessFault nr = (NpcdbAccessFault)  ex;
                   getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                   getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                   getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else {
                   getContext().getValidationErrors().add(ex.getMessage());
                   getContext().setErrorCode("UNKNOWN");
                   getContext().setErrorDescription(ex.getMessage());
               }
                
            } finally {
               MnpHelper.createEvent(getContext().getPortingOrderId(), "NPReturnCancelToNpcdbType",  Utils.marshallSoapObjectToString(npCancelReturn), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
            }
            return initNextState(this, getContext()); // Stay in here
        }        
        @Override
        public String getStateId() {
            return "NP_AWAITING_RETURN_BROADCAST";
        }
   }, NP_NUMBER_RETURN_CANCELLED {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            // Terminal state, no transition beyond this state ...
            return this; // Stay in here
        }        
        @Override
        public String getStateId() {
            return "NP_NUMBER_RETURN_CANCELLED";
        }
   }, NP_NUMBER_RETURN_DONE {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            // Terminal state, no transition beyond this state ...
            return this; // Stay in here
        }        
        @Override
        public String getStateId() {
            return "NP_NUMBER_RETURN_DONE";
        }
   }, NP_DEFAULT {
        @Override
        public TZPortInState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            NpcdbMessageAckType npcdbResponse = null;
            NPRingFenceRequestToNpcdbType npRingFence = null;
            // Throw an error - should never try to transition further from this state.
            try {
                st.systor.np.commontypes.PhoneNumberRange rejectedPhoneNumberRange = new st.systor.np.commontypes.PhoneNumberRange();
                
                if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())
                                                       ||   context.getMessageType().equals(StMNPMessageTypes.SMILE_RING_FENCE_REQUEST.value())
                                                       ||   context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_APPROVED.value())
                                                       ||   context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_DEFAULT.getStateId(), context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_NUMBER_RETURN_BROADCAST.value())) { // A broadcast for the returning of a number that used to belong to smile is taking place
                    // This number is returned back to Smile from another operator.
                    // So add it to our available_number list and free to be used by anyone.
                    for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                        log.debug("Adding returned number range [{}, {}] from subscription provider {} to the available_number table.", new Object[]{
                                         routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                         routingInfo.getPhoneNumberRange().getPhoneNumberEnd(), 
                                         context.getDonorId()});
                        
                        PortingData portingData = new PortingData();
                        portingData.setPlatformContext(new PlatformContext());
                        portingData.getPlatformContext().setTxId(context.getPlatformContext().getTxId());
                        portingData.setStartE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart()));
                        portingData.setEndE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
                        
                        portingData.setInterconnectPartnerCode(context.getRangeHolderId()); // Number is now hosted by Smile ...
                        SCAWrapper.getAdminInstance().updatePortingData_Direct(portingData);
                        
                        // 2. Add the returned numbers to available_numbers table.
                        AvailableNumberRange availableNumberRange = new AvailableNumberRange();
                        availableNumberRange.setOwnedByCustomerProfileId(0);
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
                } else
                if (context.getMessageType().equals(StMNPMessageTypes.SMILE_RING_FENCE_REQUEST.value())) {
                    npRingFence = new NPRingFenceRequestToNpcdbType();

                    npRingFence.setSenderID(getSenderId(context));
                    npRingFence.setPhoneNumberList(TZPortingHelper.mapXMLRoutingInformationListToNPCDBPhoneNumberList(context.getRoutingInfoList()));
                    
                    getContext().getRingFenceNumberList().addAll(TZPortingHelper.mapRoutingInformationListToAMPhoneNumberList(context.getRoutingInfoList()));
                    
                    try {
                        npcdbResponse = getNPCDBConnection().sendNPRingFenceRequest(npRingFence);
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                        getContext().setPortingOrderId(String.valueOf(npcdbResponse.getNPOrderID()));
                        getContext().setRangeHolderId(npcdbResponse.getRangeHolderID());
                        return initNextState(NP_RING_FENCE_REQUEST_SUBMITTED, getContext()); 
                    }  catch (Exception ex)  {
                        log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), Utils.getStackTrace(ex));
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);

                        if(ex instanceof NpcdbReject) {
                             NpcdbReject nr = (NpcdbReject)  ex;
                             getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                             getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                             getContext().setErrorDescription(nr.getFaultInfo().getDescription());
                        } else
                        if(ex instanceof NpcdbAccessFault) {
                            NpcdbAccessFault nr = (NpcdbAccessFault)  ex;
                            getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                            getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                            getContext().setErrorDescription(nr.getFaultInfo().getDescription());
                        } else {
                            getContext().getValidationErrors().add(ex.getMessage());
                            getContext().setErrorCode("UNKNOWN");
                            getContext().setErrorDescription(ex.getMessage());
                        }
                        
                        getContext().setRoutingInfoList(context.getRoutingInfoList());
                        
                        return null;
                    } finally {
                        MnpHelper.createEvent(getContext().getPortingOrderId(), "NPRingFenceRequestToNpcdbType",  Utils.marshallSoapObjectToString(npRingFence), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
                    }
                } else
                if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                    // This is a porting broacast to notify Smile about a porting that took place between other operators.
                    // 0. Set customerProfileID to -1 since this is not a Smile customer.
                    context.setCustomerProfileId(-1);
                    
                    // 1. Update Smile's network routing tables here
                    log.debug("Updating SMILE routing tables with number portability information; senderID [{}], recipientID [{}], donorID [{}], routingInformationList [{}]",
                            new Object[]{
                                context.getSenderId(),
                                context.getRecipientId(),
                                context.getDonorId(),
                                context.getRoutingInfoList()
                            });
                    // Update the porting tables here.
                    for(RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                        log.debug("Adding a ported number range [{}, {}] from donor network {} to {} into the database.", new Object[]{
                                         routingInfo.getPhoneNumberRange().getPhoneNumberStart(), 
                                         routingInfo.getPhoneNumberRange().getPhoneNumberEnd(), 
                                         context.getDonorId(), context.getRecipientId()});
                        
                        PortingData portingData = new PortingData();
                        portingData.setPlatformContext(new PlatformContext());
                        portingData.getPlatformContext().setTxId(context.getPlatformContext().getTxId());
                        portingData.setStartE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart()));
                        portingData.setEndE164(Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd()));
                        
                        portingData.setInterconnectPartnerCode(context.getRecipientId());
                        SCAWrapper.getAdminInstance().updatePortingData_Direct(portingData);
                    }
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    // 2. Go to NP_DONE status.
                    return initNextState(NP_DONE, context);
                } else
                if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_RING_FENCE_APPROVED.value())) { 
                    //Another operator is trying to ring-fence a Smile number - save  it here.
                    MnpHelper.saveRingFencedNumberList(em, context.getRingFenceNumberList(), context.getRecipientId());
                    
                } else {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_DEFAULT.getStateId() , context.getMessageType());
                }
            
            } catch (Exception ex)  {
               log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), Utils.getStackTrace(ex));
               getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
               
               if(ex instanceof NpcdbReject) {
                    NpcdbReject nr = (NpcdbReject)  ex;
                    getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                    getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                    getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else
               if(ex instanceof NpcdbAccessFault) {
                   NpcdbAccessFault nr = (NpcdbAccessFault)  ex;
                   getContext().getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
                   getContext().setErrorCode(nr.getFaultInfo().getErrorCode());
                   getContext().setErrorDescription(nr.getFaultInfo().getDescription());
               } else {
                   getContext().getValidationErrors().add(ex.getMessage());
                   getContext().setErrorCode("UNKNOWN");
                   getContext().setErrorDescription(ex.getMessage());
               }
               getContext().setRoutingInfoList(context.getRoutingInfoList());
            } finally {
               MnpHelper.createEvent(getContext().getPortingOrderId(), "NPRingFenceRequestToNpcdbType",  Utils.marshallSoapObjectToString(npRingFence), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
            }
           return null; 
        } 
        
        @Override
        public String getStateId() {
            return "NP_DEFAULT";
        }
   };
    
    private PortInEvent context;
    
    @Override
    public PortInEvent getContext() {
        return context;
    }
    
    public void setContext(PortInEvent req) throws Exception {
        if(req == null) {
            log.error("Context is NULL, cannot process this state.");
               throw new Exception();
           }
        
        context = req;
        log.error("------ context:" + context);
        log.error("------ OrderId:" + context.getPortingOrderId());
        log.error("------ RoutingInfoList.size():" + context.getPortingOrderId());
    }
    
    public TZPortInState initNextState(TZPortInState nextState, PortInEvent context) throws Exception {
            nextState.setContext(context);
            return nextState;
    }
    
    public void setContextErrorCodeAndDescription(Exception ex, PortInEvent context) {
        context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
        if(ex instanceof NpcdbReject) {
             NpcdbReject nr = (NpcdbReject)  ex;
             context.getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
             context.setErrorCode(nr.getFaultInfo().getErrorCode());
             context.setErrorDescription(nr.getFaultInfo().getDescription());
        } else
        if(ex instanceof NpcdbAccessFault) {
            NpcdbAccessFault nr = (NpcdbAccessFault)  ex;
            context.getValidationErrors().add(nr.getFaultInfo().getErrorCode() + " - " + nr.getFaultInfo().getDescription());
            context.setErrorCode(nr.getFaultInfo().getErrorCode());
            context.setErrorDescription(nr.getFaultInfo().getDescription());
        } else {
            context.getValidationErrors().add(ex.getMessage());
            context.setErrorCode("UNKNOWN");
            context.setErrorDescription(ex.getMessage());
        }
    }
    
    private static String getSenderId(PortInEvent context) {
        String senderId;
        if(BaseUtils.getBooleanProperty("env.mnp.emergency.restore.test.mode", false) 
           && (context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty())) {
            log.debug("Porting order {} is related to an Emergency Restore Id {}, and Emergency Restore is configured to run in test mode.", context.getPortingOrderId(), context.getEmergencyRestoreId());
                senderId = MnpHelper.props.getProperty("SmileTestMNPParticipatingId");
        } else {
            senderId = MnpHelper.props.getProperty("SmileMNPParticipatingId");
        }
        log.debug("SenderId to use will be {}.", senderId);
        return senderId;           
    }
    
    @Override
    public NpcdbPort getNPCDBConnection()  throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NPCDB web service");
        
        boolean emergencyRestoreAndInTestMode = false;
        
        String usernameSubProperty = "NpcdbWSUsername";
        String passwordSubProperty = "NpcdbWSPassword";
        
        if(context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty()) {
            // This port is related to an emergency restore
            if(BaseUtils.getBooleanProperty("env.mnp.emergency.restore.test.mode", false)) {
                log.debug("Porting order {} is related to an Emergency Restore Id {}, and Emergency Restore is configured to run in test mode.", context.getPortingOrderId(), context.getEmergencyRestoreId());
                // Emergency restore should be run in test more. Swap the Smile Profiles (Donor becomes Receipient and vice Versa)
                usernameSubProperty = "SmileTestNpcdbWSUsername";
                passwordSubProperty = "SmileTestNpcdbWSPassword";
                emergencyRestoreAndInTestMode = true;
            }
        }
                                                    
            
        java.net.Authenticator myAuth;
        
        if(emergencyRestoreAndInTestMode) {
            myAuth = new java.net.Authenticator() {
            @Override
              protected java.net.PasswordAuthentication getPasswordAuthentication() {
              return new java.net.PasswordAuthentication(MnpHelper.props.getProperty("SmileTestNpcdbWSUsername"), 
                                                         MnpHelper.props.getProperty("SmileTestNpcdbWSPassword").toCharArray());
              }
            };
        } else {
            myAuth = new java.net.Authenticator() {
            @Override
              protected java.net.PasswordAuthentication getPasswordAuthentication() {
              return new java.net.PasswordAuthentication(MnpHelper.props.getProperty("NpcdbWSUsername"), 
                                                         MnpHelper.props.getProperty("NpcdbWSPassword").toCharArray());
              }
            }; 
        }
            
        java.net.Authenticator.setDefault(myAuth);

        try {
            NpcdbService npcdbService = new NpcdbService(new URL(MnpHelper.props.getProperty("NpcdbWSURL")));
            NpcdbPort npcdbServicePort = (NpcdbPort) npcdbService.getNpcdbSoap();
            
            BindingProvider bindingProvider = (BindingProvider) npcdbServicePort;
            Map requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.USERNAME_PROPERTY, MnpHelper.props.getProperty(usernameSubProperty));
            requestContext.put(BindingProvider.PASSWORD_PROPERTY, MnpHelper.props.getProperty(passwordSubProperty));

            return npcdbServicePort;
        } catch (Exception ex) {
            log.error("Error while trying to connect to NPCDB [{}]", ex.getMessage());
            throw ex;
        }
    }
    
    public NpcdbPort getNPCDBConnectionAsDonor(PortInEvent context)  throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NPCDB web service");
        
        boolean emergencyRestoreInTestMode = false;
        
        String usernameSubProperty = "SmileTestNpcdbWSUsername";
        String passwordSubProperty = "SmileTestNpcdbWSPassword";
        
        if(context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty()) {
            // This port is related to an emergency restore
            if(BaseUtils.getBooleanProperty("env.mnp.emergency.restore.test.mode", false)) {
                log.debug("Porting order {} is related to an Emergency Restore Id {}, and Emergency Restore is configured to run in test mode.", context.getPortingOrderId(), context.getEmergencyRestoreId());
                // Emergency restore should be run in test more. Swap the Smile Profiles (Donor becomes Receipient and vice Versa)
                // When testing - the donor under emergency restore becomes the recipient.
                usernameSubProperty = "NpcdbWSUsername";
                passwordSubProperty = "NpcdbWSPassword";
                emergencyRestoreInTestMode = true;
            }
        }
                                                    
            
        java.net.Authenticator myAuth;
        
        if(emergencyRestoreInTestMode) { //Recipient
            myAuth = new java.net.Authenticator() {
            @Override
              protected java.net.PasswordAuthentication getPasswordAuthentication() {
              return new java.net.PasswordAuthentication(MnpHelper.props.getProperty("NpcdbWSUsername"), 
                                                         MnpHelper.props.getProperty("NpcdbWSPassword").toCharArray());
              }
            };
        } else {
            myAuth = new java.net.Authenticator() {
            @Override
              protected java.net.PasswordAuthentication getPasswordAuthentication() {
              return new java.net.PasswordAuthentication(MnpHelper.props.getProperty("SmileTestNpcdbWSUsername"), 
                                                         MnpHelper.props.getProperty("SmileTestNpcdbWSPassword").toCharArray());
              }
            }; 
        }
            
        java.net.Authenticator.setDefault(myAuth);

        try {
            NpcdbService npcdbService = new NpcdbService(new URL(MnpHelper.props.getProperty("NpcdbWSURL")));
            NpcdbPort npcdbServicePort = (NpcdbPort) npcdbService.getNpcdbSoap();
            
            BindingProvider bindingProvider = (BindingProvider) npcdbServicePort;
            Map requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.USERNAME_PROPERTY, MnpHelper.props.getProperty(usernameSubProperty));
            requestContext.put(BindingProvider.PASSWORD_PROPERTY, MnpHelper.props.getProperty(passwordSubProperty));

            return npcdbServicePort;
        } catch (Exception ex) {
            log.error("Error while trying to connect to NPCDB [{}]", ex.getMessage());
            throw ex;
        }
        
    }
    
    
    private static final Logger log = LoggerFactory.getLogger(TZPortInState.class);
    
    }
