/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.CampaignData;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceMapping;
import com.smilecoms.commons.sca.ProductServiceSpecificationMapping;
import com.smilecoms.commons.sca.ProductSpecification;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StProductInstanceLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditInstance;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class CatalogBean extends BaseBean {

    private List<UnitCreditSpecificationBean> unitCreditSpecifications;
    private List<ProductSpecificationBean> productSpecifications;
    private static final Logger LOG = LoggerFactory.getLogger(CatalogBean.class);
    public CatalogBean() {

    }

    public static CatalogBean getUnitCreditCatalog() {
        CatalogBean cat = new CatalogBean();
        cat.unitCreditSpecifications = UnitCreditSpecificationBean.wrap(NonUserSpecificCachedDataHelper.getAllUnitCreditSpecifications());
        return cat;
    }

    public static CatalogBean getProductInstanceAndUserSpecificUnitCreditCatalog(int productInstanceId, long accountId) {
        CatalogBean cat = new CatalogBean();
        cat.unitCreditSpecifications = UnitCreditSpecificationBean.wrap(getProductInstanceAndUserSpecificUnitCreditSpecifications(productInstanceId, accountId));
        return cat;
    }

    public static CatalogBean getUserSpecificUnitCreditCatalog() {
        List<UnitCreditSpecification> allUCs = NonUserSpecificCachedDataHelper.getAllUnitCreditSpecifications();
        List<UnitCreditSpecification> userSpecificUCs = new ArrayList<>();
        for (UnitCreditSpecification uc : allUCs) {
            if (isAllowed(uc.getPurchaseRoles())) {
                userSpecificUCs.add(uc);
            }
        }

        CatalogBean cat = new CatalogBean();
        cat.unitCreditSpecifications = UnitCreditSpecificationBean.wrap(userSpecificUCs);
        return cat;
    }
    
    public static CatalogBean getUserSpecificUnitCreditCatalog(Long accountId) {
        List<UnitCreditSpecification> allUCs = NonUserSpecificCachedDataHelper.getAllUnitCreditSpecifications();
        List<UnitCreditSpecification> userSpecificUCs = new ArrayList<>();
        
        Set<Integer> productInstanceIdSet = new HashSet<>();
        Set<Integer> campaignUC = new HashSet<>();
        Set<Integer> campaignID = new HashSet<>();
        
        Account account = SCAWrapper.getUserSpecificInstance().getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS_SERVICEINSTANCES);
        
        for (ServiceInstance svc : account.getServiceInstances()) {
            productInstanceIdSet.add(svc.getProductInstanceId());
        }
        
        for (Integer pi : productInstanceIdSet) {
            campaignUC.addAll(getProductInstanceCampaignUCSet(pi));
            campaignID.addAll(getProductInstanceCampaignIDSet(pi));
        }
        
        for (UnitCreditSpecification ucs : allUCs) {
            String mySmileUCDiscount = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "MySmileDiscountPercent");

            if (mySmileUCDiscount != null && mySmileUCDiscount.length() > 0) {
                double discountPercOff = Double.parseDouble(mySmileUCDiscount);
                // Apply the discount
                ucs.setPriceInCents(ucs.getPriceInCents() * (1 - discountPercOff / 100d));

            }
            
            if (!campaignUC.contains(ucs.getUnitCreditSpecificationId()) && Utils.getBooleanValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CampaignsOnly")) {
                LOG.debug("This UC is for campaigns only");
                continue;
            }

            if (ucs.getConfiguration().contains("BundleCannotBeSoldAsStandAlone=true")) {
                LOG.debug("This UC shouldn't be displayed on product catalogue");
                continue;
            }
            
            boolean isBooster = false;
            if (ucs.getConfiguration().contains("CanBeSoldWhenSpecIDExist=")) {  //We need to check if account has parent bundle
                Set<String> restrictingSet = Utils.getSetFromCommaDelimitedString(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CanBeSoldWhenSpecIDExist")); //Required bundles list
                boolean skipFromDisplay = true;
                isBooster = true;
                for (String specId : restrictingSet) {
                    LOG.debug("SpecId to test for restriction: {}", specId);
                    
                    List<UnitCreditInstance> unitCreditInstancesInAccount = account.getUnitCreditInstances(); 
                    
                    for (UnitCreditInstance uci : unitCreditInstancesInAccount) {
                        if (uci.getUnitCreditSpecificationId() == Integer.parseInt(specId)) {
                            LOG.debug("CanBeSoldWhenSpecIDExist for specId [{}]",uci.getUnitCreditSpecificationId());
                            userSpecificUCs.add(ucs);
                            skipFromDisplay = false;
                            break;
                        }
                    }
                    if (!skipFromDisplay) {  
                        LOG.debug("This UC can be shown. No need to skip it");                       
                        break;
                    }
                }  
            }
            
            if(isBooster) {
                // booster is already added to list. 
                continue;
            }
            
            if (ucs.getConfiguration().contains("BundleNotVisibleOnSCPWhenOnCampaignId=")) {
                Set<String> restrictingSet = Utils.getSetFromCommaDelimitedString(Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "BundleNotVisibleOnSCPWhenOnCampaignId"));
                boolean skipFromDisplay = false;

                for (String campaignId : restrictingSet) {
                    LOG.debug("CampaignId to test for restriction: {}", campaignId);
                    if (campaignID.contains(Integer.parseInt(campaignId))) {
                        skipFromDisplay = true;
                        break;
                    }
                }
                if (skipFromDisplay) {
                    LOG.debug("This UC shouldn't be displayed on product catalogue alternative exist for customer");
                    continue;
                }
            }
            
            if (Utils.isBetween(new Date(), ucs.getAvailableFrom(), ucs.getAvailableTo())) {
                for (ProductServiceMapping psm : ucs.getProductServiceMappings()) {
                    for (ServiceInstance si : account.getServiceInstances()) {

                        if (si.getServiceSpecificationId() == psm.getServiceSpecificationId()
                                && (psm.getProductSpecificationId() == 0
                                || psm.getProductSpecificationId() == UserSpecificCachedDataHelper.getProductInstance(si.getProductInstanceId(), StProductInstanceLookupVerbosity.MAIN).getProductSpecificationId())) {
                            //The following code checks if UC already exists to avoid duplicates
                            boolean isFound = false;
                            for (UnitCreditSpecification ucss : userSpecificUCs) {
                                if (ucss.getUnitCreditSpecificationId() == ucs.getUnitCreditSpecificationId()) {
                                    isFound = true;
                                    break;
                                }
                            }
                            if (!isFound) {
                                // Check UC purchase roles if allowed to purchase or just allow if its a campaign UC
                                if (isAllowed(ucs.getPurchaseRoles())) {
                                    //if (isAllowedInThisLocation(ucs)) {
                                    userSpecificUCs.add(ucs);
                                    //}
                                }
                            }
                        }
                    }
                }
            }
            
        }

        CatalogBean cat = new CatalogBean();
        cat.unitCreditSpecifications = UnitCreditSpecificationBean.wrap(userSpecificUCs);
        return cat;
    }

    public static CatalogBean getUserSpecificProductCatalog() {
        List<ProductSpecification> allPS = NonUserSpecificCachedDataHelper.getAllProductSpecifications();
        List<ProductSpecification> userSpecificPS = new ArrayList<>();
        for (ProductSpecification ps : allPS) {
            if (isAllowed(ps.getProvisionRoles()) && Utils.isNowInTimeframe(ps.getAvailableFrom(), ps.getAvailableTo())) {
                userSpecificPS.add(ps);
                List<ProductServiceSpecificationMapping> allowedMappings = new ArrayList<>();
                for (ProductServiceSpecificationMapping m : ps.getProductServiceSpecificationMappings()) {
                    if (isAllowed(m.getProvisionRoles()) && Utils.isNowInTimeframe(m.getAvailableFrom(), m.getAvailableTo())) {
                        allowedMappings.add(m);
                    }
                }
                ps.getProductServiceSpecificationMappings().clear();
                ps.getProductServiceSpecificationMappings().addAll(allowedMappings);
            }
        }
        CatalogBean cat = new CatalogBean();
        cat.productSpecifications = ProductSpecificationBean.wrap(userSpecificPS);
        return cat;
    }

    public static CatalogBean getProductCatalog() {
        CatalogBean cat = new CatalogBean();
        cat.productSpecifications = ProductSpecificationBean.wrap(NonUserSpecificCachedDataHelper.getAllProductSpecifications());
        return cat;
    }

    @XmlElement
    public List<UnitCreditSpecificationBean> getUnitCreditSpecifications() {
        return unitCreditSpecifications;
    }

    @XmlElement
    public List<ProductSpecificationBean> getProductSpecifications() {
        return productSpecifications;
    }

    private static List<UnitCreditSpecification> getProductInstanceAndUserSpecificUnitCreditSpecifications(int productInstanceId, long accountId) {

        List<UnitCreditSpecification> allUCs = NonUserSpecificCachedDataHelper.getAllUnitCreditSpecifications();
        List<UnitCreditSpecification> userSpecificUCs = new ArrayList<>();
        for (UnitCreditSpecification uc : allUCs) {
            if (isAllowed(uc.getPurchaseRoles())) {
                userSpecificUCs.add(uc);
            }
        }

        List<UnitCreditSpecification> ucsProductSpecific = new ArrayList();
        ProductBean product = ProductBean.getProductInstanceById(productInstanceId);
        List<ServiceBean> services = product.getServices();
        for (UnitCreditSpecification ucs : userSpecificUCs) {
            if (isAllowed(ucs.getPurchaseRoles())) {
                for (ServiceBean service : services) {
                    if (accountId == service.getAccountId()) {
                        for (ProductServiceMapping psm : ucs.getProductServiceMappings()) {
                            if (psm.getServiceSpecificationId() == service.getServiceSpecificationId()
                                    && (psm.getProductSpecificationId() == 0 || psm.getProductSpecificationId() == product.getProductSpecificationId())) {
                                ucsProductSpecific.add(ucs);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return ucsProductSpecific;
    }
    
    private static Set<Integer> getProductInstanceCampaignUCSet(int productInstanceId) {
        Set<Integer> campaignUC = new HashSet<>();
        try {
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_CAMPAIGNS_CAMPAIGNUC);
            for (CampaignData cd : pi.getCampaigns()) {
                campaignUC.addAll(cd.getCampaignUnitCredits());
            }
        } catch (Exception e) {
            LOG.warn("Error: ", e);
        }
        return campaignUC;
    }

    private static Set<Integer> getProductInstanceCampaignIDSet(int productInstanceId) {
        Set<Integer> campaignID = new HashSet<>();
        try {
            ProductInstance pi = UserSpecificCachedDataHelper.getProductInstance(productInstanceId, StProductInstanceLookupVerbosity.MAIN_CAMPAIGNS_CAMPAIGNUC);
            for (CampaignData cd : pi.getCampaigns()) {
                campaignID.add(cd.getCampaignId());
            }
        } catch (Exception e) {
            LOG.warn("Error: ", e);
        }
        return campaignID;
    }
}
