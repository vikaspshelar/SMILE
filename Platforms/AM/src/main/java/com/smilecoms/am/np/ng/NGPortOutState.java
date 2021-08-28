/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.ng;

import com.smilecoms.xml.schema.am.Number;
import com.smilecoms.am.AddressManager;
import com.smilecoms.am.db.model.AvailableNumber;
import com.smilecoms.am.db.model.RingfencedNumber;
import com.smilecoms.am.db.op.DAO;
import com.smilecoms.am.np.IPortInStateMachine;
import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.InvalidStateTransitionException;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.am.np.PortInEventHandler;
import static com.smilecoms.am.np.ng.NGPortInState.NP_REQUEST_ACCEPTED_BY_NPCDB;
// import static com.smilecoms.am.np.tz.TZPortInState.NP_ACTIVATED;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.NumberList;
import com.smilecoms.commons.sca.NumbersQuery;
import com.smilecoms.commons.sca.Organisation;
import com.smilecoms.commons.sca.OrganisationList;
import com.smilecoms.commons.sca.OrganisationQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceList;
import com.smilecoms.commons.sca.ProductInstanceQuery;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAMarshaller;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.direct.am.AvailableNumberRange;
import com.smilecoms.commons.sca.direct.am.PlatformString;
import com.smilecoms.commons.sca.direct.bm.PlatformContext;
import com.smilecoms.commons.sca.direct.bm.PortingData;
import com.smilecoms.commons.sca.direct.et.EventList;
import com.smilecoms.commons.sca.direct.et.EventQuery;
import com.smilecoms.commons.sca.direct.im.CustomerList;
import com.smilecoms.commons.sca.direct.im.CustomerQuery;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PhoneNumberRange;
import com.smilecoms.xml.schema.am.PortInEvent;
import com.smilecoms.xml.schema.am.RoutingInfo;
import com.smilecoms.xml.schema.am.RoutingInfoList;
import com.telcordia.inpac.ws.NPCWebService;
import com.telcordia.inpac.ws.NPCWebServicePortType;
import com.telcordia.inpac.ws.ProcessNPCMsgRequest;
import com.telcordia.inpac.ws.ProcessNPCMsgResponse;
import com.telcordia.inpac.ws.binding.MessageHeaderType;
import com.telcordia.inpac.ws.binding.NPCData;
import com.telcordia.inpac.ws.binding.NPCMessageType;
import com.telcordia.inpac.ws.binding.PortAppRspMsgType;
import com.telcordia.inpac.ws.binding.PortDeactReqMsgType;
import com.telcordia.inpac.ws.binding.PortDeactRspMsgType;
import com.telcordia.inpac.ws.binding.PortRevDeactRspMsgType;
import com.telcordia.inpac.ws.binding.PortRevReqMsgType;
import com.telcordia.inpac.ws.binding.PortRevRspMsgType;
import com.telcordia.inpac.ws.binding.ValidationType;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.persistence.EntityManager;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 This class will represent all the states for a port in request,
 a port in request is when a customer request for smile to initiate a request to migrate them from their
 * current network into the Smile network. 
 * this class will also code the transitions between the states.
 */

public enum NGPortOutState implements IPortState {
    
    NP_START  {
        @Override
        public IPortState nextState(EntityManager em, PortInEvent context) throws Exception {
           setContext(context);
           
           if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST.value())) { // 1003 Handle port-out request here.
                        // Porting Out
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                        getContext().setErrorCode("0");
                        getContext().setErrorDescription("");
                        st.systor.np.commontypes.PhoneNumberRange rejectedPhoneNumberRange = new st.systor.np.commontypes.PhoneNumberRange();
                        int organisationId = 0;
                        
                        List<RingfencedNumber> ringfencedNumbersList = new ArrayList<>();
                        
                        // if (MnpHelper.isEmergencyRestore(context) || 
                        //    (context.getCustomerType() != null && context.getCustomerType().equalsIgnoreCase(NGPortingHelper.CUSTOMER_TYPE_INDIVIDUAL))) {// Individual customer porting out.
                        // Check if this is a port-out request for a customer or organisation?
                        // Check that all the numbers exist on Smile network and they belong to the identified customer.
                        log.debug("Processing port-out request with port order id:{}",  context.getPortingOrderId());
                        forEachRoutingInfo:
                        for (RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                            // Check each number in the range ...
                            rejectedPhoneNumberRange.setPhoneNumberStart(routingInfo.getPhoneNumberRange().getPhoneNumberStart()); // Keep track of this phone number range in case we reject it below
                            rejectedPhoneNumberRange.setPhoneNumberEnd(routingInfo.getPhoneNumberRange().getPhoneNumberEnd()); // Keep track of this phone number range in case we reject it below

                            long fromE164 = Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart());
                            long toE164 = Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd());
                            String impu = "";
                            ServiceInstanceList siList = null;
                            ServiceInstanceQuery siQuery = null;
                            for (long num = fromE164; num <= toE164; num++) {
                                String ndd = Utils.makeNationalDirectDial(String.valueOf(num));
                                impu = Utils.getPublicIdentityForPhoneNumber(ndd);
                                log.debug("Checking if MSISDN " + ndd + " is ringfenced by a difference operator?");
                                try {
                                    //Check if this number is allowed to port to this network(ringfence) checks.
                                    RingfencedNumber ringfencedNumber = MnpHelper.doRingfenceValidation(em, Utils.getFriendlyPhoneNumberKeepingCountryCode(ndd), context.getRecipientId()); 
                                    if(ringfencedNumber != null) { // Add it here so we can clear the ringfence later on
                                        ringfencedNumbersList.add(ringfencedNumber);
                                    }
                                } catch(Exception ex) {
                                    String errorMessage = ex.getMessage();
                                    log.error(errorMessage,  ex);
                                    getContext().setErrorCode(NGPortingHelper.MNP_NUMBER_RINGFENCED);
                                    getContext().setErrorDescription(errorMessage);
                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                    break forEachRoutingInfo; // Exit from all loops as soon as we encounter this issue
                                }   
                                
                                log.debug("Checking if MSISDN " + ndd + " exists and is active on Smile Network?");
                                siQuery = new ServiceInstanceQuery();
                                siQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                                siQuery.setIdentifierType("END_USER_SIP_URI");
                                siQuery.setIdentifier(impu);
                                siList = SCAWrapper.getAdminInstance().getServiceInstances(siQuery);
                                if (siList.getNumberOfServiceInstances() != 1) { // Number does not exist on Smile Network or more than 1 services using the same number.
                                    String errorMessage = String.format("The requested number %s does not exist on Smile Network.", num);
                                    log.error("The requested number ({}) does not exist on Smile network.", impu);
                                    getContext().setErrorCode(NGPortingHelper.MNP_INVALID_NUMBER_ERROR_CODE);
                                    getContext().setErrorDescription(errorMessage);
                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                    break forEachRoutingInfo; // Exit from all loops as soon as we encounter this issue
                                } else {
                                        ServiceInstance si = siList.getServiceInstances().get(0);
                                        
                                        if(!MnpHelper.isEmergencyRestore(context)) {
                                            if (context.getCustomerType().equalsIgnoreCase(MnpHelper.MNP_CUSTOMER_TYPE_CORPORATE)) { //Porting an organisation here
                                                //And only if the organisation is not set.
                                                if(organisationId == 0) {
                                                //Get the organisation details here, retrieve product and organisation info.
                                                    ProductInstanceQuery piQ = new ProductInstanceQuery();
                                                    piQ.setResultLimit(0);
                                                    piQ.setOffset(0);
                                                    piQ.setProductInstanceId(si.getProductInstanceId());                                                    
                                                    piQ.setVerbosity(StProductInstanceLookupVerbosity.MAIN);
                                                    ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(piQ); // Bring all product instances for this organisation.    

                                                    // Get the organisation
                                                    OrganisationQuery orgQuery = new OrganisationQuery();
                                                    orgQuery.setOrganisationId(pi.getOrganisationId());
                                                    orgQuery.setVerbosity(StOrganisationLookupVerbosity.MAIN);

                                                    Organisation org = SCAWrapper.getAdminInstance().getOrganisation(orgQuery);
                                                    getContext().setOrganisationId(org.getOrganisationId());
                                                    getContext().setOrganisationName(org.getOrganisationName());
                                                    getContext().setOrganisationNumber(org.getCompanyNumber());
                                                    getContext().setOrganisationTaxNumber(org.getTaxNumber());

                                                    organisationId = org.getOrganisationId();
                                                }
                                            } else
                                            if(context.getCustomerType().equalsIgnoreCase(MnpHelper.MNP_CUSTOMER_TYPE_INDIVIDUAL)) {    
                                                getContext().setCustomerProfileId(si.getCustomerId()); // Set the customer identified for this portation
                                            } else {
                                                throw new Exception("Unknown customer type value for porting out " + context.getCustomerType() + " - do not know how to proceed.");    
                                            }
                                        }

                                        if(!si.getStatus().equals("AC")) {//Check that the SI is active
                                            String errorMessage = String.format("The service instance %d associated with the port-out number %s is not active, it is in status %s.", si.getServiceInstanceId(), Utils.getFriendlyPhoneNumber(impu), si.getStatus());
                                            log.error(errorMessage);
                                            getContext().setErrorCode(NGPortingHelper.MNP_SERVICE_NOT_ACTIVE_ERROR_CODE);
                                            getContext().setErrorDescription(NGPortingHelper.MNP_SERVICE_NOT_ACTIVE_ERROR_DESC);
                                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                            break forEachRoutingInfo; // Exit from all loops as soon as we encounter this problem
                                        } else { // All good - number is clear for porting out.
                                            //Update serviceInstanceID on this routing information.
                                            log.error("Setting routing info [{} - {}]'s service instance id to {}", new  Object[]{ routingInfo.getPhoneNumberRange().getPhoneNumberStart(),
                                                    routingInfo.getPhoneNumberRange().getPhoneNumberStart(),
                                                    si.getServiceInstanceId()});

                                            routingInfo.setServiceInstanceId(si.getServiceInstanceId());
                                            //Verify SIMSwap status.
                                            String iccid = null;
                                            for (AVP avp : si.getAVPs()) {
                                                if (avp != null && avp.getAttribute() != null) {
                                                    if (avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                                                        iccid = avp.getValue();
                                                    } 
                                                }
                                            }
                                            
                                            log.info("The port order {} is associated with ICCID:{}", getContext().getPortingOrderId(), iccid);
                                            
                                            EventQuery requestObject = new EventQuery();
                                            requestObject.setEventType("IM");
                                            requestObject.setEventSubType("performSIMSwap");
                                            // Get date from seven days ago;
                                            Calendar cal = Calendar.getInstance();
                                            cal.add(Calendar.DATE, -7);
                                            requestObject.setDateFrom(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
                                            requestObject.setEventKey(iccid);
                                            requestObject.setResultLimit(1);
                                            requestObject.setDateTo(null);
                                            Date simSwapDate = null;
                                                                                        
                                            //Check if there is a simswap less that 7 days found for this SIM
                                            EventList eventList = SCAWrapper.getAdminInstance().getEvents_Direct(requestObject);
                                            
                                            if(eventList.getNumberOfEvents() > 0) { // SIM was Swapped Less Than 7-Days ago - Reject the port.
                                                
                                                com.smilecoms.commons.sca.direct.et.Event evt = eventList.getEvents().get(0);
                                                simSwapDate = Utils.getJavaDate(evt.getDate());
                                                                                            
                                                String errorMessage = String.format("The SIM ICCID %s associated with the port-out order %s was swaped on %s which is less than seven days ago.", iccid, getContext().getPortingOrderId(), simSwapDate);
                                                log.error(errorMessage);
                                                getContext().setErrorCode(NGPortingHelper.MNP_SIM_SWAP_LESS_THAN_7_DAYS_AGO_ERROR_CODE);
                                                getContext().setErrorDescription(NGPortingHelper.MNP_SIM_SWAP_LESS_THAN_7_DAYS_AGO_ERROR_DESC);
                                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                                break forEachRoutingInfo; // Exit from all loops as soon as we encounter this problem
                                            } else {
                                                String eMessage = String.format("The SIM ICCID %s associated with the port-out order %s was not swaped in the last seven days.", iccid, getContext().getPortingOrderId(), simSwapDate);
                                                log.info(eMessage);
                                            }
                                        }
                                }
                            }
                        }
                        
                        //Save ringfence numbers, if specified here.
                        MnpHelper.saveRingFencedNumberList(em, context.getRingFenceNumberList(), context.getRecipientId());
                        
                        // If we are on the emergency restore, then always do a NPDonorAccept, the 
                        // spec says NPDonorReject is not allowed during emergency restore.
                        if(MnpHelper.isEmergencyRestore(context)
                           && !getContext().getProcessingStatus().equals(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS)){
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                        } 

                        // Populate Porting Approval Response here (Message 1004) 
                        NPCWebServicePortType connection = getNPCDBConnection();
                        ProcessNPCMsgRequest request = new ProcessNPCMsgRequest();
                        String messageType = "";
                        
                        request.setPassword(Utils.encodeBase64(MnpHelper.props.getProperty("NpcdbWSPassword").getBytes())); // ICN Password must be Base64 encoded.
                        request.setUserID(MnpHelper.props.getProperty("NpcdbWSUsername"));

                        NPCData requestData = new NPCData();
                        // Do the header bits here.
                        MessageHeaderType header = new MessageHeaderType();
                        header.setSender(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // SML for Smile in Nigeria

                        header.setHeaderID(getNewHeaderID());
                        header.setTransactionID(context.getPortingOrderId());

                        NPCMessageType npcMessage = new NPCMessageType();


                        header.setPortType(MnpHelper.props.getProperty("NpcdbMobilePortingType"));
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                        header.setMsgCreateTimestamp(sdf.format(new Date()));
                        requestData.setMessageHeader(header);

                        if(MnpHelper.isEmergencyRestore(context)) {
                            messageType = NGPortingHelper.MSG_ID_PORTING_REVERSAL_APPROVAL_RESPONSE;
                            header.setMessageID(messageType); // Send 3004
                            // Do the message bits here ...
                            PortRevRspMsgType portingMessage = new PortRevRspMsgType();
                            portingMessage.setDonor(context.getDonorId());
                            portingMessage.setRecipient(context.getRecipientId()); // Smile's MNP participating ID

                            if(getContext().getProcessingStatus().equals(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS)) {
                                log.debug("The port-out request with port order id {} has been accepted by Smile", getContext().getPortingOrderId());
                                // Set Flag Reasons to Y to show Donor Accepted.
                                portingMessage.getNumbersWithFlag().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDBNumberListWithFlag(context.getRoutingInfoList(), "Y", null));
                            } else {
                                // Set Flag Reasons to 'N' and Reason Code to the error that caused the failure.
                                portingMessage.getNumbersWithFlag().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDBNumberListWithFlag(context.getRoutingInfoList(), "N", getContext().getErrorCode()));
                            }                                
                            npcMessage.setPortReversalRsp(portingMessage);
                        } else { // Normal Port-out
                            messageType = NGPortingHelper.MSG_ID_PORTING_APPROVAL_RESPONSE;
                            header.setMessageID(messageType); // Send 1004
                            // Do the message bits here ...
                            PortAppRspMsgType portingMessage = new PortAppRspMsgType();
                            portingMessage.setSubType(NGPortingHelper.mapCustomerType(context.getCustomerType())); // Mandatory 0 (Individual) or 1 (Corporate)
                            portingMessage.setDonor(context.getDonorId());
                            portingMessage.setRecipient(context.getRecipientId()); // Smile's MNP participating ID
                            portingMessage.setPortReqFormID(context.getPortRequestFormId()); // Set it to the port request form id that was sent in.
                            
                            if(getContext().getProcessingStatus().equals(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS)) {
                                log.debug("The port-out request with port order id {} has been accepted by Smile", getContext().getPortingOrderId());
                                // Set Flag Reasons to Y to show Donor Accepted.
                                portingMessage.getNumbersWithFlagReason().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDBNumberListWithFlagReason(context.getRoutingInfoList(), "Y", null));
                            } else {
                                // Set Flag Reasons to 'N' and Reason Code to the error that caused the failure.
                                portingMessage.getNumbersWithFlagReason().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDBNumberListWithFlagReason(context.getRoutingInfoList(), "N", getContext().getErrorCode()));
                            }                                
                            npcMessage.setPortApprovalRsp(portingMessage);
                        }
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
                             response = connection.processNPCMsg(request);
                            Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);

                            SCAMarshaller.logObject(response, "XML response from ICN:");

                            NPCData npcData = NGPortingHelper.getNPCDataFromResponse(response.getResponse()); 

                            if(npcData.getNPCWebServiceResponse().getResponse().equals("Success")) { // 1004 (success) All good ...
                                getContext().setMessageId(Long.valueOf(npcData.getMessageHeader().getMessageID()));
                                // All is good, now wait for port deactivation request.
                                // We are good to clear any ringFencing here.
                                for(RingfencedNumber rfN: ringfencedNumbersList) {
                                    MnpHelper.clearRingfencedNumber(em, rfN);
                                }
                                return initNextState(NP_AWAITING_PORT_DEACT_REQUEST, context); // Go to NP_AWAITING_PORT_DEACT_REQUEST (1007)
                            } else { // Error .... (1002 (failure))
                               getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                               // Add all errors to the context and return to SCA ...
                               String errorDesc;
                               for(ValidationType validation : npcData.getNPCWebServiceResponse().getValidation()) {
                                   errorDesc = validation.getReasonCode() + " - " + validation.getReasonDesc();
                                   getContext().getValidationErrors().add(errorDesc);
                                   getContext().setErrorCode(validation.getReasonCode());
                                   getContext().setErrorDescription(errorDesc);
                               }
                               return initNextState(NP_FAILED, getContext());
                            }

                        }  catch (Exception ex)  {
                            String errorMsg = Utils.getStackTrace(ex);

                            log.error("Error while handling message type {}.", StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST.value(), ex);
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                            getContext().getValidationErrors().add(errorMsg);
                            getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                            getContext().setErrorDescription(errorMsg);
                            getContext().setRoutingInfoList(context.getRoutingInfoList());
                            return  initNextState(NP_FAILED, context);
                        } finally {
                                MnpHelper.createEvent(getContext().getPortingOrderId(), messageType,  sw.toString(), response.getResponse(), getContext().getErrorDescription());
                            }
            } else 
           if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR.value())) {
               getContext().setErrorCode("1090");
               getContext().setErrorDescription("Violation of Port Validation Time");
               return  initNextState(this, getContext()); //Stay within this state.
           } else {
                throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_START.getStateId() , context.getMessageType());
           }
        }
        @Override
        public String getStateId() {
            return "NP_START";
        }
    }, NP_AWAITING_PORT_DEACT_REQUEST { // Invoked when the recipient has activated on their network
        @Override
        public IPortState nextState(EntityManager em, PortInEvent context) throws Exception {
            log.debug("In state NP_AWAITING_PORT_DEACT_REQUEST");
            setContext(context);
            // Do nothing
            if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATION_REQUEST.value())
                        || context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_CANCEL_TO_SP.value()) 
                        || context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR.value())
                        || context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_ACTIVATION_ERROR.value())
                        || context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_AWAITING_PORT_DEACT_REQUEST.getStateId(), context.getMessageType());
            }
            
            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR.value())) { // Donor did not respond with 1007 on time.
                   getContext().setErrorCode("1092");
                   getContext().setErrorDescription("Port Deactivate Error (1092)");
                   return  initNextState(this, getContext()); //Stay within this state.
            } else
            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_ACTIVATION_ERROR.value())) { // Recipient did not respond with 1006 on time.
                   getContext().setErrorCode("1091");
                   getContext().setErrorDescription("Violation of Port Validation Time (1091)");
                   return  initNextState(this, getContext()); //Stay within this state.
            } else
            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_APPROVAL_RESPONSE_ERROR.value())) {
                   getContext().setErrorCode("1090");
                   getContext().setErrorDescription("Violation of Port Validation Time (1090)");
                   return  initNextState(this, getContext()); //Stay within this state.
            } else 
            if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATION_REQUEST.value())) { // Handle port-out request here, got message 1007
                // Number/s have been activated on Donor's network so we must deactive on Smile's network.
                for (RoutingInfo routingInfo : context.getRoutingInfoList().getRoutingInfo()) {
                    long fromE164 = Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberStart());
                    long toE164 = Long.parseLong(routingInfo.getPhoneNumberRange().getPhoneNumberEnd());
                    String impu;
                    ServiceInstanceList siList;
                    ServiceInstanceQuery siQuery;
                    for (long num = fromE164; num <= toE164; num++) {
                        String ndd = Utils.makeNationalDirectDial(String.valueOf(num));
                        impu = Utils.getPublicIdentityForPhoneNumber(ndd);
                        log.debug("Retrieve service instance associated with MSISDN " + ndd + ".");
                        siQuery = new ServiceInstanceQuery();
                        siQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                        siQuery.setIdentifierType("END_USER_SIP_URI");
                        siQuery.setIdentifier(impu);
                        siList = SCAWrapper.getAdminInstance().getServiceInstances(siQuery);
                        if (siList.getNumberOfServiceInstances() < 1) { // Number does not exist on Smile Network - nothing to deactivate
                            String errorMessage = String.format("No active service instance was found to be associated with the ported-out number - %s", impu);
                            log.error(errorMessage);
                            // getContext().setErrorCode(NGPortingHelper.TZMNP_ERROR_CODE_1);
                            getContext().setErrorDescription(errorMessage);
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                            return initNextState(NP_DEACTIVATION_FAILED, getContext());
                        } else {

                            if(siList.getNumberOfServiceInstances() > 1) {
                                String errorMessage = String.format("Error while attempting to deactivate a ported-out number - too many service instances associated with IMPU - %s, count = %d", impu, siList.getNumberOfServiceInstances());
                                log.error(errorMessage);
                                getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                getContext().setErrorDescription(errorMessage);
                                return initNextState(NP_DEACTIVATION_FAILED, getContext());
                            }
                            // On Porting Out - we are setting the status of the current service to TD and then 
                            // set its IMPU to any available number - this is to allow for emergency restore in the event
                            // a request to retore the ported out service is made.
                            ServiceInstance si = siList.getServiceInstances().get(0);
                            ProductOrder order = new ProductOrder();
                            order.setProductInstanceId(si.getProductInstanceId());
                            order.setAction(StAction.NONE);
                            order.setCustomerId(si.getCustomerId());
                            order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
                            order.getServiceInstanceOrders().get(0).setAction(StAction.DELETE);
                            order.getServiceInstanceOrders().get(0).setServiceInstance(si);
                            
                            SCAWrapper.getAdminInstance().processOrder(order);
                                                                                                                        
                            // Leave number as issued so it cannot be allocated here ....
                            PlatformString numToFreeUp = new PlatformString();
                            numToFreeUp.setString(impu);
                            SCAWrapper.getAdminInstance().issueNumber_Direct(numToFreeUp);
                            
                            //Set Impu to blank
                            /* if (si.getAVPs() != null) {
                                for (AVP avp : si.getAVPs()) {
                                    if (avp != null && avp.getAttribute() != null) {
                                        
                                        if (avp.getAttribute().equalsIgnoreCase("CanRegister")) { // This AVP does not change because its USER_DEFINED='N' in SmileDB.
                                            // Disable the service so it cannot register on the Network.
                                            avp.setValue("0");
                                        }
                                        */
                                        /* Not used for now  - the following code would assign a random number to a ported-out service. 
                                        if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {


                                            // Call SCA to get the next available number to use
                                            log.debug("This order has a public identity change. Checking if it is a golden number");
                                            NumbersQuery nq = new NumbersQuery();
                                            nq.setResultLimit(1);
                                            nq.setPattern("");
                                            nq.setPriceLimitCents(Integer.MAX_VALUE);
                                            // Ignore ownership and show all. If its not owned by this person then processOrder will pick that up
                                            nq.setOwnedByCustomerProfileId(-1);
                                            nq.setOwnedByOrganisationId(-1);
                                            log.debug("Calling SCA to obtain a randomly available number to assign to the port-out service {}", si.getServiceInstanceId());
                                            NumberList list = SCAWrapper.getAdminInstance().getAvailableNumbers(nq);
                                            if (list.getNumberOfNumbers() != 1) {
                                                String errorMessage = String.format("Failed to obtain random available number to allocade to port-out service - %s", si.getServiceInstanceId());
                                                log.error(errorMessage);
                                               // getContext().setErrorCode(NGPortingHelper.TZMNP_ERROR_CODE_1);
                                                getContext().setErrorDescription(errorMessage);
                                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                                return initNextState(NP_DEACTIVATION_FAILED, getContext());
                                            } else {//We got a random number to allocate to this service.
                                                log.debug("The IMPU for port-out service instance {} will be changed to {}", list.getNumbers().get(0).getIMPU());
                                                avp.setValue(list.getNumbers().get(0).getIMPU());
                                            } 
                                        */
                                    /*}
                                }
                            }    
                            SCAWrapper.getAdminInstance().processOrder(order); */
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                        }
                    }
                }
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                // Send Port Deactivation Response here to clearing house - MessageID: 1009
                
                // TODO
                        
                //NPDonorAcceptToNpcdbType donorAccept = new NPDonorAcceptToNpcdbType();
                //donorAccept.setSenderID(getSenderId(context));
                //donorAccept.setNPOrderID(Long.parseLong(getContext().getPortingOrderId()));
                
                // Respond with message 1004 - Donor Accept here, all is good here
                
                NPCWebServicePortType connection = getNPCDBConnection();
                ProcessNPCMsgRequest request = new ProcessNPCMsgRequest();
                String messageType = "";
                
                request.setPassword(Utils.encodeBase64(MnpHelper.props.getProperty("NpcdbWSPassword").getBytes())); // ICN Password must be Base64 encoded.
                request.setUserID(MnpHelper.props.getProperty("NpcdbWSUsername"));

                NPCData requestData = new NPCData();
                // Do the header bits here.
                MessageHeaderType header = new MessageHeaderType();
                header.setSender(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // SML for Smile in Nigeria
                header.setHeaderID(getNewHeaderID());
                header.setTransactionID(context.getPortingOrderId());
                header.setPortType(MnpHelper.props.getProperty("NpcdbMobilePortingType"));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                header.setMsgCreateTimestamp(sdf.format(new Date()));
                requestData.setMessageHeader(header);
                NPCMessageType npcMessage = new NPCMessageType();
                
                if(MnpHelper.isEmergencyRestore(context)) {
                    messageType = NGPortingHelper.MSG_ID_PORT_REVERSAL_DEACTIVATION_RESPONSE;
                    header.setMessageID(messageType); // 3009
                    // Do the message bits here ...
                    PortRevDeactRspMsgType portingMessage = new PortRevDeactRspMsgType();
                    portingMessage.setDonor(context.getDonorId());
                    portingMessage.setRecipient(context.getRecipientId()); // Smile's MNP participating ID

                    portingMessage.getNumbers().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDB(context.getRoutingInfoList()));

                    
                    npcMessage.setPortReversalDeactRsp(portingMessage);
                    
                } else {
                    messageType = NGPortingHelper.MSG_ID_PORT_DEACTIVATION_RESPONSE;
                    header.setMessageID(messageType); // 1009
                    // Do the message bits here ...
                    PortDeactRspMsgType portingMessage = new PortDeactRspMsgType();
                    portingMessage.setSubType(NGPortingHelper.mapCustomerType(context.getCustomerType())); // Mandatory 0 (Individual) or 1 (Corporate)
                    portingMessage.setDonor(context.getDonorId());
                    portingMessage.setRecipient(context.getRecipientId()); // Smile's MNP participating ID
                    portingMessage.setPortReqFormID(context.getPortRequestFormId());
                    
                    portingMessage.getNumbers().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDB(context.getRoutingInfoList()));
                    npcMessage.setPortDeactRsp(portingMessage);
                    
                }
                
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
                     response = connection.processNPCMsg(request);
                    Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);

                    SCAMarshaller.logObject(response, "XML response from ICN:");
                    NPCData npcData = (NPCData) um.unmarshal(new StringReader(response.getResponse()));

                    log.error("Response from ICN:" + response.getResponse());
                    
                    // All is good, last message on a port out. Go to NP_DONE
                    
                    return initNextState(NP_DONE, getContext()); 
                }  catch (Exception ex)  {
                    log.error("Error while trying to submit message type {} to NPCDB.", NGPortingHelper.MSG_ID_PORT_DEACTIVATION_RESPONSE, ex);
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                    getContext().getValidationErrors().add(ex.getMessage());
                    getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                    getContext().setErrorDescription(ex.getMessage());
                    getContext().setRoutingInfoList(context.getRoutingInfoList());
                } finally {
                    MnpHelper.createEvent(getContext().getPortingOrderId(), messageType,  sw.toString(), response.getResponse(), getContext().getErrorDescription());
                }
                
                return initNextState(this, getContext());
            } else
            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_CANCEL_TO_SP.value())) { 
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                return initNextState(NP_CANCELLED, context);
            } else {
                throw new InvalidStateTransitionException(true, context.getPortingOrderId(), this.getStateId() , context.getMessageType());
            }
        } 
        
        @Override
        public String getStateId() {
            return "NP_AWAITING_PORT_DEACT_REQUEST";
        }
   }, NP_PORTOUT_REQUEST_REJECTED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            return this; // Stay here ... don't move!
        } 
        
        @Override
        public String getStateId() {
            return "NP_PORTOUT_REQUEST_REJECTED";
        }
    }, NP_CANCELLED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_CANCELLED";
        }
    }, NP_DEACTIVATED {
        @Override
        public IPortState nextState(EntityManager em, PortInEvent context) throws Exception  {
            setContext(context);
            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                // An NPExecuteBroacast is received on a number that ported away from Smile
                // At this stage the ported-out number has already been freedUp during DEACTIVATION
                // All we need to do is to update ported numbers and go to NP_DONE.
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
                return initNextState(NP_DONE, context);
            } 
            return this; // No further transition from this state
        }        
        @Override
        public String getStateId() {
            return "NP_DEACTIVATED";
        }
   },NP_EMERGENCY_RESTORE_DENIED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_EMERGENCY_RESTORE_DENIED";
        }
    },NP_EMERGENCY_RESTORE_APPROVED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_EMERGENCY_RESTORE_APPROVED";
        }
    }, NP_AWAITING_EMERGENCY_RESTORE_ID {
        @Override
        public IPortState nextState(EntityManager em, PortInEvent context) {
            log.debug("TZMNP in state NP_AWAITING_EMERGENCY_RESTORE_ID");
            setContext(context);
            try {
                
                if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EMERGENCY_RESTORE_APPROVED.value())
                                                             || context.getMessageType().equals(StMNPMessageTypes.NPCDB_EMERGENCY_RESTORE_DENIED.value()))) {
                            throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_AWAITING_EMERGENCY_RESTORE_ID.getStateId(), context.getMessageType());
                    }
                
                if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_EMERGENCY_RESTORE_APPROVED.value())) {
                    // Initiate a new port order to reverse the old one here ...
                    
                    // Retrieve customer profile to use profile fields to populate the port request.
                    com.smilecoms.commons.sca.CustomerQuery q = new com.smilecoms.commons.sca.CustomerQuery();
                    q.setCustomerId(getContext().getCustomerProfileId());
                    q.setResultLimit(1);
                    q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                    Customer customer = SCAWrapper.getAdminInstance().getCustomer(q);
                    
                    
                    com.smilecoms.commons.sca.PortInEvent  scaPortInEvent = MnpHelper.makeSCAPortInEvent(context);
                    
                    scaPortInEvent.setCustomerFirstName(customer.getFirstName());
                    scaPortInEvent.setCustomerLastName(customer.getLastName());
                    scaPortInEvent.setGender(customer.getGender());
                    scaPortInEvent.setCustomerIdType(customer.getIdentityNumberType());
                    scaPortInEvent.setIdentityNumber(customer.getIdentityNumber());
                    DateFormat format = new SimpleDateFormat("yyyyMMdd");
                    Date dOB = format.parse(customer.getDateOfBirth());
                    scaPortInEvent.setDateOfBirth(Utils.getDateAsXMLGregorianCalendar(dOB));
                    // Swap donor and recipient ...
                    scaPortInEvent.setDonorId(context.getRecipientId());
                    scaPortInEvent.setErrorCode(""); // No errors yet ...
                    scaPortInEvent.setErrorDescription("");
                    scaPortInEvent.setRecipientId(context.getDonorId());
                    scaPortInEvent.setHandleManually("false");
                    
                    scaPortInEvent.setNpState(null);
                    scaPortInEvent.setPortingDate(Utils.getDateAsXMLGregorianCalendar(new Date()));
                    scaPortInEvent.setPortingOrderId(""); // Set to blank  a new Port Order ID will be generated for the restore
                    scaPortInEvent.setMessageType(StMNPMessageTypes.SMILE_NEW_PORTIN_REQUEST);
                    scaPortInEvent.setPortingDirection(MnpHelper.MNP_PORTING_DIRECTION_IN);
                    //NB - Change the routing number of the Routing information back to Smile ...
                    for(com.smilecoms.commons.sca.RoutingInfo scaRoutingInfo : scaPortInEvent.getRoutingInfoList().getRoutingInfo()) {
                        scaRoutingInfo.setRoutingNumber(Utils.getPropertyValueFromList("env.mnp.config", "SmileNetworkRoutingNumber"));
                        scaRoutingInfo.setSelectedForPortIn(true);
                    }
                    
                    // Call into SCA ...
                    SCAWrapper.getAdminInstance().handlePortInEvent(scaPortInEvent); // Do a new portin request to bring the number back to Smile.
                    log.debug("TZMNP done state NP_AWAITING_EMERGENCY_RESTORE_ID");        
                } else
                    if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EMERGENCY_RESTORE_DENIED.value())) {
                        // Nothing much to do here ... go to ... NP_EMERGENCY_RESTORE_DENIED
                        return initNextState(NP_EMERGENCY_RESTORE_DENIED, getContext()); 
                    }
            } catch (Exception ex)  {
               
               getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
               log.error("Error while handling emergency restore ",ex);
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                getContext().getValidationErrors().add(ex.getMessage());
                getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                getContext().setErrorDescription(Utils.getStackTrace(ex));
                getContext().setRoutingInfoList(context.getRoutingInfoList());
           }
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_AWAITING_EMERGENCY_RESTORE_ID";
        }
    }, NP_DONE {
        @Override
        public IPortState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            try {
                // Terminal state, no transition beyond this state ...
                // While we are in the NP_DONE state - we allow transition to return number to the range holder (NPReturn message)
                if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.SMILE_REQUEST_EMERGENCY_RESTORE_ID.value())
                                                         || context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())
                                                         || context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR.value()))) {
                        throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_DONE.getStateId(), context.getMessageType());
                }
                
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_DEACTIVATE_ERROR.value())) { // Donor did not respond with 1007 on time.
                   getContext().setErrorCode("1092");
                   getContext().setErrorDescription("Port Deactivate Error (1092)");
                   return  initNextState(this, getContext()); //Stay within this state.
                } else
                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                    /* Sometimes, when in testin mode, the execute broadcast is received twice because same URL is used for both Donor and Receipient legs.
                       Do nothing here and stay in NP_DONE */
                    getContext().setErrorDescription("Execute Broadcast Received while in NP_DONE state.");
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    return initNextState(this, getContext()); 
                } else
                if (context.getMessageType().equals(StMNPMessageTypes.SMILE_REQUEST_EMERGENCY_RESTORE_ID.value())) {
                    // Send request 3001 to the Clearing House.
                    
                    NPCWebServicePortType connection = getNPCDBConnection();
                    ProcessNPCMsgRequest request = new ProcessNPCMsgRequest();

                    request.setPassword(Utils.encodeBase64(MnpHelper.props.getProperty("NpcdbWSPassword").getBytes())); // ICN Password must be Base64 encoded.
                    request.setUserID(MnpHelper.props.getProperty("NpcdbWSUsername"));

                    NPCData requestData = new NPCData();
                    // Do the header bits here.
                    MessageHeaderType header = new MessageHeaderType();
                    header.setSender(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // SML for Smile in Nigeria
                    header.setHeaderID(getNewHeaderID());
                    // header.setTransactionID(context.getPortingOrderId()); -- Not required.
                    
                    header.setMessageID(NGPortingHelper.MSG_ID_PORT_REVERSAL_REQUEST); // 3001
                    header.setPortType(MnpHelper.props.getProperty("NpcdbMobilePortingType"));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    header.setMsgCreateTimestamp(sdf.format(new Date()));
                    requestData.setMessageHeader(header);
                    // Do the message bits here ...
                    PortRevReqMsgType portRevReqMsg = new PortRevReqMsgType();
                    portRevReqMsg.setDonor(context.getRecipientId());
                    portRevReqMsg.setRecipient(MnpHelper.props.getProperty("SmileMNPParticipatingId")); // Smile is now a recipient in this reversal request
                    
                    // Add the ported out numbers that must be returned to Smile
                    portRevReqMsg.getNumbers().addAll(NGPortingHelper.mapXMLRoutingInformationListToNPCDB(context.getRoutingInfoList()));

                    NPCMessageType npcMessage = new NPCMessageType();
                    npcMessage.setPortReversalReq(portRevReqMsg);
                    requestData.setNPCMessage(npcMessage);

                    Marshaller m = Utils.getJAXBMarshallerForSoap(NPCData.class);
                    StringWriter sw = new StringWriter();
                    m.marshal(requestData, sw);
                    request.setXmlMsg(sw.toString());
                    // Show message being sent to NPC
                    SCAMarshaller.logObject(request, "XML being sent to ICN:");
                    // SWAP Donor and Recipient
                    getContext().setDonorId(portRevReqMsg.getDonor());
                    getContext().setRecipientId(portRevReqMsg.getRecipient());

                    ProcessNPCMsgResponse response = new ProcessNPCMsgResponse();
                    
                    try { // Send message and wait for 3002 response back - Basically this will give us a new Emergency Repatriation Request ID.
                        
                        log.debug("", Utils.marshallSoapObjectToString(request));
                        response = connection.processNPCMsg(request);
                        Unmarshaller um = Utils.getJAXBUnmarshaller(NPCData.class);

                        SCAMarshaller.logObject(response, "XML response from ICN:");
                        NPCData npcData = (NPCData) um.unmarshal(new StringReader(response.getResponse()));

                        log.error("Response from ICN:" + response.getResponse());
                                                
                        if(npcData.getNPCWebServiceResponse().getResponse().equals("Success")) { //  (success) All good ...
                            // From here, everything must be handled as a new port-in request. 
                            // Transition to the inbound state machine - to NP_REQUEST_ACCEPTED_BY_NPCDB
                            getContext().setEmergencyRestoreId(getContext().getPortingOrderId()); // Preserve the old/original porting ID in here.
                            getContext().setIsEmergencyRestore("Y");
                            getContext().setPortingOrderId(npcData.getMessageHeader().getTransactionID()); // New transaction id for this Emergency Restore
                            getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS);
                            
                            getContext().setPortingDirection(MnpHelper.MNP_PORTING_DIRECTION_IN);
                            getContext().setMessageId(Long.valueOf(npcData.getMessageHeader().getMessageID()));
                            
                           return initNextState(NP_REQUEST_ACCEPTED_BY_NPCDB, getContext()); // Note NP_REQUEST_ACCEPTED_BY_NPCDB belongs to the port-in state machine Impl
                        } else { // Error .... (1002 (failure)
                           getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                           // Add all errors to the context and return to SCA ...
                           String errorCode = "";
                           String errorDesc = "";
                           for(ValidationType validation : npcData.getNPCWebServiceResponse().getValidation()) {
                               errorCode = validation.getReasonCode();
                               getContext().getValidationErrors().add(validation.getReasonCode() + " - " + validation.getReasonDesc());
                               errorDesc +=  validation.getReasonCode() + " - " + validation.getReasonDesc() + "\r\n";
                           }
                           getContext().setErrorCode(errorCode);
                           getContext().setErrorDescription(errorDesc);
                           return initNextState(NP_FAILED, getContext());
                        }
                    }  catch (Exception ex)  {
                        log.error("Error while trying to submit message type {} to NPCDB.", NGPortingHelper.MSG_ID_PORT_REVERSAL_REQUEST, ex);
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                        getContext().getValidationErrors().add(ex.getMessage());
                        getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
                        getContext().setErrorDescription(ex.getMessage());
                        getContext().setRoutingInfoList(context.getRoutingInfoList());
                    } finally {
                                MnpHelper.createEvent(getContext().getPortingOrderId(), NGPortingHelper.MSG_ID_PORT_REVERSAL_REQUEST,  sw.toString(), response.getResponse(), getContext().getErrorDescription());
                            }
                    
                } 
            } catch (Exception ex)  {
               log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), ex.getMessage());
               getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
               getContext().getValidationErrors().add(ex.getMessage());
               getContext().setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
               getContext().setErrorDescription(Utils.getStackTrace(ex));
               getContext().setRoutingInfoList(context.getRoutingInfoList());
           }
          
           return initNextState(this, getContext()); // Stay in here - don't move...
        }        
        @Override
        public String getStateId() {
            return "NP_DONE";
        }
   }, NP_DEACTIVATION_FAILED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Terminal state, no transition beyond this state ...
            return this; // Stay in here
        }        
        @Override
        public String getStateId() {
            return "NP_DEACTIVATION_FAILED";
        }
   }, NP_FAILED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Do nothing
            log.error("ERROR: Porting Order ({}) is in NP_FAILED status, cannot transition away from this state, received messaage ({})",context.getPortingOrderId(), context.getMessageType());
            return this; //Stay here
        } 
        
        @Override
        public String getStateId() {
            return "NP_FAILED";
        }
   }, NP_DONOR_ACCEPT_REJECTED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Terminal state, no transition beyond this state ...
            return this;
        } 
        
        @Override
        public String getStateId() {
            return "NP_DONOR_ACCEPT_REJECTED";
        }
   }, NP_DONOR_REJECT_REJECTED {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Terminal state, no transition beyond this state ...
            return this; // Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_DONOR_REJECT_REJECTED";
        }
   }, NP_DEFAULT {
        @Override
        public NGPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Do nothing
            return this; // Stay here ...
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
    @Override
    public void setContext(PortInEvent req) {
        context = req;
    }
    
    public IPortState initNextState(IPortState nextState, PortInEvent context) throws Exception {
            nextState.setContext(context);
            return nextState;
    }
    
    public String getNewHeaderID() {
        Random random = new Random();
        int randomNumber = random.nextInt(NGPortingHelper.MAX_MSG_HEADER_ID); // A Random Number between 0 and NGPortingHelper.MAX_MSG_HEADER_ID
        return String.valueOf(randomNumber);
    }

    
   @Override
    public NPCWebServicePortType getNPCDBConnection() throws Exception { //Invokes web service at the NPC DB and bring back resutls
        return NGPortingHelper.getNPCDBConnection();
    }   
   
   
    
    private static String getSenderId(PortInEvent context) {
        String senderId = null;
        if(BaseUtils.getBooleanProperty("env.mnp.emergency.restore.test.mode", false) 
           && (context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty())) {
            // This port is related to an emergency restore
            log.debug("Porting order {} is related to an Emergency Restore Id {}, and Emergency Restore is configured to run in test mode.", context.getPortingOrderId(), context.getEmergencyRestoreId());
            senderId = MnpHelper.props.getProperty("SmileMNPParticipatingId");
        } else {
            senderId = MnpHelper.props.getProperty("SmileTestMNPParticipatingId");
        }
        log.debug("SenderId to use will be {}.", senderId);
        return senderId;           
    }
    
    private static final Logger log = LoggerFactory.getLogger(NGPortOutState.class);
    
    
    }
