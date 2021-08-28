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
public interface PCCRule {
    
    public String getPccRuleName();

    public void setPccRuleName(String pccRuleName);

    public String getBindingIdentifier();

    public void setBindingIdentifier(String bindingIdentifier);

    public int getType();

    public void setType(int type);
    
}
