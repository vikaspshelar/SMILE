/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tpgw.api;

import com.smilecoms.commons.sca.Customer;
import java.util.Map;

/**
 *
 * @author sabza
 */
public class ValidateEmailAddressResult {

    private Customer customer;
    private Map<Long, String> accountIdFriendlyNameMapping;

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Map<Long, String> getAccountIdFriendlyNameMapping() {
        return accountIdFriendlyNameMapping;
    }

    public void setAccountIdFriendlyNameMapping(Map<Long, String> accountIdFriendlyNameMapping) {
        this.accountIdFriendlyNameMapping = accountIdFriendlyNameMapping;
    }
}
