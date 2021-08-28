/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

/**
 *
 * @author jaybeepee
 */
class ActiveRegistration {
    private String Impu;
    private String Scscf;

    public ActiveRegistration(String Impu, String Scscf) {
        this.Impu = Impu;
        this.Scscf = Scscf;
    }

    
    public String getImpu() {
        return Impu;
    }

    public void setImpu(String Impu) {
        this.Impu = Impu;
    }

    public String getScscf() {
        return Scscf;
    }

    public void setScscf(String Scscf) {
        this.Scscf = Scscf;
    }
    
    
    
}
