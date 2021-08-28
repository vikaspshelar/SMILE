/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author abhilash
 */
@XmlRootElement
public class EsdResponse {
    
    private String signature;
    private String verification_url;
    private long invoice_number;
    private int status;
    private String description;
    
    public EsdResponse() {
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVerification_url() {
        return verification_url;
    }

    public void setVerification_url(String verification_url) {
        this.verification_url = verification_url;
    }

    public long getInvoice_number() {
        return invoice_number;
    }

    public void setInvoice_number(long invoice_number) {
        this.invoice_number = invoice_number;
    }

    @Override
    public String toString() {
        return "EsdResponse{" + "signature=" + signature + ", status=" + status 
                + ", verification_url=" + verification_url + ", description=" + description 
                + ", invoice_number=" + invoice_number + '}';
    }    
    
}
