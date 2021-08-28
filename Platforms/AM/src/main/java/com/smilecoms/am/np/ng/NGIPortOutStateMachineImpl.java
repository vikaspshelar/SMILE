/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np.ng;

import com.smilecoms.am.db.model.MnpPortRequest;
import com.smilecoms.am.db.op.DAO;
import com.smilecoms.am.np.IPortInStateMachine;
import com.smilecoms.am.np.IPortState;
import com.smilecoms.am.np.InvalidStateTransitionException;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.StMNPMessageTypes;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.am.PortInEvent;
import java.util.Properties;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class NGIPortOutStateMachineImpl implements IPortInStateMachine {
    
    private static final Logger log = LoggerFactory.getLogger(NGIPortOutStateMachineImpl.class);
    
    private static String MNP_PORTING_DIRECTION = MnpHelper.MNP_PORTING_DIRECTION_OUT;
    
    public static Properties props = null;
    
    private IPortState currentState;
   
    
    @Override
    public void init() {
        props = new Properties();
        try {
            log.warn("Here are the MNP properties as contained in env.mnp.config \n");
            log.warn(BaseUtils.getProperty("env.mnp.config"));
            props.load(BaseUtils.getPropertyAsStream("env.mnp.config"));
            
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.mnp.config: ", ex);
        }
    }
        
    @Override
    public NGPortOutState getState(String id) {
       
        if (id == null) {
            return null;
        }
        
        for (NGPortOutState position : NGPortOutState.values()) {
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
            // Retrieve the current porting request
            MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, event.getPortingOrderId(), MNP_PORTING_DIRECTION);

            if(mnpPortRequest != null) {
                MnpHelper.synchDBPortRequestToPortInEvent(event, mnpPortRequest);
                currentState = (NGPortOutState) mnpPortRequest.getNpState();
                log.debug("NGMNP going to handle state [{}]", currentState.getStateId());
                currentState =  (IPortState) currentState.nextState(em, event);
            } else { // mnpPortRequest == null //Go to the DEFAULT state ...
                if(event.getMessageType().equals(StMNPMessageTypes.NPCDB_PORT_OUT_REQUEST.value())) { // Handle porting out
                    currentState = (NGPortOutState) NGPortOutState.NP_START.nextState(em, event);                    
                } else { // Go to default status
                    currentState =  NGPortOutState.NP_DEFAULT.nextState(em, event);
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
            
            event.setProcessingStatus(MnpHelper.REQUEST_PROCESSING_STATUS_ERROR); //Assume done
            
            log.error("Error while handling port order {}.", event.getPortingOrderId(), ex);
            event.getValidationErrors().add(ex.getMessage());
            event.setErrorCode(NGPortingHelper.MNP_ERROR_CODE_OTHER);
            event.setErrorDescription(Utils.getStackTrace(ex));
            
            currentState = NGPortInState.NP_FAILED;
            currentState.setContext(event);
            savePortState(em, currentState);
            
            return event;
        }
    }
   
    /*@Override
    public NGPortOutState getCurrentState(EntityManager em, String orderId) throws Exception {
        return (NGPortOutState) DAO.getCurrentState(em, orderId);
    }*/

    @Override
    public PortInEvent savePortState(EntityManager em, IPortState state) throws Exception {
            MnpPortRequest mnpPortRequest = DAO.getMnpPortRequest(em, state.getContext().getPortingOrderId(), MNP_PORTING_DIRECTION);
            boolean isANewRequest = false;
            
            if(mnpPortRequest == null) { //To handle cases like when an NP Broadcast request
                                         //(where Smile was not involved) is received, since no porting record would have existed in that scenario.
                mnpPortRequest = new MnpPortRequest();
                isANewRequest = true;
            }
            
            MnpHelper.synchXMLPortRequestToDBPortRequest(state, mnpPortRequest);
            
            DAO.savePortingRequestToDB(em, mnpPortRequest);
            
            // Now set the attached documents - if there were any attached...
            log.debug("Port-out order [{}] has [{}] attached and must be saved ...", state.getContext().getPortingOrderId(), state.getContext().getPortRequestForms().size());
            if (!state.getContext().getPortRequestForms().isEmpty() && isANewRequest) {
                    DAO.setPortOrderForms(em, state.getContext().getPortRequestForms(), state.getContext().getPortingOrderId());
            }
            // - DAO.addPortInRequestState(em, tzState.getContext().getPortingOrderId(), tzState.getContext().getMessageId(), tzState.getContext().getSenderId(), state);
            // Return the current state record.
            log.error("tzState.portingDirection = " + state.getContext().getPortingDirection());
            log.error("mnpPortRequest.portingDirection = " + mnpPortRequest.getMnpPortRequestPK().getPortingDirection());
            return MnpHelper.synchDBPortRequestToPortInEvent(state.getContext(), mnpPortRequest); 
    }
    
    
        
    }
  
