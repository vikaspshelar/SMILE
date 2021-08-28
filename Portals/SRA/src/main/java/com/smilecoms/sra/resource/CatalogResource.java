/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.InventoryList;
import com.smilecoms.commons.sca.InventoryQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StUnitCreditSpecificationLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.UnitCreditSpecificationQuery;
import com.smilecoms.commons.sca.beans.CatalogBean;
import com.smilecoms.commons.sca.beans.UnitCreditSpecificationBean;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.commons.util.DAOUtil;
import com.smilecoms.model.InternationalCallRate;
import com.smilecoms.sra.model.ExtraBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.*;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("catalogs")
public class CatalogResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(CatalogResource.class);
    private CatalogBean catalog;
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("unitcredits")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public CatalogResource getUnitCreditCatalog(@QueryParam("productInstanceId") int productInstanceId, @QueryParam("accountId") long accountId) {
        start(request);
        try {
            if (productInstanceId > 0) {
                this.catalog = CatalogBean.getProductInstanceAndUserSpecificUnitCreditCatalog(productInstanceId, accountId);
            } else {
                this.catalog = CatalogBean.getUserSpecificUnitCreditCatalog();
            }
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return this;
    }

    /**
     * API to return list of bundles available in catalog
     *
     * @return List of UnitCreditSpecification
     */
    @Path("bundles")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public List<UnitCreditSpecificationBean> getBundleList() {
        start(request);
        
        List<UnitCreditSpecificationBean> unitCreditSpecifications = new ArrayList<>();
        try {
            CatalogBean catalogs = CatalogBean.getUserSpecificUnitCreditCatalog();
            unitCreditSpecifications = catalogs.getUnitCreditSpecifications();
            return unitCreditSpecifications.parallelStream().
                        filter(ucs -> isBundleAllowed(ucs)).collect(Collectors.toList());
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
    
    private boolean isBundleAllowed(UnitCreditSpecificationBean ucs){
        boolean iscampaignOnly =  Utils.getBooleanValueFromCRDelimitedAVPString(ucs.getConfiguration(), "CampaignsOnly");
        if(iscampaignOnly){
            return false;
        }
        StringTokenizer stValues = new StringTokenizer(ucs.getPurchaseRoles(), "\r\n");
        while (stValues.hasMoreTokens()) {
            String role = stValues.nextToken().trim();
            if (!role.isEmpty() && "Customer".equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
        
    }
    
    /**
     * API to return list of upsize bundles for  specific bundles.
     *
     * @param ucId
     * @return List of UnitCreditSpecification
     */
    @Path("extrabundles/{ucId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public List<ExtraBundle> getBundleList(@PathParam("ucId") int ucId) {
        start(request);
        List<ExtraBundle> unitCreditSpecifications = new ArrayList<>();
        //Token callersToken = Token.getRequestsToken();
        //List<String> roles = callersToken.getGroups();
        try {
            UnitCreditSpecification  uc = getUnitCreditSpecification(ucId);
            String upSizeUCSpecIds = Utils.getValueFromCRDelimitedAVPString(uc.getConfiguration(), "ExtraUpsizeSpecIds");
            String[] upSizeIDs = upSizeUCSpecIds.split(",");
            for (String upsizeUCSpecID : upSizeIDs) {
                
                UnitCreditSpecification upsizeUCSpec = getUnitCreditSpecification(Integer.valueOf(upsizeUCSpecID.trim()));
                String mySmileUpsizeUCDiscount = Utils.getValueFromCRDelimitedAVPString(upsizeUCSpec.getConfiguration(), "MySmileDiscountPercent");

                if (mySmileUpsizeUCDiscount != null && mySmileUpsizeUCDiscount.length() > 0) {
                    double upsizeDiscountPercOff = Double.parseDouble(mySmileUpsizeUCDiscount);
                    upsizeUCSpec.setPriceInCents(upsizeUCSpec.getPriceInCents() * (1 - upsizeDiscountPercOff / 100d));
                }
                String purchageRoles = upsizeUCSpec.getPurchaseRoles();
                StringTokenizer stValues = new StringTokenizer(purchageRoles, "\r\n");
                /*boolean isPurchageAllowed = false;
                while (stValues.hasMoreTokens()) {
                   String role = stValues.nextToken().trim();
                   if (!role.isEmpty() && roles.contains(role)) {
                      isPurchageAllowed = true;
                   }
                }
                if (!isPurchageAllowed) {
                    continue;
                }*/
                unitCreditSpecifications.add(convertExtraBundle(upsizeUCSpec));
            }
        } catch (Exception ex) {
            throw processError("Not found","BUSINESS","SRA-0053",Response.Status.NOT_FOUND);
        } finally {
            end();
        }
        return unitCreditSpecifications;
    }
    
    private UnitCreditSpecification getUnitCreditSpecification(int ucid){
        UnitCreditSpecificationQuery ucsq = new UnitCreditSpecificationQuery();
        ucsq.setUnitCreditSpecificationId(ucid);
        ucsq.setVerbosity(StUnitCreditSpecificationLookupVerbosity.MAIN_SVCSPECIDS);
        UnitCreditSpecification upsizeUCSpec = SCAWrapper.getUserSpecificInstance().getUnitCreditSpecifications(ucsq).getUnitCreditSpecifications().get(0);
        return upsizeUCSpec;
    }
    
    /**
     * API to return list of bundles available in catalog for specific account
     * @param accountId
     * @return 
     */
    @Path("accountbundles/{accountId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public List<UnitCreditSpecificationBean> getBundleList(@PathParam("accountId") Long accountId) {
        start(request);
        List<UnitCreditSpecificationBean> unitCreditSpecifications = new ArrayList<>();
        try {
            CatalogBean catalogs = CatalogBean.getUserSpecificUnitCreditCatalog(accountId);
            unitCreditSpecifications = catalogs.getUnitCreditSpecifications();
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return unitCreditSpecifications;
    }

    @Path("products")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public CatalogResource getProductCatalog() {
        start(request);
        try {
            this.catalog = CatalogBean.getUserSpecificProductCatalog();
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return this;
    }
    
    @Path("internationalrates")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<InternationalCallRate> getInternationalCallRates() {
        return DAOUtil.getInternationalCallRates();
    }
    
    @Path("inventory")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public InventoryList getInventoryItems(@QueryParam("itemName") String itemName, @QueryParam("city") String city ) {
        if(itemName == null || itemName.trim().isEmpty()){
            throw processError("Invalid itemName","BUSINESS","SRA-0353",Response.Status.BAD_REQUEST);
        }
        String location = (city == null || city.isEmpty())? "default" : city;
        String warehouseId = getWarehouseId(location);
        
        log.debug("warehouseId for [{}] is [{}]",location, warehouseId );
        if(warehouseId == null || warehouseId.isEmpty()){
            throw processError("Invalid location","BUSINESS","SRA-1353",Response.Status.BAD_REQUEST);
            
        }
        InventoryQuery query =  new InventoryQuery();
        query.setStringMatch(itemName);
        query.setWarehouseId(warehouseId);
        return SCAWrapper.getUserSpecificInstance().getInventory(query);
    }

    public CatalogBean getCatalog() {
        return catalog;
    }

    private ExtraBundle convertExtraBundle(UnitCreditSpecification upsizeUCSpec) {
        ExtraBundle eb = new ExtraBundle();
        eb.setUnitCreditSpecificationId(upsizeUCSpec.getUnitCreditSpecificationId());
        eb.setDescription(upsizeUCSpec.getDescription());
        eb.setItemNumber(upsizeUCSpec.getItemNumber());
        eb.setName(upsizeUCSpec.getName());
        eb.setPriceInCents(upsizeUCSpec.getPriceInCents());
        eb.setUnitType(upsizeUCSpec.getUnitType());
        eb.setUnits(upsizeUCSpec.getUnits());
        eb.setUsableDays(upsizeUCSpec.getUsableDays());
        eb.setValidityDays(upsizeUCSpec.getValidityDays());
        return eb;
    }

    private String getWarehouseId(String location) {
        return BaseUtils.getSubProperty("env.pos.location.based.warehouses", location);
    }
}
