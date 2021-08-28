/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.tz;

import com.smilecoms.am.db.model.MnpPortRequest;
import com.smilecoms.am.db.op.DAO;
import com.smilecoms.am.np.IPortInStateMachine;
import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PortInEvent;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class TZIPortInStateMachineImpl implements IPortInStateMachine {
    
    private static final Logger log = LoggerFactory.getLogger(TZIPortInStateMachineImpl.class);
    
    private static String MNP_PORTING_DIRECTION = MnpHelper.MNP_PORTING_DIRECTION_IN;
    
    
    private TZPortInState currentState;
   
    
    @Override
    public void init() {
       
    }
        
    @Override
    public TZPortInState getState(String id) {
       
        if (id == null) {
            return null;
        }
        
        for (TZPortInState position : TZPortInState.values()) {
            if (id.equals(position.getStateId())) {
                return position;
            }
        }
        
        throw new IllegalArgumentException("No matching state for id " + id);
    }
        
    @Override
    public PortInEvent handleState(EntityManager em, PortInEvent event) throws Exception {
        // Handle porting in requests...
        try {
            event.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE); //Assume done
            
            log.debug("Port order has [{}] forms to be saved ...", event.getPortRequestForms().size());
            log.debug("Port order has [{}] RingFenceNumbers to be considered.", event.getRingFenceNumberList().size());

            // Handle porting in requests...
            log.debug("TZPortInState - Entering handleState with MessageType {}", event.getMessageType());
            if(event.getMessageType().equals(StMNPMessageTypes.SMILE_NEW_PORTIN_REQUEST.value())) { // Trigger for new porting request.
                currentState = (TZPortInState) TZPortInState.NP_NEW_REQUEST.nextState(em, event);
                if(currentState == TZPortInState.NP_REQUEST_REJECTED_BY_NPCDB) {
                    return currentState.getContext();
                }
            } else {            
                // Else order exists, retrieve current state, then transition to the next state and persist.
                // Retrieve the current porting request
                MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, event.getPortingOrderId(), MNP_PORTING_DIRECTION);

                if(mnpPortRequest != null) {
                    log.debug("TZPortInState - found existing entry for PortOrderId {} and in state {}", mnpPortRequest.getMnpPortRequestPK().getPortingOrderId(), mnpPortRequest.getNpState().getStateId());
                    //For testing purposes only - Force the DEFAULT state if porting out ... (to be removed before go-live)
                        MnpHelper.synchDBPortRequestToPortInEvent(event, mnpPortRequest);
                        currentState = (TZPortInState) mnpPortRequest.getNpState();
                        currentState =  (TZPortInState) currentState.nextState(em, event);
                } else { // mnpPortRequest == null //Go to the DEFAULT state ... to handle once off messages (i.e. messages without transition states, like broadcast notifications)
                    log.debug("TZPortInState - could  not find existing entry for PortOrderId {} and porting direction {}.", event.getPortingOrderId(), MNP_PORTING_DIRECTION);
                    // Check if there is an entry for the OUT direction in which case this will be a wrong message sequence.
                    MnpPortRequest mnpPortRequestOut = DAO.getMnpPortRequest(em, event.getPortingOrderId(), "OUT");
                    if(mnpPortRequestOut != null) {
                        // Incorrect Message Sequence (A message on the Input State Machine was received while the request is being processed on the Outbound side)
                        String message = "Incorrect Message Sequence: A message (with id " + event.getMessageId() + ") on the Input State Machine was received while the request is being processed on the Outbound side (Port Order Id:" + event.getPortingOrderId() + ")";
                        
                        log.error("Error while handling inboung event {}.", message);
                        
                        event.getValidationErrors().add(message);
                        event.setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_10);
                        event.setErrorDescription(message);
                        return event;

                    }
                    
                    currentState = (TZPortInState) TZPortInState.NP_DEFAULT.nextState(em, event);
                }
            }
            if(currentState == null) { // Nothing to save
                    log.debug("TZPortInState - handled message type {} and no state to save.", event.getMessageType());
                    return event;
            } else {
                    log.debug("TZPortInState - handled message type {} and saving new state {}", event.getMessageType(), currentState.getStateId());
                    savePortState(em, currentState);
                    return currentState.getContext();
            }
        }  catch (Exception ex) {
            
            log.error("Error while handling port order {}.", event.getPortingOrderId(), ex);
            event.getValidationErrors().add(ex.getMessage());
            event.setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_10);
            event.setErrorDescription(Utils.getStackTrace(ex));
            
            currentState = TZPortInState.NP_FAILED;
            currentState.setContext(event);
            savePortState(em, currentState);
            
            return event;
        }
    }
   
    /*@Override
    public TZPortInState getCurrentState(EntityManager em, String orderId) throws Exception {
        return (TZPortInState) DAO.getCurrentState(em, orderId);
    }*/

    @Override
    public PortInEvent savePortState(EntityManager em, IPortState state) throws Exception {
        
        TZPortInState tzState = (TZPortInState) state;
        
        
        if(state.equals(TZPortInState.NP_REQUEST_ACCEPTED_BY_NPCDB)) {
            // Create a new porting record
            MnpPortRequest dbPortInRequest = new MnpPortRequest();
            MnpHelper.synchXMLPortRequestToDBPortRequest(tzState, dbPortInRequest);
            
            DAO.savePortingRequestToDB(em, dbPortInRequest);
            
            // Now set the attached documents - if there were any attached...
            log.debug("Port order [{}] has [{}] documents to be saved ...", tzState.getContext().getPortingOrderId(), tzState.getContext().getPortRequestForms().size());
            if (!tzState.getContext().getPortRequestForms().isEmpty()) {
                    DAO.setPortOrderForms(em, tzState.getContext().getPortRequestForms(), tzState.getContext().getPortingOrderId());
            }
            return tzState.getContext();
            // Create a child record for NP_REQUEST_ACCEPTED_BY_NPCDB status.
            // DAO.addPortInRequestState(em, tzState.getContext().getPortingOrderId(), tzState.getContext().getMessageId(), tzState.getContext().getSenderId(), state);
        } else { // Just create a new state record and link it to the porting request header record
            // Update the exist porting record to the current state;
            log.debug("TZPortInState - saving state {} with port order id {}.", tzState.getStateId(), tzState.getContext().getPortingOrderId());
            MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, tzState.getContext().getPortingOrderId(), MNP_PORTING_DIRECTION);
                        
            if(mnpPortRequest == null) { //To handle cases like when an NP Broadcast request
                                         //(where Smile was not involved) is received, since no porting record would have existed in that scenario.
                 mnpPortRequest = new MnpPortRequest();
            }
            
            MnpHelper.synchXMLPortRequestToDBPortRequest(tzState, mnpPortRequest);
            
            log.error("Saving mnpPortRequest.getState().getSateId() = {}", mnpPortRequest.getNpState().getStateId());
            mnpPortRequest = DAO.savePortingRequestToDB(em, mnpPortRequest);
            
            // Now set the attached documents - if there were any attached...
            log.debug("Port order [{}] has [{}] documents to be saved ...", tzState.getContext().getPortingOrderId(), tzState.getContext().getPortRequestForms().size());
            if (!tzState.getContext().getPortRequestForms().isEmpty()) {
                    DAO.setPortOrderForms(em, tzState.getContext().getPortRequestForms(), tzState.getContext().getPortingOrderId());
            }
            // - DAO.addPortInRequestState(em, tzState.getContext().getPortingOrderId(), tzState.getContext().getMessageId(), tzState.getContext().getSenderId(), state);
            // Return the current state record.
            return MnpHelper.synchDBPortRequestToPortInEvent(tzState.getContext(), mnpPortRequest); 
        }
        
         
    }
               
    
    
        
    }
  
