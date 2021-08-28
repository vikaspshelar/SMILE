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
public class TZIPortOutStateMachineImpl implements IPortInStateMachine {
    
    private static final Logger log = LoggerFactory.getLogger(TZIPortOutStateMachineImpl.class);
    
    private static String MNP_PORTING_DIRECTION = MnpHelper.MNP_PORTING_DIRECTION_OUT;
    
    private TZPortOutState currentState;
    
    
    @Override
    public void init() {
        
    }
        
    @Override
    public TZPortOutState getState(String id) {
       
        if (id == null) {
            return null;
        }
        
        for (TZPortOutState position : TZPortOutState.values()) {
            if (id.equals(position.getStateId())) {
                return position;
            }
        }
        
        throw new IllegalArgumentException("No matching state for id " + id);
    }
        
    @Override
    public PortInEvent handleState(EntityManager em, PortInEvent event) throws Exception {
        try {
            event.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_DONE); //Assume done
            
            // Retrieve the current porting request
            MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, event.getPortingOrderId(), MNP_PORTING_DIRECTION);

            if(mnpPortRequest != null) {
                MnpHelper.synchDBPortRequestToPortInEvent(event, mnpPortRequest);
                currentState = (TZPortOutState) mnpPortRequest.getNpState();
                currentState =  (TZPortOutState) currentState.nextState(em, event);
            } else { // mnpPortRequest == null //Go to the DEFAULT state ...
                if(event.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST.value())) { // Handle porting out
                    currentState = (TZPortOutState) TZPortOutState.NP_START.nextState(em, event);
                } else { // Go to default status
                    currentState = (TZPortOutState) TZPortOutState.NP_DEFAULT.nextState(em, event);
                }
            }
        
            if(currentState == null) { // Nothing to save
                return event;
            } else {
                savePortState(em, currentState);
                return currentState.getContext();
            }
        } catch (Exception ex) {
            event.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR); 
            
            log.error("Error while handling port order {}.", event.getPortingOrderId(), ex);
            event.getValidationErrors().add(ex.getMessage());
            event.setErrorCode(TZPortingHelper.TZMNP_ERROR_CODE_10);
            event.setErrorDescription(Utils.getStackTrace(ex));
            
            currentState = TZPortOutState.NP_FAILED;
            currentState.setContext(event);
            savePortState(em, currentState);
            
            return event;
        }
    }
   
    /*@Override
    public TZPortOutState getCurrentState(EntityManager em, String orderId) throws Exception {
        return (TZPortOutState) DAO.getCurrentState(em, orderId);
    }*/

    @Override
    public PortInEvent savePortState(EntityManager em, IPortState state) throws Exception {
        
        TZPortOutState tzState = (TZPortOutState) state;
        
            log.error("tzState.getContext().getPortingOrderId() = " + tzState.getContext().getPortingOrderId());
            MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, tzState.getContext().getPortingOrderId(), MNP_PORTING_DIRECTION);
                        
            if(mnpPortRequest == null) { //To handle cases like when an NP Broadcast request
                                         //(where Smile was not involved) is received, since no porting record would have existed in that scenario.
                mnpPortRequest = new MnpPortRequest();
            }
            
            MnpHelper.synchXMLPortRequestToDBPortRequest(tzState, mnpPortRequest);
            
            DAO.savePortingRequestToDB(em, mnpPortRequest);
            // - DAO.addPortInRequestState(em, tzState.getContext().getPortingOrderId(), tzState.getContext().getMessageId(), tzState.getContext().getSenderId(), state);
            // Return the current state record.
            log.error("tzState.portingDirection = " + tzState.getContext().getPortingDirection());
            log.error("mnpPortRequest.portingDirection = " + mnpPortRequest.getMnpPortRequestPK().getPortingDirection());
            return MnpHelper.synchDBPortRequestToPortInEvent(tzState.getContext(), mnpPortRequest); 
    }
    
    
        
    }
  
