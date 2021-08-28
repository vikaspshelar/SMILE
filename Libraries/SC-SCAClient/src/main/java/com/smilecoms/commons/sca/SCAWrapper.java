/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca;

import com.smilecoms.commons.base.cache.CacheHelper;
import java.util.List;

/**
 *
 * @author paul
 */
public class SCAWrapper extends AbstractSCAWrapper {

    protected SCAWrapper(boolean callAsAdmin, SCAProperties props) {
        super(callAsAdmin);
    }

    public static SCAWrapper getUserSpecificInstance() {
        return new SCAWrapper(false, null);
    }

    public static SCAWrapper getAdminInstance() {
        return new SCAWrapper(true, null);
    }

    public static SCAWrapper getUserSpecificInstance(SCAProperties props) {
        return new SCAWrapper(false, props);
    }

    public static SCAWrapper getAdminInstance(SCAProperties props) {
        return new SCAWrapper(true, props);
    }

    public com.smilecoms.commons.sca.Customer getCustomer(int customerId, StCustomerLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getCustomers", makeSingleCustomerQueryByCustomerId(customerId, verbosity), callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.CustomerList) ret).customers);
        return ((com.smilecoms.commons.sca.CustomerList) ret).customers.get(0);
    }

    public com.smilecoms.commons.sca.Customer getCustomer(com.smilecoms.commons.sca.CustomerQuery requestObject) {
        requestObject.setResultLimit(1);
        Object ret = new SCADelegate().callSCA("getCustomers", requestObject, callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.CustomerList) ret).customers);
        return ((com.smilecoms.commons.sca.CustomerList) ret).customers.get(0);
    }

    public com.smilecoms.commons.sca.Account getAccount(long accountId, StAccountLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getAccounts", makeSingleAccountQueryByAccountId(accountId, verbosity), callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.AccountList) ret).accounts);
        return ((com.smilecoms.commons.sca.AccountList) ret).accounts.get(0);
    }

    public com.smilecoms.commons.sca.Account getAccount(com.smilecoms.commons.sca.AccountQuery requestObject) {
        Object ret = new SCADelegate().callSCA("getAccounts", requestObject, callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.AccountList) ret).accounts);
        return ((com.smilecoms.commons.sca.AccountList) ret).accounts.get(0);
    }

    public com.smilecoms.commons.sca.ServiceInstance getServiceInstance(int serviceInstanceId, StServiceInstanceLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getServiceInstances", makeSingleServiceInstanceQueryByServiceInstanceId(serviceInstanceId, verbosity), callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.ServiceInstanceList) ret).serviceInstances);
        return ((com.smilecoms.commons.sca.ServiceInstanceList) ret).serviceInstances.get(0);
    }

    public com.smilecoms.commons.sca.ServiceInstance getServiceInstance(com.smilecoms.commons.sca.ServiceInstanceQuery requestObject) {
        Object ret = new SCADelegate().callSCA("getServiceInstances", requestObject, callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.ServiceInstanceList) ret).serviceInstances);
        return ((com.smilecoms.commons.sca.ServiceInstanceList) ret).serviceInstances.get(0);
    }

    public com.smilecoms.commons.sca.ProductInstance getProductInstance(int productInstanceId, StProductInstanceLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getProductInstances", makeSingleProductInstanceQueryByServiceInstanceId(productInstanceId, verbosity), callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.ProductInstanceList) ret).productInstances);
        return ((com.smilecoms.commons.sca.ProductInstanceList) ret).productInstances.get(0);
    }

    public com.smilecoms.commons.sca.ProductInstance getProductInstance(com.smilecoms.commons.sca.ProductInstanceQuery requestObject) {
        Object ret = new SCADelegate().callSCA("getProductInstances", requestObject, callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.ProductInstanceList) ret).productInstances);
        return ((com.smilecoms.commons.sca.ProductInstanceList) ret).productInstances.get(0);
    }

    public com.smilecoms.commons.sca.ProductSpecification getProductSpecification(int productSpecificationId, StProductSpecificationLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getProductSpecifications", makeSingleProductSpecificationQueryByProductSpecificationId(productSpecificationId, verbosity), callAsAdmin, props);
        return ((com.smilecoms.commons.sca.ProductSpecificationList) ret).getProductSpecifications().get(0);
    }

    public com.smilecoms.commons.sca.ProductSpecificationList getAllProductSpecifications(StProductSpecificationLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getProductSpecifications", makeSingleProductSpecificationQueryByProductSpecificationId(-1, verbosity), callAsAdmin, props);
        return ((com.smilecoms.commons.sca.ProductSpecificationList) ret);
    }

    public com.smilecoms.commons.sca.UnitCreditSpecification getUnitCreditSpecification(int unitCreditSpecificationId) {
        Object ret = new SCADelegate().callSCA("getUnitCreditSpecifications", makeSingleUnitCreditSpecQuery(unitCreditSpecificationId), callAsAdmin, props);
        return ((com.smilecoms.commons.sca.UnitCreditSpecificationList) ret).getUnitCreditSpecifications().get(0);
    }

    public com.smilecoms.commons.sca.UnitCreditSpecification getUnitCreditSpecification(String itemNumber) {
        Object ret = new SCADelegate().callSCA("getUnitCreditSpecifications", makeSingleUnitCreditSpecQuery(itemNumber), callAsAdmin, props);
        return ((com.smilecoms.commons.sca.UnitCreditSpecificationList) ret).getUnitCreditSpecifications().get(0);
    }

    public com.smilecoms.commons.sca.Organisation getOrganisation(int organisationId, StOrganisationLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getOrganisations", makeSingleOrganisationQueryByOrganisationId(organisationId, verbosity), callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.OrganisationList) ret).organisations);
        return ((com.smilecoms.commons.sca.OrganisationList) ret).organisations.get(0);
    }

    public com.smilecoms.commons.sca.Sale getSale(int saleId, StSaleLookupVerbosity verbosity) {
        Object ret = new SCADelegate().callSCA("getSales", makeSingleSaleQueryBySaleId(saleId, verbosity), callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.SalesList) ret).sales);
        return ((com.smilecoms.commons.sca.SalesList) ret).sales.get(0);
    }

    public com.smilecoms.commons.sca.Organisation getOrganisation(com.smilecoms.commons.sca.OrganisationQuery requestObject) {
        requestObject.setResultLimit(1);
        Object ret = new SCADelegate().callSCA("getOrganisations", requestObject, callAsAdmin, props);
        checkForNoResult(((com.smilecoms.commons.sca.OrganisationList) ret).organisations);
        return ((com.smilecoms.commons.sca.OrganisationList) ret).organisations.get(0);
    }

    public String getSCACallInfo() {
        return SCADelegate.getSCACallInfo();
    }

    public void clearSCACallInfo() {
        SCADelegate.clearSCACallInfo();
    }

    private static final java.util.List roles = new java.util.ArrayList(1);

    static {
        roles.add("Administrator");
    }

    public static void setThreadsRequestContextAsAdmin() {
        CallersRequestContext crc = new CallersRequestContext("admin", "0.0.0.0", 1, roles);
        SCADelegate.setThreadsRequestContext(crc);
    }

    public static String getTenantBySSOIdentity(String username) {
        if (username.equals("admin")) {
            return SCADelegate.ADMIN_TENANT;
        }
        String key = "TENANT-" + username;
        String tenant = CacheHelper.getFromLocalCache(key, String.class);
        if (tenant == null) {
            TenantQuery tq = new TenantQuery();
            tq.setSSOIdentity(username);
            TenantData tenantData = SCAWrapper.getAdminInstance().getCustomerTenant(tq);
            tenant = tenantData.getTenant();
            CacheHelper.putInLocalCache(key, tenant, 1800);
        }
        return tenant;
    }

    public void setThreadsRequestContext(CallersRequestContext ctx) {
        if (callAsAdmin) {
            throw new java.lang.UnsupportedOperationException("Cannot set http request on a backend SCA Instance");
        }
        SCADelegate.setThreadsRequestContext(ctx);
    }

    public static void removeThreadsRequestContext() {
        SCADelegate.removeThreadsRequestContext();
    }

    public CallersRequestContext getThreadsRequestContext() {
        if (callAsAdmin) {
            throw new java.lang.UnsupportedOperationException("Cannot get http request on a backend SCA Instance");
        }
        return SCADelegate.getThreadsRequestContext();
    }

    /*
     * 
     * 
     * Helpers
     * 
     * 
     * 
     */
    private CustomerQuery makeSingleCustomerQueryByCustomerId(int i, StCustomerLookupVerbosity verbosity) {
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(i);
        q.setResultLimit(1);
        q.setVerbosity(verbosity);
        return q;
    }

    private AccountQuery makeSingleAccountQueryByAccountId(long i, StAccountLookupVerbosity verbosity) {
        AccountQuery q = new AccountQuery();
        q.setAccountId(i);
        q.setVerbosity(verbosity);
        return q;
    }

    private ServiceInstanceQuery makeSingleServiceInstanceQueryByServiceInstanceId(int i, StServiceInstanceLookupVerbosity verbosity) {
        ServiceInstanceQuery q = new ServiceInstanceQuery();
        q.setVerbosity(verbosity);
        q.setServiceInstanceId(i);
        return q;
    }

    private UnitCreditSpecificationQuery makeSingleUnitCreditSpecQuery(int i) {
        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        q.setUnitCreditSpecificationId(i);
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        return q;
    }

    private UnitCreditSpecificationQuery makeSingleUnitCreditSpecQuery(String itemNumber) {
        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        q.setItemNumber(itemNumber);
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        return q;
    }

    private Object makeSingleProductSpecificationQueryByProductSpecificationId(int productSpecificationId, StProductSpecificationLookupVerbosity stProductSpecificationLookupVerbosity) {
        ProductSpecificationQuery q = new ProductSpecificationQuery();
        q.setVerbosity(stProductSpecificationLookupVerbosity);
        q.setProductSpecificationId(productSpecificationId);
        return q;
    }

    private void checkForNoResult(List list) {
        if (list == null || list.isEmpty()) {
            SCABusinessError b = new SCABusinessError();
            b.setErrorCode(Errors.ERROR_CODE_SCA_NO_RESULT);
            b.setErrorDesc("No results found");
            throw b;
        }
    }

    private Object makeSingleOrganisationQueryByOrganisationId(int organisationId, StOrganisationLookupVerbosity verbosity) {
        OrganisationQuery q = new OrganisationQuery();
        q.setOrganisationId(organisationId);
        q.setVerbosity(verbosity);
        q.setResultLimit(1);
        return q;
    }

    private Object makeSingleProductInstanceQueryByServiceInstanceId(int productInstanceId, StProductInstanceLookupVerbosity verbosity) {
        ProductInstanceQuery q = new ProductInstanceQuery();
        q.setVerbosity(verbosity);
        q.setProductInstanceId(productInstanceId);
        return q;
    }

    private Object makeSingleSaleQueryBySaleId(int saleId, StSaleLookupVerbosity verbosity) {
        SalesQuery q = new SalesQuery();
        q.getSalesIds().add(saleId);
        q.setVerbosity(verbosity);
        return q;
    }
}
