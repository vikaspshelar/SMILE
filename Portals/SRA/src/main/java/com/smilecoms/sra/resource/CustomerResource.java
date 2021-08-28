/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.beans.CustomerBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lesiba
 */
public class CustomerResource extends Resource {

    private CustomerBean customer;
    private static final Logger log = LoggerFactory.getLogger(CustomerResource.class);

    public static CustomerResource getCustomerResourceByEmail(String email, StCustomerLookupVerbosity verbosity) {
        return new CustomerResource(CustomerBean.getCustomerByEmail(email, verbosity));
    }

    public static CustomerResource getCustomerResourceByUserName(String username, StCustomerLookupVerbosity verbosity) {
        return new CustomerResource(CustomerBean.getCustomerByUserName(username, verbosity));
    }
    
    public static CustomerResource getCustomerResourceById(int id, StCustomerLookupVerbosity verbosity) {
        return new CustomerResource(CustomerBean.getCustomerById(id, verbosity));
    }
    
    public static CustomerResource getCustomerResourceByIMPU(String impu, StCustomerLookupVerbosity verbosity) {
        return new CustomerResource(CustomerBean.getCustomerByIMPU(impu, verbosity));
    }

    public CustomerResource() {
    }
    
    public CustomerResource(CustomerBean customer) {
        this.customer = customer;
    }

    public CustomerBean getCustomer() {
        log.debug("In getCustomer");
        return customer;
    }
    
    public void setCustomer(CustomerBean customer) {
        log.debug("Setting customer data");
        this.customer = customer;
    }

}
