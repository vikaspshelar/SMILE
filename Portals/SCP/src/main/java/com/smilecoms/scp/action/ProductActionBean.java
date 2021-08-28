package com.smilecoms.scp.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;

/**
 *
 * @author sabelo
 */
public class ProductActionBean extends SmileActionBean {

    private int downAndUplinkInternetSpeed;
    private String userDefinedDPIRules;
    private List<Integer> sliderSpeedList;
    private boolean allowedToChangeSpeed;
    private String stripesEventName;
    private String eventSourceLink;
    private String stripesActionBeanName;

    public String getStripesEventName() {
        return stripesEventName;
    }

    public String getStripesActionBeanName() {
        return stripesActionBeanName;
    }

    public String getEventSourceLink() {
        return eventSourceLink;
    }

    public boolean getAllowedToChangeSpeed() {
        return allowedToChangeSpeed;
    }

    public void setAllowedToChangeSpeed(boolean allowedToChangeSpeed) {
        this.allowedToChangeSpeed = allowedToChangeSpeed;
    }

    public int getDownAndUplinkInternetSpeed() {
        return downAndUplinkInternetSpeed;
    }

    public void setDownAndUplinkInternetSpeed(int downAndUplinkInternetSpeed) {
        this.downAndUplinkInternetSpeed = downAndUplinkInternetSpeed;
    }

    public List<Integer> getSliderSpeedList() {
        return sliderSpeedList;
    }

    public void setSliderSpeedList(List<Integer> sliderSpeedList) {
        this.sliderSpeedList = sliderSpeedList;
    }

    public String getUserDefinedDPIRules() {
        return userDefinedDPIRules;
    }

    public void setUserDefinedDPIRules(String userDefinedDPIRules) {
        this.userDefinedDPIRules = userDefinedDPIRules;
    }

    public Resolution changeProductInstanceFriendlyName() {
        ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(getProductOrder().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);

        getProductOrder().setAction(StAction.UPDATE);
        // Ensure a hacker cannot post in unintended data
        getProductOrder().setOrganisationId(pi.getOrganisationId());
        getProductOrder().setCustomerId(pi.getCustomerId());
        getProductOrder().setSegment(pi.getSegment());
        getProductOrder().getServiceInstanceOrders().clear();
        Done done = SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());

        if (done.getDone() == StDone.TRUE) {
            pi = UserSpecificCachedDataHelper.getProductInstance(getProductOrder().getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN);
            pi.setFriendlyName(getProductOrder().getFriendlyName()); // Need to do this or else a cached version will be returned with the old value
            return getJSONResolution(pi);
        }
        setDone(done);
        return getJSONResolution(getDone());
    }

    @DefaultHandler
    public Resolution showChangeServiceInstanceConfiguration() {
        log.debug("Entering showChangeServiceInstanceConfiguration");
        double currentBitsperSec = 0;
        int serviceId = 0;
        if (getServiceInstance() == null && getAccountQuery() == null) {
            log.debug("getServiceInstance() is null, its probably a redirect");
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));

            Set<Integer> serviceIds = new HashSet<>();
            Set<Long> accountIds = new HashSet<>();
            ServiceInstanceList tmpSIList = new ServiceInstanceList();

            for (ProductInstance pi : getCustomer().getProductInstances()) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    if (si.getCustomerId() == getCustomer().getCustomerId()) {
                        if (isSIDeactivatedOrSIMCard(si) || checkIfStaffOrPartnerService(si.getServiceSpecificationId())) {
                            continue;
                        }
                        serviceIds.add(si.getServiceInstanceId());
                        accountIds.add(si.getAccountId());
                        tmpSIList.getServiceInstances().add(si);
                    }
                }
            }
            
            if (serviceIds.isEmpty()) {
                log.debug("Customer has no active services");
                return getDDForwardResolution(AccountActionBean.class, "retrieveAllUserServicesAccounts", "scp.acc.hasno.active.si.tomanage");
            }
            
            if (serviceIds.size() > 1) {

                log.debug("Customer has more than one service, going to redirect them to select account to manage");
                stripesActionBeanName = "Product";
                stripesEventName = getContext().getEventName();
                getContext().getSourcePage();
                eventSourceLink = "ChangeServiceInstanceConfiguration";

                setServiceInstanceList(new ServiceInstanceList());
                setProductInstanceList(new ProductInstanceList());
                setAccountList(new AccountList());

                for (long accId : accountIds) {
                    getAccountList().getAccounts().add(UserSpecificCachedDataHelper.getAccount(accId, StAccountLookupVerbosity.ACCOUNT));
                }
                getServiceInstanceList().getServiceInstances().addAll(tmpSIList.getServiceInstances());
                getProductInstanceList().getProductInstances().addAll(getCustomer().getProductInstances());
                
                return getDDForwardResolution("/account/select_account_to_manage.jsp");
            }
            serviceId=serviceIds.iterator().next();
            

        } else {
            setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));
            if (getAccountQuery() != null) {
                setAccount(SCAWrapper.getUserSpecificInstance().getAccount(getAccountQuery().getAccountId(), StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES));
                for (ServiceInstance si : getAccount().getServiceInstances()) {
                    if (isSIDeactivatedOrSIMCard(si) || checkIfStaffOrPartnerService(si.getServiceSpecificationId())) {
                        continue;
                    }
                    setServiceInstance(si);
                    break;
                }
            }
            serviceId = getServiceInstance().getServiceInstanceId();
            log.debug("Service to manage is: [{}]", serviceId);
        }

        setServiceInstance(SCAWrapper.getUserSpecificInstance().getServiceInstance(serviceId, StServiceInstanceLookupVerbosity.MAIN_SVCAVP));
        allowedToChangeSpeed = false;
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(getServiceInstance().getServiceSpecificationId());
        setServiceSpecification(ss);

        for (AVP siAvp : getServiceInstance().getAVPs()) {

            if (siAvp.getAttribute().equals("UserDefinedInternetDownlinkSpeed")) {
                allowedToChangeSpeed = true;
                if (!siAvp.getValue().isEmpty()) {
                    log.debug("Found UserDefinedInternetDownlinkSpeed [{}] in SI Avps going to use it as default speed in slider", siAvp.getValue());
                    currentBitsperSec = Double.parseDouble(siAvp.getValue());
                } else {
                    log.debug("Did not find UserDefinedInternetDownlinkSpeed going to used default");

                    for (AVP ssAvp : ss.getAVPs()) {

                        log.debug("Going to evaluate AVP[{}]", ssAvp.getAttribute());
                        if (ssAvp.getAttribute().equals("PCRFGxConfig")) {

                            List<String> pcrfConfig = Utils.getListFromCRDelimitedString(ssAvp.getValue());
                            for (String serviceDefaultInternetSpeed : pcrfConfig) {
                                String[] prop = serviceDefaultInternetSpeed.split("=");

                                if (prop[0].equals("DefaultBearerApnAmbrDownlink")) {
                                    log.debug("Found DefaultBearerApnAmbrDownlink [{}] in PCRFGxConfig going to use it as default speed", prop[1]);
                                    currentBitsperSec = Double.parseDouble(prop[1]);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                //break;
            }
            if (siAvp.getAttribute().equals("UserDefinedDPIRules")) {
                if (!siAvp.getValue().isEmpty()) {
                    log.debug("Found UserDefinedDPIRules [{}] in SI Avps going to use it as default", siAvp.getValue());
                    userDefinedDPIRules = siAvp.getValue();
                } else {
                    log.debug("Did not find any values for UserDefinedDPIRules avp");
                }
            }

        }

        List<String> internetSpeeds = BaseUtils.getPropertyAsList("env.scp.slider.speed.config");
        sliderSpeedList = new ArrayList<>();
        double closestSpeed = 0;
        for (String speed : internetSpeeds) {
            log.debug("closestSpeed is [{}] and this slider option is [{}]", closestSpeed, speed);
            double configSpdBitsPerSec = Double.parseDouble(speed);
            double convertedSpeed = (configSpdBitsPerSec / (1024 * 1024)); //TODO 1024 should be based on property
            sliderSpeedList.add((int) convertedSpeed);
            if (configSpdBitsPerSec >= currentBitsperSec && closestSpeed == 0) {
                closestSpeed = convertedSpeed;
                log.debug("Using closest speed of [{}]", closestSpeed);
            }
        }
        downAndUplinkInternetSpeed = (int) closestSpeed;
        //Remove all AVPs before sending response back
        getServiceInstance().getAVPs().clear();
        log.debug("Exiting showChangeServiceInstanceConfiguration");
        return getDDForwardResolution("/products/change_service_instance_configuration.jsp");
    }

    public Resolution changeServiceInstanceConfiguration() {

        checkCSRF();

        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN);

        getProductOrder().setAction(StAction.NONE);
        getProductOrder().setProductInstanceId(si.getProductInstanceId());
        getProductOrder().getServiceInstanceOrders().get(0).setAction(StAction.UPDATE);
        // Ensure a hacker cannot post in unintended data
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setCustomerId(si.getCustomerId());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setStatus(si.getStatus());
        getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance().setAccountId(si.getAccountId());

        List<String> internetSpeeds = BaseUtils.getPropertyAsList("env.scp.slider.speed.config");
        boolean speedMatches = false;
        for (String speed : internetSpeeds) {
            double configSpdBitsPerSec = Double.parseDouble(speed);
            double convertedSpeed = (configSpdBitsPerSec / (1024 * 1024));
            if (convertedSpeed == getDownAndUplinkInternetSpeed()) {
                speedMatches = true;
                log.debug("User set speed to [{}]", convertedSpeed);
                break;
            }
        }

        if (!speedMatches) {
            localiseErrorAndAddToGlobalErrors("scp.choose.oneof.available.speed");
            return showChangeServiceInstanceConfiguration();
        }

        populateUserDefinedAVPDetailsIntoServiceInstanceAVPs(getProductOrder().getServiceInstanceOrders().get(0).getServiceInstance());

        SCAWrapper.getUserSpecificInstance().processOrder(getProductOrder());

        return getDDForwardResolution(AccountActionBean.class, "retrieveAccount", "scp.service.instance.modified.successfully");
    }

    public Resolution showSliderInfoPage() {

        return getDDForwardResolution("/products/slider_message_info.jsp");
    }

    private void populateUserDefinedAVPDetailsIntoServiceInstanceAVPs(ServiceInstance si) {
        log.debug("ENTERING populateUserDefinedAVPDetailsIntoServiceInstanceAVPs");

        if (getDownAndUplinkInternetSpeed() > 0) {
            Integer mibsToBits = getDownAndUplinkInternetSpeed() * 1024 * 1024;
            String internetSpeed = String.valueOf(mibsToBits);
            AVP userDefinedDownlinkSpeedAvp = new AVP();
            userDefinedDownlinkSpeedAvp.setAttribute("UserDefinedInternetDownlinkSpeed");
            userDefinedDownlinkSpeedAvp.setValue(internetSpeed);
            si.getAVPs().add(userDefinedDownlinkSpeedAvp);
            AVP userDefinedUplinkSpeedAvp = new AVP();
            userDefinedUplinkSpeedAvp.setAttribute("UserDefinedInternetUplinkSpeed");
            userDefinedUplinkSpeedAvp.setValue(internetSpeed);
            si.getAVPs().add(userDefinedUplinkSpeedAvp);
        }

        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId());
        List<String> dpiRules = NonUserSpecificCachedDataHelper.getServiceSpecificationAvailableUserDefinedDPIRules(ss.getServiceSpecificationId());

        StringBuilder currentDPIRulesOn = new StringBuilder();
        StringBuilder currentDPIRulesOff = new StringBuilder();

        for (String rule : dpiRules) {
            String newValueOfRule = getRequest().getParameter(rule);
            if (newValueOfRule != null) {
                if (newValueOfRule.equals("off")) {
                    //removing rule from current list
                    currentDPIRulesOff.append(rule).append(",");
                }
                if (newValueOfRule.equals("on")) {
                    //add rule to current list
                    currentDPIRulesOn.append(rule).append(",");
                }
            }
        }

        String DPIRulesToAdd = currentDPIRulesOn.toString();
        String DPIRulesToRemove = currentDPIRulesOff.toString();
        String oldDPIRules = getUserDefinedDPIRules() == null ? "" : getUserDefinedDPIRules();
        Set<String> rulesToEnforceSet = new HashSet<String>();

        if (!oldDPIRules.isEmpty()) {

            if (oldDPIRules.contains(",")) {
                String[] oldDPIRulesToAddArray = oldDPIRules.split(",");
                for (int z = 0; z < oldDPIRulesToAddArray.length; z++) {
                    String oldRule = oldDPIRulesToAddArray[z];
                    rulesToEnforceSet.add(oldRule);
                }
            } else {
                rulesToEnforceSet.add(oldDPIRules);
            }
        } else {
            log.debug("No old rules at all");
        }

        //Add DPI Rule logic
        if (DPIRulesToAdd.isEmpty()) {
            log.debug("No new DPIRules to add for 'UserDefinedDPIRules' avp");
        } else {

            String newDPIRulesToAdd = DPIRulesToAdd.substring(0, DPIRulesToAdd.lastIndexOf(","));

            if (newDPIRulesToAdd.contains(",")) {

                String[] newDPIRulesToAddArray = newDPIRulesToAdd.split(",");

                for (int z = 0; z < newDPIRulesToAddArray.length; z++) {
                    String newRule = newDPIRulesToAddArray[z];
                    rulesToEnforceSet.add(newRule);
                }

            } else {
                rulesToEnforceSet.add(newDPIRulesToAdd);
            }
        }

        //Remove DPI Rule logic
        if (DPIRulesToRemove.isEmpty()) {
            log.debug("No DPIRules to remove for 'UserDefinedDPIRules' avp");
        } else {

            String newDPIRulesToRemove = DPIRulesToRemove.substring(0, DPIRulesToRemove.lastIndexOf(","));

            if (newDPIRulesToRemove.contains(",")) {

                String[] newDPIRulesArray = newDPIRulesToRemove.split(",");
                for (int i = 0; i < newDPIRulesArray.length; i++) {
                    String newRule = newDPIRulesArray[i];
                    if (rulesToEnforceSet.contains(newRule)) {
                        rulesToEnforceSet.remove(newRule);
                    }
                }

            } else {
                rulesToEnforceSet.remove(newDPIRulesToRemove);
            }
        }

        if (!rulesToEnforceSet.isEmpty()) {

            StringBuilder dpiRulesRemoved = new StringBuilder();
            for (String modifiedRules : rulesToEnforceSet) {
                dpiRulesRemoved.append(modifiedRules).append(",");
            }
            String oldRuleWithRemovedAndNewRules = dpiRulesRemoved.toString();
            userDefinedDPIRules = oldRuleWithRemovedAndNewRules.substring(0, oldRuleWithRemovedAndNewRules.lastIndexOf(","));
            log.debug("DPIRules to enforce [{}]", userDefinedDPIRules);

            AVP userDefinedDPIRulesAvp = new AVP();
            userDefinedDPIRulesAvp.setAttribute("UserDefinedDPIRules");
            userDefinedDPIRulesAvp.setValue(userDefinedDPIRules);
            si.getAVPs().add(userDefinedDPIRulesAvp);

        } else {

            log.debug("All DPI Rules have been removed");
            userDefinedDPIRules = "";
            AVP userDefinedDPIRulesAvp = new AVP();
            userDefinedDPIRulesAvp.setAttribute("UserDefinedDPIRules");
            userDefinedDPIRulesAvp.setValue(userDefinedDPIRules);
            si.getAVPs().add(userDefinedDPIRulesAvp);
        }

        log.debug("EXITING populateUserDefinedAVPDetailsIntoServiceInstanceAVPs");
    }

    private boolean isSIDeactivatedOrSIMCard(ServiceInstance si) {
        boolean ret = false;
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId());
        if (si.getStatus().equals("TD") || ss.getName().equals("SIM Card")) {
            ret = true;
        }
        return ret;
    }

    private boolean checkIfStaffOrPartnerService(int serviceSpecificationId) {
        return (serviceSpecificationId >= 1000);
    }
}
