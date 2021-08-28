/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pos;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author abhilash
 */
public class EsdRequest {
    
    private BigDecimal grand_total;
    private BigDecimal gross_amount;
    private String invoice_date;
    private long invoice_number;
    private String location;
    private String customer_tin;
    private String customer_name;
    private String customer_phone;
    private int vat;
    
    public EsdRequest()
    {
        
    }

    public BigDecimal getGrand_total() {
        return grand_total;
    }

    public void setGrand_total(BigDecimal grand_total) {
        this.grand_total = grand_total;
    }

    public BigDecimal getGross_amount() {
        return gross_amount;
    }

    public void setGross_amount(BigDecimal gross_amount) {
        this.gross_amount = gross_amount;
    }

    public String getInvoice_date() {
        return invoice_date;
    }

    public void setInvoice_date(String invoice_date) {
        this.invoice_date = invoice_date;
    }

    public long getInvoice_number() {
        return invoice_number;
    }

    public void setInvoice_number(long invoice_number) {
        this.invoice_number = invoice_number;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCustomer_tin() {
        return customer_tin;
    }

    public void setCustomer_tin(String customer_tin) {
        this.customer_tin = customer_tin;
    }

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getCustomer_phone() {
        return customer_phone;
    }

    public void setCustomer_phone(String customer_phone) {
        this.customer_phone = customer_phone;
    }

    public int getVat() {
        return vat;
    }

    public void setVat(int vat) {
        this.vat = vat;
    }  

    @Override
    public String toString() {
        return "EsdRequest{" + "grand_total=" + grand_total + ", gross_amount=" 
                + gross_amount + ", invoice_date=" + invoice_date 
                + ", invoice_number=" + invoice_number + ", location=" 
                + location + ", customer_tin=" + customer_tin 
                + ", customer_name=" + customer_name + ", customer_phone=" 
                + customer_phone + ", vat=" + vat + '}';
    }
    
}
