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
public interface AFSession {
    
    public String getBindingIdentifier();

    public void setBindingIdentifier(String bindingIdentifier);

    public String getRxServerSessionId();

    public void setRxServerSessionId(String gxServerSessionId);

    public String getState();

    public void setState(String state);

    public int getType();

    public void setType(int type);
    
}
