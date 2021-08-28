/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.ng;

import com.smilecoms.am.db.model.MnpPortRequest;
import com.smilecoms.am.db.model.MnpPortRequestPK;
import com.smilecoms.am.db.op.DAO;
import com.smilecoms.am.np.IPortInStateMachine;
import com.smilecoms.am.np.IPortInStateMachine;
import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.InvalidStateTransitionException;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PortInEvent;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class NGIPortInStateMachineImpl implements IPortInStateMachine {
    
    private static String MNP_PORTING_DIRECTION = MnpHelper.MNP_PORTING_DIRECTION_IN;
    
    private static final Logger log = LoggerFactory.getLogger(NGIPortInStateMachineImpl.class);
    
    // public static Properties props = null;
    
    private NGPortInState currentState, newState;
    
    @Override
    public void init() {
        // Nothing to do yet ...
    }
        
    @Override
    public NGPortInState getState(String id) {
       
        if (id == null) {
            return null;
        }
        
        for (NGPortInState position : NGPortInState.values()) {
            if (id.equals(position.getStateId())) {
                return position;
            }
        }
        
        throw new IllegalArgumentException("No matching state for id " + id);
    }
        
    @Override
    public PortInEvent handleState(EntityManager em, PortInEvent event) throws Exception {
        try {
            
            event.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_IN_PROGRESS); //Assume in progress, the state will set this before returning.
            // Handle porting in requests...
            log.debug("Port order has [{}] forms to be saved ...", event.getPortRequestForms().size());
            log.debug("Port order has [{}] RingFenceNumbers to be considered.", event.getRingFenceNumberList().size());

            if(event.getMessageType().equals(StMNPMessageTypes.SMILE_NEW_PORTIN_REQUEST.value())) { // Trigger for new porting request.
                currentState = (NGPortInState) NGPortInState.NP_NEW_REQUEST.nextState(em, event);
                if(currentState == NGPortInState.NP_REQUEST_REJECTED_BY_NPCDB) {
                    return currentState.getContext();
                }
            } else {            
                // Else order exists, retrieve current state, then transition to the next state and persist.
                // Retrieve the current porting request
                MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, event.getPortingOrderId(), MNP_PORTING_DIRECTION);
                if(mnpPortRequest != null) {
                    //For testing purposes only - Force the DEFAULT state if porting out ... (to be removed before go-live)
                        MnpHelper.synchDBPortRequestToPortInEvent(event, mnpPortRequest);
                        currentState = (NGPortInState) mnpPortRequest.getNpState();
                        log.debug("NGMNP going to handle state [{}]", currentState.getStateId());
                        currentState =  (NGPortInState) currentState.nextState(em, event );
                } else { // mnpPortRequest == null //Go to the DEFAULT state ... to handle once off messages there (i.e. messages without transition states, like broadcast notifications)
                    currentState = (NGPortInState) NGPortInState.NP_DEFAULT.nextState(em, event);
                }
            }

            if(currentState == null) { // Nothing to save
                    return event;
            } else {
                    savePortState(em, currentState);
                    return currentState.getContext();
            }
        
        } catch(InvalidStateTransitionException ex) {
            
            event.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR); // - Assume done
            log.error("Error while handling port order {}.", event.getPortingOrderId(), ex);
            event.getValidationErrors().add(ex.getMessage());
            event.setErrorCode(NGPortingHelper.MNP_OUT_OF_SEQUENCE);
            event.setErrorDescription(Utils.getStackTrace(ex));
            
            currentState = NGPortInState.NP_FAILED;
            currentState.setContext(event);
            savePortState(em, currentState);
            
            return event;
        } catch (Exception ex) {
            event.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR); // - Assume done
            log.error("Error while handling port order {}.", event.getPortingOrderId(), ex);
            event.getValidationErrors().add(ex.getMessage());
            event.setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
            event.setErrorDescription(Utils.getStackTrace(ex));
            
            if(event.getPortingOrderId() == null || event.getPortingOrderId().isEmpty()) { // This request failed before a porting order id was allocated, give it a random port-order id for debugging purposes
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                event.setPortingOrderId(sdf.format(new Date()));
            }
            
            currentState = NGPortInState.NP_FAILED;
            currentState.setContext(event);
            savePortState(em, currentState);
            
            return event;
        }           
    }

   @Override
    public PortInEvent savePortState(EntityManager em, IPortState state) throws Exception {
        
        NGPortInState tzState = (NGPortInState) state;
        
        if(state.equals(NGPortInState.NP_REQUEST_ACCEPTED_BY_NPCDB)) {
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
            MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, tzState.getContext().getPortingOrderId(), MNP_PORTING_DIRECTION);
                        
            if(mnpPortRequest == null) { //To handle cases like when an NP Broadcast request
                                         //(where Smile was not involved) is received, since no porting record would have existed in that scenario.
                 mnpPortRequest = new MnpPortRequest();
            }
            
            MnpHelper.synchXMLPortRequestToDBPortRequest(tzState, mnpPortRequest);
            
            mnpPortRequest = DAO.savePortingRequestToDB(em, mnpPortRequest);
            // - DAO.addPortInRequestState(em, tzState.getContext().getPortingOrderId(), tzState.getContext().getMessageId(), tzState.getContext().getSenderId(), state);
            // Return the current state record.
            return MnpHelper.synchDBPortRequestToPortInEvent(tzState.getContext(), mnpPortRequest); 
        }
    }
        
    
    }
  
