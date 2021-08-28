/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.ProductOrder;
import com.smilecoms.commons.sca.SCAContext;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstanceOrder;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StAction;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class ServiceSpecHelper {

    private static final Logger log = LoggerFactory.getLogger(ServiceSpecHelper.class);

    // NEW WAY OF DOING THINGS
    public static void applyServiceRules(ServiceInstance serviceInstance, ServiceRules rules) {
        try {
            log.debug("In applyServiceRules [{}] on SI [{}] with info [{}]", new Object[]{rules, serviceInstance.getServiceInstanceId(), serviceInstance.getInfo()});
            Set<String> existingDPIRules = getSystemDefinedDpiRules(serviceInstance); // Will be empty set if nothing found
            Set<String> newDPI = new HashSet<>();
            // Start with what we have
            newDPI.addAll(existingDPIRules);

            if (rules.isClearAllDPIRules()) {
                newDPI.clear();
            }

            if (rules.isClearAllButStickyDPIRules()) {
                Set<String> stickyRules;
                try {
                    stickyRules = BaseUtils.getPropertyAsSet("env.bm.sticky.dpi.rules");
                } catch (Exception e) {
                    log.debug("No property env.bm.sticky.dpi.rules");
                    stickyRules = new HashSet<>();
                }
                Set<String> tmp = new HashSet<>();
                for (String rule : newDPI) {
                    if (stickyRules.contains(rule)) {
                        tmp.add(rule);
                    }
                }
                newDPI = tmp;
            }

            if (rules.getSystemDefinedDpiRulesToAdd() != null && !rules.getSystemDefinedDpiRulesToAdd().isEmpty()) {
                newDPI.addAll(rules.getSystemDefinedDpiRulesToAdd());
            }

            if (rules.getSystemDefinedDpiRulesToRemove() != null && !rules.getSystemDefinedDpiRulesToRemove().isEmpty()) {
                Set<String> tmp = new HashSet<>();
                for (String rule : newDPI) {
                    if (!rules.getSystemDefinedDpiRulesToRemove().contains(rule)) {
                        tmp.add(rule);
                    }
                }
                newDPI = tmp;
            }

            if (Utils.collectionsMatch(newDPI, existingDPIRules)) {
                // No change needed
                newDPI = null;
            }

            Long newUpBps = rules.getMaxBpsUp();
            if (newUpBps != null) {
                Long currentUserDefinedUpBps = getUserDefinedUplinkBps(serviceInstance);
                if (currentUserDefinedUpBps != null) {
                    // Never increase above what the user wanted
                    newUpBps = Math.min(newUpBps, currentUserDefinedUpBps);
                }
                Long currentSystemDefinedUpBps = getSystemDefinedUplinkBps(serviceInstance);
                if ((currentSystemDefinedUpBps != null && currentSystemDefinedUpBps.equals(newUpBps))
                        || (currentSystemDefinedUpBps == null && newUpBps == -1)) {
                    // leave as is cause its the same as it is currently
                    newUpBps = null;
                }
            }

            Long newDownBps = rules.getMaxBpsDown();
            if (newDownBps != null) {
                Long currentUserDefinedDownBps = getUserDefinedDownlinkBps(serviceInstance);
                if (currentUserDefinedDownBps != null) {
                    // Never increase above what the user wanted
                    newDownBps = Math.min(newDownBps, currentUserDefinedDownBps);
                }
                Long currentSystemDefinedDownBps = getSystemDefinedDownlinkBps(serviceInstance);
                if ((currentSystemDefinedDownBps != null && currentSystemDefinedDownBps.equals(newDownBps))
                        || (currentSystemDefinedDownBps == null && newDownBps == -1)) {
                    // leave as is cause its the same as it is currently
                    newDownBps = null;
                }
            }

            Integer newQCI = rules.getQci();
            if (newQCI != null) {
                Integer currentQCI = getSystemDefinedDefaultBearerQci(serviceInstance);
                if (currentQCI != null && newQCI.equals(currentQCI)) {
                    // Leave as is
                    newQCI = null;
                } else if (newQCI == -1 && currentQCI == null) {
                    // Nothing to do
                    newQCI = null;
                }
            }

            if (newDownBps == null && newUpBps == null && newDPI == null && newQCI == null) {
                log.debug("Nothing to do");
                return;
            }

            applySystemDefinedSpeedAndDPIRuleAndQCI(serviceInstance.getServiceInstanceId(), newDownBps, newUpBps, Utils.makeCommaDelimitedString(newDPI), newQCI);
        } catch (Exception e) {
            log.warn("Error setting svc spec ", e);
            new ExceptionManager(log).reportError(e);
        }
    }

    private static void applySystemDefinedSpeedAndDPIRuleAndQCI(int siId, Long bpsDown, Long bpsUp, String dpiRules, Integer qci) {
        // for dpiRules, blank means set to blank, null means leave as is
        // for number, -1 means set to blank, null means leave as is
        try {
            log.debug("Calling SCA to change System Defined DPI Rule to [{}] and Downlink Speed to [{}] and Uplink Speed to [{}] and QCI to [{}] on SI [{}]", new Object[]{dpiRules, bpsDown, bpsUp, qci, siId});
            ServiceInstanceQuery q = new ServiceInstanceQuery();
            q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            q.setServiceInstanceId(siId);
            com.smilecoms.commons.sca.ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(q);
            ProductOrder po = new ProductOrder();
            // Call as admin so that this is permitted
            po.setSCAContext(new SCAContext());
            po.getSCAContext().setAsync(Boolean.TRUE);
            po.setAction(StAction.NONE);
            po.setProductInstanceId(si.getProductInstanceId());
            ServiceInstanceOrder sio = new ServiceInstanceOrder();
            sio.setAction(StAction.UPDATE);
            sio.setServiceInstance(new com.smilecoms.commons.sca.ServiceInstance());
            sio.getServiceInstance().setServiceInstanceId(siId);
            sio.getServiceInstance().setProductInstanceId(si.getProductInstanceId());
            sio.getServiceInstance().setCustomerId(si.getCustomerId());
            sio.getServiceInstance().setStatus(si.getStatus());
            sio.getServiceInstance().setAccountId(si.getAccountId());
            sio.getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
            if (dpiRules != null) {
                AVP dpiAVP = new AVP();
                dpiAVP.setAttribute("SystemDefinedDPIRules");
                dpiAVP.setValue(dpiRules);
                sio.getServiceInstance().getAVPs().add(dpiAVP);
            }
            if (bpsDown != null) {
                AVP downlinkAVP = new AVP();
                downlinkAVP.setAttribute("SystemDefinedInternetDownlinkSpeed");
                downlinkAVP.setValue(bpsDown == -1 ? "" : String.valueOf(bpsDown));
                sio.getServiceInstance().getAVPs().add(downlinkAVP);
            }
            if (bpsUp != null) {
                AVP uplinkAVP = new AVP();
                uplinkAVP.setAttribute("SystemDefinedInternetUplinkSpeed");
                uplinkAVP.setValue(bpsUp == -1 ? "" : String.valueOf(bpsUp));
                sio.getServiceInstance().getAVPs().add(uplinkAVP);
            }
            if (qci != null) {
                AVP qciAVP = new AVP();
                qciAVP.setAttribute("SystemDefinedDefaultBearerQci");
                qciAVP.setValue(qci == -1 ? "" : String.valueOf(qci));
                sio.getServiceInstance().getAVPs().add(qciAVP);
            }
            po.getServiceInstanceOrders().add(sio);
            SCAWrapper.getAdminInstance().processOrder(po);
            log.debug("Called SCA to change system dpi rules");
        } catch (Exception e) {
            log.warn("Error trying to change system DPI rules [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }
    }

    private static Set<String> getSystemDefinedDpiRules(ServiceInstance serviceInstance) {
        String info = serviceInstance.getInfo();
        String currentValue = Utils.getValueFromCRDelimitedAVPString(info, "SystemDefinedDPIRules");
        Set<String> ret = Utils.getSetFromCommaDelimitedString(currentValue);
        return ret;
    }

    private static Integer getSystemDefinedDefaultBearerQci(ServiceInstance serviceInstance) {
        String info = serviceInstance.getInfo();
        String currentValue = Utils.getValueFromCRDelimitedAVPString(info, "SystemDefinedDefaultBearerQci");
        if (currentValue == null || currentValue.isEmpty()) {
            return null;
        }
        return Integer.parseInt(currentValue);
    }

    private static Long getUserDefinedUplinkBps(ServiceInstance serviceInstance) {
        String info = serviceInstance.getInfo();
        String currentValue = Utils.getValueFromCRDelimitedAVPString(info, "UserDefinedInternetUplinkSpeed");
        if (currentValue == null || currentValue.isEmpty()) {
            return null;
        }
        return Long.parseLong(currentValue);
    }

    private static Long getUserDefinedDownlinkBps(ServiceInstance serviceInstance) {
        String info = serviceInstance.getInfo();
        String currentValue = Utils.getValueFromCRDelimitedAVPString(info, "UserDefinedInternetDownlinkSpeed");
        if (currentValue == null || currentValue.isEmpty()) {
            return null;
        }
        return Long.parseLong(currentValue);
    }

    private static Long getSystemDefinedUplinkBps(ServiceInstance serviceInstance) {
        String info = serviceInstance.getInfo();
        String currentValue = Utils.getValueFromCRDelimitedAVPString(info, "SystemDefinedInternetUplinkSpeed");
        if (currentValue == null || currentValue.isEmpty()) {
            return null;
        }
        return Long.parseLong(currentValue);
    }

    private static Long getSystemDefinedDownlinkBps(ServiceInstance serviceInstance) {
        String info = serviceInstance.getInfo();
        String currentValue = Utils.getValueFromCRDelimitedAVPString(info, "SystemDefinedInternetDownlinkSpeed");
        if (currentValue == null || currentValue.isEmpty()) {
            return null;
        }
        return Long.parseLong(currentValue);
    }


    /*
    
    public static boolean doesServiceConfigHaveInfoAVP(ServiceInstance serviceInstance, String AVPName, String AVPValue) {
        return doesInfoHaveAVPWithValue(serviceInstance.getInfo(), AVPName, AVPValue);
    }

    public static void ensureDefaultSpeedIsInPlace(EntityManager em, int productInstanceId) {
        ServiceInstance si = DAO.getDataServiceInstanceForProductInstance(em, productInstanceId);
        if (!doesInfoHaveAVPWithValue(em, si.getServiceInstanceId(), "SystemDefinedInternetDownlinkSpeed", "")) {
            log.debug("Service Instance should have speed changed to the service default", si.getServiceInstanceId());
            applySystemDefinedSpeed(si.getServiceInstanceId(), -1, -1);
        }
    }

    public static void ensureDefaultSpeedIsInPlace(ServiceInstance serviceInstance) {
        if (!doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedInternetDownlinkSpeed", "")) {
            log.debug("Service Instance should have speed changed to the service default", serviceInstance.getServiceInstanceId());
            applySystemDefinedSpeed(serviceInstance.getServiceInstanceId(), -1, -1);
        }
    }

    public static void ensureDefaultSpeedAndDPIAndQCIIsInPlace(ServiceInstance serviceInstance) {
        if (!doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedInternetDownlinkSpeed", "")
                || !doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedDPIRules", "")
                || !doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedDefaultBearerQci", "")) {
            log.debug("Service Instance should have speed changed to the service default and DPI rule and QCI set to blank", serviceInstance.getServiceInstanceId());
            applySystemDefinedSpeedAndDPIRuleAndQCI(serviceInstance.getServiceInstanceId(), -1L, -1L, "", -1);
        }
    }

    public static void ensureDefaultDPIIsInPlace(ServiceInstance serviceInstance) {
        ensureSystemDPIRuleIsInPlace(serviceInstance, "");
    }

    public static void ensureSystemDPIRuleIsInPlace(ServiceInstance serviceInstance, String dpiRules) {
        if (!doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedDPIRules", dpiRules)) {
            log.debug("Service [{}] needs its system defined DPI rules changed to [{}]", serviceInstance.getServiceInstanceId(), dpiRules);
            applySystemDefinedDPIRule(serviceInstance.getServiceInstanceId(), dpiRules);
        }
    }

    public static void ensureMaxSpeedCantExceedAndDefaultDPIAndQCIIsInPlace(ServiceInstance serviceInstance, long speedBPSDown, long speedBPSUp) {
        // ensure the speed is the lower of speedBPS and SystemDefinedInternetDownlinkSpeed
        String sDownlinkSpeed = Utils.getValueFromCRDelimitedAVPString(serviceInstance.getInfo(), "UserDefinedInternetDownlinkSpeed");
        if (sDownlinkSpeed == null || sDownlinkSpeed.isEmpty()) {
            ensureSystemSpeedAndDPIRuleAndQCIIsInPlace(serviceInstance, speedBPSDown, speedBPSUp, "", -1);
        } else {
            long userDownSpeed = Long.parseLong(sDownlinkSpeed);
            String sUplinkSpeed = Utils.getValueFromCRDelimitedAVPString(serviceInstance.getInfo(), "UserDefinedInternetUplinkSpeed");
            long userUpSpeed = Long.parseLong(sUplinkSpeed);
            ensureSystemSpeedAndDPIRuleAndQCIIsInPlace(serviceInstance, Math.min(userDownSpeed, speedBPSDown), Math.min(userUpSpeed, speedBPSUp), "", -1);
        }
    }

    public static void ensureSystemSpeedIsInPlace(ServiceInstance serviceInstance, long speedBPSDown, long speedBPSUp) {
        if (!doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedInternetDownlinkSpeed", String.valueOf(speedBPSDown))
                || !doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedInternetUplinkSpeed", String.valueOf(speedBPSUp))) {
            log.debug("Service Instance [{}] should have Downlink speed changed to [{}] and Uplink to [{}]", new Object[]{serviceInstance.getServiceInstanceId(), speedBPSDown, speedBPSUp});
            applySystemDefinedSpeed(serviceInstance.getServiceInstanceId(), speedBPSDown, speedBPSUp);
        }
    }

    public static void ensureSystemSpeedAndDPIRuleAndQCIIsInPlace(ServiceInstance serviceInstance, long speedBPSDown, long speedBPSUp, String dpiRules, int qci) {
        if (!doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedInternetDownlinkSpeed", String.valueOf(speedBPSDown))
                || !doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedInternetUplinkSpeed", String.valueOf(speedBPSUp))
                || !doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedDPIRules", dpiRules)
                || !doesInfoHaveAVPWithValue(serviceInstance.getInfo(), "SystemDefinedDefaultBearerQci", qci == -1 ? "" : String.valueOf(qci))) {
            log.debug("Service Instance [{}] should have speed changed to [{}] Down and [{}] Up and DPI rule to [{}] and QCI to [{}]", new Object[]{serviceInstance.getServiceInstanceId(), speedBPSDown, speedBPSUp, dpiRules, qci});
            applySystemDefinedSpeedAndDPIRuleAndQCI(serviceInstance.getServiceInstanceId(), speedBPSDown, speedBPSUp, dpiRules, qci);
        }
    }

    private static void applySystemDefinedDPIRule(int siId, String dpiRules) {
        try {
            log.debug("Calling SCA to change System Defined DPI Rule to [{}] on SI [{}]", dpiRules, siId);
            ServiceInstanceQuery q = new ServiceInstanceQuery();
            q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            q.setServiceInstanceId(siId);
            com.smilecoms.commons.sca.ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(q);
            ProductOrder po = new ProductOrder();
            // Call as admin so that this is permitted
            po.setSCAContext(new SCAContext());
            po.getSCAContext().setAsync(Boolean.TRUE);
            po.setAction(StAction.NONE);
            po.setProductInstanceId(si.getProductInstanceId());
            ServiceInstanceOrder sio = new ServiceInstanceOrder();
            sio.setAction(StAction.UPDATE);
            sio.setServiceInstance(new com.smilecoms.commons.sca.ServiceInstance());
            sio.getServiceInstance().setServiceInstanceId(siId);
            sio.getServiceInstance().setProductInstanceId(si.getProductInstanceId());
            sio.getServiceInstance().setCustomerId(si.getCustomerId());
            sio.getServiceInstance().setStatus(si.getStatus());
            sio.getServiceInstance().setAccountId(si.getAccountId());
            sio.getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
            AVP dpiAVP = new AVP();
            dpiAVP.setAttribute("SystemDefinedDPIRules");
            dpiAVP.setValue(dpiRules);
            sio.getServiceInstance().getAVPs().add(dpiAVP);
            po.getServiceInstanceOrders().add(sio);
            SCAWrapper.getAdminInstance().processOrder(po);
            log.debug("Called SCA to change system dpi rules");
        } catch (Exception e) {
            log.warn("Error trying to change system DPI rules [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }
    }

    private static void applySystemDefinedSpeed(int siId, long bpsDown, long bpsUp) {
        try {
            log.debug("Calling SCA to change System Defined Downlink Speed to [{}] and Uplink Speed to [{}] on SI [{}]", new Object[]{bpsDown, bpsUp, siId});
            ServiceInstanceQuery q = new ServiceInstanceQuery();
            q.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            q.setServiceInstanceId(siId);
            com.smilecoms.commons.sca.ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(q);
            ProductOrder po = new ProductOrder();
            // Call as admin so that this is permitted
            po.setSCAContext(new SCAContext());
            po.getSCAContext().setAsync(Boolean.TRUE);
            po.setAction(StAction.NONE);
            po.setProductInstanceId(si.getProductInstanceId());
            ServiceInstanceOrder sio = new ServiceInstanceOrder();
            sio.setAction(StAction.UPDATE);
            sio.setServiceInstance(new com.smilecoms.commons.sca.ServiceInstance());
            sio.getServiceInstance().setServiceInstanceId(siId);
            sio.getServiceInstance().setProductInstanceId(si.getProductInstanceId());
            sio.getServiceInstance().setCustomerId(si.getCustomerId());
            sio.getServiceInstance().setStatus(si.getStatus());
            sio.getServiceInstance().setAccountId(si.getAccountId());
            sio.getServiceInstance().setServiceSpecificationId(si.getServiceSpecificationId());
            AVP downlinkAVP = new AVP();
            downlinkAVP.setAttribute("SystemDefinedInternetDownlinkSpeed");
            downlinkAVP.setValue(bpsDown == -1 ? "" : String.valueOf(bpsDown));
            sio.getServiceInstance().getAVPs().add(downlinkAVP);
            AVP uplinkAVP = new AVP();
            uplinkAVP.setAttribute("SystemDefinedInternetUplinkSpeed");
            uplinkAVP.setValue(bpsUp == -1 ? "" : String.valueOf(bpsUp));
            sio.getServiceInstance().getAVPs().add(uplinkAVP);
            po.getServiceInstanceOrders().add(sio);
            SCAWrapper.getAdminInstance().processOrder(po);
            log.debug("Called SCA to change system defined speed");
        } catch (Exception e) {
            log.warn("Error trying to change system defined speed [{}]", e.toString());
            new ExceptionManager(log).reportError(e);
        }
    }

    
    private static boolean doesInfoHaveAVPWithValue(String siInfo, String avpName, String avpValue) {
        String currentValue = Utils.getValueFromCRDelimitedAVPString(siInfo, avpName);
        log.debug("Attribute [{}] has value [{}]", avpName, currentValue);

        if (currentValue == null) {
            return avpValue.isEmpty();
        } else {
            return currentValue.equals(avpValue);
        }
    }

    private static boolean doesInfoHaveAVPWithValue(EntityManager em, int serviceInstanceId, String avpName, String avpValue) {
        String siInfo = DAO.getSIInfo(em, serviceInstanceId);
        return doesInfoHaveAVPWithValue(siInfo, avpName, avpValue);
    }

     */
}
