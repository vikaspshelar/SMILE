/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author abhilash
 */
public class CeirResponse {
    
    private String VRSN;
    private String TID;
    private String MTON;
    private String MNPI;
    private String MSDN;
    private String CELL;
    private String RNDN;

    public CeirResponse() {
    }

    public CeirResponse(String VRSN, String TID, String MTON, String MNPI, String MSDN, String CELL, String RNDN) {
        this.VRSN = VRSN;
        this.TID = TID;
        this.MTON = MTON;
        this.MNPI = MNPI;
        this.MSDN = MSDN;
        this.CELL = CELL;
        this.RNDN = RNDN;
    }

    public String getVRSN() {
        return VRSN;
    }

    public void setVRSN(String VRSN) {
        this.VRSN = VRSN;
    }

    public String getTID() {
        return TID;
    }

    public void setTID(String TID) {
        this.TID = TID;
    }

    public String getMTON() {
        return MTON;
    }

    public void setMTON(String MTON) {
        this.MTON = MTON;
    }

    public String getMNPI() {
        return MNPI;
    }

    public void setMNPI(String MNPI) {
        this.MNPI = MNPI;
    }

    public String getMSDN() {
        return MSDN;
    }

    public void setMSDN(String MSDN) {
        this.MSDN = MSDN;
    }

    public String getCELL() {
        return CELL;
    }

    public void setCELL(String CELL) {
        this.CELL = CELL;
    }

    public String getRNDN() {
        return RNDN;
    }

    public void setRNDN(String RNDN) {
        this.RNDN = RNDN;
    }

    @Override
    public String toString() {
        if (Integer.parseInt(VRSN)==1)
        {
            return "/NTWKD?" + "VRSN=" + VRSN + "&TID=" + TID + "&MTON=" + MTON + "&MNPI=" + MNPI + "&MSDN=" + MSDN;
        } else {
            return "/NTWKD?" + "VRSN=" + VRSN + "&TID=" + TID + "&MTON=" + MTON + "&MNPI=" + MNPI + "&MSDN=" + MSDN + "&CELL=" + CELL + "&RNDN=" + RNDN;
        }        
    }

    
    
}
