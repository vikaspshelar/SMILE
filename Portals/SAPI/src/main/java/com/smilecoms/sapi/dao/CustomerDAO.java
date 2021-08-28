/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.dao;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AuthenticationQuery;
import com.smilecoms.commons.sca.AuthenticationResult;
import com.smilecoms.sapi.model.Customer;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.CustomerRole;
import com.smilecoms.commons.sca.IMSNestedIdentityAssociation;
import com.smilecoms.commons.sca.IMSPrivateIdentity;
import com.smilecoms.commons.sca.IMSSubscription;
import com.smilecoms.commons.sca.IMSSubscriptionQuery;
import com.smilecoms.commons.sca.ImplicitIMSPublicIdentitySet;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import org.springframework.stereotype.Component;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StDone;
import com.smilecoms.commons.sca.StIMSSubscriptionLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sapi.model.AuthenticationResponse;
import com.smilecoms.sapi.model.CustomerList;
import com.smilecoms.sapi.model.CustomerRequest;
import com.smilecoms.sapi.model.Customers;
import java.util.List;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bhaskarhg
 */
@Component
public class CustomerDAO {

    private Customers customer;
    private static final Logger LOG = LoggerFactory.getLogger(CustomerDAO.class);

    public Customers authenticateCustomer(Customers customer) throws TokenException {

        LOG.debug("CustomerDAO authenticate()");

        AuthenticationQuery authQuery = new AuthenticationQuery();
        authQuery.setSSOIdentity(customer.getUserName());
        authQuery.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(customer.getPassword()));
        boolean authenticated = false;
        AuthenticationResult authResult;
        try {
            authResult = SCAWrapper.getAdminInstance().authenticate(authQuery);
            authenticated = authResult.getDone().equals(com.smilecoms.commons.sca.StDone.TRUE);
            LOG.debug("Security group ********" + authResult.getSecurityGroups());
        } catch (SCABusinessError sbe) {
            return customer;
        }
        if (authenticated) {
            if (authResult.getSecurityGroups().contains("Customer")) {
                CustomerQuery customerQuery = new CustomerQuery();
                customerQuery.setSSOIdentity(customer.getUserName());
                customerQuery.setResultLimit(1);
                customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

                com.smilecoms.commons.sca.Customer cust = SCAWrapper.getAdminInstance().getCustomer(customerQuery);
                LOG.debug("oneWayHashed" + Utils.oneWayHash(Codec.encryptedHexStringToDecryptedString(Codec.stringToEncryptedHexString(customer.getPassword()))) + "****" + cust.getSSODigest());
                customer.setUserId(cust.getCustomerId());
                customer.setFirstName(cust.getFirstName());
                customer.setLastName(cust.getLastName());
            }
            return customer;
        }
        return customer;
    }

    public CustomerList searchCustomer(CustomerRequest customer) throws TokenException {
        LOG.debug("In CustomerController and in searchCutomer " + customer.getId() + "****" + customer.getUsername());
        CustomerList customerLists;
        CustomerQuery customerQuery = new CustomerQuery();
        customerQuery.setCustomerId(customer.getId());
        customerQuery.setFirstName(customer.getFirstName());
        customerQuery.setLastName(customer.getLastName());
        customerQuery.setSSOIdentity(customer.getUsername());
        customerQuery.setEmailAddress(customer.getEmail());
        customerQuery.setIdentityNumber(customer.getIdNumber());
        customerQuery.setResultLimit(10);
        LOG.debug("After setting customerquery " + customer.getId() + "****" + customer.getUsername());
        ServiceInstanceQuery serviceInstanceQuery = new ServiceInstanceQuery();
        serviceInstanceQuery.setIdentifier(customer.getPhoneNumber());

        IMSSubscriptionQuery imsSubscriptionQuery = new IMSSubscriptionQuery();
        imsSubscriptionQuery.setIntegratedCircuitCardIdentifier(customer.getImsi());

        try {
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            if (serviceInstanceQuery.getIdentifier() != null && !serviceInstanceQuery.getIdentifier().isEmpty()) {
                customerQuery.setAlternativeContact(serviceInstanceQuery.getIdentifier()); // Search for phone number in alternative contacts as well as for a Smile number
            }
            LOG.debug("Before sca call");
            com.smilecoms.commons.sca.CustomerList customerList = SCAWrapper.getAdminInstance().getCustomers(customerQuery);
            LOG.debug("After sca call ***" + customerList.getCustomers().get(0).getFirstName());
            if (serviceInstanceQuery.getIdentifier() != null && !serviceInstanceQuery.getIdentifier().isEmpty() && customerList.getNumberOfCustomers() < customerQuery.getResultLimit()
                    && !serviceInstanceQuery.getIdentifier().isEmpty()) {
                // Search by Phone number
                serviceInstanceQuery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                serviceInstanceQuery.setIdentifierType("END_USER_SIP_URI");
                serviceInstanceQuery.setIdentifier(Utils.getPublicIdentityForPhoneNumber(serviceInstanceQuery.getIdentifier()));
                com.smilecoms.commons.sca.ServiceInstanceList serviceInstanceList = SCAWrapper.getAdminInstance().getServiceInstances(serviceInstanceQuery);
                for (ServiceInstance si : serviceInstanceList.getServiceInstances()) {
                    if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), customerList.getCustomers())) {
                        customerList.getCustomers().add(SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                    }
                }
            }
            if (customerList.getNumberOfCustomers() < customerQuery.getResultLimit()
                    && customer.getImsi() != null && !customer.getImsi().isEmpty()) {
                // Search by IMSI
                ServiceInstanceQuery q = new ServiceInstanceQuery();
                q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                q.setIdentifierType("END_USER_PRIVATE");
                q.setIdentifier(Utils.makePrivateIdentityFromIMSI(customer.getImsi()));
                com.smilecoms.commons.sca.ServiceInstanceList serviceInstanceList = SCAWrapper.getAdminInstance().getServiceInstances(q);
                for (ServiceInstance si : serviceInstanceList.getServiceInstances()) {
                    if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), customerList.getCustomers())) {
                        customerList.getCustomers().add(SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                    }
                }
            }

            customerList.setNumberOfCustomers(customerList.getCustomers().size());
            LOG.debug("Customer list size" + "*****" + customerList.getCustomers().size());

            if (customerList.getNumberOfCustomers() < customerQuery.getResultLimit() && imsSubscriptionQuery.getIntegratedCircuitCardIdentifier() != null && !imsSubscriptionQuery.getIntegratedCircuitCardIdentifier().isEmpty()) {
                
                LOG.debug("*************************************************************************"+imsSubscriptionQuery.getIntegratedCircuitCardIdentifier());
                // Search by ICCID by getting the SIM public and private ID's and finding all mapped SI's
                try {
                    imsSubscriptionQuery.setVerbosity(StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU);
                    IMSSubscription sub = SCAWrapper.getAdminInstance().getIMSSubscription(imsSubscriptionQuery);

                    ServiceInstanceQuery siq = new ServiceInstanceQuery();
                    siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);

                    for (IMSPrivateIdentity impi : sub.getIMSPrivateIdentities()) {
                        siq.setIdentifier(impi.getIdentity());
                        siq.setIdentifierType("END_USER_PRIVATE");
                        ServiceInstanceList siList = SCAWrapper.getAdminInstance().getServiceInstances(siq);
                        for (ServiceInstance si : siList.getServiceInstances()) {
                            if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), customerList.getCustomers())) {
                                customerList.getCustomers().add(SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                            }
                        }
                        for (ImplicitIMSPublicIdentitySet set : impi.getImplicitIMSPublicIdentitySets()) {
                            for (IMSNestedIdentityAssociation association : set.getAssociatedIMSPublicIdentities()) {
                                if (association.getIMSPublicIdentity().getIdentity() != null && !association.getIMSPublicIdentity().getIdentity().isEmpty()) {
                                    siq.setIdentifier(association.getIMSPublicIdentity().getIdentity());
                                    siq.setIdentifierType("END_USER_SIP_URI");
                                    siList = SCAWrapper.getAdminInstance().getServiceInstances(siq);
                                    for (ServiceInstance si : siList.getServiceInstances()) {
                                        if (si.getCustomerId() > 0 && !isCustomerIdInList(si.getCustomerId(), customerList.getCustomers())) {
                                            customerList.getCustomers().add(SCAWrapper.getAdminInstance().getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER));
                                        }
                                    }
                                }
                            }
                        }
                    }

                } catch (SCABusinessError be) {
                    LOG.debug("No Subscription data found for ICCID [{}]", imsSubscriptionQuery.getIntegratedCircuitCardIdentifier());
                }
            }

            customerList.setNumberOfCustomers(customerList.getCustomers().size());
            LOG.debug("Size of customer list ------" + customerList.getCustomers().size());
            if (customerList.getNumberOfCustomers() <= 0) {
//                localiseErrorAndAddToGlobalErrors("no.records.found");
            }

            LOG.debug("--------Before maping------");
            ModelMapper mm = new ModelMapper();
            mm.getConfiguration().setSkipNullEnabled(true);
            customerLists = mm.map(customerList, CustomerList.class);
            LOG.debug("IMP ---***---" + customerLists.getCustomers().size() + "********" + customerLists.getCustomers().get(0).getCustomerId());
        } catch (SCABusinessError e) {
//            showSearchCustomer();
            throw e;
        }

        return customerLists;
    }

    private boolean isCustomerIdInList(int id, List<com.smilecoms.commons.sca.Customer> customerList) {
        for (com.smilecoms.commons.sca.Customer c : customerList) {
            if (c.getCustomerId() == id) {
                return true;
            }
        }
        return false;
    }

    public CustomerDAO() {
    }

    public CustomerDAO(Customers customer) {
        this.customer = customer;
    }

    public Customers getCustomer() {
        LOG.debug("In getCustomer");
        return customer;
    }

    public void setCustomer(Customers customer) {
        LOG.debug("Setting customer data");
        this.customer = customer;
    }

}
