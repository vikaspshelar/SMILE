/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.tz;

import com.smilecoms.am.db.model.RingfencedNumber;
import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.InvalidStateTransitionException;
import com.smilecoms.am.np.MnpHelper;
import static com.smilecoms.am.np.tz.TZPortInState.NP_ACTIVATED;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.direct.am.PlatformString;
import com.smilecoms.commons.sca.direct.bm.PlatformContext;
import com.smilecoms.commons.sca.direct.bm.PortingData;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PortInEvent;
import com.smilecoms.xml.schema.am.RoutingInfo;
import java.math.BigInteger;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.xml.ws.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.systor.np.commontypes.DonorReject;
import st.systor.np.commontypes.DonorRejectList;
import st.systor.np.commontypes.SubscriptionType;
import st.systor.np.npcdb.NPDeactivatedToNpcdbType;
import st.systor.np.npcdb.NPDonorAcceptToNpcdbType;
import st.systor.np.npcdb.NPDonorRejectToNpcdbType;
import st.systor.np.npcdb.NPEmergencyRestoreToNpcdbType;
import st.systor.np.npcdb.NpcdbAccessFault;
import st.systor.np.npcdb.NpcdbMessageAckType;
import st.systor.np.npcdb.NpcdbPort;
import st.systor.np.npcdb.NpcdbReject;
import st.systor.np.npcdb.NpcdbService;
import st.systor.np.npcdb.NpcdbTechnicalFault;

/**
 *
 * @author mukosi
 This class will represent all the states for a port in request,
 a port in request is when a customer request for smile to initiate a request to migrate them from their
 * current network into the Smile network. 
 * this class will also code the transitions between the states.
 */

public enum TZPortOutState implements IPortState {
    
    NP_START  {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) throws Exception {
           setContext(context);
           
           if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST.value())) { // Handle port-out request here.
                        // Porting Out
                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                        getContext().setErrorCode("0");
                        getContext().setErrorDescription("");
                        st.systor.np.commontypes.PhoneNumberRange rejectedPhoneNumberRange = new st.systor.np.commontypes.PhoneNumberRange();
                
                        List<RingfencedNumber> ringfencedNumbersList = new ArrayList<>();
                        
                        // Check if this is a port-out request for a customer or organisation?
                        if (context.getCustomerType().equalsIgnoreCase(TZPortingHelper.CUSTOMER_TYPE_INDIVIDUAL)) {// Individual customer porting out.
                                // Verify if subscription type is Prepaid - Smile only supports prepaids.
                                if(!getContext().getSubscriptionType().equalsIgnoreCase(SubscriptionType.PREPAID.value())) {
                                    String errorMessage = String.format("Invalid subscription type (%s) specified for port-out order (%s), Smile only supports PREPAID currently.", getContext().getSubscriptionType(), getContext().getPortingOrderId());
                                    log.error(errorMessage);
                                    getContext().setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_1);
                                    getContext().setErrorDescription(errorMessage);
                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);

                                } else { // We can continue - SubscribtionType is PREPAID 
                                         // Check that all the numbers exist on Smile network and they belong to the identified customer.
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
                                                log.error(errorMessage);
                                                getContext().setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_4);
                                                getContext().setErrorDescription(errorMessage);
                                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                                break forEachRoutingInfo; // Exit from all loops as soon as we encounter this problem
                                            }   
                                            
                                            log.debug("Checking if MSISDN " + ndd + " exists, is active and belongs to customer with identity " + context.getIdentityNumber() + ".");
                                            siQuery = new ServiceInstanceQuery();
                                            siQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                                            siQuery.setIdentifierType("END_USER_SIP_URI");
                                            siQuery.setIdentifier(impu);
                                            siList = SCAWrapper.getAdminInstance().getServiceInstances(siQuery);
                                            if (siList.getServiceInstances() == null || 
                                                    siList.getServiceInstances().isEmpty() || 
                                                    (siList.getNumberOfServiceInstances() < 1)) { // Number does not exist on Smile Network
                                                log.error("The requested port-out number ({}) does not exist on Smile network.", impu);
                                                getContext().setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_1);
                                                getContext().setErrorDescription("The requested MSISDN " + ndd + " is not a valid number on Smile network.");
                                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                                break forEachRoutingInfo; // Exit from all loops as soon as we encounter this issue
                                            } else {
                                                
                                                // Check if this product belongs to  an organisation
                                                log.debug("Checking if the product for service instance {} belongs to an organisation?", siList.getServiceInstances().get(0).getServiceInstanceId());
                                                ProductInstance pi = SCAWrapper.getAdminInstance().getProductInstance(siList.getServiceInstances().get(0).getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
                                                
                                                if(pi.getOrganisationId() > 0) {
                                                   String errorMessage = String.format("Product instance (%s) for IMPU (%s) and service instance (%s) belongs to organisation (%s), porting of services belonging to organisations is not supported at this point. Port Order ID: %s", 
                                                                                        pi.getProductInstanceId(), impu, siList.getServiceInstances().get(0).getServiceInstanceId(), pi.getOrganisationId(), getContext().getPortingOrderId());
                                                   log.error(errorMessage);
                                                   throw new Exception (errorMessage);
                                                } else {
                                                 
                                                getContext().setCustomerProfileId(siList.getServiceInstances().get(0).getCustomerId());
                                                for (ServiceInstance si : siList.getServiceInstances()) {
                                                    if(!si.getStatus().equals("AC")) {//Check that the SI is active
                                                        String errorMessage = String.format("The service instance %d associated with the port-out number %s is not active, it is in status %s.", si.getServiceInstanceId(), Utils.getFriendlyPhoneNumber(impu), si.getStatus());
                                                        log.error(errorMessage);
                                                        getContext().setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_4);
                                                        getContext().setErrorDescription(errorMessage);
                                                        getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                                        break forEachRoutingInfo; // Exit from all loops as soon as we encounter this problem
                                                    } else { // All good - number is clear for porting out.
                                                        //Update serviceInstanceID on this routing information.
                                                        log.error("Setting routing info [{} - {}]'s service instance id to {}", new  Object[]{ routingInfo.getPhoneNumberRange().getPhoneNumberStart(),
                                                                routingInfo.getPhoneNumberRange().getPhoneNumberStart(),
                                                                si.getServiceInstanceId()});
                                                        routingInfo.setServiceInstanceId(si.getServiceInstanceId());
                                                        // If we are testing, we must delete the service instance here so that we can free-up the number to allow the 
                                                        // activation on the donor accept leg.
                                                        //If we are in testing mode, then append .testing.com so that HSS can allow identity update
                                                        if(BaseUtils.getBooleanProperty("env.mnp.test.mode", false)) {
                                                            log.debug("MNP is running in test mode, also delete service instance {} before sending NPDonorAccept message.", si.getServiceInstanceId());
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
                                                            
                                                            }
                                                        } 
                                                    }
                                                }
                                                }
                                            }
                                        }
                                } 
                            }else // Organisation Porting Out
                            if (context.getCustomerType().equalsIgnoreCase(TZPortingHelper.CUSTOMER_TYPE_ORGANISATION)) {
                                // TODO 
                            }
                        
                            // If we are on the emergency restore, then always do a NPDonorAccept, the 
                            // spec says NPDonorReject is not allowed during emergency restore.
                            if(TZPortingHelper.isEmergencyRestore(context)
                               && !getContext().getProcessingStatus().equals(MnpHelper.REQUEST_PROCESSING_STATUS_DONE)){
                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                            }
                            
                            
                            if(getContext().getProcessingStatus().equals(MnpHelper.REQUEST_PROCESSING_STATUS_DONE)) {
                                log.debug("The port-out request with port order id {} has been accepted by Smile", getContext().getPortingOrderId());
                                // Generate and send a Donor Accept message to NPCDB
                                NPDonorAcceptToNpcdbType donorAccept = new NPDonorAcceptToNpcdbType();
                                
                                donorAccept.setSenderID(getSenderId(context));
                                donorAccept.setNPOrderID(Long.parseLong(getContext().getPortingOrderId()));
                                NpcdbMessageAckType npcdbResponse = null;
                                
                                //Clear ringfencing
                                for(RingfencedNumber rfN: ringfencedNumbersList) {
                                    MnpHelper.clearRingfencedNumber(em, rfN);
                                }
                                
                                try {
                                    
                                    npcdbResponse = getNPCDBConnectionAsDonor(getContext()).sendNPDonorAccept(donorAccept);
                                    return initNextState(NP_AWAITING_RECIPIENT_ACTIVATION, getContext());
                                } catch (Exception ex) {
                                    log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), Utils.getStackTrace(ex));
                                    getContext().setErrorCode(getNpcdbErrorCode(ex));
                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                    getContext().setErrorDescription(ex.getMessage());
                                    return initNextState(NP_DONOR_ACCEPT_REJECTED, getContext());
                                } finally {
                                    MnpHelper.createEvent(getContext().getPortingOrderId(), "NPDonorAcceptToNpcdbType",  Utils.marshallSoapObjectToString(donorAccept), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
                                }
                                
                            } else {
                                log.debug("The port-out request with port order id {} has been rejected, due to reason ({}).", getContext().getPortingOrderId(), getContext().getErrorCode() + " - " + getContext().getErrorDescription());
                                // Generate and send a Donor Reject message to NPCDB
                                NPDonorRejectToNpcdbType donorReject = new NPDonorRejectToNpcdbType();
                                NpcdbMessageAckType  npcdbResponse = null;
                                
                                DonorRejectList drList = new DonorRejectList();
                                DonorReject drListEntry = new DonorReject();
                                drListEntry.setRejectedPhoneNumberRange(rejectedPhoneNumberRange);
                                drListEntry.setDonorRejectReason(new BigInteger(getContext().getErrorCode()));
                                drListEntry.setDonorRejectMessage(getContext().getErrorDescription());
                                drList.getDonorReject().add(drListEntry);
                                
                                donorReject.setDonorRejectList(drList);
                                
                                donorReject.setNPOrderID(Long.parseLong(getContext().getPortingOrderId()));
                                donorReject.setSenderID(getSenderId(context));
                                try {
                                    npcdbResponse = getNPCDBConnectionAsDonor(getContext()).sendNPDonorReject(donorReject);
                                    return initNextState(NP_FAILED, getContext());
                                } catch(Exception ex) {
                                    log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), Utils.getStackTrace(ex));
                                    getContext().setErrorCode(getNpcdbErrorCode(ex));
                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                    getContext().setErrorDescription(ex.getMessage());
                                    log.error("Size of routing info - {}", getContext().getRoutingInfoList().getRoutingInfo().size());
                                    return initNextState(NP_DONOR_REJECT_REJECTED, getContext());
                                } finally {
                                    MnpHelper.createEvent(getContext().getPortingOrderId(), "NPDonorRejectToNpcdbType",  Utils.marshallSoapObjectToString(donorReject), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
                                }
                                
                            }
                } else {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_START.getStateId() , context.getMessageType());
                }
        }
        @Override
        public String getStateId() {
            return "NP_START";
        }
    }, NP_AWAITING_RECIPIENT_ACTIVATION {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) throws Exception {
            log.debug("In state NP_AWAITING_RECIPIENT_ACTIVATION");
            setContext(context);
            // Do nothing
            if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_OUT_RECIPIENT_ACTIVATED.value())
                        || context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_CANCEL_TO_SP.value()))) {
                    throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_ACTIVATED.getStateId(), context.getMessageType());
                }
            if (context.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_OUT_RECIPIENT_ACTIVATED.value())) { // Handle port-out request here.
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
                        //No need to do this logic if we are testing smile-to-smile
                        //If we are testing, do not deactivate here...deactivation was already done under NP_START above.
                        if(BaseUtils.getBooleanProperty("env.mnp.test.mode", false)) {
                            log.debug("MNP is running in test-mode so no need to validate and deactivate service instance on port-out deactivation, it has been deacticated under port-out's NP_START.");
                        } else {
                            log.debug("Retrieve service instance associated with MSISDN " + ndd + ".");
                            siQuery = new ServiceInstanceQuery();
                            siQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
                            siQuery.setIdentifierType("END_USER_SIP_URI");
                            siQuery.setIdentifier(impu);
                            siList = SCAWrapper.getAdminInstance().getServiceInstances(siQuery);
                            if (siList.getNumberOfServiceInstances() < 1) { // Number does not exist on Smile Network - nothing to deactivate
                                String errorMessage = String.format("No active service instance was found to be associated with the ported-out number - %s", impu);
                                log.error(errorMessage);
                                getContext().setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_1);
                                getContext().setErrorDescription(errorMessage);
                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                return initNextState(NP_DEACTIVATION_FAILED, getContext());
                            } else {

                                if(siList.getNumberOfServiceInstances() > 1) {
                                    String errorMessage = String.format("Error while attempting to deactivate ported-out number - too many service instances associated with IMPU - %s, count = %d", impu, siList.getNumberOfServiceInstances());
                                    log.error(errorMessage);
                                    getContext().setErrorCode("1000");
                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                    getContext().setErrorDescription(errorMessage);
                                    return initNextState(NP_DEACTIVATION_FAILED, getContext());
                                }
                                // On Porting Out - we are setting the status of the current service to TD and then 
                                // set its IMPU to any available number - this is to allow for emergency restore in the event
                                // a request to retore the ported out service is made.
                                ServiceInstance si = siList.getServiceInstances().get(0);
                                ProductOrder order = new ProductOrder();
                                
                                log.debug("MNP going to delete service instance {} before sending NPDonorAccept message.", si.getServiceInstanceId());
                                order.setProductInstanceId(si.getProductInstanceId());
                                order.setAction(StAction.NONE);
                                order.setCustomerId(si.getCustomerId());
                                order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
                                order.getServiceInstanceOrders().get(0).setAction(StAction.DELETE);
                                order.getServiceInstanceOrders().get(0).setServiceInstance(si);

                                SCAWrapper.getAdminInstance().processOrder(order);
                                // Leave number as issued so it cannot be allocated here ....
                                PlatformString numToIssue = new PlatformString();
                                numToIssue.setString(impu);
                                SCAWrapper.getAdminInstance().issueNumber_Direct(numToIssue);
                                
                                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                                /*if (si.getAVPs() != null) {
                                    for (AVP avp : si.getAVPs()) {
                                        if (avp != null && avp.getAttribute() != null) {
                                            
                                            if (avp.getAttribute().equalsIgnoreCase("CanRegister")) { // Disable the service so it cannot register on the Network.
                                                avp.setValue("0");
                                            }
                                        
                                        /* Not used for now  - the following code would assign a random number to a ported-out service. 
                                        
                                            if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                                               
                                                // Call SCA to get the next available number to use
                                                log.debug("This order has a public identity change. Checking if it is a golden number");
                                                NumbersQuery nq = new NumbersQuery();
                                                nq.setResultLimit(1);
                                                nq.setPattern("");
                                                nq.setPriceLimitCents(Integer.MAX_VALUE);
                                                // Making sure we are picking a number that is available to anyone.
                                                nq.setOwnedByCustomerProfileId(0);
                                                nq.setOwnedByOrganisationId(0);
                                                log.debug("Calling SCA to obtain a randomly available number to assign to the port-out service {}", si.getServiceInstanceId());
                                                NumberList list = SCAWrapper.getAdminInstance().getAvailableNumbers(nq);
                                                if (list.getNumberOfNumbers() != 1) {
                                                    String errorMessage = String.format("Failed to obtain random available number to allocade to port-out service - %s", si.getServiceInstanceId());
                                                    log.error(errorMessage);
                                                    getContext().setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_1);
                                                    getContext().setErrorDescription(errorMessage);
                                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                                    return initNextState(NP_DEACTIVATION_FAILED, getContext());
                                                } else {//We got a random number to allocate to this service.
                                                    log.debug("The IMPU for port-out service instance {} will be changed to {}", list.getNumbers().get(0).getIMPU());
                                                    avp.setValue(list.getNumbers().get(0).getIMPU());
                                                } 
                                            } */
                                       /* } 
                                    }
                                } */
                                
                            }
                        }
                    }
                }
                
                NpcdbMessageAckType npcdbResponse = null;
                NPDeactivatedToNpcdbType npDeac = null;
                
                try {
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                // Send NPDeactivated to clearing house.
                npDeac = new NPDeactivatedToNpcdbType();
                npDeac.setNPOrderID(Long.parseLong(getContext().getPortingOrderId()));
                npDeac.setSenderID(getSenderId(context));
                                
                npcdbResponse = getNPCDBConnectionAsDonor(getContext()).sendNPDeactivated(npDeac);
                } catch (Exception ex) {
                                    log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), Utils.getStackTrace(ex));
                                    getContext().setErrorCode(getNpcdbErrorCode(ex));
                                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                                    getContext().setErrorDescription(ex.getMessage());   
                } finally {
                    MnpHelper.createEvent(getContext().getPortingOrderId(), "NPDeactivatedToNpcdbType",  Utils.marshallSoapObjectToString(npDeac), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
                }
                return initNextState(NP_DEACTIVATED, getContext());
            } else
            if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_CANCEL_TO_SP.value())) { // Handle port-out request here.
                getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR);
                return initNextState(NP_CANCELLED, context);
            } else {
                throw new InvalidStateTransitionException(true, context.getPortingOrderId(), this.getStateId() , context.getMessageType());
            }
        } 
        
        @Override
        public String getStateId() {
            return "NP_AWAITING_RECIPIENT_ACTIVATION";
        }
   }, NP_CANCELLED {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_CANCELLED";
        }
    }, NP_DEACTIVATED {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
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
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_EMERGENCY_RESTORE_DENIED";
        }
    },NP_EMERGENCY_RESTORE_APPROVED {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_EMERGENCY_RESTORE_APPROVED";
        }
    }, NP_AWAITING_EMERGENCY_RESTORE_ID {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
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
           }
            return this; //Stay here ...
        } 
        
        @Override
        public String getStateId() {
            return "NP_AWAITING_EMERGENCY_RESTORE_ID";
        }
    }, NP_DONE {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) throws Exception {
            setContext(context);
            NpcdbMessageAckType npcdbResponse = null;
            NPEmergencyRestoreToNpcdbType request = null;
            
            try {
                // Terminal state, no transition beyond this state ...
                // While we are in the NP_DONE state - we allow transition to return number to the range holder (NPReturn message)
                if ((context.getMessageType() == null) || !(context.getMessageType().equals(StMNPMessageTypes.SMILE_REQUEST_EMERGENCY_RESTORE_ID.value())
                                                         || context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value()))) {
                        throw new InvalidStateTransitionException(true, context.getPortingOrderId(), NP_DONE.getStateId(), context.getMessageType());
                }

                if(context.getMessageType().equals(StMNPMessageTypes.NPCDB_EXECUTE_BROADCAST_TO_SP.value())) {
                    /* Sometimes, when in testin mode, the execute broadcast is received twice because same URL is used for both Donor and Receipient legs.
                       Do nothing here and stay in NP_DONE */
                    getContext().setErrorDescription("Execute Broadcast Received while in NP_DONE state.");
                    getContext().setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    return initNextState(NP_DONE, getContext()); 
                } else
                if (context.getMessageType().equals(StMNPMessageTypes.SMILE_REQUEST_EMERGENCY_RESTORE_ID.value())) {
                    // Send request for EmergencyRestoreID to Clearing House.
                    request = new NPEmergencyRestoreToNpcdbType();
                    request.setOriginalNPOrderID(Long.valueOf(context.getPortingOrderId()));
                    request.setSenderID(getSenderId(context));
                    npcdbResponse = getNPCDBConnectionAsDonor(getContext()).sendNPEmergencyRestore(request);
                    //All good - request for emergency restore was successfully submitted to Clearing House.
                    context.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE);
                    return initNextState(NP_AWAITING_EMERGENCY_RESTORE_ID, getContext()); 
                    
                } 
            } catch (Exception ex)  {
               log.error("Error while handling message {}, error [{}]", getContext().getMessageType(), Utils.getStackTrace(ex));
               log.warn("Error: ", ex);
               
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
                MnpHelper.createEvent(getContext().getPortingOrderId(), "NPEmergencyRestoreToNpcdbType",  Utils.marshallSoapObjectToString(request), Utils.marshallSoapObjectToString(npcdbResponse), getContext().getErrorDescription());
            }
          
           return initNextState(this, getContext()); // Stay in here - don't move...
        }        
        @Override
        public String getStateId() {
            return "NP_DONE";
        }
   }, NP_DEACTIVATION_FAILED {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
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
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
            setContext(context);
            // Do nothing
            return this; //Stay here
        } 
        
        @Override
        public String getStateId() {
            return "NP_FAILED";
        }
   }, NP_DONOR_ACCEPT_REJECTED {
        @Override
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
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
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
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
        public TZPortOutState nextState(EntityManager em, PortInEvent context) {
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
    
    public void setContext(PortInEvent req) {
        context = req;
    }
    
    public TZPortOutState initNextState(TZPortOutState nextState, PortInEvent context) {
            nextState.setContext(context);
            return nextState;
    }
    
    public String getNpcdbErrorCode(Exception ex) {
        if(ex instanceof NpcdbAccessFault) {
            NpcdbAccessFault naf = (NpcdbAccessFault) ex;
            return naf.getFaultInfo().getErrorCode();
        } else 
        if(ex instanceof NpcdbTechnicalFault) {
            NpcdbTechnicalFault ntf = (NpcdbTechnicalFault) ex;
            return ntf.getFaultInfo().getErrorCode();
        } else 
            if(ex instanceof NpcdbReject) {
                NpcdbReject nr = (NpcdbReject) ex;
                return nr.getFaultInfo().getErrorCode();
            }
        return "";
    }
    
    @Override
    public NpcdbPort getNPCDBConnection()  throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NPCDB web service");
        
        boolean emergencyRestoreInTestMode = false;
        
        String usernameSubProperty = "NpcdbWSUsername";
        String passwordSubProperty = "NpcdbWSPassword";
        
        if(context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty()) {
            // This port is related to an emergency restore
            if(BaseUtils.getBooleanProperty("env.mnp.emergency.restore.test.mode", false)) {
                log.debug("Porting order {} is related to an Emergency Restore Id {}, and Emergency Restore is configured to run in test mode.", context.getPortingOrderId(), context.getEmergencyRestoreId());
                // Emergency restore should be run in test more. Swap the Smile Profiles (Donor becomes Receipient and vice Versa)
                usernameSubProperty = "SmileTestNpcdbWSUsername";
                passwordSubProperty = "SmileTestNpcdbWSPassword";
                emergencyRestoreInTestMode = true;
            }
        }
                                                    
            
        java.net.Authenticator myAuth;
        
        if(emergencyRestoreInTestMode) {
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
        
        String usernameSubProperty = "NpcdbWSUsername";
        String passwordSubProperty = "NpcdbWSPassword";
        
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
            if(BaseUtils.getBooleanProperty("env.mnp.test.mode", false)) {
                usernameSubProperty = "SmileTestNpcdbWSUsername";
                passwordSubProperty = "SmileTestNpcdbWSPassword";
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
    
    
    
    private static String getSenderId(PortInEvent context) {
        String senderId = null;
        if(BaseUtils.getBooleanProperty("env.mnp.emergency.restore.test.mode", false) 
           && (context.getEmergencyRestoreId() != null && !context.getEmergencyRestoreId().isEmpty())) {
            // This port is related to an emergency restore
            log.debug("Porting order {} is related to an Emergency Restore Id {}, and Emergency Restore is configured to run in test mode.", context.getPortingOrderId(), context.getEmergencyRestoreId());
            senderId = MnpHelper.props.getProperty("SmileMNPParticipatingId");
        } else {
            if(BaseUtils.getBooleanProperty("env.mnp.test.mode", false)) {
                senderId = MnpHelper.props.getProperty("SmileTestMNPParticipatingId");
            } else {
                senderId = MnpHelper.props.getProperty("SmileMNPParticipatingId");
            }
        }
        log.debug("SenderId to use will be {}.", senderId);
        return senderId;           
    }
    
    private static final Logger log = LoggerFactory.getLogger(TZPortOutState.class);
    
    }
