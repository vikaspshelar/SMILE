/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.*;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.direct.im.NiraVerifyQuery;
import com.smilecoms.commons.sca.direct.im.NiraVerifyReply;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.tags.SmileTags;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.stripes.InsufficientPrivilegesException;
import static com.smilecoms.commons.stripes.SmileActionBean.SESSION_KEY_CUSTOMER_ORG_ID;
import com.smilecoms.commons.util.JPAUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import static java.lang.Math.log;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.servlet.http.HttpSession;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import org.apache.commons.lang.StringUtils;
import org.asteriskjava.manager.event.AgentLoginEvent;
import sun.misc.VMSupport;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


/**
 *
 * @author PCB
 *
 */
public class SalesActionBean extends SmileActionBean {

    @DefaultHandler
    public Resolution showMakeSaleWizard() {
        checkPermissions(Permissions.MAKE_SALE);
        
        HttpSession session = getContext().getRequest().getSession();  
        session.removeAttribute("fromSale");
    
        return getDDForwardResolution("/sales/make_sale_get_customer.jsp");
    }

    private String imsi;

    public void setIMSI(String imsi) {
        this.imsi = imsi;
    }

    @DontValidate()
    public Resolution searchCustomerForSale() {
        CustomerActionBean cab = cloneToActionBean(CustomerActionBean.class);
        cab.searchCustomer();
        setCustomerList(cab.getCustomerList());
        return getDDForwardResolution("/sales/make_sale_get_customer.jsp");
    }

    @DontValidate()
    public Resolution showStockLocation() {
        setSoldStockLocationQuery(new SoldStockLocationQuery());        
        boolean hasAnypermission = false;
        try {
            checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_ALL);
            hasAnypermission = true;
        } catch (InsufficientPrivilegesException ipe) {            
            int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();
            getSoldStockLocationQuery().setSoldToOrganisationId(orgId);
        }
        
        if (!hasAnypermission) {
            try {
                checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_MD);
                hasAnypermission = true;
            } catch (InsufficientPrivilegesException ipe) {
                int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();
                getSoldStockLocationQuery().setHeldByOrganisationId(orgId);
            }
        }
        
        if (!hasAnypermission) {
            try {
                checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_SD);
                hasAnypermission = true;
            } catch (InsufficientPrivilegesException ipe) {
                int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();
                getSoldStockLocationQuery().setHeldByOrganisationId(orgId);
            }
        }
        if (!hasAnypermission) {
            checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_ICP);
            getSoldStockLocationQuery().setSoldToOrganisationId(0);
        }
        
        if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled", false)) {
            return getDDForwardResolution("/sales/view_stock_location_ng.jsp");
        }  else {
            return getDDForwardResolution("/sales/view_stock_location.jsp");
        }
    }

    @DontValidate()
    public Resolution retrieveStockLocation() {
        clearValidationErrors();
        
        boolean hasAnypermission = false;
        try {
            checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_ALL);
            hasAnypermission = true;
        } catch (InsufficientPrivilegesException ipe) {
            int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();
            getSoldStockLocationQuery().setSoldToOrganisationId(orgId);
        }
        
        if (!hasAnypermission) {
            try {
                checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_MD);
                hasAnypermission = true;
            } catch (InsufficientPrivilegesException ipe) {
                int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();
                getSoldStockLocationQuery().setHeldByOrganisationId(orgId);
            }
        }

        if (!hasAnypermission) {
            try {
                checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_SD);
                hasAnypermission = true;
            } catch (InsufficientPrivilegesException ipe) {
                int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();
                getSoldStockLocationQuery().setHeldByOrganisationId(orgId);
            }
        }

        if (!hasAnypermission) {
            checkPermissions(Permissions.VIEW_SOLD_STOCK_LOCATION_ICP);
            getSoldStockLocationQuery().setSoldToOrganisationId(0);
        }
    
     if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled", false)) {   
        if(isICP(getSoldStockLocationQuery().getSoldToOrganisationId()) || isSuperDealer(getSoldStockLocationQuery().getSoldToOrganisationId())) {
            getSoldStockLocationQuery().setHeldByOrganisationId(getSoldStockLocationQuery().getSoldToOrganisationId());
            getSoldStockLocationQuery().setSoldToOrganisationId(0);            
        }
     }
        
        setSoldStockLocationList(SCAWrapper.getUserSpecificInstance().getSoldStockLocations(getSoldStockLocationQuery()));
        if (getSoldStockLocationList().getSoldStockLocations().isEmpty()) {
            localiseErrorAndAddToGlobalErrors("no.records.found");
            
            if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled", false)) {
                return getDDForwardResolution("/sales/view_stock_location_ng.jsp");
            }  else {
                return getDDForwardResolution("/sales/view_stock_location.jsp");
            }
        }
        
        if(getSoldStockLocationList().getSoldStockLocations().size()>0) {
            if(isSuperDealer(getSoldStockLocationList().getSoldStockLocations().get(0).getHeldByOrganisationId())) {
                setSuperDealerSeller(true);
            }

            if(isICP(getSoldStockLocationList().getSoldStockLocations().get(0).getHeldByOrganisationId())) {
                setICPSeller(true);
            }
        }
        if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled", false)) {
            return getDDForwardResolution("/sales/view_stock_location_ng.jsp");
        }  else {
            return getDDForwardResolution("/sales/view_stock_location.jsp");
        }
    }
    
    boolean superDealerSeller=false;
    boolean ICPSeller=false;

    public boolean isICPSeller() {
        return ICPSeller;
    }

    public void setICPSeller(boolean ICPSeller) {
        this.ICPSeller = ICPSeller;
    }

   
    public boolean isSuperDealerSeller() {
        return superDealerSeller;
    }

    public void setSuperDealerSeller(boolean superDealerSeller) {
        this.superDealerSeller = superDealerSeller;
    }
    
    @DontValidate()
    public Resolution setStockLocation() {
        clearValidationErrors();
        boolean isReplacement = false;
        String serialList = getParameter("serialList");
        if (getSoldStockLocationData() == null) {
            return showStockLocation();
        }
        
        if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled", false)) {  //BuyerSeller Operation Enabled
            
            return setStockLocationToLinkedSeller();
        }
   
        if (serialList != null && !serialList.isEmpty()) {
            String[] serials = serialList.split("\r\n");
            String itemNumber = this.getSoldStockLocationData().getSoldStockLocations().get(0).getItemNumber();
            Integer soldTo = getSoldStockLocationData().getSoldStockLocations().get(0).getSoldToOrganisationId();
            Integer heldBy = getSoldStockLocationData().getSoldStockLocations().get(0).getHeldByOrganisationId();
            isReplacement = getSoldStockLocationData().getSoldStockLocations().get(0).isUsedAsReplacement() == null ? false : getSoldStockLocationData().getSoldStockLocations().get(0).isUsedAsReplacement();
            if (soldTo == null || soldTo == 0 || heldBy == null || heldBy == 0) {
                localiseErrorAndAddToGlobalErrors("invalid.stock.data");
                return showStockLocation();
            }
            
            this.getSoldStockLocationData().getSoldStockLocations().clear();
            for (String serial : serials) {
                SoldStockLocation s = new SoldStockLocation();
                s.setHeldByOrganisationId(heldBy);
                s.setSoldToOrganisationId(soldTo);
                s.setSerialNumber(serial);
                s.setUsedAsReplacement(isReplacement);
                s.setItemNumber(itemNumber);
                getSoldStockLocationData().getSoldStockLocations().add(s);
            }
        }

        boolean hasAnypermission = false;
        int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();

        try {
            checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_ALL);
            hasAnypermission = true;
        } catch (InsufficientPrivilegesException ipe) {            
            for (SoldStockLocation location : getSoldStockLocationData().getSoldStockLocations()) {
                if (location.getSoldToOrganisationId() == null || location.getSoldToOrganisationId() == 0) {
                    localiseErrorAndAddToGlobalErrors("invalid.stock.data");
                    return showStockLocation();
                }

                if (location.getSoldToOrganisationId() != orgId) {
                    try {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_ALL);
                    } catch (InsufficientPrivilegesException ipe3) {
                        if (isReplacement) {
                            checkPermissions(Permissions.EDIT_SOLD_STOCK_USED_AS_REPLACEMENT);
                            hasAnypermission = true;
                        }
                    }
                }

                if ((location.getHeldByOrganisationId() != null) && (location.getHeldByOrganisationId() != orgId)) {
                    if(isMegaDealer(orgId)) {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_MD);
                    } else {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_SD);
                    }
                }

                try {
                    if(isMegaDealer(orgId)) {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_MD);
                    } else {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_SD);
                    }                    
                } catch (InsufficientPrivilegesException ipe2) {
                    if (isReplacement) {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_USED_AS_REPLACEMENT);
                        hasAnypermission = true;
                    } else {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_ICP);
                        hasAnypermission = true;
                    }
                }
            }
        }

        if (!hasAnypermission) {
            if(isMegaDealer(orgId)) {
                checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_MD);
            } else {
                checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_SD);
            }
            
        }

        clearValidationErrors();
        if (getSoldStockLocationData() == null) {
            return showStockLocation();
        }
        SCAWrapper.getUserSpecificInstance().setSoldStockLocations(getSoldStockLocationData());
        setPageMessage("stock.locations.updated.successfully");
        return showStockLocation();
    }
    
    @DontValidate()
    public Resolution setStockLocationToLinkedSeller() {
        clearValidationErrors();
        boolean isReplacement = false;
        String serialList = getParameter("serialList");
        if (getSoldStockLocationData() == null) {
            return showStockLocation();
        }
        
        Integer soldTo = getSoldStockLocationData().getSoldStockLocations().get(0).getSoldToOrganisationId();
        Integer heldBy = getSoldStockLocationData().getSoldStockLocations().get(0).getHeldByOrganisationId();        

        if (soldTo == null || soldTo == 0 || heldBy == null || heldBy == 0) {
            localiseErrorAndAddToGlobalErrors("invalid.stock.data");
            return showStockLocation();
        }
        
        if(BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled", false)) {  //BuyerSeller Operation Enabled    
                 
            if(!(isMegaDealer(soldTo) && isSuperDealer(heldBy)) && !(isSuperDealer(soldTo) && isICP(heldBy))) {
                localiseErrorAndAddToGlobalErrors("error", "Invalid sale/stock movement. Only MegaDealers can sell to SuperDealers & Only SuperDealers can sell to ICPs");
                return showStockLocation();
            }

            if(isMegaDealer(soldTo) && !isSuperDealer(heldBy)) {
                soldTo=null;
                heldBy=null;
                setSoldStockLocationData(new SoldStockLocationData());
                localiseErrorAndAddToGlobalErrors("error", "Invalid sale/stock movement. Megadealer can only sell to Superdealer");
                return showStockLocation();
            }

            if(isSuperDealer(soldTo) && !isICP(heldBy)) {
                soldTo=null;
                heldBy=null;
                setSoldStockLocationData(new SoldStockLocationData());
                localiseErrorAndAddToGlobalErrors("error", "Invalid sale/stock movement. Superdealer can only sell to ICP");
                return showStockLocation();
            }

            boolean canSellToBuyer=false;
            List<Integer> allowedSellers = SCAWrapper.getUserSpecificInstance().getOrganisation(heldBy, StOrganisationLookupVerbosity.MAIN).getOrganisationSellers();

            for(int allowedSeller: allowedSellers) {                    
                if(allowedSeller == soldTo) {
                    canSellToBuyer=true;
                    break;
                }
            }

             if(!canSellToBuyer) {
                 localiseErrorAndAddToGlobalErrors("error", "Seller not listed in buyer\'s allowed sellers");
                 return showStockLocation();
             }
        }    
   
        if (serialList != null && !serialList.isEmpty()) {
            String[] serials = serialList.split("\r\n");
            String itemNumber = this.getSoldStockLocationData().getSoldStockLocations().get(0).getItemNumber();            
            isReplacement = getSoldStockLocationData().getSoldStockLocations().get(0).isUsedAsReplacement() == null ? false : getSoldStockLocationData().getSoldStockLocations().get(0).isUsedAsReplacement();
                   
            
            this.getSoldStockLocationData().getSoldStockLocations().clear();
            for (String serial : serials) {
                SoldStockLocation s = new SoldStockLocation();
                s.setHeldByOrganisationId(heldBy);
                s.setSoldToOrganisationId(soldTo);
                s.setSerialNumber(serial);
                s.setUsedAsReplacement(isReplacement);
                s.setItemNumber(itemNumber);
                getSoldStockLocationData().getSoldStockLocations().add(s);
            }
        }

        boolean hasAnypermission = false;
        int orgId = UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER).getCustomerRoles().get(0).getOrganisationId();

        try {
            checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_ALL);
            hasAnypermission = true;
        } catch (InsufficientPrivilegesException ipe) {            
            for (SoldStockLocation location : getSoldStockLocationData().getSoldStockLocations()) {
                if (location.getSoldToOrganisationId() == null || location.getSoldToOrganisationId() == 0) {
                    localiseErrorAndAddToGlobalErrors("invalid.stock.data");
                    return showStockLocation();
                }
                 
                if (location.getSoldToOrganisationId() != orgId) {
                    try {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_ALL);
                    } catch (InsufficientPrivilegesException ipe3) {
                        if (isReplacement) {
                            checkPermissions(Permissions.EDIT_SOLD_STOCK_USED_AS_REPLACEMENT);
                            hasAnypermission = true;
                        }
                    }
                }
                 
                if ((location.getHeldByOrganisationId() != null) && (location.getHeldByOrganisationId() != orgId)) {
                    if(isMegaDealer(orgId)) {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_MD);
                    } else {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_SD);
                    }
                }

                try {
                    if(isMegaDealer(orgId)) {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_MD);
                    } else {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_SD);
                    }                    
                } catch (InsufficientPrivilegesException ipe2) {
                    if (isReplacement) {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_USED_AS_REPLACEMENT);
                        hasAnypermission = true;
                    } else {
                        checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_ICP);
                        hasAnypermission = true;
                    }
                }
            }
        }

        if (!hasAnypermission) {
            if(isMegaDealer(orgId)) {
                checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_MD);
            } else {
                checkPermissions(Permissions.EDIT_SOLD_STOCK_LOCATION_SD);
            }
            
        }

        clearValidationErrors();
        if (getSoldStockLocationData() == null) {
            return showStockLocation();
        }
        SCAWrapper.getUserSpecificInstance().setSoldStockLocations(getSoldStockLocationData());
        setPageMessage("stock.locations.updated.successfully");
        return showStockLocation();
    }

    public Resolution anonymousSale() {
        return collectItemsInSale();
    }

    List<String> p2pInvoicingDataOptions = new ArrayList<>();

    public List<java.lang.String> getP2pInvoicingDataOptions() {
        return p2pInvoicingDataOptions;
    }

    public void setP2pInvoicingDataOptions(List<String> p2pInvoicingDataOptions) {
        this.p2pInvoicingDataOptions = p2pInvoicingDataOptions;
    }

    public Resolution generateP2PQuote() {
        String P2PCalendarInvoicing = getParameter("P2PCalendarInvoicing") == null ? null : getParameter("P2PCalendarInvoicing");
        String P2PInvoicingPeriod = getParameter("P2PInvoicingPeriod") == null ? null : getParameter("P2PInvoicingPeriod");
        log.warn("Invoicing date is: {}", P2PInvoicingPeriod);
        //String dateAsString = Utils.getValueFromCRDelimitedAVPString(P2PInvoicingPeriod, "P2PInvoicingPeriod");
        Date P2PInvoicingDate = Utils.getStringAsDate(P2PInvoicingPeriod, "yyyy-MM-dd");

        log.warn("Invoicing date is: {}", P2PInvoicingDate);

        P2PInvoicingPeriod = Utils.getDateAsString(P2PInvoicingDate, "yyyy/MM/dd");
        log.warn("Invoicing date after change is: {}", P2PInvoicingPeriod);
        String numOfDays = "0";

        for (SaleLine s : getSale().getSaleLines()) {
            if (s.getInventoryItem().getItemNumber().startsWith("BENTMW")) {
                numOfDays = Long.toString(s.getQuantity());
                break;
            }// if                
        }
        if (notConfirmed()) {
            return confirm(numOfDays);
        }

        //Info to be sent to POS and BM to do p2p invoicing related calculations
        if ((P2PInvoicingPeriod != null && !P2PInvoicingPeriod.isEmpty())
                && (P2PCalendarInvoicing != null && !P2PCalendarInvoicing.isEmpty())) {

            String extraInfo = getSale().getExtraInfo() == null ? "" : getSale().getExtraInfo();
            getSale().setExtraInfo(extraInfo.isEmpty() ? "P2PInvoicingPeriod=" + P2PInvoicingPeriod : extraInfo + "\r\nP2PInvoicingPeriod=" + P2PInvoicingPeriod);

            for (SaleLine line : getSale().getSaleLines()) {
                if (line.getInventoryItem().getItemNumber().startsWith("BENTMW")) {
                    String provData = line.getProvisioningData() == null ? "" : line.getProvisioningData();
                    line.setProvisioningData(provData.isEmpty() ? "P2PCalendarInvoicing=" + P2PCalendarInvoicing : provData + "\r\nP2PCalendarInvoicing=" + P2PCalendarInvoicing);
                    p2pInvoicingDataOptions.add(line.getProvisioningData());
                    p2pInvoicingDataOptions.add("P2PExpiryDate=" + Utils.getDateOfLastDayOfMonthAsString(new Date(), "yyyy/MM/dd"));
                    p2pInvoicingDataOptions.add("P2PInvoicingPeriod=" + P2PInvoicingPeriod);
                }
            }
        }

        return generateQuote();
    }

    private boolean comingFromMakeSale = false;

    public boolean isComingFromMakeSale() {
        return comingFromMakeSale;
    }

    public void setComingFromMakeSale(boolean comingFromMakeSale) {
        this.comingFromMakeSale = comingFromMakeSale;
    }
    
    private boolean nimcApproved=false;

    public boolean isNimcApproved() {
        return nimcApproved;
    }

    public void setNimcApproved(boolean nimcApproved) {
        this.nimcApproved = nimcApproved;
    }
    
    private boolean tcraApproved = false;

    public boolean isTcraApproved() {
        return tcraApproved;
    }

    public void setTcraApproved(boolean tcraApproved) {
        this.tcraApproved = tcraApproved;
    }
    

    private String freelanceSalePersonFullName;

    public java.lang.String getFreelanceSalePersonFullName() {
        return freelanceSalePersonFullName;
    }

    public void setFreelanceSalePersonFullName(java.lang.String freelanceSalePersonFullName) {
        this.freelanceSalePersonFullName = freelanceSalePersonFullName;
    }
    
    public Resolution generateQuote() {
        checkPermissions(Permissions.MAKE_SALE);
        if (!getSale().getPromotionCode().isEmpty()) {
            checkPermissions(Permissions.USE_SALE_PROMOTION_CODE);
        }
        if (getSale().isTaxExempt()) {
            checkPermissions(Permissions.MAKE_SALE_TAX_EXEMPT);
        }

        // if (log.isDebugEnabled()) {
        //Check if we must prevent ICPs from buying directly at SMILE;
        log.debug("Sale line details...");          
        boolean mustSelectTargetAccount = false;
        
        
        for (SaleLine s : getSale().getSaleLines()) {
            log.debug("XXLine [{}] Serial [{}]", s.getLineNumber(), s.getInventoryItem().getSerialNumber());
            // - Check for NIRA stuff here...
            if (s.getInventoryItem().getBoxSize() > 1) {
                if (StringUtils.isEmpty(s.getProvisioningData())) {
                    s.setProvisioningData("BoxSize=" + s.getInventoryItem().getBoxSize());
                } else {
                    s.setProvisioningData(s.getProvisioningData() + "\nBoxSize=" + s.getInventoryItem().getBoxSize());
                }
            }

            if (isItemNumberUnitCredit(s.getInventoryItem().getItemNumber()) || isAirtime(s.getInventoryItem().getSerialNumber())) {
                mustSelectTargetAccount = true;
            }

            if (s.getInventoryItem().getItemNumber().startsWith("KIT")
                    && BaseUtils.getBooleanProperty("env.customer.verify.with.nira", false)
                    && isAllowed(BaseUtils.getProperty("env.sep.roles.allowed.to.verify.customers.with.nira", ""))) {

                if (getSale().getRecipientCustomerId() > 0) {
                    // CustomerQuery q = new CustomerQuery();
                    setCustomerQuery(new CustomerQuery());
                    getCustomerQuery().setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                    getCustomerQuery().setCustomerId(getSale().getRecipientCustomerId());
                    getCustomerQuery().setResultLimit(1);
                    setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomerQuery()));
                }

                if (getCustomer().getNationality().equalsIgnoreCase(BaseUtils.getProperty("env.locale.country.for.language.en"))
                        && getSale().getRecipientOrganisationId() <= 0) {
                    // !(getCustomer().getKYCStatus() != null && getCustomer().getKYCStatus().equals("V")) && 
                    //Go yo update customer;
                    getCustomer().setDateOfBirth(Utils.addSlashesToDate(getCustomer().getDateOfBirth()));
                    if (!getCustomer().getPassportExpiryDate().isEmpty()) {
                        getCustomer().setPassportExpiryDate(Utils.addSlashesToDate(getCustomer().getPassportExpiryDate()));
                    }

                    if (!getCustomer().getVisaExpiryDate().isEmpty()) {
                        getCustomer().setVisaExpiryDate(Utils.addSlashesToDate(getCustomer().getVisaExpiryDate()));
                    }

                    NiraVerifyQuery nvQ = new NiraVerifyQuery();
                    nvQ.setCardNumber(getCustomer().getCardNumber());
                    nvQ.setDateOfBirth(getCustomer().getDateOfBirth());
                    nvQ.setFirstName(getCustomer().getFirstName());
                    nvQ.setIdentityNumber(getCustomer().getIdentityNumber());
                    nvQ.setLastName(getCustomer().getLastName());
                    nvQ.setOtherNames("");

                    NiraVerifyReply nvR = SCAWrapper.getUserSpecificInstance().verifyCustomerWithNIRA_Direct(nvQ);

                    if (!nvR.isStatus()) { //Verify failed - redirecr to the verify page
                        // As resquested by UG.
                        getCustomer().setFirstName("");
                        getCustomer().setMiddleName("");
                        getCustomer().setLastName("");
                        getCustomer().setDateOfBirth("");
                        getCustomer().setIdentityNumber("");
                        getCustomer().setCardNumber("");

                        setComingFromMakeSale(true);

                        // setPageMessage("customer.nira.verify.failed");
                        // return getDDForwardResolution("/customer/edit_customer.jsp");
                        // return getDDForwardResolution(CustomerActionBean.class, "editCustomer");
                        ForwardResolution forward = (ForwardResolution) getDDForwardResolution(CustomerActionBean.class, "editCustomer");
                        // forward.addParameter("customerQuery", getCustomerQuery());
                        forward.addParameter("customerQuery.customerId", getCustomer().getCustomerId());
                        forward.addParameter("customerQuery.verbosity", StCustomerLookupVerbosity.CUSTOMER);
                        forward.addParameter("customerQuery.resultLimit", 1);
                        forward.addParameter("comingFromMakeSale", true);
                        setCustomer(null);
                        return forward;
                    } else {
                        setPageMessage("customer.nira.verify.successful");
                        
                    }
                }
            }
            
            for (SaleLine ss : s.getSubSaleLines()) {
                log.warn("SubLine Serial [{}] and Item [{}]", ss.getInventoryItem().getSerialNumber(), ss.getInventoryItem().getItemNumber());
                
            }
            
        }

        if (getSale().getRecipientAccountId() <= 0 && mustSelectTargetAccount) {
            checkPermissions(Permissions.ALLOW_NONSTOCK_ITEM_SALE_WITHNO_RECIPIENT_ACCOUNT);
        }

        getSale().setPaymentMethod(null);
        getSale().setCreditAccountNumber(null);
        getSale().setAmountTenderedCents(0);
        getSale().setSalesPersonCustomerId(getUserCustomerIdFromSession());
        getSale().setWarehouseId(getWarehouseIdFromSession());
        if (getParameter("invoiceExpiryDays") != null && !getParameter("invoiceExpiryDays").isEmpty()) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, Integer.parseInt(getParameter("invoiceExpiryDays")));
            getSale().setExpiryDate(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
        }
        
        SCABusinessError sbe = null;
        if (getSale().getStatus() == null || (!getSale().getStatus().equals("QT") && !getSale().getStatus().equals("SP"))) { // Do not call generate quote is a sale is already in a quote status
            try {
                log.debug("Calling SCA to generate quote");
                setSale(SCAWrapper.getUserSpecificInstance().generateQuote(getSale()));
                log.debug("Called SCA to generate quote");
            } catch (SCABusinessError e) {
                log.debug("Got an error [{}]", e.toString());
                sbe = e;
            }
        }

        /*CHECK SD CONSTRAINTS -- HBT-9230*/
        log.debug("Going to check SD constraints");
        try {
            checkSuperDealerConstraints(getSale());
        } catch (SCABusinessError ex) {
            sbe = ex;
        }

        possibleAccountIdsForTransfer = new TreeSet<>();
        accountTypes = new HashMap();
        accountChannels = new HashMap();

        //Get the possible sales persons account Id's
        CustomerQuery q = new CustomerQuery();
        q.setCustomerId(getUserCustomerIdFromSession());
        q.setResultLimit(1);
        q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
        Customer salesPersonCust = SCAWrapper.getUserSpecificInstance().getCustomer(q);
        for (ProductInstance pi : salesPersonCust.getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == salesPersonCust.getCustomerId() && si.getAccountId() > 1100000000) {
                    /*
                     * Special Service Spec Id's:
                     1000 - Direct Sales (held by a Smile sales agent for selling to customers via SEP)
                     1001 - TPGW (held by a Smile Partner for transferring to customers via TPGW)
                     1002 - Corporate Sales (Held by a Smile sales agent for selling specifically to corporates via SEP)
                     1003 - Indirect Sales (held by a Smile partner and their staff for transferring to customes via SEP)
                     1004 - Partner Sales (held by a Smile sales agent for selling to partner accounts of spec 1001 or 1003) 
                     1007 - Direct Airtime Sales (held by a Smile sales agent for selling to customers via SEP) 
                     */
                    if (si.getServiceSpecificationId() == 1000 || si.getServiceSpecificationId() == 1002 || si.getServiceSpecificationId() == 1004 || si.getServiceSpecificationId() == 1007) {
                        Set<String> possibleChannels = getAccountsPossibleChannels(si.getAccountId());
                        if (!possibleChannels.isEmpty()) {
                            possibleAccountIdsForTransfer.add(si.getAccountId());
                            accountTypes.put(si.getAccountId(), NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId()).getName());
                            accountChannels.put(si.getAccountId(), possibleChannels);
                        } else {
                            log.debug("Account [{}] has no channels configured", si.getAccountId());
                        }
                    }
                }
            }
        }
        // Default first account id
        log.debug("Found [{}] possible accounts", possibleAccountIdsForTransfer.size());
        if (!possibleAccountIdsForTransfer.isEmpty()) {
            getSale().setSalesPersonAccountId(possibleAccountIdsForTransfer.iterator().next());
        }

        if (getSale().getRecipientCustomerId() > 0) {
            q = new CustomerQuery();
            q.setCustomerId(getSale().getRecipientCustomerId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(q));
        }
        if (getSale().getRecipientOrganisationId() > 0) {
            OrganisationQuery oq = new OrganisationQuery();
            oq.setOrganisationId(getSale().getRecipientOrganisationId());
            oq.setResultLimit(1);
            oq.setVerbosity(StOrganisationLookupVerbosity.MAIN);
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(oq));

            // Get contracts (if any) associated with the above organisation
            setContractQuery(new ContractQuery());
            getContractQuery().setOrganisationId(getOrganisation().getOrganisationId());
            setContractList(SCAWrapper.getUserSpecificInstance().getContracts(getContractQuery()));
        }

        getSale().setPaymentMethod(BaseUtils.getPropertyAsList("env.pos.payment.methods").get(0));

        provisioningDataOptions = new TreeSet<>();
        if (getCustomer() != null) {
            for (ProductInstance pi : getCustomer().getProductInstances()) {
                for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                    ServiceInstance si = m.getServiceInstance();
                    if (si.getAccountId() > 1100000000) {
                        StringBuilder desc = new StringBuilder();
                        String prodName = NonUserSpecificCachedDataHelper.getProductSpecification(pi.getProductSpecificationId()).getName();
                        desc.append("Product:");
                        desc.append(prodName);
                        desc.append(", ");
                        String friendlyName = pi.getFriendlyName();
                        if (friendlyName != null && !friendlyName.isEmpty()) {
                            desc.append("Friendly Name:");
                            desc.append(friendlyName);
                            desc.append(", ");
                        }
                        long acc = si.getAccountId();

                        if (acc > 0) {
                            desc.append("Acc:");
                            desc.append(acc);
                            desc.append(", ");
                        }
                        String physicalId = pi.getPhysicalId();
                        if (physicalId != null && !physicalId.isEmpty()) {
                            desc.append("ICCID:");
                            desc.append(physicalId);
                            desc.append(", ");
                        }
                        desc.append("PIID:");
                        desc.append(pi.getProductInstanceId());
                        provisioningDataOptions.add(desc.toString());
                    }
                }
            }
        }
        
        
        
        for (SaleLine line : getSale().getSaleLines()) {
            if (!line.getSubSaleLines().isEmpty()) {
                provisioningDataOptions.add("SIM Provisioned for line: " + line.getLineNumber());
            }
        }

        if (sbe != null) {
            log.debug("Throwing the business error we got earlier");
            throw sbe;
        }

        if (BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {

            if (getSale().getFreelancerCustomerId() > 0) {
                CustomerQuery cq = new CustomerQuery();
                cq.setCustomerId(Long.valueOf(getSale().getFreelancerCustomerId()).intValue());
                cq.setResultLimit(1);
                cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                Customer freelancer = SCAWrapper.getAdminInstance().getCustomer(cq);
                if (freelancer != null) {
                    //Check if freelancer is KYC verified?

                    //if(freelancer.getKYCStatus() != null && freelancer.getKYCStatus().equalsIgnoreCase("V")) {
                    setFreelanceSalePersonFullName(freelancer.getFirstName() + " " + freelancer.getLastName());
                    /*} else { //Freelance not verified
                        localiseErrorAndAddToGlobalErrors("freelance.sales.person.not.kyc.verified", " Freelancer Customer Profile Id: " + freelancer.getCustomerId());
                        // return getDDForwardResolution("/sales/make_sale_get_items.jsp");
                        return backToGetItems();
                    }*/
                }
            } else {
                if (getSale().getFreelancerCustomerId() == -1) {  // i.e N/A selected
                    localiseErrorAndAddToGlobalErrors("freelance.sales.person.not.selected");
                    return backToGetItems();
                    //return getDDForwardResolution("/sales/make_sale_get_items.jsp");
                }
            }
        }
        
        if(BaseUtils.getProperty("env.country.name").equals("Tanzania") && !isTcraApproved() && getCustomer().getProductInstances().size()>0) { 
            boolean hasSim = false;
            for (SaleLine s : getSale().getSaleLines()) {
                if(hasSim) break;                
                if (s.getInventoryItem().getDescription().toLowerCase().contains("sim")) {
                    hasSim = true;
                    break;                  
                } 
 
                for (SaleLine ss : s.getSubSaleLines()) {                
                    if (ss.getInventoryItem().getDescription().toLowerCase().contains("sim")) {
                        hasSim = true;   
                        break;
                    } 
                }               

            }
            
            if(hasSim) {
                
                HttpSession session = getContext().getRequest().getSession();               
                session.setAttribute("sessionSale", getSale());
                session.removeAttribute("sessionAddProduct");
                return showVerifyAddAdditionalSim();                    
            }
                
        }
        
       
        if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria") && !isNimcApproved()) { 
            boolean hasSim = false;
            for (SaleLine s : getSale().getSaleLines()) {
                if(hasSim) break;                
                if (s.getInventoryItem().getDescription().toLowerCase().contains("sim")) {
                    hasSim = true;
                    break;                  
                } 
 
                for (SaleLine ss : s.getSubSaleLines()) {                
                    if (ss.getInventoryItem().getDescription().toLowerCase().contains("sim")) {
                        hasSim = true;   
                        break;
                    } 
                }               

            }
            
            if(hasSim && BaseUtils.getBooleanProperty("env.nimc.use.fingerprint",false)) {
                                
                if(getCustomer().getNationalIdentityNumber()==null || getCustomer().getNationalIdentityNumber().trim().length()==0) {
                    //Cannot make sim sale without NIN
                    localiseErrorAndAddToGlobalErrors("customer.nin.not.captured");
                    return getDDForwardResolution("/customer/view_customer.jsp");
                } else {
                    HttpSession session = getContext().getRequest().getSession();               
                    session.setAttribute("sessionSale", getSale());  
                    session.setAttribute("customerNIN", getCustomer().getNationalIdentityNumber());
                    return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");                
                }
            }
                
        }
        

        log.debug("Rendering quote");

        return getDDForwardResolution("/sales/quote.jsp");
    }
    
    
    public Resolution proceedToQuote() {
        setninVerified(true);
        setNimcApproved(true);

        HttpSession session = getContext().getRequest().getSession();               
        setSale((Sale) session.getAttribute("sessionSale"));
        session.setAttribute("fromSale", "true");
        session.removeAttribute("sessionSale");
        
        return generateQuote();
    }
    
    public Resolution verifySimRegistration () {        
        
        if(!BaseUtils.getProperty("env.country.name").equals("Tanzania")) {
            localiseErrorAndAddToGlobalErrors("error.system");
            return getDDForwardResolution("/sales/additional_sim_purchase.jsp");
            
        }
        
            NumbersQuery q = new NumbersQuery();
            q.setResultLimit(50);
            q.setPriceLimitCents(0);
            // Dont list any numbers owned by somebody
            q.setOwnedByCustomerProfileId(0);
            q.setOwnedByOrganisationId(0);
            NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(q);
            List<String> ret = new ArrayList();

            for (com.smilecoms.commons.sca.Number num : list.getNumbers()) {
                ret.add(Utils.getFriendlyPhoneNumber(num.getIMPU()));
            }            

           setAvailableNumbers(ret);
          
            String response="";            
            
            CustomerQuery query = new CustomerQuery();
            query.setSSOIdentity(getUser());
            query.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            query.setResultLimit(1);
            Customer agent = SCAWrapper.getUserSpecificInstance().getCustomer(query);
            
            CustomerQuery custQuery = new CustomerQuery();
            custQuery.setIdentityNumber(getParameter("customerNIN"));
            custQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            custQuery.setResultLimit(1);
            Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(custQuery);
                    
            int agentCode = agent.getCustomerId();
            String agentNIN = agent.getIdentityNumber().trim();
            
            String agentMSISDN = Utils.getCleanDestination(agent.getAlternativeContact1()).trim();
            
            String conversationId = String.valueOf(agent.getCustomerId()) + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            ArrayList otherNumbers = new ArrayList();
            
            if(!Utils.getCleanDestination(cust.getAlternativeContact1()).isEmpty() && cust.getAlternativeContact1()!= null)
                otherNumbers.add(Utils.getCleanDestination(cust.getAlternativeContact1()).trim());
            
            if(!Utils.getCleanDestination(cust.getAlternativeContact2()).isEmpty() && cust.getAlternativeContact2()!= null )
                otherNumbers.add(Utils.getCleanDestination(cust.getAlternativeContact2()).trim());
            
             try {
                    URL url = new URL("http://10.24.64.20:8090/additionalSIMCard");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");

                    String nums="";
                    for(Object otherNumber: otherNumbers) {

                        if(nums.isEmpty())
                            nums = "\""+ otherNumber + "\"";
                        else
                            nums += ",\""+ otherNumber + "\"";
                    }
                    
                    String input = "{"
                            + "\"agentCode\":\"" + agentCode + "\","
                            + "\"agentNIN\": \"" + agentNIN + "\","
                            + "\"agentMSISDN\":\"" + agentMSISDN + "\","
                            + "\"conversationId\":\"" + conversationId + "\","
                            + "\"customerMSISDN\":\"" + getParameter("customerMSISDN").trim() + "\","
                            + "\"customerNIN\":\"" + getParameter("customerNIN").trim() + "\","
                            + "\"reasonCode\":\"" + getParameter("addSimReasonCode").trim() + "\","
                            + "\"registrationCategoryCode\": \"" + getParameter("simRegistrationCategory").trim() + "\","                        
                            + "\"iccid\": \"" + getParameter("iccid").trim() + "\","                        
                            + "\"registrationType\": \"Existing\"," ;
                    
                        if(!getParameter("simRegistrationCategory").equals("2000")) {    
                           input += "\"otherNumbers\": [" + nums + "]";
                        } else {
                            input += "\"otherNumbers\": []";
                        }
                        
                        input += "}";
                    
                    OutputStream os = conn.getOutputStream();
                    os.write(input.getBytes());
                    os.flush();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode() + " - "
                                    + conn.getResponseMessage());
                    }
                    
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                                    (conn.getInputStream())));

                    
                    JSONParser parser = new JSONParser(); 
                    JSONObject json = new JSONObject();
                    
                    while ((response = br.readLine()) != null) {
                        
                        try {
                            json = (JSONObject) parser.parse(response);
                        } catch (Exception e) { log.warn("Problem parsing response [{}]", response);}
                    }                
                    
                log.debug("TCRA AddSimVerify INPUT: [{}]", input);    
                log.debug("TCRA AddSimVerify RESPONSE: [{}]", json.toJSONString());
                
                String customerNIN="", customerMSISDN="", customerICCID="", tcraResponseDesc="";
                try {
                    JSONObject jsonInput = (JSONObject) parser.parse(input);
                    customerNIN= jsonInput.get("customerNIN").toString();
                    customerMSISDN= jsonInput.get("customerMSISDN").toString();
                    customerICCID= jsonInput.get("iccid").toString();
                    tcraResponseDesc= json.get("responseDescription").toString();
                    
                } catch (Exception e) {}
                
                storeAdditionalSimData(input, json.toJSONString(), customerNIN, customerMSISDN, customerICCID, tcraResponseDesc);
                
                if(!json.get("responseCode").equals("150")) {
                    if(json.get("responseCode").equals("999")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("151")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("152")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("153")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("154")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("155")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("156")) {
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                    
                    } else if(json.get("responseCode").equals("000")) {                        
                        setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                       
                    } else {
                        localiseErrorAndAddToGlobalErrors("system.error");
                        
                    }
                     
                    return getDDForwardResolution("/sales/additional_sim_purchase.jsp");                   
                } else {
                    //log.warn("Returned: [{}]", json.toJSONString());
                    
                    setRegulatorResponse("Regulator Response: " + json.get("responseDescription").toString());
                }                      
                    
                conn.disconnect();
              } catch (MalformedURLException e) {

                    e.printStackTrace();
                    localiseErrorAndAddToGlobalErrors("System error");
                     
                    return getDDForwardResolution("/sales/additional_sim_purchase.jsp");  

              } catch (IOException e) {

                    e.printStackTrace();
                    localiseErrorAndAddToGlobalErrors("System error");
                     
                    return getDDForwardResolution("/sales/additional_sim_purchase.jsp");  
             } 
             
                setTcraApproved(true);
                
                HttpSession session = getContext().getRequest().getSession();               
                setSale((Sale) session.getAttribute("sessionSale"));
                session.removeAttribute("sessionSale");
                
                return generateQuote();
            
    }
    
    public void storeAdditionalSimData(String tcraInput,String tcraResponse, String customerNIN, String customerMSISDN, String customerICCID, String tcraResponseDesc)
    {
        PreparedStatement ps = null;
        Connection conn = null;
        try {
                conn = JPAUtils.getNonJTAConnection("jdbc/SmileDB");
                conn.setAutoCommit(false);
                String query="insert into additional_sim_webservice_info (REQUEST_INPUT, RETURNED_RESPONSE, CUSTOMER_NIN, CUSTOMER_MSISDN,CUSTOMER_ICCID,TCRA_RESPONSE) values (?,?,?,?,?,?)";
                ps = conn.prepareStatement(query);              
                ps.setString(1, tcraInput);
                ps.setString(2, tcraResponse);
                ps.setString(3, customerNIN);
                ps.setString(4, customerMSISDN);
                ps.setString(5, customerICCID);
                ps.setString(6, tcraResponseDesc);                
                
                log.warn("SQL: [{}]", ps.toString());
                int del = ps.executeUpdate();
                
                log.warn("Returned: [{}] ", del);
                ps.close();
                conn.commit();
                conn.close();
            } catch (Exception ex) {
                log.error("Error occured creating tcra data: "+ex);
            }
            finally
                {
                    if (ps != null) 
                    {
                        try {
                                ps.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the prepared statement " + ex);
                        }
                    }
                    if (conn != null) 
                    {
                        try {
                                conn.close();
                        } catch (SQLException ex) {
                                log.error("Error closing the connection " + ex);
                        }
                    }
                }
        
    }
    
    public Resolution showCustomerKYCDetails() {
        return getDDForwardResolution("/customer/retrive_customer_details_from_kyc.jsp");
    }
    
    public Resolution showVerifyAddAdditionalSim() {
        
        log.debug("IN showVerifyAddAdditionalSim with sale for [{}]", getSale().getSaleLines().get(0).getInventoryItem().getDescription());
        
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        
        NumbersQuery q = new NumbersQuery();
        q.setResultLimit(5);
        q.setPriceLimitCents(0);
        // Dont list any numbers owned by somebody
        q.setOwnedByCustomerProfileId(0);
        q.setOwnedByOrganisationId(0);
        NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(q);
        List<String> ret = new ArrayList();
        
        for (com.smilecoms.commons.sca.Number num : list.getNumbers()) {
            ret.add(Utils.getFriendlyPhoneNumber(num.getIMPU()));
        }
               
        setAvailableNumbers(ret);
        
        return getDDForwardResolution("/sales/additional_sim_purchase.jsp");
    }

    private static ServiceInstance getSIMServiceInstanceForPI(ProductInstance pi) {
        List<ServiceInstance> siList = getServiceInstanceListWithAVPsForPI(pi);
        for (ServiceInstance si : siList) {
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("ProvisioningFlow") && avp.getValue().equals("lte")) {
                    return si;
                }
            }
        }
        return null;
    }
    
    private static List<ServiceInstance> getServiceInstanceListWithAVPsForPI(ProductInstance pi) {
        List<ServiceInstance> siList = new ArrayList();
        for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
            siList.add(m.getServiceInstance());
        }
        return siList;
    }
    
    private Set<String> provisioningDataOptions;

    public Set<java.lang.String> getProvisioningDataOptions() {
        return provisioningDataOptions;
    }

    private final Map<String, String> extraInfoProvisioningDataOptions = new HashMap<>();

    public Map<String, String> getExtraInfoProvisioningDataOptions() {
        return extraInfoProvisioningDataOptions;
    }

    private java.lang.String normaliseProvisioningData(java.lang.String provisioningData) {
        String ret = "";
        if (provisioningData == null) {
            return ret;
        }
        String[] bits = provisioningData.split(",");
        for (String bit : bits) {
            String[] avp = bit.split(":");
            if (avp.length == 2) {
                String attribute = avp[0].trim();
                String val = avp[1].trim();
                switch (attribute) {
                    case "PIID":
                        ret = ret + "\r\nProductInstanceId=" + val;
                        break;
                    case "Acc":
                        ret = ret + "\r\nToAccountId=" + val;
                        break;
                    case "SIM Provisioned for line":
                        ret = ret + "\r\nLineNumber=" + val;
                        break;
                }
            }
        }
        return ret;
    }

    public Resolution makeSaleFromQuote() {
        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
        if (sl.getNumberOfSales() == 0) {
            localiseErrorAndAddToGlobalErrors("sale.not.found");
        } else {
            setSale(sl.getSales().get(0));
        }
        if (!getSale().getStatus().equals("QT") && !getSale().getStatus().equals("SP")) {
            localiseErrorAndAddToGlobalErrors("not.a.quote");
            return showSale();
        }
        return generateQuote();
    }
    private Set<Long> possibleAccountIdsForTransfer;

    public Set<Long> getPossibleAccountIdsForTransfer() {
        return possibleAccountIdsForTransfer;
    }
    private Map<Long, String> accountTypes;

    public Map<Long, String> getAccountTypes() {
        return accountTypes;
    }
    private Map<Long, Set<String>> accountChannels;

    public Map<Long, Set<String>> getAccountChannels() {
        return accountChannels;
    }

    public Resolution addNewCustomer() {
        return getDDForwardResolution(CustomerActionBean.class, "showAddCustomerWizard");
    }

    public Resolution putSaleBackInGatewayQueue() {
        getSaleModificationData().setNewPaymentStatus("PP");
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("sale.resubmitted.successfully");
        return showSale();
    }

    public Resolution putSaleBackInX3Queue() {
        checkPermissions(Permissions.RESEND_SALE_TO_X3);
        if (notConfirmed()) {
            return confirm();
        }
        getSaleModificationData().setNewPaymentStatus("X3SL");
        try {
            SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        } catch (SCABusinessError e) {
            showSale();
            throw e;
        }
        setPageMessage("sale.resent.to.x3.successfully");
        return showSale();
    }

    public Resolution putCashInBackInX3Queue() {
        checkPermissions(Permissions.RESEND_CASHIN_TO_X3);
        if (notConfirmed()) {
            return confirm();
        }
        getSaleModificationData().setNewPaymentStatus("X3CI");
        try {
            SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        } catch (SCABusinessError e) {
            showSale();
            throw e;
        }
        setPageMessage("cashin.resent.to.x3.successfully");
        return showSale();
    }

    @DontValidate
    public Resolution getItemsForSaleJSON() {
        try {
            getInventoryQuery().setSalesPersonCustomerId(getUserCustomerIdFromSession());
            getInventoryQuery().setWarehouseId(getWarehouseIdFromSession());
            log.debug("Getting inventory JSON for string [{}] and currency [{}]", getInventoryQuery().getStringMatch(), getInventoryQuery().getCurrency());
            setInventoryList(SCAWrapper.getUserSpecificInstance().getInventory(getInventoryQuery()));
            List<InventoryItem> toUse = new ArrayList<>();

            log.debug("Inventory list size for search string [{}] is [{}]", getInventoryQuery().getStringMatch(), getInventoryList().getInventoryItems().size());

            for (InventoryItem item : getInventoryList().getInventoryItems()) {
                if (item.getItemNumber().startsWith("BUN") && !isAllowedInThisLocation(item.getItemNumber())) {
                    log.debug("[{}] is not allowed for sale here", item.getItemNumber());
                    continue;
                }
                // Do round half even
                item.setPriceInCentsExcl(Utils.round(item.getPriceInCentsExcl(), 0));
                item.setPriceInCentsIncl(Utils.round(item.getPriceInCentsIncl(), 0));
                item.setDescription(item.getDescription() + " [" + item.getItemNumber() + "][" + item.getSerialNumber() + "]");
                toUse.add(item);
            }
            getInventoryList().getInventoryItems().clear();
            getInventoryList().getInventoryItems().addAll(toUse);
            getInventoryList().setNumberOfInventoryItems(toUse.size());
            Resolution res = getJSONResolution(getInventoryList());
            return res;
        } catch (Exception e) {
            log.debug("Error getting inventory", e);
            Resolution res = new StreamingResolution("text", new StringReader(e.toString()));
            this.getResponse().setStatus(500);
            return res;
        }
    }

    @DontValidate
    public Resolution getUpSizeBundlesJSON() {
        try {
            // getInventoryQuery().setSalesPersonCustomerId(getUserCustomerIdFromSession());
            // getInventoryQuery().setWarehouseId(getWarehouseIdFromSession());
            log.debug("Getting Upsize bundle JSON for bundle [{}]", getInventoryQuery().getStringMatch());
            List<InventoryItem> toUse = new ArrayList<>();

            setInventoryList(new InventoryList());

            if (BaseUtils.getBooleanProperty("env.sep.upsize.enabled", false)) {

                UpSizeInventoryQuery uspQ = new UpSizeInventoryQuery();
                uspQ.setMainBundleItemNumber(getInventoryQuery().getStringMatch());
                uspQ.setSalesPersonCustomerId(getUserCustomerIdFromSession());
                uspQ.setWarehouseId(getWarehouseIdFromSession());

                uspQ.setRecipientAccountId(getInventoryQuery().getRecipientAccountId());
                uspQ.setCurrency((getInventoryQuery().getCurrency() == null ? " " : getInventoryQuery().getCurrency()));
                uspQ.setRecipientOrganisationId(getInventoryQuery().getRecipientOrganisationId());
                uspQ.setRecipientCustomerId(getInventoryQuery().getRecipientCustomerId());

                InventoryList tmpUse = SCAWrapper.getUserSpecificInstance().getUpSizeInventory(uspQ);
                for (InventoryItem item : tmpUse.getInventoryItems()) {
                    if (item.getItemNumber().startsWith("BUN") && !isAllowedInThisLocation(item.getItemNumber())) {
                        log.debug("[{}] is not allowed for sale here", item.getItemNumber());
                        continue;
                    }
                    // Do round half even
                    item.setPriceInCentsExcl(Utils.round(item.getPriceInCentsExcl(), 0));
                    item.setPriceInCentsIncl(Utils.round(item.getPriceInCentsIncl(), 0));
                    item.setDescription(item.getDescription() + " [" + item.getItemNumber() + "][" + item.getSerialNumber() + "]");
                    toUse.add(item);
                }
            }

            getInventoryList().getInventoryItems().addAll(toUse);
            getInventoryList().setNumberOfInventoryItems(toUse.size());
            Resolution res = getJSONResolution(getInventoryList());

            log.debug("Total number of UpSize items = " + getInventoryList().getInventoryItems().size());

            return res;
        } catch (Exception e) {
            log.debug("Error getting upsize inventory", e);
            Resolution res = new StreamingResolution("text", new StringReader("Error while trying to pull the UpSize bundles" + e.toString()));
            this.getResponse().setStatus(500);
            return res;
        }
    }

    private boolean isAllowedInThisLocation(String itemNumber) {
        UnitCreditSpecification ucs;
        try {
            ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecificationByItemNumber(itemNumber);
        } catch (Exception e) {
            log.warn("Error checking is bundle is allowed. Allowing anyway", e);
            return true;
        }
        String allowedTowns = Utils.getValueFromCRDelimitedAVPString(ucs.getConfiguration(), "AllowedTowns");
        if (allowedTowns == null) {
            log.debug("UCS [{}] is allowed in any town", ucs.getUnitCreditSpecificationId());
            return true;
        }
        log.debug("UCS [{}] is allowed in [{}]", ucs.getUnitCreditSpecificationId(), allowedTowns);
        Set<String> allowedTownSet = Utils.getSetFromCommaDelimitedString(allowedTowns.toLowerCase());

        String town = getUserTownBasedOnIPAddress();
        log.debug("Logged in customers town is [{}]", town);
        return allowedTownSet.contains(town);
    }

    HashMap<Integer, String> salesPersonsFreelancers = new HashMap<Integer, String>();

    public HashMap<Integer, String> getSalesPersonsFreelancers() {
        return salesPersonsFreelancers;
    }

    public void setSalesPersonsFreelancers(HashMap<Integer, String> freelancers) {
        this.salesPersonsFreelancers = freelancers;
    }

    public Resolution collectItemsInSale() {
        // log.debug("Organisation Id is [{}] and Customer Id is [{}] and Account Id is [{}]", new Object[]{getSale().getRecipientOrganisationId(), getSale().getRecipientCustomerId(), getSale().getRecipientAccountId()});
        // Reset warehouse Id
        if (!BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {
            clearValidationErrors(); // In case there is no sales id and stripes validation has errors
        }

        setWarehouseIdInSession();
        getSale().setWarehouseId(getWarehouseIdFromSession());
        if (BaseUtils.getBooleanProperty("env.pos.icps.barred.from.buying.directly", false)) {
            if (isICP(getSale().getRecipientOrganisationId())) {
                localiseErrorAndAddToGlobalErrors("icp.barred.from.buying.directly", " Organisation Id:" + getSale().getRecipientOrganisationId());
                return getDDForwardResolution("/sales/make_sale_get_account_org.jsp");
            }
        }

        if (getSale().getTenderedCurrency() == null || getSale().getTenderedCurrency().isEmpty()) {
            getSale().setTenderedCurrency(BaseUtils.getProperty("env.currency.official.symbol"));
        }

        if (BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {
            // setStickyNoteList(new StickyNoteList());
            setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
            getStickyNoteEntityIdentifier().setEntityId(getUserCustomerIdFromSession());
            getStickyNoteEntityIdentifier().setEntityType("Customer");
            StickyNoteList snl = SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier());
            // StickyNoteTypeList sntl = SCAWrapper.getUserSpecificInstance().getStickyNoteTypeList(makeSCAString(getStickyNoteEntityIdentifier().getEntityType()));
            salesPersonsFreelancers.clear();

            log.debug("Found [{}] freelancers for sales person [{}].", snl.getNumberOfNotes(), getUserCustomerIdFromSession());

            String freelancerName;
            String freelancerId;
            for (StickyNote sn : snl.getStickyNotes()) {

                setStickyNote(SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(sn.getNoteId())));

                CustomerQuery cq = new CustomerQuery();

                log.error("Sticky note type name [{}], number of fields [{}]", sn.getTypeName(), sn.getNumberOfFields());

                if (sn.getTypeName().equals("FreelanceSalesPerson")) {

                    if (salesPersonsFreelancers.isEmpty()) {
                        salesPersonsFreelancers.put(0, "N/A");
                    }

                    for (StickyNoteField snf : getStickyNote().getFields()) {
                        if (snf.getFieldName().equals("FreelancerCustomerProfileId")) {
                            freelancerId = snf.getFieldData();
                            //Pull the customer profile of the freelancer
                            cq.setCustomerId(Integer.parseInt(freelancerId));
                            cq.setResultLimit(1);
                            cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                            Customer freelancer = SCAWrapper.getAdminInstance().getCustomer(cq);

                            if (freelancer != null) {
                                boolean isStaff = false;
                                for (CustomerRole role : freelancer.getCustomerRoles()) {
                                    if (role.getOrganisationId() == 1) { // Is a staff
                                        isStaff = true;
                                    }
                                }

                                if (!isStaff) { // Only add non-staff (staff cannot be freelancers)
                                    freelancerName = freelancer.getFirstName() + " " + freelancer.getLastName();
                                    salesPersonsFreelancers.put(freelancer.getCustomerId(), freelancerName);
                                }
                            }
                        }
                    }
                }
            }
        }

        return getDDForwardResolution("/sales/make_sale_get_items.jsp");
    }

    public Resolution collectItemsInUSDSale() {
        checkPermissions(Permissions.MAKE_SALE_USD);
        getSale().setTenderedCurrency("USD");
        return collectItemsInSale();
    }

    public Resolution collectItemsInEURSale() {
        checkPermissions(Permissions.MAKE_SALE_EUR);
        getSale().setTenderedCurrency("EUR");
        return collectItemsInSale();
    }

    @DontValidate()
    public Resolution processSaleCash() {
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleCardPayment() {
        getSale().setTillId(getParameter("cardPaymentBank"));
        getSale().setPaymentTransactionData(getParameter("cardReferenceNumber"));
        if (getSale().getPaymentTransactionData() == null
                || getSale().getPaymentTransactionData().isEmpty()
                || !Utils.matchesWithPatternCache(getSale().getPaymentTransactionData(), BaseUtils.getProperty("env.pos.cardpayment.transactionid.regex", "^[0-9]{12,12}$"))) {
            clearValidationErrors(); // In case there is no sales id and stripes validation has errors
            localiseErrorAndAddToGlobalErrors("invalid.card.payment.transaction.id");
            return generateQuote();
        }
        if (getSale().getTillId().trim().isEmpty()) {
            clearValidationErrors(); // In case there is no sales id and stripes validation has errors
            localiseErrorAndAddToGlobalErrors("invalid.card.payment.bank");
            return generateQuote();
        }
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleCardIntegration() {
        getSale().setTillId(getParameter("cardIntegrationBank"));
        getSale().setPaymentGatewayCode(getParameter("cardIntegrationBank"));
        if (getSale().getTillId().trim().isEmpty()) {
            clearValidationErrors(); // In case there is no sales id and stripes validation has errors
            localiseErrorAndAddToGlobalErrors("invalid.card.integration.bank");
            return generateQuote();
        }
        return processSale();
    }

    @DontValidate
    public Resolution getSaleStatus() {
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSale().getSaleId());
        getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE);
        Sale sale = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery()).getSales().get(0);
        InputStream stream = null;
        try {
            stream = Utils.stringToStream(sale.getStatus());
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return new StreamingResolution("text/html", stream);
    }

    @DontValidate()
    public Resolution processSaleBankTransfer() {
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleDeliveryService() {
        checkPermissions(Permissions.MAKE_SALE_CREDIT_ACCOUNT);
        if (getSale().getPurchaseOrderData() == null || getSale().getPurchaseOrderData().isEmpty()) {
            clearValidationErrors(); // In case there is no sales id and stripes validation has errors
            localiseErrorAndAddToGlobalErrors("invalid.delivery.service.order.number");
            return generateQuote();
        }
        return processSale();
    }

    @DontValidate()
    public Resolution processSalePaymentGateway() {
        checkPermissions(Permissions.MAKE_SALE_PAYMENT_GATEWAY);
        if (getSale().getPaymentGatewayCode() == null || getSale().getPaymentGatewayCode().isEmpty()) {
            clearValidationErrors(); // In case there is no sales id and stripes validation has errors
            localiseErrorAndAddToGlobalErrors("invalid.payment.gateway.code");
            return generateQuote();
        }
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleCreditAccount() {
        checkPermissions(Permissions.MAKE_SALE_CREDIT_ACCOUNT);
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleClearingBureau() {
        checkPermissions(Permissions.MAKE_SALE_CLEARING_BUREAU);
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleLoan() {
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleStaff() {
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleQuote() {
        return processSale();
    }

    private boolean isDirectAirtimeAccount(long accountId) {
        if (accountId <= 0) {
            return false;
        }
        ServiceInstanceQuery siquery = new ServiceInstanceQuery();
        siquery.setAccountId(accountId);
        siquery.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
        ServiceInstance si = SCAWrapper.getUserSpecificInstance().getServiceInstance(siquery);
        log.debug("service specification id for [{}] is [{}]", accountId, si.getServiceSpecificationId());
        return 1007 == si.getServiceSpecificationId();
    }

    /**
     * make sale through direct airtime account
     *
     * @return
     */
    @DontValidate()
    public Resolution processDirectAirtimePayment() {
        boolean isDirectAirtimeAccountSupported = BaseUtils.getBooleanProperty("env.direct.airtime.account.sales", false);
        if (!isDirectAirtimeAccountSupported) {
            localiseErrorAndAddToGlobalErrors("invalid.payment.method.operation");
            return generateQuote();
        }
        if (!isDirectAirtimeAccount(getSale().getSalesPersonAccountId())) {
            localiseErrorAndAddToGlobalErrors("invalid.sales.account.id");
            return generateQuote();
        }
        checkPermissions(Permissions.MAKE_DIRECT_AIRTIME_SALE);
        log.debug("Sale for direct airtime account is approved");
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleAirtime() {
        if (!getSale().getPaymentMethod().equals("Airtime")) {
            localiseErrorAndAddToGlobalErrors("invalid.payment.method.operation");
            return generateQuote();
        }

        boolean foundInvalidItemForPaymentMethod = false;

        for (SaleLine s : getSale().getSaleLines()) {
            if (!isItemNumberUnitCredit(s.getInventoryItem().getItemNumber())) {
                foundInvalidItemForPaymentMethod = true;
            }
        }

        if (foundInvalidItemForPaymentMethod) {
            localiseErrorAndAddToGlobalErrors("invalid.item.for.payment.method");
            return generateQuote();
        }

        if (getSale().getRecipientAccountId() < 1100000000) {
            localiseErrorAndAddToGlobalErrors("invalid.pay.by.airtime.account");
            return generateQuote();
        }
        getSale().setChannel(BaseUtils.getProperty("env.unitcredit.provisioning.x3.channel"));

        getSale().setSalesPersonAccountId(getSale().getRecipientAccountId());
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleCreditNote() {
        getSale().setPaymentTransactionData(getParameter("creditNoteNumber"));
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleCreditFacility() {
        getSale().setPaymentTransactionData(getParameter("creditFacilityReference"));
        getSale().setExtTxId(getParameter("creditFacilityReference"));
        boolean isValidFacilitator = true;
        try {
            getSale().setCreditAccountNumber(BaseUtils.getSubProperty("env.pos.payment.methods.credit.facility.types", getParameter("creditFacilitator")));
        } catch (Exception ex) {
            isValidFacilitator = false;
        }
        if (!isValidFacilitator) {
            localiseErrorAndAddToGlobalErrors("invalid.credit.facilitator.credit.account");
            return generateQuote();
        }
        return processSale();
    }

    @DontValidate()
    public Resolution processSaleContract() {
        checkPermissions(Permissions.MAKE_SALE_CONTRACT);
        String hod = getParameter("hod");
        String dow = getParameter("dow");
        String dom = getParameter("dom");
        String daysGap = getParameter("daysgap");
        String ab = getParameter("ab");

        String schedule = "";
        if (!hod.equals("Any")) {
            schedule += "HOD=" + hod + "\r\n";
        }

        switch (dow) {
            case "Sun":
                schedule += "DOW=" + 1 + "\r\n";
                break;
            case "Mon":
                schedule += "DOW=" + 2 + "\r\n";
                break;
            case "Tue":
                schedule += "DOW=" + 3 + "\r\n";
                break;
            case "Wed":
                schedule += "DOW=" + 4 + "\r\n";
                break;
            case "Thu":
                schedule += "DOW=" + 5 + "\r\n";
                break;
            case "Fri":
                schedule += "DOW=" + 6 + "\r\n";
                break;
            case "Sat":
                schedule += "DOW=" + 7 + "\r\n";
                break;
        }

        if (!dom.equals("Any")) {
            schedule += "DOM=" + dom + "\r\n";
        }

        if (!ab.isEmpty() && Utils.isNumeric(ab)) {
            schedule += "AB=" + Long.parseLong(ab) * 100 + "\r\n";
        }

        if (!daysGap.isEmpty() && Utils.isNumeric(ab)) {
            schedule += "DAYSGAP=" + Long.parseLong(daysGap) + "\r\n";
        }

        getSale().setFulfilmentScheduleInfo(schedule);

        return processSale();
    }

    @DontValidate()
    public Resolution backToGetItems() {
        return collectItemsInSale();
    }

    @DontValidate()
    public Resolution processSale() {
        log.debug("---------- started processSale------------");
        log.debug("sale data is :" + getSale().toString());
        checkPermissions(Permissions.MAKE_SALE);
        checkCSRF();  
        boolean isSimSale=false;
        List<SaleLine> additionalSaleLinesOverall = new ArrayList<>();
        clearValidationErrors(); // In case there is no sales id and stripes validation has errors
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getSale().getRecipientCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        if (BaseUtils.getBooleanProperty("env.customer.sim.limit.enabled", false)) {
            int count = 0;
            if (getCustomer().getKYCStatus() != null) {
                if (!getCustomer().getKYCStatus().equalsIgnoreCase("O")) {
                    if (getCustomer().getCustomerRoles().isEmpty()) {
                        for (SaleLine s : getSale().getSaleLines()) {
                            for (SaleLine sl : s.getSubSaleLines()) {
                                if (sl.getInventoryItem().getItemNumber().startsWith("SIM") && !sl.getInventoryItem().getSerialNumber().isEmpty()) {
                                    isSimSale=true;
                                    for (ProductInstance pi : getCustomer().getProductInstances()) {
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
                                        localiseErrorAndAddToGlobalErrors("sim.limit.allowed.to.provision.exceeded", " " + BaseUtils.getIntProperty("sim.limit.to.no", 10) + " CID" + getCustomer().getCustomerId());
                                        return generateQuote();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        //In Nigeria, we need to check if client does not have other profiles. Allowed SIMs in NG is for ALL Profiles belonging to the same customer
        if(BaseUtils.getProperty("env.country.name").equalsIgnoreCase("Nigeria") && isSimSale) {        
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
                            localiseErrorAndAddToGlobalErrors("error", "Cannot continue with provisioning.  Client already has " + BaseUtils.getIntProperty("sim.limit.to.no", 4) + " or more accounts with active SIM.");
                            return getDDForwardResolution("/customer/view_customer.jsp");
                        }
                    }
                }
            }

        if (!getSale().getPromotionCode().isEmpty() && (getSale().getSCAContext().getComment() == null || getSale().getSCAContext().getComment().isEmpty())) {
            localiseErrorAndAddToGlobalErrors("provide.comment");
            return generateQuote();
        }

        // check permission for sale   to direct airtime accounts 
        if (BaseUtils.getBooleanProperty("env.direct.airtime.account.sales", false)
                && isDirectAirtimeAccount(getSale().getRecipientAccountId())) {
            log.debug("this sale receipient account is an direct airtime account. checking permission");
            checkPermissions(Permissions.SALE_TO_DIRECT_AIRTIME_ACCOUNT);
        }

        if (!getSale().getCreditAccountNumber().isEmpty() && !getSale().getPaymentMethod().equals("Credit Facility")) {
            checkPermissions(Permissions.OVERRIDE_CREDIT_ACCOUNT_NUMBER);
        } else if (getSale().getRecipientOrganisationId() > 0) {
            OrganisationQuery oq = new OrganisationQuery();
            oq.setOrganisationId(getSale().getRecipientOrganisationId());
            oq.setResultLimit(1);
            oq.setVerbosity(StOrganisationLookupVerbosity.MAIN);
            Organisation org = SCAWrapper.getUserSpecificInstance().getOrganisation(oq);
            getSale().setCreditAccountNumber(org.getCreditAccountNumber());
            log.debug("Orgainsations credit account number is [{}]", getSale().getCreditAccountNumber());
        }

        long freelanceCustomerId = getSale().getFreelancerCustomerId();

        boolean isP2PInvoicingPeriodOn = false;
        boolean mustSelectTargetAccount = false;

        if (!getP2pInvoicingDataOptions().isEmpty()) {
            isP2PInvoicingPeriodOn = true;
            for (String dd : getP2pInvoicingDataOptions()) {
                if (dd.startsWith("P2PInvoicingPeriod=")) {
                    String extraInfo = getSale().getExtraInfo() == null ? "" : getSale().getExtraInfo();
                    getSale().setExtraInfo(extraInfo.isEmpty() ? dd : extraInfo + "\r\n" + dd);
                    break;
                }
            }
        }

        // Reformat the provisioning data into the required format
        for (SaleLine s : getSale().getSaleLines()) {
            s.setProvisioningData(normaliseProvisioningData(s.getProvisioningData()));

            if (s.getInventoryItem().getItemNumber().startsWith("BUN") && !(BaseUtils.getProperty("env.bm.unit.credit.info." + s.getInventoryItem().getItemNumber(), "null").equals("null"))) {
                StringBuilder sb = new StringBuilder(s.getProvisioningData());
                Set<String> itemNumberAsMapKey = extraInfoProvisioningDataOptions.keySet();
                for (String itemNum : itemNumberAsMapKey) {
                    if (itemNum.equals(s.getInventoryItem().getItemNumber())) {
                        sb.append("\r\n").append((String) extraInfoProvisioningDataOptions.get(itemNum));
                        break;
                    }
                }
                s.setProvisioningData(sb.toString());
            }

            /* Allow non-stock item sale with no recipient account selected */
            if (isItemNumberUnitCredit(s.getInventoryItem().getItemNumber()) || isAirtime(s.getInventoryItem().getSerialNumber())) {
                mustSelectTargetAccount = true;
            }

            /* P2P Stuff here*/
            if (s.getInventoryItem().getItemNumber().startsWith("BENTMW") && isP2PInvoicingPeriodOn) {
                for (String option : getP2pInvoicingDataOptions()) {
                    String provData = s.getProvisioningData() == null ? "" : s.getProvisioningData();
                    s.setProvisioningData(provData.isEmpty() ? option : provData + "\r\n" + option);
                }
            }

            /* BOXSIZE: Do box handling stuff here...*/
            if (s.getInventoryItem().getBoxSize() > 1) {
                String provData = s.getProvisioningData() == null ? "" : s.getProvisioningData();
                String option = "BoxSize=" + s.getInventoryItem().getBoxSize();
                s.setProvisioningData(provData.isEmpty() ? option : provData + "\r\n" + option);
            }

            /* ENTERPRICE INTERNET ACCESS: HBT-7184-Create Enterprise Internet Access as a Product */
            if (s.getInventoryItem().getItemNumber().startsWith("BENTDI")) {
                int maxQuantity = BaseUtils.getIntProperty("env.pos.dedicated.internet.maxquantity", 10000000);
                //int MinQuantity=BaseUtils.getIntProperty("env.pos.dedicated.internet.minquantity",100000);
                if (s.getQuantity() > maxQuantity) {
                    log.debug("Maximum quantity is [{}] " + maxQuantity);
                    localiseErrorAndAddToGlobalErrors("dedicated.internet.quantity");
                    return generateQuote();
                }
                String provDedicatedInternet = s.getProvisioningData() == null ? "" : s.getProvisioningData();
                s.setProvisioningData(provDedicatedInternet.isEmpty() ? "ProvDedicatedInternetBundle=1" : provDedicatedInternet + "\r\n" + "ProvDedicatedInternetBundle=1");
            }
        }

        if (getSale().getRecipientAccountId() <= 0 && mustSelectTargetAccount) {
            checkPermissions(Permissions.ALLOW_NONSTOCK_ITEM_SALE_WITHNO_RECIPIENT_ACCOUNT);
        }

        for (SaleLine s : getSale().getSaleLines()) {
            log.debug("Line [{}] Serial [{}]", s.getLineNumber(), s.getInventoryItem().getSerialNumber());
            if (!s.getSubSaleLines().isEmpty() && s.getQuantity() > 1) {
                log.debug("This sale line is for a kit with a quantity greater than 1. It will be broken up into many kits");
                List<SaleLine> additionalSaleLinesOfThisSameLine = new ArrayList<>();
                for (int i = 1; i < s.getQuantity(); i++) {
                    // Looping through each new line we need to add
                    SaleLine newLine = new SaleLine();
                    newLine.setQuantity(1);
                    newLine.setInventoryItem(s.getInventoryItem());
                    newLine.setProvisioningData(s.getProvisioningData());
                    additionalSaleLinesOfThisSameLine.add(newLine);
                }
                int i = 1;
                for (SaleLine newLineToPopulate : additionalSaleLinesOfThisSameLine) {
                    // Looping through each added line
                    for (SaleLine subLine : s.getSubSaleLines()) {
                        // Looping through each sub line to be added
                        String serial;
                        String[] serials = getSerialsArray(subLine.getInventoryItem().getSerialNumber());
                        log.debug("Number of serial numbers in [{}] is [{}]", subLine.getInventoryItem().getSerialNumber(), serials.length);
                        if (serials.length == 1) {
                            serial = serials[0];
                        } else if (serials.length == s.getQuantity()) {
                            serial = serials[i].trim();
                        } else {
                            serial = "";
                        }
                        log.debug("Setting serial number to [{}]", serial);
                        SaleLine newSubLine = new SaleLine();
                        newSubLine.setInventoryItem(new InventoryItem());
                        newSubLine.getInventoryItem().setItemNumber(subLine.getInventoryItem().getItemNumber());
                        newSubLine.getInventoryItem().setDescription(subLine.getInventoryItem().getDescription());
                        newSubLine.getInventoryItem().setSerialNumber(serial);
                        newSubLine.setQuantity(1);
                        newLineToPopulate.getSubSaleLines().add(newSubLine);
                    }
                    i++;
                    additionalSaleLinesOverall.add(newLineToPopulate);
                }
                s.setQuantity(1);
                // Change the serial number to only the first one on the fields which had many pasted
                for (SaleLine subLine : s.getSubSaleLines()) {
                    subLine.getInventoryItem().setSerialNumber(getSerialsArray(subLine.getInventoryItem().getSerialNumber())[0]);
                }
            } // end if was kitted with quantity > 1

            String[] serials = getSerialsArray(s.getInventoryItem().getSerialNumber());
            if (s.getSubSaleLines().isEmpty() && s.getQuantity() > 1 && s.getQuantity() == serials.length) {
                log.debug("This is a sale line with no sublines and with quantity greater than one and a number of serials [{}] pasted in the serial number field", serials.length);
                // add a row to additionalSaleLinesOverall for each serial
                boolean firstOne = true;
                for (String serial : serials) {
                    if (firstOne) {
                        firstOne = false;
                        // Done add a new row for the first one
                        continue;
                    }
                    log.debug("Creating a new sale line for serial number [{}]", serial);
                    SaleLine newLine = new SaleLine();
                    newLine.setQuantity(1);
                    newLine.setInventoryItem(new InventoryItem());
                    newLine.getInventoryItem().setItemNumber(s.getInventoryItem().getItemNumber());
                    newLine.getInventoryItem().setDescription(s.getInventoryItem().getDescription());
                    newLine.getInventoryItem().setSerialNumber(serial);
                    newLine.setProvisioningData(s.getProvisioningData());
                    additionalSaleLinesOverall.add(newLine);
                }
                // Set the existing lines serial to the first one in the list
                s.setQuantity(1);
                s.getInventoryItem().setSerialNumber(serials[0]);
            }

        } // end looping through sale lines

        int nextLineNumber = getSale().getSaleLines().size() + 1;
        for (SaleLine lineToAdd : additionalSaleLinesOverall) {
            log.debug("Sale currently has [{}] lines in it. Next line number will be [{}]", getSale().getSaleLines().size(), nextLineNumber);
            lineToAdd.setLineNumber(nextLineNumber);
            getSale().getSaleLines().add(lineToAdd);
            nextLineNumber++;
        }

        if (log.isDebugEnabled()) {
            log.debug("Sale line details...");
            for (SaleLine s : getSale().getSaleLines()) {
                log.debug("Line [{}] Serial [{}]", s.getLineNumber(), s.getInventoryItem().getSerialNumber());
                for (SaleLine ss : s.getSubSaleLines()) {
                    log.debug("SubLine Serial [{}] and Item [{}]", ss.getInventoryItem().getSerialNumber(), ss.getInventoryItem().getItemNumber());
                }
            }
        }

        /*CHECK SD CONSTRAINTS -- HBT-9230*/
        log.debug("Going to check SD constraints");
        checkSuperDealerConstraints(getSale());

        clearValidationErrors(); // In case there is no sales id and stripes validation has errors

        if (getSale().getChannel() == null || getSale().getChannel().isEmpty()) {
            getSale().setChannel(getParameter("channel_" + getSale().getSalesPersonAccountId()));
        }
        if (getSale().getChannel() == null || getSale().getChannel().isEmpty()) {
            throw new RuntimeException("Channel cannot be empty");
        }
        if (!getSale().getPaymentMethod().equals("Contract")) {
            getSale().setContractId(0);
        }

        getSale().setAmountTenderedCents(getSale().getAmountTenderedCents() * 100);
        if (getSale().getSalesPersonCustomerId() == 0) {
            getSale().setSalesPersonCustomerId(getUserCustomerIdFromSession());
        } else if (getSale().getSalesPersonCustomerId() != getUserCustomerIdFromSession()) {

            Customer attributedTo = SCAWrapper.getUserSpecificInstance().getCustomer(getSale().getSalesPersonCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
            if (attributedTo.getWarehouseId() == null || attributedTo.getWarehouseId().isEmpty()) {
                throw new RuntimeException("Sale must be attributed to a salesperson");
            }

            checkPermissions(Permissions.ATTRIBUTE_SALE);
        }
        getSale().setWarehouseId(getWarehouseIdFromSession());
        getSale().setRecipientPhoneNumber("");
        getSale().setSaleLocation("");
        getSale().setRecipientName("");
        if (getSale().getCreditAccountNumber() == null) {
            getSale().setCreditAccountNumber("");
        }

        if (getSale().getPaymentTransactionData() == null) {
            getSale().setPaymentTransactionData("");
        }
        try {
            setSale(SCAWrapper.getUserSpecificInstance().processSale(getSale()));
        } catch (SCABusinessError sbe) {
            log.debug("error processSale from SEP...");
            generateQuote();
            throw sbe;
        }

        //Is sale has a freelance sales person, add the corresponding sticky note.
        log.debug("Sale [{}] is a freelance sale and freelance sale person id is [{}].", getSale().getSaleId(), freelanceCustomerId);
        if (BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {
            if (freelanceCustomerId > 0) {
                try {
                    NewStickyNote freelanceSale = new NewStickyNote();
                    freelanceSale.setCreatedBy(getUser());
                    freelanceSale.setEntityId(getSale().getSaleId().longValue());
                    freelanceSale.setEntityType("Sale");
                    freelanceSale.setTypeName("FreelanceSale");
                    NewStickyNoteField newStickyNoteField = new NewStickyNoteField();
                    newStickyNoteField.setFieldData(String.valueOf(freelanceCustomerId));
                    newStickyNoteField.setFieldName("FreelancerCustomerProfileId");
                    newStickyNoteField.setFieldType("L");
                    newStickyNoteField.setDocumentData("");
                    freelanceSale.getFields().add(newStickyNoteField);
                    SCAWrapper.getAdminInstance().addStickyNote(freelanceSale);
                } catch (Exception ex) {
                    log.error("Error while trying to create freelance sticky note for sale - [{}], error [{}].", getSale().getSaleId(), ex);
                    throw ex;
                }
            }
        }

        if (getSale().getRecipientCustomerId() > 0) {
            CustomerQuery q = new CustomerQuery();
            q.setCustomerId(getSale().getRecipientCustomerId());
            q.setResultLimit(1);
            q.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(q));
        }
        if (getSale().getRecipientOrganisationId() > 0) {
            OrganisationQuery oq = new OrganisationQuery();
            oq.setOrganisationId(getSale().getRecipientOrganisationId());
            oq.setResultLimit(1);
            oq.setVerbosity(StOrganisationLookupVerbosity.MAIN);
            setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(oq));
        }
        setPageMessage("sale.processed.successfully");
        return showSale();
    }
    
    public Resolution collectCustomerRoleAndAccountDataForSale() {
        setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getCustomer().getCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
        
        if(BaseUtils.getProperty("env.country.name","").equalsIgnoreCase("Nigeria") && BaseUtils.getBooleanProperty("env.buyer.seller.op.enabled",false)) {
           //In NG there is a Buyer/Seller link for SD and ICPs          
            List<String> custRoles = getCustomer().getSecurityGroups();
        
                for (String icpRole : BaseUtils.getPropertyAsList("env.icp.allowed.roles")) {                    
                    if (custRoles.contains(icpRole)) {                        
                        if(getCustomer().getCustomerSellers().size()==0 || !getCustomer().getCustomerSellers().contains(getUserCustomerIdFromSession())) {
                            localiseErrorAndAddToGlobalErrors("cannot.sell.to.buyer");
                            return showMakeSaleWizard();
                        }
                    }                        
                }
                
                for (String superDealerRole : BaseUtils.getPropertyAsList("env.superdealer.allowed.roles")) {                    
                    if (custRoles.contains(superDealerRole)) {
                        if(getCustomer().getCustomerSellers().size()==0 || !getCustomer().getCustomerSellers().contains(getUserCustomerIdFromSession())) {
                            localiseErrorAndAddToGlobalErrors("cannot.sell.to.buyer");
                            return showMakeSaleWizard();
                        }                        
                    }
                }         
        } 
        
        return getDDForwardResolution("/sales/make_sale_get_account_org.jsp");
    }
    
    
    
    Map<Integer, Set<Long>> accountsPerOrg;

    public List<String> getAllowedOptInLevels() {
        return BaseUtils.getPropertyAsList("global.opt.in.levels");
    }

    // A map of the account Ids per organisation id for this customer
    public Map<Integer, Set<Long>> getCustomersAccountsPerOrganisation() {
        if (accountsPerOrg != null) {
            return accountsPerOrg;
        }

        accountsPerOrg = new HashMap<>();
        if (getCustomer() == null) {
            return accountsPerOrg;
        }

        for (ProductInstance pi : getCustomer().getProductInstances()) {
            for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                ServiceInstance si = m.getServiceInstance();
                if (si.getCustomerId() == getCustomer().getCustomerId()) {
                    log.debug("Looking at product instance id [{}] which has organisation id [{}]", pi.getProductInstanceId(), pi.getOrganisationId());
                    Set<Long> accountIds = accountsPerOrg.get(pi.getOrganisationId());
                    if (accountIds == null) {
                        accountIds = new TreeSet<>();
                        accountsPerOrg.put(pi.getOrganisationId(), accountIds);
                    }
                    log.debug("Org Id [{}] has an account id [{}]", pi.getOrganisationId(), si.getAccountId());
                    accountIds.add(si.getAccountId());
                    break;
                }
            }
            log.info("returning  accountsPerOrg map :" + accountsPerOrg.toString());
        }
        return accountsPerOrg;
    }

    public Set<Long> getCustomersPrivateAccounts() {
        return getCustomersAccountsPerOrganisation().get(0);
    }

    @DontValidate()
    public Resolution searchSales() {
        checkPermissions(Permissions.VIEW_SALE);
        clearValidationErrors(); // Cause stripes still doesn't listen to @DontValidate properly
        if (getSalesQuery() != null) {
            getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE);
            if (getSalesQuery().getDateFrom() == null || getSalesQuery().getDateTo() == null) {
                getSalesQuery().setDateFrom(Utils.getDateAsXMLGregorianCalendar(Utils.getPastDate(Calendar.MONTH, 1)));
                getSalesQuery().setDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
            }
            getSalesQuery().setDateTo(Utils.getEndOfDay(getSalesQuery().getDateTo())); // Include till end of the date
            if (getIsIndirectChannelPartner()) {
                getSalesQuery().setRecipientCustomerId(getUserCustomerIdFromSession());
            }
            setSalesList(SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery()));
            if (getSalesList().getNumberOfSales() <= 0) {
                localiseErrorAndAddToGlobalErrors("no.records.found");
            }
        } else {
            setSalesQuery(new SalesQuery());
            getSalesQuery().setDateFrom(Utils.getDateAsXMLGregorianCalendar(Utils.getPastDate(Calendar.MONTH, 1)));
            getSalesQuery().setDateTo(Utils.getDateAsXMLGregorianCalendar(new Date()));
            if (getIsIndirectChannelPartner()) {
                getSalesQuery().setRecipientCustomerId(getUserCustomerIdFromSession());
            }
        }

        return getDDForwardResolution("/sales/search_sale.jsp");
    }

    public Resolution showSaleAfterPaid() {
        setPageMessage("sale.processed.successfully");
        return showSale();
    }

    public Resolution showSale() {
        checkPermissions(Permissions.VIEW_SALE);
        if (getSale() != null && getSale().getSaleId() != null && getSalesQuery() == null) {
            setSalesQuery(new SalesQuery());
            getSalesQuery().getSalesIds().add(getSale().getSaleId());
        }

        if (getSalesQuery() != null) {
            getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE_LINES_DOCUMENTS_FINANCEDATA);
            SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
            if (sl.getNumberOfSales() == 0) {
                localiseErrorAndAddToGlobalErrors("sale.not.found");
            } else {
                setSale(sl.getSales().get(0));
                if (getSale().getRecipientCustomerId() > 0) {
                    if (getIsIndirectChannelPartner() && getSale().getRecipientCustomerId() != getUserCustomerIdFromSession()) {
                        // Only allow ICPs to see sales to them
                        localiseErrorAndAddToGlobalErrors("sale.not.found");
                        return searchSales();
                    }
                    setCustomer(SCAWrapper.getUserSpecificInstance().getCustomer(getSale().getRecipientCustomerId(), StCustomerLookupVerbosity.CUSTOMER_ADDRESS_PRODUCTS_SERVICES));
                    possibleAccountIdsForTransfer = new TreeSet<>();
                    for (ProductInstance pi : getCustomer().getProductInstances()) {
                        for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
                            ServiceInstance si = m.getServiceInstance();
                            if (si.getCustomerId() == getCustomer().getCustomerId()) {
                                possibleAccountIdsForTransfer.add(si.getAccountId());
                            }
                        }
                    }
                }
                if (getSale().getRecipientOrganisationId() > 0) {
                    setOrganisation(SCAWrapper.getUserSpecificInstance().getOrganisation(getSale().getRecipientOrganisationId(), StOrganisationLookupVerbosity.MAIN));
                }
                salesPerson = SCAWrapper.getUserSpecificInstance().getCustomer(getSale().getSalesPersonCustomerId(), StCustomerLookupVerbosity.CUSTOMER);

                salesPersonsFreelancers.clear();
                // Get freelance
                if (BaseUtils.getBooleanProperty("env.pos.sales.freelance.model.enabled", false)) {
                    //Check if the sale has a freelancer
                    setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
                    getStickyNoteEntityIdentifier().setEntityId(getSale().getSaleId());
                    getStickyNoteEntityIdentifier().setEntityType("Sale");
                    StickyNoteList snl = SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier());

                    for (StickyNote sn : snl.getStickyNotes()) {
                        log.error("Sticky note type name [{}], number of fields [{}]", sn.getTypeName(), sn.getNumberOfFields());

                        if (sn.getTypeName().equals("FreelanceSale")) {
                            log.error("Sale [{}] is a freelance sale", sn.getEntityId());

                            setStickyNote(SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(sn.getNoteId())));

                            CustomerQuery cq = new CustomerQuery();
                            for (StickyNoteField snf : getStickyNote().getFields()) {
                                if (snf.getFieldName().equals("FreelancerCustomerProfileId")) {
                                    String freelancerId = snf.getFieldData();
                                    //Pull the customer profile of the freelancer
                                    cq.setCustomerId(Integer.parseInt(freelancerId));
                                    cq.setResultLimit(1);
                                    cq.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
                                    Customer freelancer = SCAWrapper.getAdminInstance().getCustomer(cq);
                                    if (freelancer != null) {
                                        String freelancerName = freelancer.getFirstName() + " " + freelancer.getLastName();
                                        salesPersonsFreelancers.put(freelancer.getCustomerId(), freelancerName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            return searchSales();
        }
        return getDDForwardResolution("/sales/view_sale.jsp");
    }

    public Resolution showDoReturnReplacement() {
        checkPermissions(Permissions.SALE_RETURNS_REPLACEMENTS);
        showSale();
        return getDDForwardResolution("/sales/do_sale_returns_replacements.jsp");
    }

    public Resolution showApprovePromotionCode() {
        return getDDForwardResolution("/sales/approve_promo_code.jsp");
    }

    public Resolution approvePromotionCode() {
        checkPermissions(Permissions.APPROVE_PROMO_CODE);
        try {
            String code = getParameter("promoCodeApprovalCode");
            String[] bits = code.split("\\_");
            String hash = bits[0];
            String promoCode = Codec.encryptedHexStringToDecryptedString(bits[1]);
            String spCustId = Codec.encryptedHexStringToDecryptedString(bits[2]);
            Set<String> allowedCodes = SmileTags.getRoleBasedSubsetOfPropertyAsSet("env.promotion.codes.sales", getRequest());
            log.debug("Promo code being approved is [{}]", promoCode);
            if (!allowedCodes.contains(promoCode)) {
                localiseErrorAndAddToGlobalErrors("no.permission.for.requested.approval");
            } else {
                log.debug("Submitting approval");
                PromotionCodeApprovalData approvalData = new PromotionCodeApprovalData();
                approvalData.setCustomerId(this.getUserCustomerIdFromSession());
                approvalData.setHash(hash);
                approvalData.setSalesPersonCustomerId(Integer.parseInt(spCustId));
                SCAWrapper.getUserSpecificInstance().approvePromotionCode(approvalData);
                setPageMessage("approval.submitted.successfully");
            }
        } catch (Exception e) {
            log.warn("Error in approvePromotionCode", e);
            localiseErrorAndAddToGlobalErrors("invalid.approval.code");
        }
        return showApprovePromotionCode();
    }

    public Resolution createReturnOrReplacement() {
        try {
            getReturn().setCreatedByCustomerId(getUserCustomerIdFromSession());

            if (getReturn().getReplacementItem() == null) { // Handle return here ...
                String desc = getReturn().getDescription();

                if (desc == null || desc.trim().length() <= 0) {
                    localiseErrorAndAddToGlobalErrors("sale.line.return.no.description");
                } else {
                    // Line ticked for return
                    getReturn().setParentReturnId(0); // Returns does not have parent return id 
                    log.debug("Sale line id [{}] is being returned.", getReturn().getSaleLineId());
                    SCAWrapper.getUserSpecificInstance().processReturnOrReplacement(getReturn());
                    setPageMessage("return.processed.successfully");
                }
            } else { // Let's do a replacement here ...
                log.debug("Sale line id [{}] is being replaced.", getReturn().getSaleLineId());
                SCAWrapper.getUserSpecificInstance().processReturnOrReplacement(getReturn());
                setPageMessage("replacement.processed.successfully");
            }

            setSalesQuery(new SalesQuery());
            getSalesQuery().setSaleLineId(getReturn().getSaleLineId());
            showSale();
        } catch (SCABusinessError e) {
            setSalesQuery(new SalesQuery());
            getSalesQuery().setSaleLineId(getReturn().getSaleLineId());
            showSale();
            throw e;
        }
        return getDDForwardResolution("/sales/do_sale_returns_replacements.jsp");

    }

    public Resolution createReplacement() {
        return createReturnOrReplacement();
    }

    public Resolution getSaleInvoice() {
        getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE_LINES_DOCUMENTS);
        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
        byte[] pdf = null;
        Sale sale;
        try {
            sale = sl.getSales().get(0);
            pdf = Utils.decodeBase64(sale.getInvoicePDFBase64());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        StreamingResolution res = new StreamingResolution("application/pdf", new ByteArrayInputStream(pdf));
        res.setFilename("Smile Invoice - " + sale.getSaleId() + ".pdf");
        return res;
    }

    public Resolution getSmallSaleInvoice() {
        getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE_LINES_DOCUMENTS);
        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
        byte[] pdf = null;
        Sale sale;
        try {
            sale = sl.getSales().get(0);
            pdf = Utils.decodeBase64(sale.getSmallInvoicePDFBase64());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        StreamingResolution res = new StreamingResolution("application/pdf", new ByteArrayInputStream(pdf));
        res.setFilename("Smile Invoice - " + sale.getSaleId() + ".pdf");
        return res;
    }

    public Resolution getReturnReplacementPDF() {

        getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE_LINES_DOCUMENTS_FINANCEDATA_RETURNSDOCUMENTS);
        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());

        byte[] pdf = null;
        try {
            for (SaleLine saleRow : sl.getSales().get(0).getSaleLines()) {
                if (saleRow.getLineId().equals(getSalesQuery().getSaleLineId())) {// Line we are looking for ...
                    for (Return _return : saleRow.getReturns()) { // Look for the return
                        if (_return.getReturnId() == getReturn().getReturnId()) { // This is the return we are looking for, get its document
                            pdf = Utils.decodeBase64(_return.getReturnReplacementPDFBase64());
                            break;
                        }
                    }
                }
                //We have still not found the line ... Let's look at the subLines ...
                for (SaleLine ss : saleRow.getSubSaleLines()) {
                    if (ss.getLineId().equals(getSalesQuery().getSaleLineId())) {
                        for (Return _return2 : ss.getReturns()) { // Look for the return
                            if (_return2.getReturnId() == getReturn().getReturnId()) { // This is the return we are looking for, get its document
                                pdf = Utils.decodeBase64(_return2.getReturnReplacementPDFBase64());
                                break;
                            }
                        }
                    }
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        StreamingResolution res = new StreamingResolution("application/pdf", new ByteArrayInputStream(pdf));

        if (getReturn().getReplacementItem() == null) { // This is a return
            res.setFilename("Smile Return - " + getReturn().getReturnId() + ".pdf");
        } else {// This is a replacement
            res.setFilename("Smile Replacement - " + getReturn().getReturnId() + ".pdf");
        }

        return res;
    }

    public Resolution getSaleReversal() {
        getSalesQuery().setVerbosity(StSaleLookupVerbosity.SALE_LINES_DOCUMENTS);
        SalesList sl = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
        byte[] pdf = null;
        Sale sale;
        try {
            sale = sl.getSales().get(0);
            pdf = Utils.decodeBase64(sale.getReversalPDFBase64());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        StreamingResolution res = new StreamingResolution("application/pdf", new ByteArrayInputStream(pdf));
        res.setFilename("Smile Reversal - " + sale.getSaleId() + ".pdf");
        return res;
    }
    private Customer salesPerson;

    public Customer getSalesPerson() {
        return salesPerson;
    }

    @DontValidate
    public Resolution doPostProcessing() {
        checkPermissions(Permissions.MAKE_SALE);
        checkCSRF();
        try {   
            if (notConfirmed()) {
                return confirm(String.valueOf(getSalePostProcessingData().getAccountId()));
            }
            SCAWrapper.getUserSpecificInstance().postProcessSale(getSalePostProcessingData());
        } catch (Exception e) {
            showSale();
            throw e;
        }
        setPageMessage("postprocessing.completed.successfully");
        return showSale();
    }

    public Resolution regenerateInvoice() {
        checkPermissions(Permissions.REGENERATE_INVOICE);
        getSaleModificationData().setNewPaymentStatus("RG");
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("invoice.regenerated.successfully");
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSaleModificationData().getSaleId());
        return showSale();
    }

    public Resolution modifyTillId() {
        checkPermissions(Permissions.MODIFY_TILL_ID);
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("till.id.modified..successfully");
        return showSale();
    }

    public Resolution modifyPaymentTransactionData() {
        checkPermissions(Permissions.MODIFY_PAYMENT_TRANSACTION_DATA);
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("transaction.data.modified.successfully");
        return showSale();
    }

    public Resolution processPaymentOfNonCashSale() {
        checkPermissions(Permissions.PROCESS_BANK_PAYMENT);
        checkCSRF();
        if (notConfirmed()) {
            return confirm(getSaleModificationData().getPaymentTransactionData(), SmileTags.convertCentsToCurrencyShort(getSaleModificationData().getPaymentInCents()));
        }
        getSaleModificationData().setNewPaymentStatus("PV");
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("payment.processed.successfully");
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSaleModificationData().getSaleId());
        return showSale();
    }

    public Resolution processLoanCompletion() {
        checkPermissions(Permissions.MAKE_SALE);
        checkCSRF();
        if (notConfirmed()) {
            return confirm();
        }
        getSaleModificationData().setNewPaymentStatus("LC");
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("loan.completion.processed.successfully");
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSaleModificationData().getSaleId());
        return showSale();
    }

    public Resolution sendDeliveryNote() {
        checkPermissions(Permissions.SEND_DELIVERY_NOTE);
        checkCSRF();
        if (getSaleModificationData().getDeliveryCustomerId() > 0) {
            SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
            setPageMessage("delivery.note.sent.successfully");
        }
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSaleModificationData().getSaleId());
        return showSale();
    }

    public Resolution reverseSale() {
        checkCSRF();
        Sale s = SCAWrapper.getUserSpecificInstance().getSale(getSaleModificationData().getSaleId(), StSaleLookupVerbosity.SALE);
        switch (s.getPaymentMethod()) {
            case "Clearing Bureau":
                checkPermissions(Permissions.REVERSE_SALE_CLEARING_BUREAU);
                break;
            case "Airtime":
                checkPermissions(Permissions.REVERSE_SALE_AIRTIME);
                break;
            default:
                checkPermissions(Permissions.REVERSE_SALE);
                break;
        }
        if (notConfirmed()) {
            return confirm();
        }
        getSaleModificationData().setNewPaymentStatus("RV");
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("sale.reversed.successfully");
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSaleModificationData().getSaleId());
        return showSale();
    }

    public Resolution showReturnItemsInSale() {
        checkPermissions(Permissions.SALE_RETURNS);
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSale().getSaleId());
        showSale();
        return getDDForwardResolution("/sales/return_sale.jsp");
    }

    public String getDefaultReturnLocation() {
        return getWarehouseIdFromSession();
    }

    public Resolution processSaleReturn() {
        checkPermissions(Permissions.SALE_RETURNS);
        getReturnData().setSalesPersonCustomerId(getUserCustomerIdFromSession());
        log.debug("Return location is [{}]", getReturnData().getReturnLocation());
        Enumeration<String> paramNames = getRequest().getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (paramName.startsWith("return_")) {
                String val = getParameter(paramName);
                if (val.equalsIgnoreCase("true")) {
                    String id = paramName.substring(7);
                    int saleLineId = Integer.parseInt(id);
                    log.warn("Sale line id [{}] is being returned", id);
                    LineItem lineItem = new LineItem();
                    lineItem.setLineId(saleLineId);
                    // locate the assocated lines qty

                    String qtyVal = getParameter("quantity_" + id) == null ? null : getParameter("quantity_" + id);
                    log.warn("Sale line quantity to return [{}]", qtyVal);
                    if (qtyVal != null && !qtyVal.isEmpty() && Utils.isNumeric(qtyVal)) {
                        lineItem.setQuantity(Long.parseLong(qtyVal));
                    }
                    getReturnData().getLineItems().add(lineItem);
                }
            }
        }

        try {
            setCreditNote(SCAWrapper.getUserSpecificInstance().processReturn(getReturnData()));
        } catch (SCABusinessError e) {
            setSale(new Sale());
            getSale().setSaleId(getReturnData().getSaleId());
            showReturnItemsInSale();
            throw e;
        }
        salesPerson = SCAWrapper.getUserSpecificInstance().getCustomer(getCreditNote().getSalesPersonCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
        return getDDForwardResolution("/sales/credit_note.jsp");
    }

    public Resolution processPaymentVerificationOfNonCashSale() {
        checkPermissions(Permissions.PROCESS_BANK_PAYMENT_VERIFICATION);
        checkCSRF();
        if (notConfirmed()) {
            return confirm(getSaleModificationData().getPaymentTransactionData(), SmileTags.convertCentsToCurrencyShort(getSaleModificationData().getPaymentInCents()));
        }
        getSaleModificationData().setNewPaymentStatus("PD");
        SCAWrapper.getUserSpecificInstance().modifySale(getSaleModificationData());
        setPageMessage("payment.processed.successfully");
        setSalesQuery(new SalesQuery());
        getSalesQuery().getSalesIds().add(getSaleModificationData().getSaleId());
        return showSale();
    }

    /**
     *
     * CASHING IN
     *
     */
    public Resolution showCashInSelection() {
        return getDDForwardResolution("/sales/cash_in_selection.jsp");
    }

    public Resolution showPendingBankDepositCashIns() {
        checkPermissions(Permissions.VIEW_CASH_IN);

        if (!isAllowedEmptyAllowsAll(BaseUtils.getProperty("env.cashin.otheractivity.allowed.roles", ""))) {
            log.debug("User not allowed to process bank deposit cashin");
            return showCashInSelection();
        }

        getCashInQuery().setSalesAdministratorCustomerId(getUserCustomerIdFromSession());
        setCustomer(UserSpecificCachedDataHelper.getCustomer(getUserCustomerIdFromSession(), StCustomerLookupVerbosity.CUSTOMER));

        if (hasPermissions(Permissions.PROCESS_CASH_IN)) {
            //View other people's bank deposit cashin
            getCashInQuery().setSalesAdministratorCustomerId(0);
        }

        setCashInList(null);

        setCashInList(SCAWrapper.getUserSpecificInstance().getCashIns(getCashInQuery()));

        return getDDForwardResolution("/sales/view_bank_deposit_cash_ins.jsp");
    }

    public Resolution showCashInOffice() {
        return showCashIn();
    }

    public Resolution showCashInBankDeposit() {
        checkPermissions(Permissions.PROCESS_BANK_DEPOSIT_CASH_IN);
        return showCashIn();
    }

    public Resolution showProcessBankDepositCashIn() {
        checkPermissions(Permissions.VIEW_CASH_IN);
        checkPermissions(Permissions.PROCESS_BANK_DEPOSIT_CASH_IN);

        if (getCashInData() == null || getCashInData().getCashInType() == null) {
            return showCashInBankDeposit();
        }

        if (getCashInData().getCashInType().equals("bankdeposit") && getCashInData().getStatus().equals("BDP")) {
            checkPermissions(Permissions.PROCESS_CASH_IN);
        }

        setSalesQuery(new SalesQuery());
        getSalesQuery().setSerialNumber("");
        getSalesQuery().setSalesPersonCustomerId(0);
        getSalesQuery().setRecipientCustomerId(0);
        getSalesQuery().setPurchaseOrderData("");

        getSalesQuery().getSalesIds().clear();
        getSalesQuery().getSalesIds().addAll(getCashInData().getSalesIds());
        setSalesList(SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery()));

        for (Sale sale : getSalesList().getSales()) {
            sale.setTotalLessWithholdingTaxCents(Utils.round(sale.getTotalLessWithholdingTaxCents(), 0));
            if (sale.getPaymentMethod().equals("Cash")) {
                requiredCents += sale.getTotalLessWithholdingTaxCents();
            }
        }
        return getDDForwardResolution("/sales/process_bank_deposit_cash_in.jsp");
    }

    public Resolution showCashIn() {
        checkPermissions(Permissions.VIEW_CASH_IN);
        boolean byProxyAllowed = false; // whether the logged in person can cash in on behalf of someone else
        boolean canSkipLogin = false;
        if (getCashInData() == null || getCashInData().getCashInType() == null) {
            return showCashInSelection();
        }

        /*
         if (getCashInData().getCashInType().equals("bankdeposit")) {
         canSkipLogin = true;
         username = getUser();
         } */
        if (username != null || canSkipLogin) {
            if (password != null && password.isEmpty()) {
                log.debug("Checking if cash in can be done by proxy");
                try {
                    checkPermissions(Permissions.PROCESS_CASH_IN_BY_PROXY);
                    byProxyAllowed = true;
                } catch (Exception e) {
                }
            }
            if (!byProxyAllowed && !canSkipLogin) {
                AuthenticationQuery q = new AuthenticationQuery();
                q.setSSOIdentity(username);
                q.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(password));
                AuthenticationResult res = SCAWrapper.getUserSpecificInstance().authenticate(q);
                if (!res.getDone().equals(StDone.TRUE)) {
                    localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
                    return getDDForwardResolution("/sales/cash_in.jsp");
                }
            }
            CustomerQuery custQ = new CustomerQuery();
            custQ.setSSOIdentity(username);
            custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
            Customer cust = SCAWrapper.getUserSpecificInstance().getCustomer(custQ);
            setSalesQuery(new SalesQuery());
            getSalesQuery().setSerialNumber("");
            getSalesQuery().setSalesPersonCustomerId(cust.getCustomerId());
            SalesList sales = new SalesList();
            SalesList allsales = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
            for (Sale sale : allsales.getSales()) {
                if (getCashInData().getCashInType().equals("office") && sale.getPaymentMethod().equals("Cash") && !isAllowedEmptyAllowsAll(BaseUtils.getProperty("env.cashin.otheractivity.allowed.roles", ""))) {
                    log.debug("User not allowed to process office-cashin cash sale");
                    continue;
                }
                sale.setTotalLessWithholdingTaxCents(Utils.round(sale.getTotalLessWithholdingTaxCents(), 0));
                if (sale.getPaymentMethod().equals("Cash")) {
                    requiredCents += sale.getTotalLessWithholdingTaxCents();
                }
                if (getCashInData().getCashInType().equals("bankdeposit") && !sale.getPaymentMethod().equals("Cash")) {
                    // Bank deposit cashin must only have cash sales
                    continue;
                }
                sales.getSales().add(sale);
            }
            setSalesList(sales);
        }
        return getDDForwardResolution("/sales/cash_in.jsp");
    }
    private double requiredCents = 0;

    public double getCashInRequiredCents() {
        return requiredCents;
    }

    @DontValidate()
    public Resolution processCashIn() {

        if (getCashInData() == null || getCashInData().getCashInType() == null) {
            return showCashIn();
        }

        if (!getCashInData().getCashInType().equals("bankdeposit") || (getCashInData().getCashInType().equals("bankdeposit") && getCashInData().getStatus().equals("BDP"))) {
            // Bank deposit can be done by sales person directly
            checkPermissions(Permissions.PROCESS_CASH_IN);
        }

        boolean processingBankDepositCashIn = false;
        if (getCashInData().getCashInType().equals("bankdeposit") && getCashInData().getStatus().equals("BDP")) {
            processingBankDepositCashIn = true;
        }

        checkCSRF();

        getCashInData().setCashReceiptedInCents(getCashInData().getCashReceiptedInCents() * 100.0d);
        getSalesQuery().setSerialNumber("");
        SalesList sales = SCAWrapper.getUserSpecificInstance().getSales(getSalesQuery());
        if (sales.getSales().isEmpty() || getCashInData() == null) {
            localiseErrorAndAddToGlobalErrors("sale.is.empty");
            if (processingBankDepositCashIn) {
                return showProcessBankDepositCashIn();
            } else {
                return showCashIn();
            }
        }

        boolean hasOne = false;
        boolean skippedOne = false;
        StringBuilder skipped = new StringBuilder("");
        for (Sale sale : sales.getSales()) {
            sale.setTotalLessWithholdingTaxCents(Utils.round(sale.getTotalLessWithholdingTaxCents(), 0));
            String checked = getParameter("cashin_" + sale.getSaleId());
            if (checked != null && checked.equals("true")) {
                hasOne = true;
                getCashInData().getSalesIds().add(sale.getSaleId());
                if (sale.getPaymentMethod().equals("Cash")) {
                    requiredCents += sale.getTotalLessWithholdingTaxCents();
                }
            } else {
                skippedOne = true;
                skipped.append("Sale Id:").append(sale.getSaleId()).append(" (").append(sale.getSaleDate().toString()).append(") ");
            }
        }

        if (!hasOne) {
            clearValidationErrors();
            localiseErrorAndAddToGlobalErrors("no.sales.selected.for.cashin");
            if (processingBankDepositCashIn) {
                return showProcessBankDepositCashIn();
            } else {
                return showCashIn();
            }
        }

        double centsDifference = requiredCents - getCashInData().getCashReceiptedInCents();
        log.debug("Cents difference between required [{}] and receipted [{}] is [{}]", new Object[]{requiredCents, getCashInData().getCashReceiptedInCents(), centsDifference});

        if (centsDifference > BaseUtils.getIntProperty("env.pos.cashin.shortdifference.cents.allowed", 1)) { // Allow 1c difference
            checkPermissions(Permissions.PROCESS_SHORT_CASH_IN);
        }

        if (centsDifference < BaseUtils.getIntProperty("env.pos.cashin.shortdifference.cents.allowed", 1) * -1) {
            // Cant cash in more than the required amount
            localiseErrorAndAddToGlobalErrors("invalid.cashin.amount");
            setConfirmed(false);
            return processCashIn();
        }

        getCashInData().setSalesAdministratorCustomerId(getUserCustomerIdFromSession());

        if (!processingBankDepositCashIn) {
            getCashInData().setSalesPersonCustomerId(getSalesQuery().getSalesPersonCustomerId());
        }

        getCashInData().setCashRequiredInCents(requiredCents);
        if (notConfirmed()) {
            return getDDForwardResolution("/sales/cash_in_confirm.jsp");
        }

        CustomerQuery custQ = new CustomerQuery();
        custQ.setCustomerId(getCashInData().getSalesPersonCustomerId());
        custQ.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);
        Customer cust;

        if (getCashInData().getCashInType().equals("bankdeposit")) {
            cust = SCAWrapper.getAdminInstance().getCustomer(custQ); //Use admin since the returned username can obvisticated if role is blocked
        } else {
            cust = SCAWrapper.getUserSpecificInstance().getCustomer(custQ);
        }

        boolean byProxyAllowed = false; // whether the logged in person can cash in on behalf of someone else
        if (password.isEmpty()) {
            log.debug("Checking if cash in can be done by proxy");
            try {
                if (getCashInData().getCashInType().equals("bankdeposit") && getCashInData().getStatus().isEmpty()) {
                    checkPermissions(Permissions.PROCESS_BANK_DEPOSIT_CASH_IN);// the logged in person can create a bank deposit cash in on behalf of someone else
                    byProxyAllowed = true;
                } else {
                    checkPermissions(Permissions.PROCESS_CASH_IN_BY_PROXY);
                    byProxyAllowed = true;
                }
            } catch (Exception e) {
            }
        }
        if (!byProxyAllowed) {
            AuthenticationQuery q = new AuthenticationQuery();
            q.setSSOIdentity(cust.getSSOIdentity());
            q.setSSOEncryptedPassword(Codec.stringToEncryptedHexString(password));
            AuthenticationResult res = SCAWrapper.getUserSpecificInstance().authenticate(q);
            if (!res.getDone().equals(StDone.TRUE)) {
                localiseErrorAndAddToGlobalErrors("invalid.login.credentials");
                setConfirmed(false);
                return processCashIn();
            }
        }

        setCashInData(SCAWrapper.getUserSpecificInstance().processCashIn(getCashInData()));

        if (skippedOne) {
            log.debug("A cash in for a sale was skipped. This must result in a Jira ticket");
            NewTTIssue tt = new NewTTIssue();

            tt.setCustomerId("0");
            tt.setMindMapFields(new MindMapFields());
            JiraField f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Description");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("Sales person with customer id " + getCashInData().getSalesPersonCustomerId() + " cashed in with sales administrator customer id " + getCashInData().getSalesAdministratorCustomerId() + " and skipped some sales for cashin. Sales skipped were: " + skipped.toString());
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Issue Type");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("Customer Incident");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Summary");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("Sale with a cash in skipped by " + getUser());
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("TT_FIXED_FIELD_Project");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue(BaseUtils.getProperty("env.locale.country.for.language.en") + "FIN");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("SEP Reporter");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("admin admin");
            tt.getMindMapFields().getJiraField().add(f);

            f = new JiraField();
            f.setFieldName("Incident Channel");
            f.setFieldType("TT_FIXED_FIELD");
            f.setFieldValue("System");
            tt.getMindMapFields().getJiraField().add(f);

            SCAWrapper.getAdminInstance().createTroubleTicketIssue(tt);
        }
        if (processingBankDepositCashIn) {
            setPageMessage("bank.deposit.cashin.processed.successfully");
            setCashInQuery(null);
            setCashInQuery(new CashInQuery());
            getCashInQuery().setCashInType("bankdeposit");
            getCashInQuery().setStatus("BDP");
            setCashInData(null);
            setCashInList(null);
            return showPendingBankDepositCashIns();
        } else {
            return getDDForwardResolution("/sales/cash_in_summary.jsp");
        }
    }
    private String username;
    private String password;

    public void setPassword(java.lang.String password) {
        this.password = password;
    }

    public void setUsername(java.lang.String username) {
        this.username = username;
    }

    public java.lang.String getUsername() {
        return username;
    }

    private Set<String> getAccountsPossibleChannels(long accountId) {
        Set<String> channels = new TreeSet<>();
        channels.add(getWarehouseIdFromSession());
        setStickyNoteEntityIdentifier(new StickyNoteEntityIdentifier());
        getStickyNoteEntityIdentifier().setEntityId(accountId);
        getStickyNoteEntityIdentifier().setEntityType("Account");
        StickyNoteList notes = SCAWrapper.getUserSpecificInstance().getEntitiesStickyNotes(getStickyNoteEntityIdentifier());
        for (StickyNote note : notes.getStickyNotes()) {
            log.debug("Looking at sticky note id [{}] of type [{}] for account id [{}]", new Object[]{note.getNoteId(), note.getTypeName(), accountId});
            if (note.getTypeName().equalsIgnoreCase("channel")) {
                StickyNote fullNote = SCAWrapper.getUserSpecificInstance().getStickyNote(makeSCAInteger(note.getNoteId()));
                channels.add(fullNote.getFields().get(0).getFieldData());
            }
        }
        return channels;
    }

    public Resolution showUploadBankStatement() {
        checkPermissions(Permissions.UPLOAD_BANK_STATEMENT);
        return getDDForwardResolution("/statement/load_statement.jsp");
    }

    public Resolution uploadBankStatement() throws IOException {
        checkPermissions(Permissions.UPLOAD_BANK_STATEMENT);
        log.debug("Loading bank statement");
        if (bankStatementFile != null) {
            try {
                new File("/var/bankstatements/in").mkdirs();
            } catch (Exception e) {
                log.debug("Error making bank statement directory: [{}]", e.toString());
            }
            setPageMessage("statement.queued.successfully");
            Utils.writeStreamToDisk(bankStatementFile.getFileName() + ".tmp", bankStatementFile.getInputStream(), "/var/bankstatements/in");
            Utils.moveFile("/var/bankstatements/in/" + bankStatementFile.getFileName() + ".tmp", "/var/bankstatements/in/" + bankStatementFile.getFileName());
            log.debug("Wrote file to [{}] ", bankStatementFile.getFileName());
        }
        return showUploadBankStatement();
    }

    private FileBean bankStatementFile;

    public FileBean getBankStatementFile() {
        return bankStatementFile;
    }

    public void setBankStatementFile(FileBean bankStatementFile) {
        this.bankStatementFile = bankStatementFile;
    }

    private java.lang.String[] getSerialsArray(String serials) {
        // Return an array of serial numbers based on how to serials were entered
        // If the string has " to " in it then its a range of serials
        // If each serial is 20 digits then assume its an ICCID
        // If its just a list then split on a space or carriage return
        log.debug("Expanding serial number [{}]", serials);
        if (serials.contains(" to ")) {
            log.debug("This is a range of serial numbers");
            String[] bits = serials.split("\\ to\\ ");
            String start = bits[0].trim();
            String end = bits[1].trim();
            log.debug("Start is [{}] and end is [{}]", start, end);
            // Are these ICCID's?
            if (start.length() == 20 && end.length() == 20 && Utils.isNumeric(start) && Utils.isNumeric(end)) {
                log.debug("Serials are 20 characters. Is the 20'th a luhn check digit?");
                int cdStartShouldBe = Utils.getLuhnCheckDigit(start.substring(0, 19));
                int cdEndShouldBe = Utils.getLuhnCheckDigit(end.substring(0, 19));
                int cdStartIs = Integer.parseInt(start.substring(19));
                int cdEndIs = Integer.parseInt(end.substring(19));
                if (cdStartShouldBe == cdStartIs && cdEndShouldBe == cdEndIs) {
                    log.debug("These are IMEIs");
                    long startL = Long.parseLong(start.substring(0, 19));
                    long endL = Long.parseLong(end.substring(0, 19));
                    int size = (int) (endL - startL + 1);
                    String[] ret = new String[size];
                    int index = 0;
                    for (long iccidWithoutCD = startL; iccidWithoutCD <= endL; iccidWithoutCD++) {
                        String s = String.valueOf(iccidWithoutCD);
                        ret[index] = s + Utils.getLuhnCheckDigit(s);
                        log.debug("Generated ICCID [{}]", ret[index]);
                        index++;
                    }
                    log.debug("Returning [{}] ICCID's", ret.length);
                    return ret;
                }
            }
            log.debug("Treating serials as a basic number sequence");
            String lastNumericPartOfStart = Utils.getLastNumericPartOfStringAsString(start);
            String lastNumericPartOfEnd = Utils.getLastNumericPartOfStringAsString(end);

            if (lastNumericPartOfStart.length() > 19 || lastNumericPartOfEnd.length() > 19) {
                log.debug("At least one of the serials is too long for a long");
                return serials.split("  *");
            }
            long lastNumericPartOfStartLong = Long.valueOf(lastNumericPartOfStart);
            long lastNumericPartOfEndLong = Long.valueOf(lastNumericPartOfEnd);
            if (!start.endsWith(String.valueOf(lastNumericPartOfStart)) || !end.endsWith(String.valueOf(lastNumericPartOfEnd))) {
                log.debug("The serials have a number part but dont end in the numeric part");
                return serials.split("  *");
            }
            String firstBit = start.replace(String.valueOf(lastNumericPartOfStart), "");
            int firstBitLength = firstBit.length();
            int size = (int) (lastNumericPartOfEndLong - lastNumericPartOfStartLong + 1);
            log.debug("lastNumericPartOfStart [{}] lastNumericPartOfEnd [{}]", lastNumericPartOfStart, lastNumericPartOfEnd);
            if (size < 2) {
                throw new RuntimeException("Start serial number must be less than end serial number");
            }
            String[] ret = new String[size];
            int index = 0;
            int serialLength = start.length();
            for (long numericPart = lastNumericPartOfStartLong; numericPart <= lastNumericPartOfEndLong; numericPart++) {
                String numericPartString = String.valueOf(numericPart);
                int numericPartLength = numericPartString.length();
                int pads = serialLength - firstBitLength - numericPartLength;
                if (pads < 0) {
                    throw new RuntimeException("Start serial number and end serial number must be the same length");
                }
                String val = firstBit;
                for (int i = 0; i < pads; i++) {
                    val = val + "0";
                }
                ret[index] = val + numericPartString;
                log.debug("Generated Serial [{}]", ret[index]);
                index++;
            }
            log.debug("Returning [{}] Serials", ret.length);
            return ret;
        } else {
            log.debug("This is just a list of serials");
            return serials.split("  *");
        }
    }

    public static boolean isItemNumberUnitCredit(String itemNumber) {
        if (itemNumber.startsWith("BUN")) {
            return true;
        }
        List<String[]> itemNumberList = BaseUtils.getPropertyFromSQL("global.pos.uc.item.numbers");
        for (String[] row : itemNumberList) {
            if (row[0].equals(itemNumber)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAirtime(String serialNumber) {
        return serialNumber.equalsIgnoreCase("airtime");
    }

    private void checkSuperDealerConstraints(Sale sale) {
        /*CHECK SD CONSTRAINTS -- HBT-9230*/
        boolean foundSDKit = false;
        String kitItemNumber = "";
        boolean nonStockItem = false;
        List<String[]> kitItems = BaseUtils.getPropertyFromSQLWithoutCache("env.pos.superdealer.kits.items");
        for (SaleLine s : sale.getSaleLines()) {
            if (!s.getInventoryItem().getItemNumber().startsWith("KIT")) {
                //Dont check super dealer contraints if not KIT
                nonStockItem = true;
                continue;
            }

            // Hacky to get this from a property but POS does not allow getting subitems by CATEGORY so its a shortcut to writing all that code
            kitItemNumber = s.getInventoryItem().getItemNumber();
            
            for (String[] kitItem : kitItems) {
                if (kitItem[0].equals(kitItemNumber)) {
                    log.debug("This is an SD kit {}", s.getInventoryItem().getItemNumber());
                    foundSDKit = true;
                    break;
                }
            }
            if (foundSDKit) {
                break;
            }
        }

        /*If its an SD kit being sold then verify if sales person is a super dealer*/
        Customer sp = UserSpecificCachedDataHelper.getCustomer(sale.getRecipientCustomerId(), StCustomerLookupVerbosity.CUSTOMER);
        boolean isASuperDealer = false;
        for (CustomerRole cr : sp.getCustomerRoles()) {
            int orgId = cr.getOrganisationId();
            if (isSuperDealer(orgId) || isFranchise(orgId) || isMegaDealer(orgId)) {
                isASuperDealer = true;
                break;
            }
        }

        if (foundSDKit) {
            log.debug("Verifying if customer is an SD");
            if (!isASuperDealer) {
                SCABusinessError sbe = new SCABusinessError();
                sbe.setErrorCode("SEP-0003");
                sbe.setErrorDesc("This KIT can only be sold to a super dealer -- " + kitItemNumber);
                sbe.setRequest("");
                throw sbe;
            }
            log.debug("Customer [{}] is an SD", sp.getCustomerId());
        }

        if (isASuperDealer && (!foundSDKit && !nonStockItem)) {

            SCABusinessError sbe = new SCABusinessError();
            sbe.setErrorCode("SEP-0004");
            sbe.setErrorDesc("This KIT cannot be sold to a super dealer -- " + kitItemNumber);
            sbe.setRequest("");
            throw sbe;
        }
    }
    
     private List<String> availableNumbers;

    public List<String> getAvailableNumbers() {
        return availableNumbers;
    }

    public void setAvailableNumbers(List<String> availableNumbers) {
        this.availableNumbers = availableNumbers;
    }
    
    private String regulatorResponse;

    public String getRegulatorResponse() {
        return regulatorResponse;
    }

    public void setRegulatorResponse(String regulatorResponse) {
        this.regulatorResponse = regulatorResponse;
    }
    
    public boolean ninVerified = false;

    public boolean isNinVerified() {
        return ninVerified;
    }

    public void setninVerified(boolean nin) {
        this.ninVerified = nin;
    }

}
