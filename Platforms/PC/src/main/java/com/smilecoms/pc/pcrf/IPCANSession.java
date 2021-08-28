/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf;

/**
 *
 * @author richard
 */
public interface IPCANSession {
    
    public String getBindingIdentifier();

    public void setBindingIdentifier(String bindingIdentifier);

    public String getGxServerSessionId();

    public void setGxServerSessionId(String gxServerSessionId);

    public String getState();

    public void setState(String state);

    public String getCalledStationId();

    public void setCalledStationId(String calledStationId);
    
    public int getHighestPriorityServiceId();

    public void setHighestPriorityServiceId(int highestPriorityServiceId);
    
    public String getEndUserPrivate();

    public void setEndUserPrivate(String endUserPrivate);
    
    
}
