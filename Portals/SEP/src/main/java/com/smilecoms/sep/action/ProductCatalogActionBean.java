/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.CustomerList;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.NumberList;
import com.smilecoms.commons.sca.IMSPrivateIdentity;
import com.smilecoms.commons.sca.IMSSubscription;
import com.smilecoms.commons.sca.NAIIdentity;
import com.smilecoms.commons.sca.NAIIdentityQuery;
import com.smilecoms.commons.sca.NumbersQuery;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.ProductServiceSpecificationMapping;
import com.smilecoms.commons.sca.ProductSpecification;
import com.smilecoms.commons.sca.ProductSpecificationList;
import com.smilecoms.commons.sca.ProductSpecificationQuery;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.Sale;
import com.smilecoms.commons.sca.SaleLine;
import com.smilecoms.commons.sca.SalesList;
import com.smilecoms.commons.sca.SalesQuery;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.ServiceSpecification;
import com.smilecoms.commons.sca.SoldStockLocation;
import com.smilecoms.commons.sca.SoldStockLocationList;
import com.smilecoms.commons.sca.SoldStockLocationQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StIMSSubscriptionLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StProductSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.StSaleLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.StickyNote;
import com.smilecoms.commons.sca.StickyNoteEntityIdentifier;
import com.smilecoms.commons.sca.StickyNoteList;
import com.smilecoms.commons.sca.StickyNoteType;
import com.smilecoms.commons.sca.StickyNoteTypeList;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.direct.et.EventList;
import com.smilecoms.commons.sca.direct.et.EventQuery;
import com.smilecoms.commons.stripes.SmileActionBean;
import static com.smilecoms.commons.stripes.SmileActionBean.makeSCAString;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.util.JPAUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpSession;
import javax.xml.datatype.DatatypeFactory;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author PCB
 */
public class ProductCatalogActionBean extends SmileActionBean {

    private Set<Long> accountIdSet = new HashSet<>();
    private int publicIdentityCostCents;

    public int getPublicIdentityCostCents() {
        return publicIdentityCostCents;
    }

    public void setPublicIdentityCostCents(int publicIdentityCostCents) {
        this.publicIdentityCostCents = publicIdentityCostCents;
    }

    public List<Long> getAccountIdList() {
        List ret = new ArrayList();
        ret.addAll(accountIdSet);
        return ret;
    }

    @DefaultHandler
    public Resolution showProductCatalog() {
        checkPermissions(Permissions.VIEW_PRODUCT_CATALOG);
        ProductSpecificationQuery q = new ProductSpecificationQuery();
        q.setProductSpecificationId(-1);
        q.setVerbosity(StProductSpecificationLookupVerbosity.MAIN);
        setProductSpecificationList(SCAWrapper.getUserSpecificInstance().getProductSpecifications(q));
        return getDDForwardResolution("/product_catalog/view_product_catalog.jsp");
    }

    public Resolution showSearchProductInstance() {
        return getDDForwardResolution("/product_catalog/search_product_instance.jsp");
    }

    public Resolution showUnitCreditCatalog() {
        checkPermissions(Permissions.VIEW_PRODUCT_CATALOG);
        UnitCreditSpecificationQuery q = new UnitCreditSpecificationQuery();
        q.setUnitCreditSpecificationId(-1);
        q.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        setUnitCreditSpecificationList(SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(q));

        return getDDForwardResolution("/product_catalog/view_unit_credit_catalog.jsp");
    }

    public Resolution retrieveProductSpecification() {
        checkPermissions(Permissions.VIEW_PRODUCT_CATALOG);
        if (getProductSpecification() != null) {
            setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(getProductSpecification().getProductSpecificationId()));
        }
        return getDDForwardResolution("/product_catalog/view_product_specification.jsp");
    }

    public Resolution removeServiceInstance() {
        checkPermissions(Permissions.REMOVE_SERVICE_INSTANCE);
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }

        ProductOrder order = new ProductOrder();
        order.setProductInstanceId(getProductInstance().getProductInstanceId());
        order.setAction(StAction.NONE);
        order.setCustomerId(getProductInstance().getCustomerId());
        order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
        order.getServiceInstanceOrders().get(0).setAction(StAction.DELETE);
        order.getServiceInstanceOrders().get(0).setServiceInstance(UserSpecificCachedDataHelper.getServiceInstance(getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN));

        try {
            SCAWrapper.getUserSpecificInstance().processOrder(order);
        } catch (SCABusinessError e) {
            retrieveServiceInstance();
            throw e;
        }
        setPageMessage("service.instance.removed.successfully");
        return retrieveProductInstance();
    }

    public Resolution temporarilyDeactivateServiceInstance() {
        log.info("TestNIDA temporarilyDeactivateServiceInstance");
        checkPermissions(Permissions.TEMPORARILY_DEACTIVATE_SERVICE_INSTANCE);
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }

        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN);
        ProductOrder order = new ProductOrder();
        order.setProductInstanceId(getProductInstance().getProductInstanceId());
        order.setAction(StAction.NONE);
        order.setCustomerId(getProductInstance().getCustomerId());
        order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
        order.getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
        order.getServiceInstanceOrders().get(0).setServiceInstance(getServiceInstance());
        order.getServiceInstanceOrders().get(0).getServiceInstance().setStatus("TD");
        order.getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());
        try {
            SCAWrapper.getUserSpecificInstance().processOrder(order);
        } catch (SCABusinessError e) {
            retrieveServiceInstance();
            throw e;
        }
        setPageMessage("service.instance.temporarily.deactivated.successfully");
        return retrieveServiceInstance();
    }

    public Resolution reactivateServiceInstance() {
        log.debug("TestNIDA reactivateServiceInstance");
        checkPermissions(Permissions.REACTIVATE_SERVICE_INSTANCE);
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }
        ServiceInstanceQuery q = new ServiceInstanceQuery();
        q.setServiceInstanceId(getServiceInstance().getServiceInstanceId());
        q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(q);
        ProductOrder order = new ProductOrder();
        order.setProductInstanceId(getProductInstance().getProductInstanceId());
        order.setAction(StAction.NONE);
        order.setCustomerId(getProductInstance().getCustomerId());
        order.getServiceInstanceOrders().add(new ServiceInstanceOrder());
        order.getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
        order.getServiceInstanceOrders().get(0).setServiceInstance(si);
        order.getServiceInstanceOrders().get(0).getServiceInstance().setStatus("AC");
       /* 
        if(BaseUtils.getProperty("env.country.name").trim().equalsIgnoreCase("Nigeria") && !SCAWrapper.getUserSpecificInstance().getCustomer(getProductInstance().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER).getIsNinVerified().equalsIgnoreCase("Y")) {
            localiseErrorAndAddToGlobalErrors("nin.not.verified");
            return retrieveServiceInstance();
        }
        */
        try {
            SCAWrapper.getUserSpecificInstance().processOrder(order);
        } catch (SCABusinessError e) {
            retrieveServiceInstance();
            throw e;
        }
        setPageMessage("service.instance.reactivated.successfully");
        return retrieveServiceInstance();
    }

    public Resolution removeProductInstance() {
        checkPermissions(Permissions.REMOVE_PRODUCT_INSTANCE);
        if (notConfirmed()) {
            return confirm();
        }
        checkCSRF();
        ProductInstance pi = SCAWrapper.getUserSpecificInstance().getProductInstance(getProductInstance().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC);
        ProductOrder order = new ProductOrder();
        order.setProductInstanceId(getProductInstance().getProductInstanceId());
        order.setAction(StAction.DELETE);
        for (ProductServiceInstanceMapping mapping : pi.getProductServiceInstanceMappings()) {
            ServiceInstanceOrder siOrder = new ServiceInstanceOrder();
            siOrder.setAction(StAction.DELETE);
            siOrder.setServiceInstance(mapping.getServiceInstance());
            order.getServiceInstanceOrders().add(siOrder);
        }
        try {
            SCAWrapper.getUserSpecificInstance().processOrder(order);
        } catch (SCABusinessError e) {
            retrieveProductInstance();
            throw e;
        }
        return getDDForwardResolution(CustomerActionBean.class, "retrieveCustomer", "product.instance.removed.successfully");
    }

    public Resolution removeSIMUnbundlingRestriction() {
        checkPermissions(Permissions.REMOVE_SIM_UNBUNDLING_RESTRICTION);
        if (notConfirmed()) {
            return confirm();
        }
        String siIDs = "";
        checkCSRF();
        ProductInstance pi = SCAWrapper.getUserSpecificInstance().getProductInstance(getProductInstance().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP);

        ProductOrder pOrder = new ProductOrder();
        pOrder.setAction(StAction.NONE);
        pOrder.setProductInstanceId(pi.getProductInstanceId());

        for (ProductServiceInstanceMapping psiMapping : pi.getProductServiceInstanceMappings()) {

            ServiceInstance si = psiMapping.getServiceInstance();

            //Set IMEI on non-SIM services only
            if (si.getServiceSpecificationId() >= 1) {
                // Utils.setValueInCRDelimitedAVPString(si.get, RETURN, RETURN)
                ServiceInstanceOrder siOrder = new ServiceInstanceOrder();

                siOrder.setAction(StAction.UPDATE);
                siOrder.setServiceInstance(new ServiceInstance());
                siOrder.getServiceInstance().setServiceInstanceId(si.getServiceInstanceId());
                siOrder.getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
                siOrder.getServiceInstance().setCustomerId(si.getCustomerId());
                siOrder.getServiceInstance().setStatus(si.getStatus());
                siOrder.getServiceInstance().setAccountId(si.getAccountId());

                //Clear the IMEI restriction
                AVP imei = new AVP();
                imei.setAttribute("LockedToDeviceIMEI");
                imei.setValue("");
                siOrder.getServiceInstance().getAVPs().add(imei);

                formatAVPsForSendingToSCA(siOrder.getServiceInstance().getAVPs(), siOrder.getServiceInstance().getServiceSpecificationId(), true);
                pOrder.getServiceInstanceOrders().add(siOrder);

            }
        }
        try {
            if (pOrder.getServiceInstanceOrders().size() > 0) {
                SCAWrapper.getAdminInstance().processOrder(pOrder);
                // Log event here as requested  by BI for reporting
                com.smilecoms.commons.sca.SCAWrapper SCAWrapper = com.smilecoms.commons.sca.SCAWrapper.getAdminInstance();
                com.smilecoms.commons.sca.Event eventData = new com.smilecoms.commons.sca.Event();
                eventData.setEventType("SEP");
                eventData.setEventSubType("UNLOCK_IMEI");
                eventData.setEventKey(String.valueOf(pi.getProductInstanceId()));
                eventData.setEventData(getUser() + "|SiIDs=" + siIDs);
                //this ensures event is created async
                eventData.setSCAContext(new com.smilecoms.commons.sca.SCAContext());
                eventData.getSCAContext().setAsync(java.lang.Boolean.TRUE);
                eventData.setUniqueKey("UNLOCK_IMEI_" + pi.getProductInstanceId());

                SCAWrapper.createEvent(eventData);
            }
        } catch (SCABusinessError e) {
            retrieveProductInstance();
            throw e;
        }

        return getDDForwardResolution(ProductCatalogActionBean.class, "retrieveProductInstance", "sim.unbundling.restriction.removed.successfully");
    }
    
    public String getVerifier() {
        com.smilecoms.commons.sca.EventQuery evq = new com.smilecoms.commons.sca.EventQuery();
        
        evq.setEventKey(String.valueOf(getCustomer().getCustomerId()));
        evq.setEventType("IM");
        evq.setEventSubType("KYCChange");
        evq.setResultLimit(4);
        
        String log="";
        com.smilecoms.commons.sca.EventList eventsData = SCAWrapper.getUserSpecificInstance().getEvents(evq);
        
        
        for(Event ev: eventsData.getEvents()) {
            String[] evData = ev.getEventData().split("\\|");
            
            if(log.length()>0) {
                log += "<br>- " + Utils.formatDateLong(Utils.getXMLGregorianCalendarAsDate(ev.getDate(), new Date()))  + " Verification by Username: " + evData[2];
            } else {
                log = "- " + Utils.formatDateLong(Utils.getXMLGregorianCalendarAsDate(ev.getDate(), new Date())) + " Verification by Username: " + evData[2];
            }
            
        }        
        
        return log;
    }

    public Resolution showEditProductInstanceSIMService() {
        int piId = getProductInstance().getProductInstanceId();
        for (ProductServiceInstanceMapping m : UserSpecificCachedDataHelper.getProductInstance(piId, StProductInstanceLookupVerbosity.MAIN_SVC).getProductServiceInstanceMappings()) {
            ServiceInstance si = m.getServiceInstance();
            if (si.getServiceSpecificationId() == 1) {
                setServiceInstance(si);
                return showChangeServiceInstanceConfiguration();
            }
        }
        return getDDForwardResolution(LoginActionBean.class, "goHome");
    }

    public Resolution showChangeServiceInstanceAccount() {

        if (!canChangeServiceToAnyAccount()) {
            checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_EXISTING_ACCOUNT);
        }

        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        accountIdSet = new HashSet<>();
        for (ProductInstance pi : getCustomer().getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == getCustomer().getCustomerId()) {
                    accountIdSet.add(si.getAccountId());
                }
            }
        }
        setServiceSpecification(NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceInstance().getServiceSpecificationId()));
        return getDDForwardResolution("/product_catalog/change_service_instance_account.jsp");
    }

    public Resolution changeServiceInstanceAccount() {

        if (!canChangeServiceToAnyAccount()) {
            checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_EXISTING_ACCOUNT);
        }

        if (notConfirmed()) {
            return confirm();
        }
        checkCSRF();

        getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN);

        if (!canChangeServiceToAnyAccount()) {
            // Verify account belong to customer
            accountIdSet = new HashSet<>();
            for (ProductInstance pi : UserSpecificCachedDataHelper.getCustomer(si.getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES).getProductInstances()) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance svcInst = m.getServiceInstance();
                    if (svcInst.getCustomerId() == si.getCustomerId()) {
                        accountIdSet.add(svcInst.getAccountId());
                    }
                }
            }
            if (!accountIdSet.contains(getAccount().getAccountId())) {
                log.debug("Account is not allowed");
                localiseErrorAndAddToGlobalErrors("invalid.provisioning.account");
                setCustomerQuery(new CustomerQuery());
                getCustomerQuery().setCustomerId(si.getCustomerId());
                setServiceInstance(new ServiceInstance());
                getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
                return showChangeServiceInstanceAccount();
            }
        }

        getProductOrder().setAction(StAction.NONE);
        getProductOrder().setProductInstanceId(si.getProductInstanceId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(getAccount().getAccountId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setCustomerId(si.getCustomerId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus(si.getStatus());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
        verifyChangeAccountAllowed(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAccountId(), si.getCustomerId());
        SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
        setServiceInstance(new ServiceInstance());
        getServiceInstance().setServiceInstanceId(si.getServiceInstanceId());
        setPageMessage("service.instance.modified.successfully");
        return retrieveServiceInstance();
    }

    public Resolution showChangeServiceInstanceSpecification() {
        checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_SPECIFICATION);
        // Set service spec info for the current service
        setServiceSpecification(NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceInstance().getServiceSpecificationId()));

        // Set product spec of current product
        setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(getProductInstance().getProductSpecificationId()));

        // Now we need to get the group id of the service instance
        int groupId = -1;
        for (ProductServiceSpecificationMapping pssm : getProductSpecification().getProductServiceSpecificationMappings()) {
            if (pssm.getServiceSpecification().getServiceSpecificationId() == getServiceSpecification().getServiceSpecificationId()) {
                groupId = pssm.getGroupId();
                log.debug("Group Id is [{}]", groupId);
                break;
            }
        }

        setProductServiceSpecificationMappings(new ArrayList<ProductServiceSpecificationMapping>());
        for (ProductServiceSpecificationMapping pssm : getProductSpecification().getProductServiceSpecificationMappings()) {
            if (pssm.getGroupId() != 0 && pssm.getGroupId() == groupId && pssm.getServiceSpecification().getServiceSpecificationId() != getServiceSpecification().getServiceSpecificationId()) {
                log.debug("Specification  [{}] is also in group id [{}] which is non zero", pssm.getServiceSpecification().getName(), groupId);
                getProductServiceSpecificationMappings().add(pssm);
            }
        }

        return getDDForwardResolution("/product_catalog/change_service_instance_specification.jsp");
    }

    public Resolution showChangeServiceInstanceConfiguration() {
        checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_CONFIGURATION);
        setServiceInstance(SCAWrapper.getUserSpecificInstance().getServiceInstance(getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN_SVCAVP));
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setCustomerId(getServiceInstance().getCustomerId());
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS));

        populateAVPDetailsIntoServiceInstanceAVPs(getServiceInstance());
        formatAVPsForDisplay(getServiceInstance().getAVPs());
        doICCIDPublicIdentityMapping(getServiceInstance().getAVPs());
        for (AVP avp : getServiceInstance().getAVPs()) {
            if (avp != null && avp.getAttribute() != null
                    && BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {

                if (getCustomer().getKYCStatus() != null
                        && getCustomer().getKYCStatus().equalsIgnoreCase("V")) {
                    //Check if this is the first product and customer is already verified?
                    if (avp.getAttribute().equalsIgnoreCase("KYCStatus")
                            && getCustomer().getProductInstancesTotalCount() == null) {
                        avp.setValue("Complete"); // For KYC complete/verified
                        log.warn("Setting AVP KYCStatus value to 'Complete' since this is firt product annd customer is NIDA verified.");
                    }
                    // In Tanzania, customer who are NIDA pilot must also use Nida Pilot to KYC their SIMS - http://jira.smilecoms.com/browse/HBT-9643
                    if (avp.getAttribute().equalsIgnoreCase("KYCVerifyingMethod")) {
                        if (avp.getValue().equalsIgnoreCase("Immigration")) {
                            avp.setValidationRule("option|Immigration");
                        } else if (avp.getValue().equalsIgnoreCase("NIDA Pilot")) {
                            avp.setValidationRule("option|NIDA Pilot");
                        }
                    }
                }
            }
        }

        return getDDForwardResolution("/product_catalog/change_service_instance_configuration.jsp");
    }

    private AVP getAVPConfig(String attributeName, int serviceSpecificationId) {
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(serviceSpecificationId);
        for (AVP ssAVP : ss.getAVPs()) {
            if (attributeName.equals(ssAVP.getAttribute())) {
                return ssAVP;
            }
        }
        return null;
    }

    /**
     * Service instance AVPs just have the name and value in them. This function
     * gets the service spec of the SI and copies the other AVP info into them
     */
    private void populateAVPDetailsIntoServiceInstanceAVPs(ServiceInstance si) {
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId());
        for (AVP siAVP : si.getAVPs()) {
            for (AVP ssAVP : ss.getAVPs()) {
                if (siAVP.getAttribute().equals(ssAVP.getAttribute())) {
                    siAVP.setInputType(ssAVP.getInputType());
                    siAVP.setProvisionRoles(ssAVP.getProvisionRoles());
                    siAVP.setTechnicalDescription(ssAVP.getTechnicalDescription());
                    siAVP.setUserDefined(ssAVP.isUserDefined());
                    siAVP.setValidationRule(ssAVP.getValidationRule());
                    if (siAVP.getValue() == null) {
                        siAVP.setValue(ssAVP.getValue());
                    }
                    break;
                }
            }
        }
        // Add any missing AVPs that are in the spec but not on the SI
        for (AVP ssAVP : ss.getAVPs()) {
            boolean found = false;
            for (AVP siAVP : si.getAVPs()) {
                if (siAVP.getAttribute().equals(ssAVP.getAttribute())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.debug("Adding attribute [{}]", ssAVP.getAttribute());
                si.getAVPs().add(ssAVP);
            }
        }
    }

    public Resolution configureChangeServiceInstanceSpecification() {
        checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_SPECIFICATION);

        setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(getProductInstance().getProductSpecificationId()));
        setProductServiceSpecificationMappings(new ArrayList<ProductServiceSpecificationMapping>());
        for (ProductServiceSpecificationMapping mapping : getProductSpecification().getProductServiceSpecificationMappings()) {
            if (mapping.getServiceSpecification().getServiceSpecificationId() == getNewServiceSpecificationId()) {
                getProductServiceSpecificationMappings().add(mapping);
                if (!isAllowed(mapping.getProvisionRoles())) {
                    localiseErrorAndAddToGlobalErrors("service.specification.not.allowed");
                    return showChangeServiceInstanceSpecification();
                }
                break;
            }
        }

        return getDDForwardResolution("/product_catalog/configure_change_service_instance_specification.jsp");

    }

    public Resolution changeServiceInstanceSpecification() {
        checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_SPECIFICATION);
        checkCSRF();
        getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN);

        //Verify user has permissions to change to this service spec
        ProductSpecification prodSpec = NonUserSpecificCachedDataHelper.getProductSpecification(getProductInstance().getProductSpecificationId());
        for (ProductServiceSpecificationMapping mapping : prodSpec.getProductServiceSpecificationMappings()) {
            if (mapping.getServiceSpecification().getServiceSpecificationId() == getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId()) {
                if (!isAllowed(mapping.getProvisionRoles())) {
                    localiseErrorAndAddToGlobalErrors("service.specification.not.allowed");
                    return showChangeServiceInstanceSpecification();
                }
                break;
            }
        }

        getProductOrder().setAction(StAction.NONE);
        getProductOrder().setProductInstanceId(si.getProductInstanceId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setCustomerId(si.getCustomerId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus(si.getStatus());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());
        formatAVPsForSendingToSCA(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs(), getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId(), true);
        SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
        setServiceInstance(new ServiceInstance());
        getServiceInstance().setServiceInstanceId(si.getServiceInstanceId());
        setPageMessage("service.instance.modified.successfully");
        return retrieveServiceInstance();
    }

    public Resolution changeServiceInstanceConfiguration() {
        checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_CONFIGURATION);
        checkCSRF();
        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
        String publicIdentityOld = null;
        if (si.getAVPs() != null) {
            for (AVP avp : si.getAVPs()) {
                if (avp != null && avp.getAttribute() != null) {
                    if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                        publicIdentityOld = avp.getValue();
                    }
                }
                if (avp != null && avp.getAttribute() != null) {
                    if (avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                        impuMappedICCID = avp.getValue();
                    }
                }
            }
        }
        ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
        getProductOrder().setAction(StAction.NONE);
        getProductOrder().setOrganisationId(pi.getOrganisationId());
        getProductOrder().setCustomerId(pi.getCustomerId());
        getProductOrder().setProductInstanceId(si.getProductInstanceId());
        getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setCustomerId(si.getCustomerId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus(si.getStatus());
        // getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(verifyChangeAccountAllowed(si.getAccountId(), si.getCustomerId()));
        // No need to verify change of account here since we are using the same account id linked to the service we are modifying.
        // The KiosSales agents in Tanzania who do not have the  CHANGE_SERVICE_INSTANCE_EXISTING_ACCOUNT must still be able to change service instance configurstion
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());
        formatAVPsForSendingToSCA(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs(), getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId(),
                true);
        String publicIdentityNew = null;
        for (ServiceInstanceOrder sio : getProductOrder().getServiceInstanceOrders()) {
            if (sio.getServiceInstance() != null && sio.getServiceInstance().getAVPs() != null) {
                for (AVP avp : sio.getServiceInstance().getAVPs()) {
                    if (avp != null && avp.getAttribute() != null) {
                        if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                            publicIdentityNew = avp.getValue();
                        }
                    }
                }
            }
        }
        log.debug("Old IMPU [{}] New IMPU [{}]", publicIdentityOld, publicIdentityNew);
        if (publicIdentityOld != null && publicIdentityNew != null && !publicIdentityOld.equals(publicIdentityNew)) {
            log.debug("This order has a public identity change. Checking if it is a golden number");
            NumbersQuery nq = new NumbersQuery();
            nq.setResultLimit(1);
            nq.setPattern(publicIdentityNew);
            nq.setPriceLimitCents(Integer.MAX_VALUE);
            // Ignore ownership and show all. If its not owned by this person then processOrder will pick that up
            nq.setOwnedByCustomerProfileId(-1);
            nq.setOwnedByOrganisationId(-1);
            nq.setICCID("");
            NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(nq);
            if (list.getNumberOfNumbers() != 1 || !list.getNumbers().get(0).getIMPU().equals(publicIdentityNew)) {
                localiseErrorAndAddToGlobalErrors("error.invalid.public.identity");
            } else {

                if (BaseUtils.getBooleanProperty("env.iccid.impu.strict.mapping.enabled", false)
                        && !list.getNumbers().get(0).getICCID().isEmpty()
                        && !list.getNumbers().get(0).getICCID().equals(impuMappedICCID)) {
                    localiseErrorAndAddToGlobalErrors("error.invalid.iccid.public.identity.mapping");
                    return showChangeServiceInstanceConfiguration();
                }

                publicIdentityCostCents = list.getNumbers().get(0).getPriceCents();
                if (publicIdentityCostCents > 0) {
                    checkPermissions(Permissions.PROVISION_GOLDEN_NUMBER);
                }
            }
        } else {
            log.debug("No IMPU Change");
        }
        try {
            SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
            setServiceInstance(new ServiceInstance());
            getServiceInstance().setServiceInstanceId(si.getServiceInstanceId());
            setPageMessage("service.instance.modified.successfully");
        } catch (SCABusinessError e) {
            showChangeServiceInstanceConfiguration();
            throw e;
        }
        return retrieveServiceInstance();
    }

    public Resolution showChangeServiceInstanceCustomer() {
        checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_CUSTOMER);
        setServiceSpecification(NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceInstance().getServiceSpecificationId()));
        return getDDForwardResolution("/product_catalog/change_service_instance_customer.jsp");
    }

    public Resolution showChangeProductInstanceCustomer() {
        checkPermissions(Permissions.CHANGE_PRODUCT_INSTANCE_CUSTOMER);
        setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(getProductInstance().getProductSpecificationId()));
        segments = Utils.getListFromCRDelimitedString(getProductSpecification().getSegments());
        getCustomerQuery().setResultLimit(1);
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        return getDDForwardResolution("/product_catalog/change_product_instance_customer.jsp");
    }

    public Resolution changeServiceInstanceCustomer() {
        checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_CUSTOMER);
        if (notConfirmed()) {
            return confirm();
        }
        checkCSRF();
        getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN);
        getProductOrder().setAction(StAction.NONE);
        getProductOrder().setProductInstanceId(si.getProductInstanceId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus(si.getStatus());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
        SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
        setServiceInstance(new ServiceInstance());
        getServiceInstance().setServiceInstanceId(si.getServiceInstanceId());
        setPageMessage("service.instance.modified.successfully");
        return retrieveServiceInstance();
    }

    public Resolution changeProductInstanceCustomer() {
        checkPermissions(Permissions.CHANGE_PRODUCT_INSTANCE_CUSTOMER);
        if (notConfirmed()) {
            return confirm();
        }
        checkCSRF();
        getProductOrder().setAction(StAction.UPDATE);
        if (getParameter("applyToAllServiceInstances") != null && getParameter("applyToAllServiceInstances").equals("true")) {
            ProductInstance pi = SCAWrapper.getUserSpecificInstance().getProductInstance(getProductOrder().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC);
            for (ProductServiceInstanceMapping mapping : pi.getProductServiceInstanceMappings()) {
                ServiceInstanceOrder siOrder = new ServiceInstanceOrder();
                siOrder.setAction(StAction.UPDATE);
                siOrder.setServiceInstance(mapping.getServiceInstance());
                siOrder.getServiceInstance().setCustomerId(getProductOrder().getCustomerId());
                getProductOrder().getServiceInstanceOrders().add(siOrder);
            }
        }
        if (getProductOrder().getOrganisationId() == null) {
            getProductOrder().setOrganisationId(0);
        }
        SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
        setProductInstance(new ProductInstance());
        getProductInstance().setProductInstanceId(getProductOrder().getProductInstanceId());
        setPageMessage("product.instance.modified.successfully");
        return retrieveProductInstance();
    }

    public Resolution retrieveServiceSpecification() {
        checkPermissions(Permissions.VIEW_PRODUCT_CATALOG);
        if (getServiceSpecification() != null) {
            setServiceSpecification(NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceSpecification().getServiceSpecificationId()));
        }

        return getDDForwardResolution("/product_catalog/view_service_specification.jsp");
    }

    public Resolution retrieveServiceInstances() {
        checkPermissions(Permissions.VIEW_PRODUCT_OR_SERVICE_INSTANCES);
        getServiceInstanceQuery().setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        setServiceInstanceList(SCAWrapper.getUserSpecificInstance().getServiceInstances(getServiceInstanceQuery()));
        return getDDForwardResolution("/product_catalog/view_service_instances.jsp");
    }

    public Resolution retrieveServiceInstance() {
        checkPermissions(Permissions.VIEW_PRODUCT_OR_SERVICE_INSTANCES);
        setServiceInstance(SCAWrapper.getUserSpecificInstance().getServiceInstance(getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN_SVCAVP_MAPPINGS));
        populateAVPDetailsIntoServiceInstanceAVPs(getServiceInstance());
        setProductInstance(SCAWrapper.getUserSpecificInstance().getProductInstance(getServiceInstance().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC));
        setRatePlan(SCAWrapper.getUserSpecificInstance().getRatePlan(makeSCAInteger(getServiceInstance().getRatePlanId())));
        CustomerQuery cq = new CustomerQuery();
        cq.setCustomerId(getServiceInstance().getCustomerId());
        cq.setResultLimit(1);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(cq));
        setServiceSpecification(NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceInstance().getServiceSpecificationId()));

        return getDDForwardResolution("/product_catalog/view_service_instance.jsp");
    }

    @DontValidate()
    public Resolution retrieveProductInstance() {
        clearValidationErrors(); // In case we got here from the search product instance id screen and no id was captured
        checkPermissions(Permissions.VIEW_PRODUCT_OR_SERVICE_INSTANCES);
        if (getProductInstance() != null && getProductInstance().getProductInstanceId() > 0) {
            // Search by product instance id
            try {
                setProductInstance(SCAWrapper.getUserSpecificInstance().getProductInstance(getProductInstance().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC_SVCAVP_CAMPAIGNS_CAMPAIGNUC));
            } catch (SCABusinessError be) {
                log.debug("No product instance data found for Product Instance Id [{}]", getProductInstance().getProductInstanceId());
            }
        } else if (getIMSSubscriptionQuery() != null
                && (!getIMSSubscriptionQuery().getIntegratedCircuitCardIdentifier().isEmpty()
                || !getIMSSubscriptionQuery().getIMSPrivateIdentity().isEmpty()
                || !getIMSSubscriptionQuery().getIMSPublicIdentity().isEmpty())) {
            // Search by ICCID, IMPI or IMPU by getting the SIM public and private ID's and finding all mapped SI's
            try {
                getIMSSubscriptionQuery().setIMSPublicIdentity(Utils.getPublicIdentityForPhoneNumber(getIMSSubscriptionQuery().getIMSPublicIdentity()));
                getIMSSubscriptionQuery().setVerbosity(StIMSSubscriptionLookupVerbosity.IMSU_IMPI_IMPU);
                IMSSubscription sub = SCAWrapper.getUserSpecificInstance().getIMSSubscription(getIMSSubscriptionQuery());
                ServiceInstanceQuery siq = new ServiceInstanceQuery();
                siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
                for (IMSPrivateIdentity impi : sub.getIMSPrivateIdentities()) {
                    siq.setIdentifier(impi.getIdentity());
                    siq.setIdentifierType("END_USER_PRIVATE");
                    ServiceInstanceList siList = SCAWrapper.getUserSpecificInstance().getServiceInstances(siq);
                    if (siList.getNumberOfServiceInstances() > 0) {
                        setProductInstance(SCAWrapper.getUserSpecificInstance().getProductInstance(siList.getServiceInstances().get(0).getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC));
                        break;
                    }
                }
            } catch (SCABusinessError be) {
                log.debug("No Subscription data found for ICCID [{}]", getIMSSubscriptionQuery().getIntegratedCircuitCardIdentifier());
            }
        } else if (getIMSI() != null && !getIMSI().isEmpty()) {
            // Search by IMSI
            ServiceInstanceQuery q = new ServiceInstanceQuery();
            q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            q.setIdentifierType("END_USER_PRIVATE");
            q.setIdentifier(Utils.makePrivateIdentityFromIMSI(imsi));
            ServiceInstanceList siList = SCAWrapper.getUserSpecificInstance().getServiceInstances(q);
            if (siList.getNumberOfServiceInstances() > 0) {
                setProductInstance(SCAWrapper.getUserSpecificInstance().getProductInstance(siList.getServiceInstances().get(0).getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC));
            }
        }
        if (getProductInstance() == null || getProductInstance().getProductSpecificationId() == 0) {
            localiseErrorAndAddToGlobalErrors("no.records.found");
            return showSearchProductInstance();
        }
        CustomerQuery cq = new CustomerQuery();
        cq.setCustomerId(getProductInstance().getCustomerId());
        cq.setResultLimit(1);
        cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(cq));
        setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(getProductInstance().getProductSpecificationId()));
        return getDDForwardResolution("/product_catalog/view_product_instance.jsp");
    }

    public Resolution retrieveRatePlan() {
        checkPermissions(Permissions.VIEW_PRODUCT_CATALOG);
        if (getServiceInstance() != null) {
            setRatePlan(SCAWrapper.getUserSpecificInstance().getRatePlan(makeSCAInteger(getServiceInstance().getRatePlanId())));
        } else {
            setRatePlan(SCAWrapper.getUserSpecificInstance().getRatePlan(makeSCAInteger(getRatePlan().getRatePlanId())));
        }

        return getDDForwardResolution("/product_catalog/view_rate_plan.jsp");
    }

    public Resolution retrieveCustomer() {
        checkPermissions(Permissions.VIEW_CUSTOMER);
        return getDDForwardResolution(CustomerActionBean.class, "retrieveCustomer");
    }
    /**
     * **************************
     * PRODUCT PROVISIONING **************************
     */
    private List<String> segments;

    public List<String> getSegments() {
        return segments;
    }

    public List<String[]> getKits() {
        List<String[]> dashBoardItems = BaseUtils.getPropertyFromSQLWithoutCache("env.pos.kits.itemnumber.description");
        Set<String> reservedRetailKits = Utils.getSetFromCRDelimitedString(BaseUtils.getPropertyWithoutCache("env.pos.reserved.retail.kits"));
        List<String[]> dashBoardItemsToDisplay = new ArrayList<>();

        boolean mustAddKitToDashboard = true;
        log.debug("Number of dash board items BEFORE filtering out reserved kits [{}]", dashBoardItems.size());
        for (String[] dashBoardItem : dashBoardItems) {
            for (String reservedKit : reservedRetailKits) {
                if (reservedKit.equals(dashBoardItem[0])) {
                    mustAddKitToDashboard = false;
                    break;
                }
            }
            log.debug("Looking at kit [{}] mustAddToDashboard [{}]", dashBoardItem[0], mustAddKitToDashboard);
            if (mustAddKitToDashboard) {
                dashBoardItemsToDisplay.add(dashBoardItem);
            }
            mustAddKitToDashboard = true; //reset
        }
        log.debug("Number of dash board items AFTER filtering out reserved kits [{}]", dashBoardItemsToDisplay.size());
        return dashBoardItemsToDisplay;
    }

    public String getProductAccount() {
        if (getAccount().getAccountId() == -1) {
            return "Create New Account";
        }
        return String.valueOf(getAccount().getAccountId());
    }

    public void setProductAccount(String productAccount) {
        if (productAccount.equals("Create New Account")) {
            productAccount = "-1";
        }
        setAccount(new Account());
        getAccount().setAccountId(Long.valueOf(productAccount));
    }

    @DontValidate()
    public Resolution collectGeneralDataForProductInstall() {

        if (getProductOrder().getProductSpecificationId() == null) {
            return showAddProductWizard();
        }

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));

        if (canChangeServiceToAnyAccount() || canChangeServiceToExistingAccount()) {
            log.debug("Getting allowed accounts");
            accountIdSet = new HashSet<>();
            for (ProductInstance pi : getCustomer().getProductInstances()) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    if (si.getCustomerId() == getCustomer().getCustomerId()) {
                        accountIdSet.add(si.getAccountId());
                    }
                }
            }
        }

        segments = Utils.getListFromCRDelimitedString(NonUserSpecificCachedDataHelper.getProductSpecification(getProductOrder().getProductSpecificationId()).getSegments());

        return getDDForwardResolution("/product_catalog/add_product_general_data.jsp");

    }

    @DontValidate()
    public Resolution goBackToCollectGeneralDataForProductInstall() {
        return collectGeneralDataForProductInstall();
    }

    public Resolution validateGeneralData() {

        // Prevent any NPE's
        if (getProductOrder() == null || getProductOrder().getSegment() == null || getProductOrder().getSegment().isEmpty() || getProductOrder().getProductSpecificationId() == null) {
            localiseErrorAndAddToGlobalErrors("invalid.segment");
            return collectGeneralDataForProductInstall();
        }

        setCustomer(UserSpecificCachedDataHelper.getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        
        log.debug("Validating kit data");
        if (getProductOrder().getDeviceSerialNumber() != null && !getProductOrder().getDeviceSerialNumber().isEmpty()) {
            if (getProductOrder().getKitItemNumber() == null || getProductOrder().getKitItemNumber().isEmpty()) {
                localiseErrorAndAddToGlobalErrors("device.requires.kit");
                return collectGeneralDataForProductInstall();
            }
            // Check 2 things:
            // 1 - the device is not a SIM and is part of the kit
            // 2 - the device is held by the organisation
            SoldStockLocationQuery q = new SoldStockLocationQuery();
            q.setSerialNumber(getProductOrder().getDeviceSerialNumber());
            SoldStockLocationList list = SCAWrapper.getUserSpecificInstance().getSoldStockLocations(q);
            int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();
            if (list.getNumberOfSoldStockLocations() == 0 || list.getSoldStockLocations().get(0).getHeldByOrganisationId() != orgId) {
                localiseErrorAndAddToGlobalErrors("device.stock.not.held");
                return collectGeneralDataForProductInstall();
            }
            String deviceItemNumber = list.getSoldStockLocations().get(0).getItemNumber();
            if (deviceItemNumber.startsWith("SIM") || deviceItemNumber.startsWith("BUN")) {
                localiseErrorAndAddToGlobalErrors("device.serial.not.provided");
                return collectGeneralDataForProductInstall();
            }
            String kitItemNumber = getProductOrder().getKitItemNumber();

            // Hacky to get this from a property but POS does not allow getting subitems so its a shortcut to writing all that code
            List<String[]> kitItems = BaseUtils.getPropertyFromSQL("env.pos.kits.items");
            boolean found = false;
            for (String[] kitItem : kitItems) {
                if (kitItem[0].equals(kitItemNumber) && kitItem[1].equalsIgnoreCase(deviceItemNumber)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                localiseErrorAndAddToGlobalErrors("device.not.part.of.kit");
                return collectGeneralDataForProductInstall();
            }
        }

        if (getProductOrder().getKitItemNumber() != null && !getProductOrder().getKitItemNumber().isEmpty()
                && (getProductOrder().getDeviceSerialNumber() == null || getProductOrder().getDeviceSerialNumber().isEmpty())) {
            // Verify this kit is a SIM only
            List<String[]> kitItems = BaseUtils.getPropertyFromSQL("env.pos.kits.items");
            boolean foundDevice = false;
            for (String[] kitItem : kitItems) {
                if (kitItem[0].equals(getProductOrder().getKitItemNumber()) && !kitItem[1].startsWith("SIM") && !kitItem[1].startsWith("BUN")) {
                    foundDevice = true;
                    break;
                }
            }
            if (foundDevice) {
                localiseErrorAndAddToGlobalErrors("device.required.in.kit");
                return collectGeneralDataForProductInstall();
            }
        }

        log.debug("Validating segment/org data");

        String segment = getProductOrder().getSegment().toLowerCase();
        if (getProductOrder().getOrganisationId() == null) {
            getProductOrder().setOrganisationId(0);
        }
        if (segment.contains("work") || segment.contains("us") || segment.contains("staff") || segment.contains("business")) {
            log.debug("This is a Smile@Work or Smile@Us or Smile@Business or Staff segment so an organisation must be selected");
            if (getCustomer().getCustomerRoles().isEmpty()) {
                log.debug("Customer has no organisation roles and thus cannot use Smile@Work or Smile@Us or Smile@Business or Smile Staff");
                localiseErrorAndAddToGlobalErrors("segment.needs.an.organisation");
                return collectGeneralDataForProductInstall();
            }
        } else if (getProductOrder().getOrganisationId() != 0) {
            localiseErrorAndAddToGlobalErrors("segment.cannot.have.an.organisation");
            return collectGeneralDataForProductInstall();
        }

        if (segment.contains("staff") && getProductOrder().getOrganisationId() != 1) {
            log.debug("Staff segment cannot be selected on customer who does not have a role at Smile");
            localiseErrorAndAddToGlobalErrors("staff.segment.for.smile.only");
            return collectGeneralDataForProductInstall();
        }
        if ((segment.contains("work") || segment.contains("us") || segment.contains("business")) && getProductOrder().getOrganisationId() == 0) {
            localiseErrorAndAddToGlobalErrors("segment.needs.an.organisation");
            return collectGeneralDataForProductInstall();
        }
        if ((getProductOrder().getSegment().toLowerCase().contains("work")
                || getProductOrder().getSegment().toLowerCase().contains("us")
                || getProductOrder().getSegment().toLowerCase().contains("business"))
                && getProductOrder().getOrganisationId() == 1) {
            localiseErrorAndAddToGlobalErrors("segment.needs.a.non.smile.organisation");
            return collectGeneralDataForProductInstall();
        }

        if (getAccount() == null) {
            log.debug("No account specified. Must be -1");
            setAccount(new Account());
            getAccount().setAccountId(-1);
        }

        log.debug("Validating account");
        if (getAccount().getAccountId() != -1) {

            if (!canChangeServiceToAnyAccount() && !canChangeServiceToExistingAccount()) {
                localiseErrorAndAddToGlobalErrors("invalid.provisioning.account");
                return collectGeneralDataForProductInstall();
            }

            accountIdSet = new HashSet<>();
            for (ProductInstance pi : getCustomer().getProductInstances()) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    if (si.getCustomerId() == getCustomer().getCustomerId()) {
                        accountIdSet.add(si.getAccountId());
                    }
                }
            }

            if (!canChangeServiceToAnyAccount() && !accountIdSet.contains(getAccount().getAccountId())) {
                log.debug("Account is not allowed");
                localiseErrorAndAddToGlobalErrors("invalid.provisioning.account");
                return collectGeneralDataForProductInstall();
            }
        }

        return collectServiceInstanceDataForProductInstall("start");
    }

    /**
     * We now have the customer id, default account id and product spec id We
     * need to get the service specification avp data in order to populate that
     * into the productOrder
     *
     * This function must check which service spec in the product we are working
     * with (currentServiceSpecificationId), and display the form to collect the
     * data User can go back or forward and all data must be retained
     *
     * @param direction
     * @return
     */
    
    private boolean nimcApproved=false;

    public boolean isNimcApproved() {
        return nimcApproved;
    }

    public void setNimcApproved(boolean nimcApproved) {
        this.nimcApproved = nimcApproved;
    }
    
    public Resolution proceedToAddProduct() {
        
        setninVerified(true);
        setNimcApproved(true);

        HttpSession session = getContext().getRequest().getSession();                       
        setProductOrder((ProductOrder) session.getAttribute("sessionAddProduct"));
        session.removeAttribute("sessionAddProduct");        
        session.removeAttribute("sessionSale");
        session.setAttribute("ninVerified","true");
        
        setCustomerQuery(new CustomerQuery());
        getCustomerQuery().setCustomerId(Integer.parseInt(String.valueOf(session.getAttribute("custId"))));
        getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
        
        session.removeAttribute("custId");
       return showAddProductWizard();
    }
    
    public Resolution collectServiceInstanceDataForProductInstall(String direction) {
        boolean hasSim = false;
        boolean simInSale = false;
        log.debug("Forward is [{}]", direction);
        checkPermissions(Permissions.ADD_PRODUCT_INSTANCE);
            
        if (getProductOrder().getProductSpecificationId() == null) {
            return collectGeneralDataForProductInstall();
        }
        int productSpecificationId = getProductOrder().getProductSpecificationId();
        log.debug("The product specification Id for this order is [{}]. Going to get the service specification data", productSpecificationId);
        setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(productSpecificationId));
        log.debug("This product has [{}] service specifications", getProductSpecification().getProductServiceSpecificationMappings().size());

        if (direction.equalsIgnoreCase("forward")) {
            currentServiceSpecificationIndex++;
            log.debug("currentServiceSpecificationIndex has been incremented. It is now [{}]", currentServiceSpecificationIndex);
        } else if (direction.equalsIgnoreCase("backward")) {
            currentServiceSpecificationIndex--;
            log.debug("currentServiceSpecificationIndex has been decremented. It is now [{}]", currentServiceSpecificationIndex);
        }

        if (currentServiceSpecificationIndex >= getProductSpecification().getProductServiceSpecificationMappings().size()) {
            log.debug("That was the last service specification. Now we can confirm the data");
            return showAddProductSummary();
        } else if (currentServiceSpecificationIndex < 0) {
            log.debug("currentServiceSpecificationIndex is less than zero. It is [{}]", currentServiceSpecificationIndex);
            return collectGeneralDataForProductInstall();
        }

        log.debug("currentServiceSpecificationIndex is [{}]. Checking if it can be used", currentServiceSpecificationIndex);

        if (!isCurrentServiceAllowed()) {
            log.debug("Service spec at index [{}] cannot be used. Setting it to skipped", currentServiceSpecificationIndex);
            String newDirection = direction;
            if (direction.equalsIgnoreCase("start")) {
                newDirection = "forward";
            }
            return collectServiceInstanceDataForProductInstall(newDirection);
        }
        log.debug("Service spec at index [{}] can be used", currentServiceSpecificationIndex);

        if (getProductOrder().getServiceInstanceOrders().size() > 0) {
            if (impuMappedICCID == null || impuMappedICCID.isEmpty()) {
                log.debug("impuMappedICCID is NULL");
                boolean canBreak = false;
                for (ServiceInstanceOrder s : getProductOrder().getServiceInstanceOrders()) {
                    for (AVP avp : s.getServiceInstance().getAVPs()) {
                        if (avp != null && avp.getAttribute() != null && avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                            log.debug("IntegratedCircuitCardIdentifier FOUND and MappedICCID is [{}]", impuMappedICCID);
                            simInSale=true;
                            if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria") && !isNimcApproved() && BaseUtils.getBooleanProperty("env.nimc.use.fingerprint",false)) {
                                
                                hasSim = true;               
                            }
                            
                            impuMappedICCID = avp.getValue();
                            canBreak = true;
                            break;
                        }
                    }
                    if (canBreak) {
                        break;
                    }
                }
            }
            if (currentServiceSpecificationIndex > -1) {
                log.debug("IN currentServiceSpecificationIndex > -1 Going to doICCIDPublicIdentityMapping");
                doICCIDPublicIdentityMapping(getProductSpecification().getProductServiceSpecificationMappings().get(currentServiceSpecificationIndex).getServiceSpecification().getAVPs());
            }
        }

        if (BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getProductOrder().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS));
        }

        //  <c:forEach items="${actionBean.productSpecification.productServiceSpecificationMappings[actionBean.currentServiceSpecificationIndex].serviceSpecification.AVPs}" var="avp" varStatus="loop">
        EventList eL = null;
        for (AVP avp : getProductSpecification().getProductServiceSpecificationMappings().get(currentServiceSpecificationIndex).getServiceSpecification().getAVPs()) {
            if (avp != null && avp.getAttribute() != null
                    && BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)) {

                setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getProductOrder().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS));

                if (getCustomer() != null
                        && (getCustomer().getKYCStatus() != null
                        && getCustomer().getKYCStatus().equalsIgnoreCase("V"))) {
                    //Check if this is the first product and customer is already verified?
                    if (avp.getAttribute().equalsIgnoreCase("KYCStatus")
                            && getCustomer().getProductInstancesTotalCount() == null) {
                        avp.setValue("Complete"); // For KYC complete/verified
                        log.warn("Setting AVP KYCStatus value to 'Complete' since this is firt product annd customer is NIDA verified.");

                        // Go and extract the customer's NIDA verification status:
                        if (eL == null) {

                            GregorianCalendar dateFrom = getCustomer().getCreatedDateTime().toGregorianCalendar();
                            dateFrom.add(Calendar.DAY_OF_MONTH, -1);
                            EventQuery eq = new EventQuery();
                            try {
                                eq.setDateFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(dateFrom));
                            } catch (Exception ex) {
                                log.error("Error while setting EventQuery.dateFrom, will search without it");
                            }
                            eq.setEventKey(String.valueOf(getProductOrder().getCustomerId()));
                            eq.setEventType("SEP");
                            eq.setEventSubType("NIDAQueryResponse");

                            eq.setResultLimit(1);

                            eL = SCAWrapper.getUserSpecificInstance().getEvents_Direct(eq);
                        }

                        if (eL != null && eL.getNumberOfEvents() > 0) {

                            String[] nidaFields = eL.getEvents().get(0).getEventData().split("|");
                            if (nidaFields.length >= 6) {
                                String entityType = nidaFields[0];
                                String responseCode = nidaFields[1];
                                String transactionId = nidaFields[4];
                                String verifiedDate = nidaFields[5];
                                if (avp.getAttribute().equalsIgnoreCase("NIDATransactionId")) {
                                    avp.setValue(transactionId);
                                }

                                if (avp.getAttribute().equalsIgnoreCase("NIDAVerifiedOnDate")) {
                                    avp.setValue(verifiedDate);
                                }
                            }
                        }
                    }
                    // In Tanzania, customer who are NIDA pilot must also use Nida Pilot to KYC their SIMS - http://jira.smilecoms.com/browse/HBT-9643     
//                    if (avp.getAttribute().equalsIgnoreCase("KYCVerifyingMethod")) {
//                        avp.setValidationRule("option|NIDA Pilot");
//                    }

                }
            }
        }
        
     if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria") && simInSale) {        
        //Search for other profiles sharing the same NIN as the purchasing profile (Customer may have more than one profile)
        CustomerQuery custQ = new CustomerQuery();
        custQ.setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
        custQ.setResultLimit(20);
        custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES_SVCAVP);
        CustomerList custList = SCAWrapper.getAdminInstance().getCustomers(custQ);        
        Set accounts= new HashSet();
        
        for(Customer cust: custList.getCustomers()) {            
            List<ProductInstance> productInstances = cust.getProductInstances();

            if (cust.getCustomerRoles().isEmpty()) {  //Do this to individuals only                                
                for (ProductInstance pi : productInstances) {                    
                    if (pi.getStatus().equalsIgnoreCase("AC")) {                        
                        for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {                            
                            ServiceInstance si = m.getServiceInstance();                            
                            if (si.getCustomerId() == cust.getCustomerId()) {
                                if (si.getStatus().equalsIgnoreCase("AC") && !accounts.contains(si.getAccountId())) {
                                    accounts.add(si.getAccountId());                                    
                                }
                            }
                        }
                    }
                }

                if (accounts.size() >= BaseUtils.getIntProperty("sim.limit.to.no", 4)) {
                    localiseErrorAndAddToGlobalErrors("error", "Cannot continue with product provisioning.  Client already has " + BaseUtils.getIntProperty("sim.limit.to.no", 4) + " or more accounts with active SIM.");
                    return getDDForwardResolution("/customer/view_customer.jsp");
                }
            }
        }
    }
   
        
    HttpSession session = getContext().getRequest().getSession();  
    if(session.getAttribute("fromSale")==null || !session.getAttribute("fromSale").equals("true")) {
        if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria") && hasSim && (session.getAttribute("ninVerified")==null || !session.getAttribute("ninVerified").equals("true"))) { 
            
            if(getCustomer().getNationalIdentityNumber()==null || getCustomer().getNationalIdentityNumber().trim().length()==0) {
                //Cannot make sim sale without NIN
                session.removeAttribute("fromSale");
                localiseErrorAndAddToGlobalErrors("customer.nin.not.captured");
                return getDDForwardResolution("/customer/view_customer.jsp");
            }
            
            session.setAttribute("sessionAddProduct", getProductOrder());  
            session.setAttribute("custId", getCustomer().getCustomerId());  
            session.removeAttribute("sessionSale");            
            return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");                                            
        }
    }
        
        
        getCustomer().setNationality(BaseUtils.getProperty("env.locale.country.for.language.en"));
        return getDDForwardResolution("/product_catalog/add_product_configure_service_instance.jsp");
    }

    public boolean isCurrentServiceAllowed() {

        if (!isAllowed(getProductSpecification().getProductServiceSpecificationMappings().get(currentServiceSpecificationIndex).getProvisionRoles())) {
            log.debug("Logged in user is not allowed to provision this service with spec id [{}]. Skip it. Index is [{}]", getProductSpecification().getProductServiceSpecificationMappings().get(currentServiceSpecificationIndex).getServiceSpecification().getServiceSpecificationId(), currentServiceSpecificationIndex);
            return false;
        }

        ProductServiceSpecificationMapping mapping = getProductSpecification().getProductServiceSpecificationMappings().get(currentServiceSpecificationIndex);
        return Utils.isBetween(new Date(), mapping.getAvailableFrom(), mapping.getAvailableTo());
    }

    @DontValidate
    public Resolution showAddProductWizard() {
        checkPermissions(Permissions.ADD_PRODUCT_INSTANCE);
        if (getCustomer() == null || getCustomer().getCustomerId() <= 0) {
            return getDDForwardResolution(CustomerActionBean.class, "showSearchCustomer");
        }
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS));
        ProductSpecificationList list = SCAWrapper.getUserSpecificInstance().getAllProductSpecifications(StProductSpecificationLookupVerbosity.MAIN);
        setProductSpecificationList(new ProductSpecificationList());
        for (ProductSpecification ps : list.getProductSpecifications()) {
            log.debug("Product spec has available from [{}] available to [{}] and provision roles [{}]", new Object[]{ps.getAvailableFrom(), ps.getAvailableTo(), ps.getProvisionRoles()});
            if (Utils.isBetween(new Date(), ps.getAvailableFrom(), ps.getAvailableTo()) && isAllowed(ps.getProvisionRoles())) {
                getProductSpecificationList().getProductSpecifications().add(ps);
            }
        }
        clearValidationErrors(); // In case we got here from the account id screen and no id was captured
        if (!getCustomer().getClassification().equals("customer") && !getCustomer().getClassification().equals("diplomat") && !getCustomer().getClassification().equals("foreigner")) {
            localiseErrorAndAddToGlobalErrors("customer.not.fully.registered");
        }
        return getDDForwardResolution("/product_catalog/add_product_select_product.jsp");
    }

    public Resolution showAddProductSummary() {
        checkPermissions(Permissions.ADD_PRODUCT_INSTANCE);
        ProductOrder order = getProductOrder();
        if (order == null) {
            return getDDForwardResolution(LoginActionBean.class, "goHome");
        }
        // Get the customer so we can show the name etc
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(order.getCustomerId());
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(q));

        String publicIdentity = null;
        String svcIdentifier = null;
        for (ServiceInstanceOrder sio : order.getServiceInstanceOrders()) {
            if (sio != null && sio.getServiceInstance() != null && sio.getServiceInstance().getAVPs() != null) {
                formatAVPsForSendingToSCA(sio.getServiceInstance().getAVPs(), sio.getServiceInstance().getServiceSpecificationId(), false);
                for (AVP avp : sio.getServiceInstance().getAVPs()) {
                    if (avp != null && avp.getAttribute() != null) {
                        if (avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                            iccid = avp.getValue();
                        }
                        if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                            publicIdentity = avp.getValue();
                        }
                        if (avp.getAttribute().equalsIgnoreCase("NAIUsername")) {
                            svcIdentifier = avp.getValue();
                        }
                    }
                }
            }
        }

        if (getAccount() == null) {
            setAccount(new Account());
            getAccount().setAccountId(-1);
        }

        boolean isICCIDMappedToPublicIdentity = (iccid != null && provisionForMappedImpuICCID(iccid));

        if (publicIdentity != null && !publicIdentity.isEmpty() && !isICCIDMappedToPublicIdentity) {
            log.debug("This order has a public identity. Checking if it is a golden number");
            NumbersQuery nq = new NumbersQuery();
            nq.setResultLimit(1);
            nq.setPattern(publicIdentity);
            nq.setPriceLimitCents(Integer.MAX_VALUE);
            nq.setOwnedByCustomerProfileId(-1);
            nq.setOwnedByOrganisationId(-1);
            nq.setICCID("");
            NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(nq);
            if (list.getNumberOfNumbers() != 1 || !list.getNumbers().get(0).getIMPU().equals(publicIdentity)) {
                localiseErrorAndAddToGlobalErrors("error.invalid.public.identity");
            } else {

                if (!list.getNumbers().get(0).getICCID().isEmpty() && !list.getNumbers().get(0).getICCID().equals(iccid)) {
                    localiseErrorAndAddToGlobalErrors("error.invalid.iccid.public.identity.mapping");
                    return collectServiceInstanceDataForProductInstall("backward");
                }

                publicIdentityCostCents = list.getNumbers().get(0).getPriceCents();
                if (publicIdentityCostCents > 0) {
                    checkPermissions(Permissions.PROVISION_GOLDEN_NUMBER);
                }
            }
        }

        if (svcIdentifier != null && !svcIdentifier.isEmpty()) {
            log.debug("This order has a PPPX identifier. Checking if username [{}] is available for use", svcIdentifier);
            boolean svcIdentifierAvailable = true;
            try {
                NAIIdentityQuery naiIdentityQuery = new NAIIdentityQuery();
                naiIdentityQuery.setNAIUsername(svcIdentifier);
                NAIIdentity naiIdentity = SCAWrapper.getUserSpecificInstance().getNAIIdentity(naiIdentityQuery);
                svcIdentifierAvailable = false;
                log.debug("PPP identifier [{}] is not available for use", naiIdentity.getNAIUsername());
            } catch (Exception e) {
                log.warn("Error occured: ", e);
            }
            if (!svcIdentifierAvailable) {
                boolean canBreakId = false;
                boolean canBreakPwd = false;
                for (ServiceInstanceOrder sio : order.getServiceInstanceOrders()) {
                    for (AVP avp : sio.getServiceInstance().getAVPs()) {
                        if (avp != null && avp.getAttribute() != null) {
                            if (avp.getAttribute().equalsIgnoreCase("NAIUsername")) {
                                avp.setValue(Utils.getFriendlyNAI(avp.getValue()));
                                canBreakId = true;
                            }
                            if (avp.getAttribute().equalsIgnoreCase("NAIPassword")) {
                                avp.setValue("");
                                canBreakPwd = true;
                            }
                        }
                        if (canBreakPwd && canBreakId) {
                            break;
                        }
                    }
                    if (canBreakPwd && canBreakId) {
                        break;
                    }
                }
                localiseErrorAndAddToGlobalErrors("error.network.access.identity.exist");
                return collectServiceInstanceDataForProductInstall("backward");
            }
        }
        log.debug("Going to show product Summary. currentServiceSpecificationIndex is [{}]", currentServiceSpecificationIndex);
        return getDDForwardResolution("/product_catalog/add_product_summary.jsp");
    }

    public Resolution collectServiceInstanceDataForProductInstallBack() {
        return collectServiceInstanceDataForProductInstall("backward");
    }

    public Resolution collectServiceInstanceDataForProductInstallNext() {
        return collectServiceInstanceDataForProductInstall("forward");
    }

    /**
     * First get rid of any skipped service instances Note that SCA will
     * populate all non-user defined avps as well as the rate plan for each SI
     *
     * @return
     */
    public Resolution doProductProvisioning() {
        checkCSRF();
        checkPermissions(Permissions.ADD_PRODUCT_INSTANCE);

        if (getProductOrder().getPromotionCode() == null) {
            getProductOrder().setPromotionCode("");
        }
        if (!getProductOrder().getPromotionCode().isEmpty()) {
            checkPermissions(Permissions.USE_PRODUCT_PROMOTION_CODE);
        }

        if (getProductOrder().isAllowPendingSIMSale()) {
            checkPermissions(Permissions.IGNORE_PP_SALE_STATUS_ON_SIM);
        }

        getProductOrder().setAction(StAction.CREATE);
        List<ServiceInstanceOrder> finalSIOList = new ArrayList<>();

        log.debug("In doProductProvisioning");

        if (getProductOrder().getOrganisationId() == null) {
            getProductOrder().setOrganisationId(0);
        }

        String ordersICCID = null;
        SaleLine iccidSaleLine = null;
        
        if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria")) {            
            CustomerQuery custQ = new CustomerQuery();
            custQ.setNationalIdentityNumber(getCustomer().getNationalIdentityNumber());
            custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS);
            CustomerList custList = SCAWrapper.getAdminInstance().getCustomers(custQ);
            int numberOfAccounts=0;
            
            for(Customer cust: custList.getCustomers()) {            
                List<ProductInstance> productInstances = cust.getProductInstances();
                
                if (cust.getCustomerRoles().isEmpty()) {  //Apply this to individuals only                    
                    for (ProductInstance pi : productInstances) {
                        boolean prodis = false;                        
                        if (pi.getStatus().equalsIgnoreCase("AC")) {                            
                            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {                                
                                ServiceInstance si = m.getServiceInstance();
                                
                                boolean serviceis = false;
                                if (si.getCustomerId() == cust.getCustomerId()) {
                                    if (si.getStatus().equalsIgnoreCase("AC")) {
                                        serviceis = true;
                                    }
                                }

                                if (serviceis) {
                                    prodis = true;
                                }
                            }
                        }
                        if (prodis) {                            
                            numberOfAccounts += 1;
                        }
                    }

                    if (numberOfAccounts >= BaseUtils.getIntProperty("sim.limit.to.no", 4)) {
                        localiseErrorAndAddToGlobalErrors("sim.limit.allowed.to.provision.exceeded");
                        return showAddProductSummary();
                    }
                }
            }
        }
        
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS));
        if (BaseUtils.getBooleanProperty("env.customer.sim.limit.enabled", false)) {
            List<ProductInstance> productInstances = getCustomer().getProductInstances();
            int count = 0;
            if (getCustomer().getKYCStatus() != null) {
                if (!getCustomer().getKYCStatus().equalsIgnoreCase("O")) {
                    if (getCustomer().getCustomerRoles().isEmpty()) {
                        for (ProductInstance pi : productInstances) {
                            boolean prodis = false;
                            if (pi.getStatus().equalsIgnoreCase("AC")) {
                                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                                    ServiceInstance si = m.getServiceInstance();
                                    boolean serviceis = false;
                                    if (si.getCustomerId() == getCustomer().getCustomerId()) {
                                        if (si.getStatus().equalsIgnoreCase("AC")) {
                                            serviceis = true;
                                        }
                                    }

                                    if (serviceis) {
                                        prodis = true;
                                    }
                                }
                            }
                            if (prodis) {
                                count = count + 1;
                            }
                        }

                        if (count >= BaseUtils.getIntProperty("sim.limit.to.no", 10)) {
                            localiseErrorAndAddToGlobalErrors("sim.limit.allowed.to.provision.exceeded");
                            return showAddProductSummary();
                        }
                    }
                }
            }
        }

        for (ServiceInstanceOrder sio : getProductOrder().getServiceInstanceOrders()) {
            if (sio != null && sio.getServiceInstance() != null && sio.getServiceInstance().getAVPs() != null) {
                for (AVP avp : sio.getServiceInstance().getAVPs()) {
                    if (avp != null && avp.getAttribute() != null) {
                        if (avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                            ordersICCID = avp.getValue();
                            iccidSaleLine = getSaleRowUsingSerialNumber(ordersICCID);
                            if (!verifySIMLineItem(iccidSaleLine) && !hasPermissions(Permissions.PROVISION_INVALID_ICCID)) {
                                log.error("Invalid SIM item number for provisioning [{}]", ordersICCID);
                                localiseErrorAndAddToGlobalErrors("sim.invalid.item.code");
                                return showAddProductSummary();
                            }
                        }

                        if (getCustomer() != null && getCustomer().getProductInstancesTotalCount() != null
                                && BaseUtils.getBooleanProperty("env.customer.verify.with.nida", false)
                                && avp.getAttribute().equalsIgnoreCase("eKYCResults")
                                && getCustomer().getProductInstancesTotalCount() == 0 // Only do this on first product provisioning
                                && (getCustomer().getKYCStatus() != null && getCustomer().getKYCStatus().equals("V"))) {
                            avp.setValue("Complete"); // For KYC complete/verified
                        } else { //This is the second product

                        }
                    }
                }
            }
        }

        //Check if this SIM is restricted to be sold on its own original KIT it was sold on;
        if (iccidSaleLine != null) {
            //Check if this KIT does not allow SIM to be provisioned under any other KIT?
            if (isSimRestrictedToOriginalKit(iccidSaleLine, getProductOrder().getKitItemNumber())) {
                localiseErrorAndAddToGlobalErrors("sim.cannot.be.provisioned.under.different.kit");
                return showAddProductSummary();
            }
        }

        boolean simSoldToSD = false;
        try {
            if (ordersICCID != null) {
                log.debug("Checking if ICCID [{}] was transferred to an ICP", ordersICCID);
                SoldStockLocationQuery q = new SoldStockLocationQuery();
                q.setSerialNumber(ordersICCID);
                SoldStockLocationList list = SCAWrapper.getUserSpecificInstance().getSoldStockLocations(q);
                if (!list.getSoldStockLocations().isEmpty()) {
                    SoldStockLocation location = list.getSoldStockLocations().get(0);
                    if (location.getSoldToOrganisationId() != null && location.getSoldToOrganisationId() > 0) {
                        log.debug("SIM [{}] is sold to Org [{}] ProvisionDate [{}]", ordersICCID, location.getSoldToOrganisationId(), location.getProvisionData());                        
                        if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled",false)) {
                            if (isMegaDealer(location.getSoldToOrganisationId())) {
                                if((location.getProvisionData()!=null && location.getProvisionData().contains("MovedFrom")) && location.getHeldByOrganisationId()>0) {
                                        int tmpSDSoldTo=0;
                                        String movedText = "";
                                        StringTokenizer st = new StringTokenizer(location.getProvisionData(),"\n");  
                                        while (st.hasMoreTokens()) {                                             
                                            String tmpText=st.nextToken();
                                            
                                            if(tmpText.contains("MovedFrom")) {
                                                tmpSDSoldTo= Integer.parseInt(tmpText.substring(tmpText.indexOf("=")+1).replaceAll("[\n\r]", ""));
                                                log.warn("Found MoveFrom value: {}", tmpSDSoldTo);
                                            }
                                        }  
                                        
                                        if (getProductOrder().getKitItemNumber() == null || getProductOrder().getKitItemNumber().isEmpty()) {
                                            if (isSuperDealerAllowedToUseOldProvisioningMethod(tmpSDSoldTo, iccidSaleLine)) { // Only allows Kaduna ICPs as per http://jira.smilecoms.com/browse/HBT-7239
                                                simSoldToSD = true;
                                            } else {
                                                localiseErrorAndAddToGlobalErrors("icp.not.allowed.to.provision.old.kits");
                                                return showAddProductSummary();
                                            }
                                        } else { // New kits ...

                                            if (location.getHeldByOrganisationId() == null || location.getHeldByOrganisationId() == 0) {
                                                localiseErrorAndAddToGlobalErrors("sim.stock.not.held");
                                                return showAddProductSummary();
                                            }
                                            log.debug("SIM is held by OrgId [{}", location.getHeldByOrganisationId());
                                            if (getProductOrder().getKitItemNumber() == null || getProductOrder().getKitItemNumber().isEmpty()) {
                                                localiseErrorAndAddToGlobalErrors("sim.must.be.sold.in.a.kit");
                                                return showAddProductSummary();
                                            }

                                            Customer sp = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
                                            if (sp.getCustomerRoles().size() != 1) {
                                                localiseErrorAndAddToGlobalErrors("salesperson.must.have.one.organisation");
                                                return showAddProductSummary();
                                            }
                                            int orgId = sp.getCustomerRoles().get(0).getOrganisationId();
                                            log.debug("Logged in user is in org [{}]", orgId);
                                            if (location.getHeldByOrganisationId() != orgId) {
                                                localiseErrorAndAddToGlobalErrors("sim.held.by.different.org");
                                                return showAddProductSummary();
                                            }
                                            simSoldToSD = true;
                                        }
                                } else {
                                        localiseErrorAndAddToGlobalErrors("Error", "SIM move to SuperDealer info missing from provisioning data.");
                                        return showAddProductSummary();
                                }
                            }
                        } else {
                             if (isSuperDealer(location.getSoldToOrganisationId()) || isFranchise(location.getSoldToOrganisationId())) {

                                if (getProductOrder().getKitItemNumber() == null || getProductOrder().getKitItemNumber().isEmpty()) {
                                    if (isSuperDealerAllowedToUseOldProvisioningMethod(location.getSoldToOrganisationId(), iccidSaleLine)) { // Only allows Kaduna ICPs as per http://jira.smilecoms.com/browse/HBT-7239
                                        simSoldToSD = true;
                                    } else {
                                        localiseErrorAndAddToGlobalErrors("icp.not.allowed.to.provision.old.kits");
                                        return showAddProductSummary();
                                    }
                                } else { // New kits ...

                                    if (location.getHeldByOrganisationId() == null || location.getHeldByOrganisationId() == 0) {
                                        localiseErrorAndAddToGlobalErrors("sim.stock.not.held");
                                        return showAddProductSummary();
                                    }
                                    log.debug("SIM is held by OrgId [{}", location.getHeldByOrganisationId());
                                    if (getProductOrder().getKitItemNumber() == null || getProductOrder().getKitItemNumber().isEmpty()) {
                                        localiseErrorAndAddToGlobalErrors("sim.must.be.sold.in.a.kit");
                                        return showAddProductSummary();
                                    }

                                    Customer sp = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
                                    if (sp.getCustomerRoles().size() != 1) {
                                        localiseErrorAndAddToGlobalErrors("salesperson.must.have.one.organisation");
                                        return showAddProductSummary();
                                    }
                                    int orgId = sp.getCustomerRoles().get(0).getOrganisationId();
                                    log.debug("Logged in user is in org [{}]", orgId);
                                    if (location.getHeldByOrganisationId() != orgId) {
                                        localiseErrorAndAddToGlobalErrors("sim.held.by.different.org");
                                        return showAddProductSummary();
                                    }
                                    simSoldToSD = true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking if SIM must be in a kit", e);
        }
        if (getProductOrder().getKitItemNumber() != null && !getProductOrder().getKitItemNumber().isEmpty()) {
            if (!simSoldToSD) {
                localiseErrorAndAddToGlobalErrors("sim.must.be.sold.to.superdealer");
                return showAddProductSummary();
            }
            Customer sp = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER);
            if (sp.getCustomerRoles().size() != 1) {
                localiseErrorAndAddToGlobalErrors("salesperson.must.have.one.organisation");
                return showAddProductSummary();
            }

            int orgId = sp.getCustomerRoles().get(0).getOrganisationId();

            try {
                long acc = getICPNettOutAccount(orgId);
                double disc = getICPDiscount(orgId);
                log.debug("Salesperson Customer Id [{}] has Org Account [{}] and discount [{}]%", new Object[]{getUserCustomerIdFromSession(), acc, disc});

                Account account = UserSpecificCachedDataHelper.getAccount(acc);

                double accountMustHaveCents = 100 * BaseUtils.getDoubleProperty("env.exchange.rate") * BaseUtils.getDoubleProperty("env.icp.nettoff.account.min.usd", 50);
                if (account.getAvailableBalanceInCents() < accountMustHaveCents) {
                    log.warn("Account balance is too low [{}]c", account.getAvailableBalanceInCents());
                    localiseErrorAndAddToGlobalErrors("icp.true.up.account.low", "Balance on " + acc + " is " + Utils.convertCentsToCurrencyLong(account.getAvailableBalanceInCents()) + " but must be more than " + Utils.convertCentsToCurrencyLong(accountMustHaveCents));
                    return showAddProductSummary();
                }

            } catch (Exception e) {
                log.warn("Error getting org account and discount", e);
                localiseErrorAndAddToGlobalErrors("salesperson.org.invalid.config");
                return showAddProductSummary();
            }

        }

        for (ServiceInstanceOrder sio : getProductOrder().getServiceInstanceOrders()) {
            if (sio == null) {
                log.debug("SInstance [{}] is skipped completely");
                continue;
            }
            if (sio.getAction() == null || sio.getAction().equals(StAction.NONE)) {
                //This one is skipped
                log.debug("This SI is skipped for Spec Id [{}]", sio.getServiceInstance().getServiceSpecificationId());
                continue;
            } else {
                log.debug("This SI is not skipped for Spec Id [{}]", sio.getServiceInstance().getServiceSpecificationId());
            }

            ServiceInstanceOrder newSIO = new ServiceInstanceOrder();
            newSIO.setAction(StAction.CREATE);
            newSIO.setServiceInstance(new ServiceInstance());

            if (getAccount() == null) {
                setAccount(new Account());
                getAccount().setAccountId(-1);
            }
            newSIO.getServiceInstance().setAccountId(verifyChangeAccountAllowed(getAccount().getAccountId(), getProductOrder().getCustomerId()));
            newSIO.getServiceInstance().setCustomerId(getProductOrder().getCustomerId());
            newSIO.getServiceInstance().setServiceSpecificationId(sio.getServiceInstance().getServiceSpecificationId());
            newSIO.getServiceInstance().getAVPs().addAll(sio.getServiceInstance().getAVPs());
            formatAVPsForSendingToSCA(newSIO.getServiceInstance().getAVPs(), sio.getServiceInstance().getServiceSpecificationId(), true);
            newSIO.getServiceInstance().setStatus("AC");
            finalSIOList.add(newSIO);
        }
        //Replace orders with non skipped ones
        getProductOrder().getServiceInstanceOrders().clear();
        getProductOrder().getServiceInstanceOrders().addAll(finalSIOList);
        
        if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Tanzania") || BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Uganda")) { //No Voice Service, stop provision
            boolean voiceServiceExists=false;
            boolean simCardExists=false;
            if(getProductOrder() !=null && getProductOrder().getServiceInstanceOrders()!=null) {
                for(ServiceInstanceOrder serv: getProductOrder().getServiceInstanceOrders()) {
                    if(serv.getServiceInstance().getServiceSpecificationId()==1) {
                        simCardExists=true;                        
                    }
                    
                    if(serv.getServiceInstance().getServiceSpecificationId()==100) {
                        voiceServiceExists=true;                        
                    }
                }
            }
              
            if(simCardExists && !voiceServiceExists) { //Found SIM but No Voice Service, stop provision
                log.warn("Voice service missing. It is mandatory where SIM is included in {}", BaseUtils.getProperty("env.country.name"));
                localiseErrorAndAddToGlobalErrors("missing.voice.service");
                return showAddProductSummary();
            }          
        } 
        boolean simCardExists=false;
        if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria")) {
            
            if(getProductOrder() !=null && getProductOrder().getServiceInstanceOrders()!=null) {
                for(ServiceInstanceOrder serv: getProductOrder().getServiceInstanceOrders()) {
                    if(serv.getServiceInstance().getServiceSpecificationId()==1) {
                        simCardExists=true;
                        break;
                    }
                }
                
                if(simCardExists) {                    
                    finalSIOList = getProductOrder().getServiceInstanceOrders();
                    List<ServiceInstanceOrder> newSIOList = new ArrayList<ServiceInstanceOrder>();                    
                    
                   for(ServiceInstanceOrder serv: finalSIOList) {                         
                       serv.getServiceInstance().setStatus("TD");
                       newSIOList.add(serv);
                   } 
                   getProductOrder().getServiceInstanceOrders().clear();
                   getProductOrder().getServiceInstanceOrders().addAll(newSIOList);
                }
            }
        }
        
        try {
            SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
        } catch (SCABusinessError e) {
            collectServiceInstanceDataForProductInstall("backward");
            throw e;
        }
        String bulkICCIDs = getRequest().getParameter("bulkICCIDs");
        if (bulkICCIDs != null && !bulkICCIDs.isEmpty()) {
            for (String iccid : Utils.getListFromCRDelimitedString(bulkICCIDs)) {
                log.debug("Bulk provisioning ICCID [{}]", iccid);
                try {
                    for (ServiceInstanceOrder sio : getProductOrder().getServiceInstanceOrders()) {
                        for (AVP avp : sio.getServiceInstance().getAVPs()) {
                            if (avp == null || avp.getAttribute() == null) {
                                continue;
                            }
                            if (avp.getAttribute().equals("IntegratedCircuitCardIdentifier")) {

                                avp.setValue(iccid);
                                iccidSaleLine = getSaleRowUsingSerialNumber(ordersICCID);
                                if (!verifySIMLineItem(iccidSaleLine) && !hasPermissions(Permissions.PROVISION_INVALID_ICCID)) {
                                    log.error("Invalid SIM item number for provisioning [{}]", ordersICCID);
                                    localiseErrorAndAddToGlobalErrors("sim.invalid.item.code");
                                    return showAddProductSummary();
                                }
                            }
                            if (avp.getAttribute().equals("PublicIdentity")) {
                                avp.setValue("-1");
                            }
                        }
                    }
                    SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
                } catch (SCABusinessError e) {
                    collectServiceInstanceDataForProductInstall("backward");
                    throw e;
                }
            }
        }
        
        //cleanup
        HttpSession session = getContext().getRequest().getSession();               
        session.removeAttribute("fromSale");
        session.removeAttribute("sessionSale");
        session.removeAttribute("sessionProduct");
        
        if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria") && simCardExists) {
            
            createNewProductKycEntry();
            return getDDForwardResolution(CustomerActionBean.class, "retrieveCustomer", "order.completed.successfully.awaiting.kyc");
        } else {
            return getDDForwardResolution(CustomerActionBean.class, "retrieveCustomer", "order.completed.successfully");
        }
    }
    
    public void createNewProductKycEntry() {
        PreparedStatement ps = null;
        Connection conn = null;
        
        try {
            conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
            conn.setAutoCommit(false);
            String query = "insert into products_kyc_verification (customer_id, nin,sales_person) "
                    + "values (?,?,?)";
            ps = conn.prepareStatement(query);
            ps.setInt(1, getCustomer().getCustomerId());
            ps.setString(2, getCustomer().getNationalIdentityNumber()); 
            ps.setInt(3, getUserCustomerIdFromSession());

            //log.warn(ps.toString());
            
            try {
                ps.executeUpdate();
                conn.commit();
                ps.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("Unable to insert values: {}", e.getMessage());
                ps.close();
                conn.close();
                localiseErrorAndAddToGlobalErrors("system.error.please.retry");                
            } finally {
                try {
                    ps.close();
                    conn.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            log.warn("{}", e);
            try {
                    ps.close();
                    conn.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
        }
    }
    
    
    public Resolution showAddServiceInstanceList() {
        checkPermissions(Permissions.ADD_SERVICE_INSTANCE);
        setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(getProductSpecification().getProductSpecificationId()));
        return getDDForwardResolution("/product_catalog/add_service_instance_list.jsp");
    }

    public Resolution showAddServiceInstance() {
        checkPermissions(Permissions.ADD_SERVICE_INSTANCE);
        setProductSpecification(NonUserSpecificCachedDataHelper.getProductSpecification(getProductSpecification().getProductSpecificationId()));
        setProductServiceSpecificationMappings(new ArrayList<ProductServiceSpecificationMapping>());
        for (ProductServiceSpecificationMapping mapping : getProductSpecification().getProductServiceSpecificationMappings()) {
            if (mapping.getServiceSpecification().getServiceSpecificationId() == getNewServiceSpecificationId()) {
                getProductServiceSpecificationMappings().add(mapping);
                if (!isAllowed(mapping.getProvisionRoles())) {
                    localiseErrorAndAddToGlobalErrors("service.specification.not.allowed");
                    return showAddServiceInstanceList();
                }
                break;
            }
        }
        return getDDForwardResolution("/product_catalog/add_service_instance.jsp");
    }

    public Resolution provisionNewServiceInstance() {
        checkPermissions(Permissions.ADD_SERVICE_INSTANCE);
        checkCSRF();
        getProductOrder().setAction(StAction.NONE);
        getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.CREATE);

        // We need to set the customer and account id to that of the first service instance in the product instance
        setProductInstance(SCAWrapper.getUserSpecificInstance().getProductInstance(getProductOrder().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN_SVC));

        //Verify user has permissions to add this service spec
        ProductSpecification prodSpec = NonUserSpecificCachedDataHelper.getProductSpecification(getProductInstance().getProductSpecificationId());
        for (ProductServiceSpecificationMapping mapping : prodSpec.getProductServiceSpecificationMappings()) {
            if (mapping.getServiceSpecification().getServiceSpecificationId() == getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId()) {
                if (!isAllowed(mapping.getProvisionRoles())) {
                    localiseErrorAndAddToGlobalErrors("service.specification.not.allowed");
                    return showAddServiceInstanceList();
                }
                break;
            }
        }

        if (getProductInstance().getProductServiceInstanceMappings().isEmpty()) {
            // there is no account so create a new one
            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(-1);
        } else {
            getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(getProductInstance().getProductServiceInstanceMappings().get(0).getServiceInstance().getAccountId());
        }
        getProductOrder().setOrganisationId(getProductInstance().getOrganisationId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setCustomerId(getProductInstance().getCustomerId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus("AC");
        formatAVPsForSendingToSCA(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getAVPs(), getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceSpecificationId(), true);
        try {
            SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());
            setPageMessage("service.instance.added");
        } catch (SCABusinessError e) {
            log.warn("Error adding service instance [{}]", e.toString());
            showAddServiceInstance();
            throw e;
        }
        return retrieveProductInstance();
    }
    private int currentServiceSpecificationIndex;

    public void setCurrentServiceSpecificationIndex(int currentServiceSpecificationIndex) {
        this.currentServiceSpecificationIndex = currentServiceSpecificationIndex;
        log.debug("currentServiceSpecificationIndex has been set. It is now [{}]", currentServiceSpecificationIndex);
    }

    public int getCurrentServiceSpecificationIndex() {
        return currentServiceSpecificationIndex;
    }
    private int newServiceSpecificationId;

    public int getNewServiceSpecificationId() {
        return newServiceSpecificationId;
    }

    public void setNewServiceSpecificationId(int newServiceSpecificationId) {
        this.newServiceSpecificationId = newServiceSpecificationId;
    }

    public boolean isChangeServiceInstanceAnyAccountPermitted() {
        return canChangeServiceToAnyAccount();
    }

    String iccid;

    public java.lang.String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }
    private String imsi;

    public void setIMSI(String imsi) {
        this.imsi = imsi;
    }

    public java.lang.String getIMSI() {
        return imsi;
    }
    List<ProductServiceSpecificationMapping> productServiceSpecificationMappings;

    public List<ProductServiceSpecificationMapping> getProductServiceSpecificationMappings() {
        return productServiceSpecificationMappings;
    }

    public void setProductServiceSpecificationMappings(List<ProductServiceSpecificationMapping> productServiceSpecificationMappings) {
        this.productServiceSpecificationMappings = productServiceSpecificationMappings;
    }

    public Resolution retrieveNoteTypes() {
        checkPermissions(Permissions.STICKY_NOTES);
        // Need to pass the notes page the info about this entity so it knows where to attach the note        
        setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
        getStickyNoteEntityIdentifier().setEntityId(getProductInstance().getProductInstanceId());
        getStickyNoteEntityIdentifier().setEntityType("ProductInstance");
        setStickyNoteTypeList(SCAWrapper.getUserSpecificInstance().getStickyNoteTypeList(makeSCAString("ProductInstance")));
        return getDDForwardResolution("/note/view_note_types.jsp");
    }

    public Resolution retrieveNotes() {
        checkPermissions(Permissions.STICKY_NOTES);
        // Need to pass the notes page the info about this entity so it knows where to attach the note
        setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
        getStickyNoteEntityIdentifier().setEntityId(getProductInstance().getProductInstanceId());
        getStickyNoteEntityIdentifier().setEntityType("ProductInstance");
        setStickyNoteList((StickyNoteList) SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier()));
        return getDDForwardResolution("/note/view_notes.jsp");
    }

    public Resolution retrieveStickyNoteListSnippet() {
        try {
            setStickyNoteList(new StickyNoteList());
            setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
            getStickyNoteEntityIdentifier().setEntityId(getProductInstance().getProductInstanceId());
            getStickyNoteEntityIdentifier().setEntityType("ProductInstance");

            StickyNoteList snl = SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier());
            StickyNoteTypeList sntl = SCAWrapper.getUserSpecificInstance().getStickyNoteTypeList(makeSCAString(getStickyNoteEntityIdentifier().getEntityType()));

            for (StickyNote sn : snl.getStickyNotes()) {
                for (StickyNoteType snt : sntl.getStickyNoteTypes()) {
                    if (snt.getDisplayPriority() == new Short("1") && sn.getTypeName().equals(snt.getTypeName())) { //TODO : only notes that have a display priority of 1 will be displayed.                        
                        getStickyNoteList().getStickyNotes().add(sn);

                    }
                }
            }

            return new ForwardResolution("/note/note_xmlhttp.jsp");
        } catch (Exception ex) {
            setStickyNoteList(null);
            return new ForwardResolution("/note/note_xmlhttp.jsp");
        }
    }

    private void formatAVPsForSendingToSCA(List<AVP> avPs, int serviceSpecificationId, boolean populatePhotoData) {
        if (avPs == null) {
            return;
        }
        for (AVP avp : avPs) {
            if (avp != null && avp.getAttribute() != null) {
                AVP avpConfig = getAVPConfig(avp.getAttribute(), serviceSpecificationId);
                avp.setInputType(avpConfig.getInputType());
                if (avpConfig.getInputType().equals("photo") && populatePhotoData) {
                    String guid = avp.getValue();
                    log.debug("The value of the GUID for attribute [{}] is [{}]", avp.getAttribute(), guid);
                    try {
                        avp.setValue(guid + "|" + Utils.encodeBase64(Utils.getDataFromTempFile(guid)));
                    } catch (Exception ex) {
                        log.warn("Error", ex);
                    }
                } else if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                    avp.setValue(Utils.getPublicIdentityForPhoneNumber(avp.getValue()));
                } else if (avp.getAttribute().equalsIgnoreCase("NAIUsername")) {
                    avp.setValue(Utils.makeNAIIdentityFromUsername(avp.getValue()));
                } else if (avp.getAttribute().equalsIgnoreCase("NAIPassword")) {
                    try {
                        avp.setValue(Utils.hashPasswordWithComplexityCheck(avp.getValue()));
                    } catch (Exception ex) {
                        avp.setValue(Utils.oneWayHash(avp.getValue()));
                    }
                } else {
                    avp.setValue(StringEscapeUtils.unescapeHtml(avp.getValue()));
                }

            }
        }
    }

    private void formatAVPsForDisplay(List<AVP> avPs) {
        if (avPs == null) {
            return;
        }
        for (AVP avp : avPs) {
            if (avp != null && avp.getAttribute() != null) {
                if (avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {
                    avp.setValue(Utils.getFriendlyPhoneNumber(avp.getValue()));
                }
                if (avp.getAttribute().equalsIgnoreCase("NAIUsername")) {
                    avp.setValue(Utils.getFriendlyNAI(avp.getValue()));
                }
            }
        }
    }

    String impuMappedICCID;

    public java.lang.String getIiccIdentifier() {
        return impuMappedICCID;
    }

    public void setIiccIdentifier(String iccIdentifier) {
        this.impuMappedICCID = iccIdentifier;
    }

    private void doICCIDPublicIdentityMapping(List<AVP> avPs) {
        if (avPs == null) {
            return;
        }
        if (!BaseUtils.getBooleanProperty("env.iccid.impu.strict.mapping.enabled", false)) {
            return;
        }
        log.debug("ICCID mapping to public identity is enabled");

        if (impuMappedICCID == null || impuMappedICCID.isEmpty()) {
            for (AVP avp : avPs) {
                if (avp != null && avp.getAttribute() != null && avp.getAttribute().equalsIgnoreCase("IntegratedCircuitCardIdentifier")) {
                    impuMappedICCID = avp.getValue();
                    break;
                }
            }
        }

        log.debug("Going to use ICCID [{}] to seach on available number", impuMappedICCID);
        String mappedICCID = BaseUtils.getProperty("env.iccid.mappedto.public.identity", "");
        Set<String> mappedICCIDSet = Utils.getSetFromCRDelimitedString(mappedICCID);
        //|| !mappedICCIDSet.contains(impuMappedICCID)

        if (impuMappedICCID == null || impuMappedICCID.isEmpty()) { //|| !impuMappedICCID.startsWith(BaseUtils.getProperty("env.iccid.impu.strict.mapping.prefix", "8723427"))
            log.debug("This ICCID cannot be used in available number searching");
            return;
        }
        log.debug("This ICCID is mapped to a specific IMPU, going to retrieve the public identity");

        for (AVP avp : avPs) {
            if (avp != null && avp.getAttribute() != null && avp.getAttribute().equalsIgnoreCase("PublicIdentity")) {                
                NumbersQuery nq = new NumbersQuery();
                nq.setResultLimit(1);
                nq.setPattern("");
                nq.setICCID(impuMappedICCID);
                nq.setPriceLimitCents(Integer.MAX_VALUE);
                nq.setOwnedByCustomerProfileId(0);
                nq.setOwnedByOrganisationId(0);                
                NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(nq);
                
                if(list.getNumberOfNumbers() > 0) {
                    if (list.getNumberOfNumbers() == 1 || list.getNumbers().get(0).getICCID().equals(impuMappedICCID)) {                    
                        //Override the validation rule & set the default value to this number
                        log.debug("This order is for an ICCID mapped to IMPU [{}], now going to override validation rule", list.getNumbers().get(0).getIMPU());
                        avp.setValidationRule("option|" + Utils.getFriendlyPhoneNumber(list.getNumbers().get(0).getIMPU()));
                    }
                }
                break;
            }
        }
    }

    private Set getCustomersAccounts(int customerId) {
        setCustomer(UserSpecificCachedDataHelper.getCustomer(customerId, StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        Set ret = new HashSet<>();
        for (ProductInstance pi : getCustomer().getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == getCustomer().getCustomerId()) {
                    ret.add(si.getAccountId());
                }
            }
        }
        return ret;
    }

    private long verifyChangeAccountAllowed(long accountId, int customerProfileId) {

        if (accountId <= 0) {
            return accountId;
        }

        Set customersAccounts = getCustomersAccounts(customerProfileId);

        if (!customersAccounts.contains(accountId)) {
            checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_ANY_ACCOUNT);
        } else {
            checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_EXISTING_ACCOUNT);
        }
        return accountId;
    }

    private boolean canChangeServiceToAnyAccount() {
        try {
            checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_ANY_ACCOUNT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canChangeServiceToExistingAccount() {
        try {
            checkPermissions(Permissions.CHANGE_SERVICE_INSTANCE_EXISTING_ACCOUNT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private long getICPNettOutAccount(int orgId) {
        long acc;
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15%
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        String bit = discCode.substring(configStart);
        int accStart = bit.indexOf(" ACC: ") + 6;
        bit = bit.substring(accStart).trim();
        acc = Long.parseLong(bit.substring(0, 10));
        log.debug("Org Id [{}] must have nett out payments made to [{}]", orgId, acc);
        return acc;
    }

    private double getICPDiscount(int orgId) {
        // Look in the discount code comments to get the info
        String discCode = BaseUtils.getProperty("env.pos.discount.code");
        // 12345 ACC: 1307000001 DISC: 15%
        int configStart = discCode.indexOf(" " + orgId + " ACC: ");
        String bit = discCode.substring(configStart);
        int discStart = bit.indexOf(" DISC: ") + 7;
        bit = bit.substring(discStart).trim();
        double disc = Utils.getFirstNumericPartOfString(bit);
        log.debug("Org Id [{}] has discount of [{}]%", orgId, disc);
        return disc;
    }

    private boolean isSimRestrictedToOriginalKit(SaleLine iccidSaleLine, String provisioningKitCode) {

        Set<String> restricredSIMKitCodes = null;

        log.error("Checking if SIM [{}] is allowed to be provisioned inder different SIM.", iccidSaleLine.getLineId());
        if (iccidSaleLine.getParentLineId() == 0) {
            //This is a standalone SIM, no need to check its parent KIT.
            return false;
        }

        if (provisioningKitCode == null || provisioningKitCode.isEmpty()) {
            return false;
        }

        try {
            restricredSIMKitCodes = BaseUtils.getPropertyAsSet("env.pos.kits.with.restricted.sim");
        } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
            log.debug("env.pos.kits.with.locked.sim does not exist");
        }

        if (restricredSIMKitCodes == null) {
            return false;
        }

        //Get the original KIT this SIM was sold with.
        String originalSimKitCode = getSaleRowUsingSaleRowId(iccidSaleLine.getParentLineId()).getInventoryItem().getItemNumber();

        return restricredSIMKitCodes.contains(originalSimKitCode)
                && (!provisioningKitCode.equalsIgnoreCase(originalSimKitCode)); //SIMS provisioned under old kits were always sold at sub-items of a KIT.
    }

    private boolean isSuperDealerAllowedToUseOldProvisioningMethod(int orgId, SaleLine iccidSaleLine) {
        Set<String> allowedSuperDealers = null;

        log.error("Checking if organisation [{}] is allowed to sell old kits.", orgId);
        if (iccidSaleLine == null) {
            return false;
        }

        try {
            allowedSuperDealers = BaseUtils.getPropertyAsSet("env.pos.superdealers.allowed.provision.old.kits");
        } catch (com.smilecoms.commons.base.props.PropertyFetchException e) {
            log.debug("env.pos.superdealers.allowed.provision.old.kits does not exist");
        }

        if (allowedSuperDealers == null) {
            return false;
        }

        return allowedSuperDealers.contains(String.valueOf(orgId))
                && (iccidSaleLine.getParentLineId() > 0); //SIMS provisioned under old kits were always sold at sub-items of a KIT.
    }

    public SaleLine getSaleRowUsingSaleRowId(int saleRowId) {
        SalesQuery sq = new SalesQuery();
        sq.setSaleLineId(saleRowId);
        sq.setVerbosity(StSaleLookupVerbosity.SALE_LINES);

        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(sq);

        try {
            for (Sale sale : sl.getSales()) {
                for (SaleLine saleRow : sale.getSaleLines()) {
                    if (saleRow.getLineId().intValue() == saleRowId) {// Line we are looking for ...
                        //Check if is it a SIM
                        return saleRow;
                    }
                    //We have still not found the ICCID line ... Let's look at the subLines ...
                    for (SaleLine ss : saleRow.getSubSaleLines()) {
                        if (ss.getLineId().intValue() == saleRowId) {
                            return ss;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw ex;
        }

        log.error("Could not find original sale row for sale_row_id [{}].", saleRowId);
        // Not found ....
        return null;
    }

    public SaleLine getSaleRowUsingSerialNumber(String serialNumber) {
        SalesQuery sq = new SalesQuery();
        sq.setSerialNumber(serialNumber);
        sq.setVerbosity(StSaleLookupVerbosity.SALE_LINES);

        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(sq);

        try {
            for (Sale sale : sl.getSales()) {
                for (SaleLine saleRow : sale.getSaleLines()) {
                    if (saleRow.getInventoryItem().getSerialNumber().equals(serialNumber)) {// Line we are looking for ...
                        //Check if is it a SIM
                        return saleRow;
                    }
                    //We have still not found the ICCID line ... Let's look at the subLines ...
                    for (SaleLine ss : saleRow.getSubSaleLines()) {
                        if (ss.getInventoryItem().getSerialNumber().equals(serialNumber)) {
                            return ss;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw ex;
        }

        log.error("Could not find original sale row for serial number [{}].", serialNumber);
        // Not found ....
        return null;
    }

    public boolean verifySIMLineItem(SaleLine iccidLine) {
        try {
            if (iccidLine == null) {
                return false;
            } else {
                return isSIM(iccidLine.getInventoryItem().getItemNumber());
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    public boolean isSIM(String itemNumber) {
        return itemNumber.toUpperCase().startsWith("SIM");
    }
    
    public boolean provisionForMappedImpuICCID(String iccid) {
        NumbersQuery nq = new NumbersQuery();
        nq.setResultLimit(1);
        nq.setPattern("");
        nq.setICCID(iccid);
        nq.setPriceLimitCents(Integer.MAX_VALUE);
        nq.setOwnedByCustomerProfileId(0);
        nq.setOwnedByOrganisationId(0);                
        NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(nq);

        if(list.getNumberOfNumbers() > 0) {
            if (list.getNumberOfNumbers() == 1 || list.getNumbers().get(0).getICCID().equals(iccid)) {                    
                //Override the validation rule & set the default value to this number
                log.debug("This order is for an ICCID mapped to IMPU [{}], now going to override validation rule", list.getNumbers().get(0).getIMPU());
               return true;
            }
        }
        return false;
    }
    
    public boolean ninVerified = false;

    public boolean isNinVerified() {
        return ninVerified;
    }

    public void setninVerified(boolean nin) {
        this.ninVerified = nin;
    }

    
}
