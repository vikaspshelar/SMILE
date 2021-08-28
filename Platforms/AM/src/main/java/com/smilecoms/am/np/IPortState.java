/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.np;

import com.smilecoms.am.np.ng.NGPortInState;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.xml.schema.am.PortInEvent;
import java.util.Properties;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public interface IPortState {
    
    public String getStateId();
    public IPortState nextState(EntityManager em, PortInEvent event) throws Exception ;
    public PortInEvent getContext();
    public void setContext(PortInEvent event) throws Exception;
    public Object getNPCDBConnection() throws Exception;
    
}