/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tpgw.api;

import com.smilecoms.commons.sca.Customer;

/**
 *
 * @author sabza
 */
public class ValidateReferenceIdResult {

    private double amountInCents;
    private String validationResult;
    private Customer customer;

    public double getAmountInCents() {
        return amountInCents;
    }

    public String getValidationResult() {
        return validationResult;
    }

    public void setAmountInCents(double amountInCents) {
        this.amountInCents = amountInCents;
    }

    public void setValidationResult(String validationResult) {
        this.validationResult = validationResult;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
