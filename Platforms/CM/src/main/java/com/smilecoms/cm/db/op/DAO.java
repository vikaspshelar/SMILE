/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.cm.db.op;

import com.smilecoms.cm.CatalogManager;
import com.smilecoms.cm.db.model.*;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Address;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.cm.AVP;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DAO {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);

    private static final String PI_HISTORY_INSERT = "INSERT INTO SmileDB.product_instance_history\n"
            + "(PRODUCT_INSTANCE_ID, PRODUCT_SPECIFICATION_ID, CUSTOMER_PROFILE_ID, ORGANISATION_ID, STATUS, SEGMENT, CREATED_BY_CUSTOMER_PROFILE_ID, \n"
            + "CREATED_BY_ORGANISATION_ID, CREATED_DATETIME, PROMOTION_CODE, LAST_MODIFIED, FRIENDLY_NAME, \n"
            + "LOGICAL_ID, PHYSICAL_ID, LAST_ACTIVITY_DATETIME, FIRST_ACTIVITY_DATETIME, LAST_RECONNECTION_DATETIME, \n"
            + "LAST_IMEI, REFERRAL_CODE, PAUSED_ACTIVITY_DATETIME, FIRST_RG_ACTIVITY_DATETIME, LAST_RG_ACTIVITY_DATETIME, \n"
            + "PAUSED_RG_ACTIVITY_DATETIME, LAST_RG_RECONNECTION_DATETIME, INSERTED_DATETIME) \n"
            + "SELECT *, NOW() from product_instance where PRODUCT_INSTANCE_ID=?";

    public static ProductSpecification getProductSpecificationById(EntityManager em, int prodSpecId) {
        return JPAUtils.findAndThrowENFE(em, ProductSpecification.class, prodSpecId);
    }

    public static ServiceSpecification getServiceSpecificationById(EntityManager em, int svcSpecId) {
        return JPAUtils.findAndThrowENFE(em, ServiceSpecification.class, svcSpecId);
    }

    public static UnitCreditSpecification getUnitCreditSpecificationById(EntityManager em, int unitCrdId) {
        return JPAUtils.findAndThrowENFE(em, UnitCreditSpecification.class, unitCrdId);
    }

    public static UnitCreditSpecification getUnitCreditSpecificationByName(EntityManager em, String unitCreditName) {
        Query q = em.createNativeQuery("SELECT * FROM unit_credit_specification where unit_credit_name=? AND AVAILABLE_FROM <= now() and AVAILABLE_TO > now() ORDER BY PRICE_CENTS DESC LIMIT 1", UnitCreditSpecification.class);
        q.setParameter(1, unitCreditName);
        return (UnitCreditSpecification) q.getSingleResult();
    }

    public static UnitCreditSpecification getUnitCreditSpecificationByItemNumber(EntityManager em, String itemNumber) {
        Query q = em.createNativeQuery("SELECT * FROM unit_credit_specification where ITEM_NUMBER=? order by unit_credit_specification_id LIMIT 1", UnitCreditSpecification.class);
        q.setParameter(1, itemNumber);
        return (UnitCreditSpecification) q.getSingleResult();
    }

    public static Collection<ProductSpecification> getAllProductSpecifications(EntityManager em) {
        Query q = em.createNativeQuery("SELECT * FROM product_specification order by product_specification_id", ProductSpecification.class);
        return (Collection< ProductSpecification>) (q.getResultList());
    }

    public static Collection<UnitCreditSpecification> getAllAvailableUnitCreditSpecifications(EntityManager em) {
        Query q = em.createNativeQuery("SELECT * FROM unit_credit_specification where available_from <= now() and available_to > now() order by price_cents", UnitCreditSpecification.class);
        return (Collection<UnitCreditSpecification>) (q.getResultList());
    }

    public static Collection<UnitCreditSpecification> getAllAvailableUnitCreditSpecificationsWithEquivalentParents(EntityManager em) {
        Query q = em.createNativeQuery("SELECT * FROM unit_credit_specification where configuration like '%EquivalentParentSpecId%'", UnitCreditSpecification.class);
        return (Collection<UnitCreditSpecification>) (q.getResultList());
    }

    public static Collection<ServiceSpecificationAvp> getServiceSpecificationAVPs(EntityManager em, int serviceSpecificationId) {
        Query q = em.createNativeQuery("SELECT * FROM service_specification_avp where service_specification_id = ?", ServiceSpecificationAvp.class);
        q.setParameter(1, serviceSpecificationId);
        return (Collection<ServiceSpecificationAvp>) (q.getResultList());
    }

    public static ServiceInstance getServiceInstanceById(EntityManager em, int svcInstId) {
        ServiceInstance si = JPAUtils.findAndThrowENFE(em, ServiceInstance.class, svcInstId);
        if (si.getStatus().equals(CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString())) {
            throw new javax.persistence.EntityNotFoundException();
        }
        return si;
    }

    public static void setServiceInstancePhotographs(EntityManager em, int serviceInstanceId, List<AVP> avps) throws Exception {

        // Dont delete if we arent adding any photos
        if (avps.isEmpty()) {
            return;
        }

        Query q = em.createNativeQuery("DELETE FROM SmileDB.photograph WHERE SERVICE_INSTANCE_ID = ?");
        q.setParameter(1, serviceInstanceId);
        q.executeUpdate();

        for (AVP avp : avps) {

            int pipe = avp.getValue().indexOf("|");
            String guid = avp.getValue().substring(0, pipe);
            byte[] data = Utils.decodeBase64(avp.getValue().substring(pipe + 1));
            Photograph photo = new Photograph();
            photo.setServiceInstanceId(serviceInstanceId);
            photo.setPhotoGuid(guid);
            photo.setPhotoType(avp.getAttribute());
            if (data.length < 1000) {
                throw new Exception("Invalid/empty photograph -- " + photo.getPhotoType());
            }

            photo.setData(data);
            photo.setPhotoHash(HashUtils.md5(photo.getData()));

            //In some countries, like Tanzania, fingerprints are not allowed to be stored on the system.
            if (avp.getAttribute() != null
                    && avp.getAttribute().equalsIgnoreCase("fingerprints")
                    && BaseUtils.getBooleanProperty("env.kyc.do.not.store.fingerprints", false)) {
                photo.setData(null);
            }

            Query q2 = em.createNativeQuery("select concat(ifnull(customer_profile_id,''),ifnull(organisation_id,''),ifnull(service_instance_id,'')) FROM SmileDB.photograph WHERE PHOTO_HASH = ? limit 1");
            q2.setParameter(1, photo.getPhotoHash());
            List res = q2.getResultList();
            if (!res.isEmpty()) {
                throw new Exception("Duplicate photograph -- " + photo.getPhotoType() + " exists on a customer or organisation or service instance with Id " + res.get(0));
            }
            em.persist(photo);
        }
        em.flush();
    }

    public static List<CampaignRun> getCampaignRunList(EntityManager em, int productInstanceId) {
        Query q = em.createNativeQuery("select * from campaign_run where product_instance_id = ?", CampaignRun.class);
        q.setParameter(1, productInstanceId);
        List<CampaignRun> ret = q.getResultList();
        log.debug("Product instance id [{}] has [{}] campaigns", productInstanceId, ret.size());
        return ret;
    }

    public static void deleteWaitingCampaignsForProductInstance(EntityManager em, int piId) {
        Query q = em.createNativeQuery("delete from campaign_run where product_instance_id = ? and STATUS='NW'");
        q.setParameter(1, piId);
        q.executeUpdate();
    }

    public static ProductInstance getProductInstanceById(EntityManager em, int prodInstId) {
        ProductInstance pi = JPAUtils.findAndThrowENFE(em, ProductInstance.class, prodInstId);
        if (pi.getStatus().equals(CatalogManager.PRODUCT_INSTANCE_STATUS.DE.toString())) {
            throw new javax.persistence.EntityNotFoundException();
        }
        return pi;
    }

    public static ProductInstance getProductInstanceByServiceInstanceId(EntityManager em, int serviceInstanceId) {
        Query q = em.createNativeQuery("SELECT PI.* FROM service_instance SI, product_instance PI WHERE SI.service_instance_id = ? and PI.product_instance_id = SI.product_instance_id", ProductInstance.class);
        q.setParameter(1, serviceInstanceId);
        return (ProductInstance) q.getSingleResult();
    }

    public static ProductServiceMapping getProductToServiceMapping(EntityManager em, int prodSpecId, int svcSpecId) {
        return em.find(ProductServiceMapping.class, new ProductServiceMappingPK(prodSpecId, svcSpecId));
    }

    public static Collection<ProductServiceMapping> getProductToServiceMappings(EntityManager em, int prodSpecId) {
        Query q = em.createNativeQuery("SELECT * FROM product_service_mapping p WHERE p.product_specification_id = ? ORDER BY SERVICE_SPECIFICATION_ID ASC", ProductServiceMapping.class);
        q.setParameter(1, prodSpecId);
        return (Collection<ProductServiceMapping>) q.getResultList();
    }

    public static ProductInstance createProductInstance(EntityManager em, ProductInstance dbProductInstance) {
        dbProductInstance.setStatus("AC");
        dbProductInstance.setLastModified(new Date());
        em.persist(dbProductInstance);
        em.flush();
        em.refresh(dbProductInstance);
        return dbProductInstance;
    }

    public static void tempDeactivateAllSIsInProductInstance(EntityManager em, int productInstanceId) {
        Query q = em.createNativeQuery("UPDATE service_instance set STATUS='TD', LAST_MODIFIED=now() WHERE PRODUCT_INSTANCE_ID=?");
        q.setParameter(1, productInstanceId);
        q.executeUpdate();
    }

    public static void reactivateAllSIsInProductInstance(EntityManager em, int productInstanceId) {
        Query q = em.createNativeQuery("UPDATE service_instance set STATUS='AC', LAST_MODIFIED=now() WHERE PRODUCT_INSTANCE_ID=?");
        q.setParameter(1, productInstanceId);
        q.executeUpdate();
    }

    public static void syncUnitCreditPrices(EntityManager em) {
        Query q = em.createNativeQuery("update unit_credit_specification UCS "
                + "join x3_offline_inventory INV on (INV.ITEM_NUMBER=UCS.ITEM_NUMBER AND INV.CURRENCY=?) "
                + "set UCS.PRICE_CENTS = round(INV.PRICE*100*(select PROPERTY_VALUE/100 + 1 from property where PROPERTY_NAME = 'env.sales.tax.percent')) "
                + "WHERE UCS.PRICE_CENTS != round(INV.PRICE*100*(select PROPERTY_VALUE/100 + 1 from property where PROPERTY_NAME = 'env.sales.tax.percent')) "
                + "AND CONFIGURATION NOT LIKE '%IgnoreX3Price=true%' "
                + "AND CONFIGURATION NOT LIKE '%EquivalentParentSpecId=%'");
        q.setParameter(1, BaseUtils.getProperty("env.currency.official.symbol"));
        q.executeUpdate();
    }

    public static ServiceInstance persistServiceInstance(EntityManager em, ServiceInstance dbServiceInstance) {
        if (!dbServiceInstance.getStatus().equals(CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString())) {
            // An active service instance must have a unique remote resource id
            log.debug("Service instance is being created or modified and its not deleted. Verifying that remote resource id [{}] and status is unique", dbServiceInstance.getRemoteResourceId());
            log.debug("This SIs service_instance_id is [{}]", dbServiceInstance.getServiceInstanceId());
            Query q = em.createNativeQuery("SELECT count(*) FROM service_instance SI WHERE SI.remote_resource_id = ? and SI.status != '"
                    + CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString()
                    + "' and SI.service_instance_id != ?");
            q.setParameter(1, dbServiceInstance.getRemoteResourceId());
            q.setParameter(2, dbServiceInstance.getServiceInstanceId() == null ? 0 : dbServiceInstance.getServiceInstanceId());
            Long count = (Long) q.getSingleResult();
            log.debug("Count of existing non DE rows in service_instance with remote resource id [{}] is [{}]", dbServiceInstance.getRemoteResourceId(), count);
            if (count > 0) {
                throw new javax.persistence.PersistenceException("An active service instance must have a unique remote resource id");
            }
        }
        dbServiceInstance.setLastModified(new Date());        
        em.persist(dbServiceInstance);
        em.flush();
        em.refresh(dbServiceInstance);        
        
        return dbServiceInstance;
    }

    public static List<ServiceInstance> getServiceInstancesByProductInstanceId(EntityManager em, int prodInstanceId) {
        Query q = em.createNativeQuery("SELECT * FROM service_instance where product_instance_id=? and status != 'DE' order by service_specification_id", ServiceInstance.class);
        q.setParameter(1, prodInstanceId);
        return q.getResultList();
    }

    public static Collection<ProductInstance> getCustomerNonOrgProductInstances(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("select * from product_instance where customer_profile_id=? and status != 'DE' and organisation_id=0", ProductInstance.class);
        q.setParameter(1, customerId);
        return (Collection<ProductInstance>) q.getResultList();
    }

    public static Collection<ProductInstance> getCustomerRelatedProductInstances(EntityManager em, int customerId, int offset, int limit) {
        log.debug("Limit [{}] Offset [{}]", limit, offset);
        if (limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        if (offset < 0) {
            offset = 0;
        }
        Query q = em.createNativeQuery("SELECT * from (SELECT PI.* FROM product_instance PI where PI.CUSTOMER_PROFILE_ID = ? AND PI.STATUS != 'DE' UNION "
                + "SELECT PI.* FROM product_instance PI JOIN service_instance SI on PI.PRODUCT_INSTANCE_ID = SI.PRODUCT_INSTANCE_ID WHERE SI.CUSTOMER_PROFILE_ID=? AND PI.STATUS != 'DE') as tmp "
                + "ORDER BY tmp.PRODUCT_INSTANCE_ID LIMIT ? OFFSET ?", ProductInstance.class);
        q.setParameter(1, customerId);
        q.setParameter(2, customerId);
        q.setParameter(3, limit);
        q.setParameter(4, offset); // Number to skip
        return (Collection<ProductInstance>) q.getResultList();
    }

    public static int getCustomerRelatedProductInstancesTotalCount(EntityManager em, int customerId) {
        if (customerId == 0) {
            return 0;
        }
        Query q = em.createNativeQuery("select count(*) from product_instance where customer_profile_id=? and status != 'DE'");
        q.setParameter(1, customerId);
        try {
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            log.warn("No results found for product Instances with customer Id [{}], because {}", customerId, e.toString());
            return 0;
        }
    }

    public static int getOrganisationProductInstancesTotalCount(EntityManager em, int orgId) {
        if (orgId == 0) {
            return 0;
        }
        Query q = em.createNativeQuery("select count(*) from product_instance where organisation_id=? and status != 'DE'");
        q.setParameter(1, orgId);
        try {
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            log.warn("No results found for product Instances with organisation Id [{}]", orgId);
            return 0;
        }
    }

    public static Collection<ProductInstance> getOrganisationProductInstances(EntityManager em, int organisationId, int offset, int limit) {
        log.debug("Limit [{}] Offset [{}]", limit, offset);
        if (limit <= 0) {
            limit = Integer.MAX_VALUE;
        }
        if (offset < 0) {
            offset = 0;
        }
        Query q = em.createNativeQuery("select * from product_instance where organisation_id=? and status != 'DE' ORDER BY PRODUCT_INSTANCE_ID LIMIT ? OFFSET ?", ProductInstance.class);
        q.setParameter(1, organisationId);
        q.setParameter(2, limit);
        q.setParameter(3, offset); // Number to skip
        return (Collection<ProductInstance>) q.getResultList();
    }

    public static Collection<ProductInstance> getProductInstancesByPhysicalId(EntityManager em, String physicalId) {
        Query q = em.createNativeQuery("select * from product_instance where physical_id=? and status != 'DE' limit ?", ProductInstance.class);
        q.setParameter(1, physicalId);
        q.setParameter(2, BaseUtils.getIntProperty("env.cm.pi.by.org.limit", 1000));
        return (Collection<ProductInstance>) q.getResultList();
    }

    public static Collection<ProductInstance> getOrganisationProductInstancesWithSpecId(EntityManager em, int organisationId, int productSpecificationId) {
        Query q = em.createNativeQuery("select * from product_instance where organisation_id=? and status != 'DE' and product_specification_id = ? limit ?", ProductInstance.class);
        q.setParameter(1, organisationId);
        q.setParameter(2, productSpecificationId);
        q.setParameter(3, BaseUtils.getIntProperty("env.cm.pi.by.org.limit", 1000));
        return (Collection<ProductInstance>) q.getResultList();
    }

    public static Collection<ServiceInstance> getCustomerServiceInstances(EntityManager em, int customerId) {
        Query q = em.createNativeQuery("SELECT * FROM service_instance where customer_profile_id=? and status != 'DE' order by product_instance_id, service_specification_id", ServiceInstance.class);
        q.setParameter(1, customerId);
        return (Collection<ServiceInstance>) q.getResultList();
    }

    public static Collection<ServiceInstance> getAccountServiceInstances(EntityManager em, long accountId) {
        Query q = em.createNativeQuery("SELECT * FROM service_instance where account_id=? and status != 'DE' order by product_instance_id, service_specification_id", ServiceInstance.class);
        q.setParameter(1, accountId);
        return (Collection<ServiceInstance>) q.getResultList();
    }

    public static Collection<ServiceSpecification> getAllServiceSpecifications(EntityManager em) {
        Query q = em.createNativeQuery("SELECT * FROM service_specification order by service_specification_id", ServiceSpecification.class);
        return (Collection<ServiceSpecification>) (q.getResultList());
    }

    public static int getNumOfServiceIntances(EntityManager em, int prodInstId, int svcSpecId) {
        Query q = em.createNativeQuery("SELECT COUNT(SI.service_instance_id) FROM service_instance SI WHERE SI.product_instance_id = ? AND SI.service_specification_id = ? and SI.status != '"
                + CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString()
                + "'");
        q.setParameter(1, prodInstId);
        q.setParameter(2, svcSpecId);
        Long count = (Long) q.getSingleResult();
        return count.intValue();
    }

    public static int getServiceInstancesRatePlanId(EntityManager em, int prodInstID, int serviceSpecId) {

        if (serviceSpecId == 0 || serviceSpecId >= 1000) {
            // Shortcut if we know the rate plan. Saves processing as this function is called a LOT
            return 1;
        }
        Query q = em.createNativeQuery("SELECT PSM.RATE_PLAN_ID FROM "
                + "product_instance PI, "
                + "product_service_mapping PSM "
                + "WHERE PI.PRODUCT_INSTANCE_ID =? "
                + "AND PI.PRODUCT_SPECIFICATION_ID = PSM.PRODUCT_SPECIFICATION_ID "
                + "AND PSM.SERVICE_SPECIFICATION_ID = ?");
        q.setParameter(1, prodInstID);
        q.setParameter(2, serviceSpecId);
        try {
            return (Integer) q.getSingleResult();
        } catch (Exception e) {
            log.warn("No rate plan found for product Instance Id [{}] and service spec Id [{}]", prodInstID, serviceSpecId);
            throw e;
        }
    }

    public static Collection<ServiceInstance> getServiceInstancesByIdentifierAndType(EntityManager em, String identifier, String identifierType) {
        log.debug("Getting service instances for ID [{}] Type [{}]", identifier, identifierType);
        Query q = em.createNativeQuery("select SI.* from "
                + "service_instance SI, "
                + "service_instance_mapping SIM "
                + "WHERE SIM.IDENTIFIER = ? "
                + "AND SIM.IDENTIFIER_TYPE = ? "
                + "AND SIM.SERVICE_INSTANCE_ID = SI.SERVICE_INSTANCE_ID AND SI.STATUS != '"
                + CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString()
                + "'", ServiceInstance.class);
        q.setParameter(1, identifier);
        q.setParameter(2, identifierType);
        return q.getResultList();
    }

    public static Collection<ServiceInstance> getServiceInstancesByProductInstanceAndServiceSpec(EntityManager em, int productInstanceId, int serviceSpecificationId) {
        log.debug("Getting service instances for product instance [{}] and service spec [{}]", productInstanceId, serviceSpecificationId);
        Query q = em.createNativeQuery("select SI.* from "
                + "service_instance SI "
                + "WHERE SI.PRODUCT_INSTANCE_ID = ? "
                + "AND SI.SERVICE_SPECIFICATION_ID = ? AND SI.STATUS != '"
                + CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString()
                + "'", ServiceInstance.class);
        q.setParameter(1, productInstanceId);
        q.setParameter(2, serviceSpecificationId);
        return q.getResultList();
    }

    public static Collection<ServiceInstance> getServiceInstancesByServiceSpec(EntityManager em, int serviceSpecificationId) {
        log.debug("Getting service instances for service spec [{}]", serviceSpecificationId);
        Query q = em.createNativeQuery("select SI.* from "
                + "service_instance SI "
                + "WHERE SI.SERVICE_SPECIFICATION_ID = ? AND SI.STATUS != '"
                + CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString()
                + "' LIMIT 200", ServiceInstance.class);
        q.setParameter(1, serviceSpecificationId);
        return q.getResultList();
    }

    public static ServiceInstance getServiceInstanceByIPAddress(EntityManager em, String ipAddress) {
        log.debug("Getting service instances for IPAddress [{}]", ipAddress);
        Query q = em.createNativeQuery("select SI.* from "
                + "service_instance SI, pcrf.ipcan_sessions SESS "
                + "WHERE SI.SERVICE_INSTANCE_ID = SESS.highest_priority_service_id "
                + "AND SESS.binding_identifier = ? AND SI.STATUS != '"
                + CatalogManager.SERVICE_INSTANCE_STATUS.DE.toString()
                + "'", ServiceInstance.class);
        q.setParameter(1, "/" + ipAddress);
        return (ServiceInstance) q.getSingleResult();
    }

    public static String getIMEIForIPAddress(EntityManager em, String ipAddress) {
        log.debug("Getting IMEI for IPAddress [{}]", ipAddress);
        Query q = em.createNativeQuery("select imeisv from pcrf.ipcan_sessions WHERE binding_identifier = ?");
        q.setParameter(1, "/" + ipAddress);
        return (String) q.getSingleResult();
    }

    /**
     * Ideally this should use IM, but for performance reasons, we go direct
     *
     * @param em
     * @param originatingIdentity
     * @return
     */
    public static int getCustomerProfileIdBySSOIdentity(EntityManager em, String originatingIdentity) {
        Query q = em.createNativeQuery("SELECT customer_profile_id FROM SmileDB.customer_profile WHERE SSO_IDENTITY = ?");
        q.setParameter(1, originatingIdentity);
        return (Integer) q.getSingleResult();
    }

    public static int getFirstOrgIdBySSOIdentity(EntityManager em, String originatingIdentity) {
        Query q = em.createNativeQuery("SELECT customer_role.ORGANISATION_ID FROM SmileDB.customer_profile "
                + "join SmileDB.customer_role on customer_role.CUSTOMER_PROFILE_ID = customer_profile.CUSTOMER_PROFILE_ID "
                + "WHERE SSO_IDENTITY=? ORDER BY customer_role.ORGANISATION_ID ASC LIMIT 1");
        q.setParameter(1, originatingIdentity);
        return (Integer) q.getSingleResult();
    }

    public static int getLogicalIdForPhysicalId(EntityManager em, String physicalId) throws Exception {
        Query q = em.createNativeQuery("SELECT distinct LOGICAL_ID FROM product_instance where physical_id = ?");
        q.setParameter(1, physicalId);
        List<Integer> res = q.getResultList();
        if (res.isEmpty()) {
            return 0;
        }
        if (res.size() > 1) {
            throw new Exception("A physical Id cannot map to more than one logical Id");
        } else {
            return res.get(0);
        }
    }

    //Campaign Management.
    public static List<Campaign> getActiveCampaigns(EntityManager em) {
        Query q = em.createNativeQuery("select * from campaign where start_date_time < NOW() and end_date_time > now() and STATUS='AC'", Campaign.class);
        return q.getResultList();
    }

    public static Campaign getCampaign(EntityManager em, int campaignId) {
        Query q = em.createNativeQuery("select * from campaign where campaign_id = ?", Campaign.class);
        q.setParameter(1, campaignId);

        return (Campaign) q.getSingleResult();
    }

    public static Campaign getLockedCampaign(EntityManager em, int campaignId) {
        Campaign campaign = getCampaign(em, campaignId);
        em.refresh(campaign, LockModeType.PESSIMISTIC_READ);
        return campaign;
    }

    public static List<Object[]> getCampaignEnrolData(EntityManager em, Campaign campaign) throws Exception {
        String useDw = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "UseDwForParticipants");
        if (useDw != null && useDw.equals("true")) {
            String dwUser = BaseUtils.getSubProperty("env.cm.dw.config", "user");
            String dwPass = BaseUtils.getSubProperty("env.cm.dw.config", "password");
            String dwHost = BaseUtils.getSubProperty("env.cm.dw.config", "host");
            String dwDbName = BaseUtils.getSubProperty("env.cm.dw.config", "dbname");
            Connection dw = null;
            PreparedStatement ps = null;
            try {
                log.debug("Getting connection to EDW");
                dw = JPAUtils.getMSSQLConnection(dwUser, dwPass, dwHost, dwDbName);
                log.debug("Got connection to EDW");
                long start = 0;
                List<Object[]> ret = new ArrayList<>();
                if (log.isDebugEnabled()) {
                    start = System.currentTimeMillis();
                }
                String query = campaign.getParticipantQuery();
                ps = dw.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 0; i < cols; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    ret.add(row);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Took [{}]ms to get [{}] product instances from EDW in the campaign", System.currentTimeMillis() - start, ret.size());
                }
                return ret;
            } finally {
                if (ps != null) {
                    ps.close();
                }
                if (dw != null) {
                    dw.close();
                }
            }
        } else {
            long start = 0;
            List<Object[]> ret;
            if (log.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }
            String query = campaign.getParticipantQuery();
            Query q = em.createNativeQuery(query);
            List unknownResType = q.getResultList();
            if (!unknownResType.isEmpty()
                    && !unknownResType.get(0).getClass().isArray()) {
                log.debug("This is the old way returning a product instance id");
                ret = new ArrayList<>();
                for (Object obj : unknownResType) {
                    ret.add(new Object[]{obj});
                }
            } else {
                ret = unknownResType;
            }
            if (log.isDebugEnabled()) {
                log.debug("Took [{}]ms to get [{}] product instances in the campaign", System.currentTimeMillis() - start, ret.size());
            }
            return ret;
        }
    }

    public static List<Object[]> getCampaignRemovals(EntityManager em, Campaign campaign) throws Exception {
        String useDw = Utils.getValueFromCRDelimitedAVPString(campaign.getActionConfig(), "UseDwForRemovals");
        if (useDw != null && useDw.equals("true")) {
            String dwUser = BaseUtils.getSubProperty("env.cm.dw.config", "user");
            String dwPass = BaseUtils.getSubProperty("env.cm.dw.config", "password");
            String dwHost = BaseUtils.getSubProperty("env.cm.dw.config", "host");
            String dwDbName = BaseUtils.getSubProperty("env.cm.dw.config", "dbname");
            Connection dw = null;
            PreparedStatement ps = null;
            try {
                log.debug("Getting connection to EDW");
                dw = JPAUtils.getMSSQLConnection(dwUser, dwPass, dwHost, dwDbName);
                log.debug("Got connection to EDW");
                long start = 0;
                List<Object[]> ret = new ArrayList<>();
                if (log.isDebugEnabled()) {
                    start = System.currentTimeMillis();
                }
                String query = campaign.getRemovalQuery();
                ps = dw.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 0; i < cols; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    ret.add(row);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Took [{}]ms to get [{}] product instances from EDW to remove from the campaign", System.currentTimeMillis() - start, ret.size());
                }
                return ret;
            } finally {
                if (ps != null) {
                    ps.close();
                }
                if (dw != null) {
                    dw.close();
                }
            }
        } else {
            long start = 0;
            List<Object[]> ret;
            if (log.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }
            String query = campaign.getRemovalQuery();
            Query q = em.createNativeQuery(query);
            List unknownResType = q.getResultList();
            if (!unknownResType.isEmpty()
                    && !unknownResType.get(0).getClass().isArray()) {
                log.debug("This is the old way returning a product instance id");
                ret = new ArrayList<>();
                for (Object obj : unknownResType) {
                    ret.add(new Object[]{obj});
                }
            } else {
                ret = unknownResType;
            }
            if (log.isDebugEnabled()) {
                log.debug("Took [{}]ms to get [{}] product instances to remove from the campaign", System.currentTimeMillis() - start, ret.size());
            }
            return ret;
        }
    }

    public static int createCampaignRunEntry(EntityManager em, int campaignId, int productInstanceId, String runType) {
        Query q = em.createNativeQuery("insert ignore into campaign_run (CAMPAIGN_ID, PRODUCT_INSTANCE_ID, STATUS, LAST_CHECK_DATE_TIME, ACTION_DATA, CREATED_DATE_TIME, RUN_TYPE) values (?, ?, 'NW', now(), '', now(), ?)");
        q.setParameter(1, campaignId);
        q.setParameter(2, productInstanceId);
        q.setParameter(3, runType);
        return q.executeUpdate();
    }

    public static int removeCampaignRunEntry(EntityManager em, int campaignId, int productInstanceId) {
        Query q = em.createNativeQuery("update campaign_run set STATUS='RV' where CAMPAIGN_ID=? and PRODUCT_INSTANCE_ID=? and STATUS='NW'");
        q.setParameter(1, campaignId);
        q.setParameter(2, productInstanceId);
        return q.executeUpdate();
    }

    public static CampaignTriggerId createCampaignTriggerId(EntityManager em, int campaignRunId, String triggerKey, String enticementKey) {
        CampaignTriggerId campaignTriggerId = new CampaignTriggerId();
        try {
            campaignTriggerId.setCampaignTriggerIdPK(new CampaignTriggerIdPK(campaignRunId, triggerKey));
            campaignTriggerId.setTriggerDateTime(new Date());
            campaignTriggerId.setEnticementKey(enticementKey);
            em.persist(campaignTriggerId);
            em.flush();
            em.refresh(campaignTriggerId);
        } catch (Exception e) {
            log.warn("CampaignTriggerId for [{}] already exists", campaignRunId);
            em.clear();
            campaignTriggerId = JPAUtils.findAndThrowENFE(em, CampaignTriggerId.class, campaignTriggerId.getCampaignTriggerIdPK());
        }

        return campaignTriggerId;

    }

    public static CampaignEnticementId createCampaignEnticementId(EntityManager em, int campaignRunId, String enticementKey, String triggerKey) {
        CampaignEnticementId campaignEnticementId = new CampaignEnticementId();
        try {
            campaignEnticementId.setCampaignEnticementIdPK(new CampaignEnticementIdPK(campaignRunId, enticementKey));
            campaignEnticementId.setEnticementDateTime(new Date());
            campaignEnticementId.setTriggerKey(triggerKey);
            em.persist(campaignEnticementId);
            em.flush();
            em.refresh(campaignEnticementId);
        } catch (Exception e) {
            log.warn("CampaignEnticementId for [{}] already exists", campaignRunId);
            em.clear();
            campaignEnticementId = JPAUtils.findAndThrowENFE(em, CampaignEnticementId.class, campaignEnticementId.getCampaignEnticementIdPK());
        }

        return campaignEnticementId;

    }

    public static boolean triggerIdExists(EntityManager em, String campaignTriggerId, int campaignRunId) {
        Query q = em.createNativeQuery("SELECT count(*) from campaign_trigger_id where campaign_run_id=? and trigger_key=?");
        q.setParameter(1, campaignRunId);
        q.setParameter(2, campaignTriggerId);
        long cnt = (Long) q.getSingleResult();
        return cnt >= 1;
    }

    public static boolean enticementIdExists(EntityManager em, String campaignEnticementId, int campaignRunId) {
        Query q = em.createNativeQuery("SELECT count(*) from campaign_enticement_id where campaign_run_id=? and enticement_key=?");
        q.setParameter(1, campaignRunId);
        q.setParameter(2, campaignEnticementId);
        long cnt = (Long) q.getSingleResult();
        return cnt >= 1;
    }

    public static List<CampaignRun> getCampaignRunsByStatus(EntityManager em, int campaignId, String status, int intervalInSeconds) {

        Query q = em.createNativeQuery("select * from campaign_run where campaign_id = ? and status = ? and last_check_date_time < now() - interval ? second order by last_check_date_time asc limit 100", CampaignRun.class);
        q.setParameter(1, campaignId);
        q.setParameter(2, status);
        q.setParameter(3, intervalInSeconds);

        return (List<CampaignRun>) q.getResultList();

    }

    public static String doesCampaignParticipantSatisfyTrigger(EntityManager em, String triggerQuery, int productInstanceId) {
        long start = 0;
        if (log.isDebugEnabled()) {
            log.debug("Checking if Prod Inst Id [{}] satisfies trigger. [{}]", productInstanceId, triggerQuery);
            start = System.currentTimeMillis();
        }
        String res = "";
        if (!triggerQuery.equals("")) {
            Query q = em.createNativeQuery(triggerQuery);
            q.setParameter(1, productInstanceId);
            res = q.getSingleResult().toString();
        }
        if (log.isDebugEnabled()) {
            log.debug("Trigger query took [{}]ms to run", System.currentTimeMillis() - start);
        }
        return res;
    }

    public static byte[] getPhoto(EntityManager em, String val) {
        Query q = em.createNativeQuery("select * from photograph where photo_guid=?", Photograph.class);
        q.setParameter(1, val);
        return ((Photograph) q.getSingleResult()).getData();
    }

    public static String getReportingTypeForLogicalId(EntityManager em, int logicalId) throws Exception {
        Query q = em.createNativeQuery("select distinct PS.REPORTING_TYPE from product_instance PI join product_specification PS on PI.PRODUCT_SPECIFICATION_ID = PS.PRODUCT_SPECIFICATION_ID where PI.LOGICAL_ID=?");
        q.setParameter(1, logicalId);
        List<String> res = q.getResultList();
        if (res.size() > 1) {
            throw new Exception("A logical SIM cannot have more than one reporting type. SIM must be discarded or left as is");
        } else {
            return res.get(0);
        }
    }

    public static Iterable<UnitCreditServiceMapping> getUnitCreditProductServiceMappings(EntityManager em, int unitCreditSpecificationId) {
        Query q = em.createNativeQuery("select * from unit_credit_service_mapping where unit_credit_specification_id=?", UnitCreditServiceMapping.class);
        q.setParameter(1, unitCreditSpecificationId);
        return q.getResultList();
    }

    public static long getCampaignParticipantsCount(EntityManager em, Campaign campaign) {
        Query q = em.createNativeQuery("select count(*) from campaign_run where campaign_id = ?");
        q.setParameter(1, campaign.getCampaignPK().getCampaignId());
        return (Long) q.getSingleResult();
    }

    public static List<Object[]> getTriggerResults(EntityManager em, String triggerQuery) {
        Query q = em.createNativeQuery(triggerQuery);
        return (List<Object[]>) q.getResultList();
    }

    public static List<Object[]> getEnticementResults(EntityManager em, String enticementQuery) {
        Query q = em.createNativeQuery(enticementQuery);
        return (List<Object[]>) q.getResultList();
    }

    public static CampaignRun getLockedCampaignRun(EntityManager em, int campaignId, int productInstanceId) {
        Query q = em.createNativeQuery("select * from campaign_run where product_instance_id = ? and campaign_id = ? FOR UPDATE", CampaignRun.class);
        q.setParameter(1, productInstanceId);
        q.setParameter(2, campaignId);
        try {
            return (CampaignRun) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public static List<Integer> getProductInstancesOnNetworkForTimePeriod(EntityManager em, int period, String periodType) {
        //Get product instances on network for parameter days
        Query q;
        if (periodType.equalsIgnoreCase("day")) {
            q = em.createNativeQuery("select PI.PRODUCT_INSTANCE_ID from product_instance PI where PI.FIRST_ACTIVITY_DATETIME < now() - interval ? day AND PI.FIRST_ACTIVITY_DATETIME > now() - interval ? day");
            q.setParameter(1, period);
            q.setParameter(2, period + 1);
        } else {//minute
            q = em.createNativeQuery("select PI.PRODUCT_INSTANCE_ID from product_instance PI where PI.FIRST_ACTIVITY_DATETIME < now() - interval ? minute AND PI.FIRST_ACTIVITY_DATETIME > now() - interval ? minute");
            q.setParameter(1, period);
            q.setParameter(2, period + 120);
        }
        return q.getResultList();
    }

    public static List<Integer> getProductInstancesWithNoNetworkActivityForTimePeriod(EntityManager em, int period) {
        Query q = em.createNativeQuery("select PI.PRODUCT_INSTANCE_ID from product_instance PI join product_specification PS on (PS.PRODUCT_SPECIFICATION_ID = PI.PRODUCT_SPECIFICATION_ID) "
                + "where PS.REPORTING_TYPE='LOGICAL SIM' "
                + "and PI.LAST_ACTIVITY_DATETIME >  now() - INTERVAL ? HOUR and PI.LAST_ACTIVITY_DATETIME < now() - INTERVAL ? HOUR "
                + "group by PI.LOGICAL_ID");
        q.setParameter(1, period + 1);
        q.setParameter(2, period);

        return q.getResultList();
    }

    public static boolean doesProductInstanceHaveNoSmileVoiceActivity(EntityManager em, Integer productInstanceId) {
        boolean noSmileVoiceActivity;
        String result = "";
        Query q = em.createNativeQuery("select PI.PRODUCT_INSTANCE_ID from product_instance PI left join service_instance SI on (SI.PRODUCT_INSTANCE_ID = PI.PRODUCT_INSTANCE_ID and SI.SERVICE_SPECIFICATION_ID=100) where PI.PRODUCT_INSTANCE_ID=? and (SI.SERVICE_INSTANCE_ID is null or SI.FIRST_ACTIVITY_DATETIME is null)");
        q.setParameter(1, productInstanceId);

        try {
            result = ((Integer) q.getSingleResult()).toString();
        } catch (javax.persistence.NoResultException nre) {
            result = "";
        }
        if (result.isEmpty()) {
            log.debug("This PI [{}] has had SmileVoice activity");
            noSmileVoiceActivity = false;
        } else {
            log.debug("This PI [{}] has had no SmileVoice activity");
            noSmileVoiceActivity = true;
        }
        return noSmileVoiceActivity;
    }

    public static boolean doesProductInstanceHaveUnlimitedPremiumBundle(EntityManager em, Integer productInstanceId) {
        boolean hasUnlimited;
        String unlimited;
        Query q = em.createNativeQuery("select if(sum(if(UCS.UNIT_CREDIT_SPECIFICATION_ID is null,0,1))>0,'Unlimited','Not Unlimited') AS UNLIMITED from product_instance PI join unit_credit_instance UCI on UCI.PRODUCT_INSTANCE_ID = PI.PRODUCT_INSTANCE_ID left join unit_credit_specification UCS on (UCS.UNIT_CREDIT_SPECIFICATION_ID = UCI.UNIT_CREDIT_SPECIFICATION_ID and (UCS.UNIT_CREDIT_SPECIFICATION_ID = 235 OR UCS.UNIT_CREDIT_SPECIFICATION_ID = 238)) where PI.product_instance_id = ? and UCI.EXPIRY_DATE > now()");
        q.setParameter(1, productInstanceId);
        try {
            unlimited = (String) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            unlimited = "Not Unlimited";
        }
        if (unlimited.equalsIgnoreCase("Unlimited")) {
            log.debug("This PI [{}] has unlimited bundle");
            hasUnlimited = true;
        } else {
            log.debug("This PI [{}] has no unlimited bundle");
            hasUnlimited = false;
        }
        return hasUnlimited;
    }

    public static boolean doesProductInstanceHaveUnlimitedBundle(EntityManager em, Integer productInstanceId) {
        boolean hasUnlimited;
        String unlimited;
        Query q = em.createNativeQuery("select if(sum(if(UCS.UNIT_CREDIT_SPECIFICATION_ID is null,0,1))>0,'Unlimited','Not Unlimited') AS UNLIMITED from product_instance PI join unit_credit_instance UCI on UCI.PRODUCT_INSTANCE_ID = PI.PRODUCT_INSTANCE_ID left join unit_credit_specification UCS on (UCS.UNIT_CREDIT_SPECIFICATION_ID = UCI.UNIT_CREDIT_SPECIFICATION_ID and UCS.CONFIGURATION like '%Unlimited=true%') where PI.product_instance_id = ? and UCI.EXPIRY_DATE > now()");
        q.setParameter(1, productInstanceId);
        try {
            unlimited = (String) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            unlimited = "Not Unlimited";
        }
        if (unlimited.equalsIgnoreCase("Unlimited")) {
            log.debug("This PI [{}] has unlimited bundle");
            hasUnlimited = true;
        } else {
            log.debug("This PI [{}] has no unlimited bundle");
            hasUnlimited = false;
        }
        return hasUnlimited;
    }

    public static List<Integer> getProductInstancesTimePeriodAfterFirstSmileVoiceActivity(EntityManager em, int days) {
        //Get product instances with first Smilevoice activity parameter days ago
        Query q = em.createNativeQuery("select SI.PRODUCT_INSTANCE_ID from service_instance SI where SI.SERVICE_SPECIFICATION_ID=100 and SI.STATUS='AC' and SI.FIRST_ACTIVITY_DATETIME < now() - INTERVAL ? day and SI.FIRST_ACTIVITY_DATETIME > now() - INTERVAL ? day");
        q.setParameter(1, days);
        q.setParameter(2, days + 1);
        return q.getResultList();
    }

    public static List<Integer> getCampaignUnitCreditSpecIds(EntityManager em, String unitCreditSpecWhitelistQuery, int productInstanceId) {
        Query q = em.createNativeQuery(unitCreditSpecWhitelistQuery);
        q.setParameter(1, productInstanceId);
        return q.getResultList();
    }

    public static List<Integer> getProductInstancesOutgoingVoiceCallTimePeriod(EntityManager em, int minutes) {
        //Get product instances with first Smilevoice activity parameter days ago
        Query q = em.createNativeQuery("select SI.PRODUCT_INSTANCE_ID from service_instance SI join account_history AH on AH.SERVICE_INSTANCE_ID = SI.SERVICE_INSTANCE_ID "
                + "where SI.SERVICE_SPECIFICATION_ID = 100 and AH.INCOMING_TRUNK = 0 and AH.START_DATE > now() - interval ? minute AND AH.TRANSACTION_TYPE like 'ext%'");
        q.setParameter(1, minutes);
        return q.getResultList();
    }

    public static boolean hasProductInstanceMadeTwoCalls(EntityManager em, Integer productInstanceId) {
        boolean secondCall;
        Long result;
        Query q = em.createNativeQuery("select if(sum(if(AH.ID is null,0,1))>=2,SI.PRODUCT_INSTANCE_ID,0) AS TWO_CALLS from service_instance SI "
                + "left join account_history AH on AH.SERVICE_INSTANCE_ID = SI.SERVICE_INSTANCE_ID "
                + "where SI.SERVICE_SPECIFICATION_ID = 100 and AH.INCOMING_TRUNK = 0 and AH.START_DATE > now() - interval 90 day and SI.PRODUCT_INSTANCE_ID=? "
                + "AND SI.FIRST_ACTIVITY_DATETIME > now() - INTERVAL 7 DAY AND AH.TRANSACTION_TYPE like 'ext%'");
        // Bit about 7 days is for optimisation to not even look at services that have used voice for more than a week
        q.setParameter(1, productInstanceId);
        try {
            result = (Long) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            result = 0L;
        }
        if (result > 0) {
            log.debug("This PI [{}] has made 2 calls");
            secondCall = true;
        } else {
            log.debug("This PI [{}] has not made 2 calls");
            secondCall = false;
        }
        return secondCall;
    }

    public static void createCampaignListEntries(EntityManager em, int campId, List<String> piIds) {
        Query q = em.createNativeQuery("replace into campaign_list values " + makeInsert(campId, piIds));
        q.executeUpdate();
    }

    private static String makeInsert(int campId, List<String> piIds) {
        StringBuilder sb = new StringBuilder();
        for (String pi : piIds) {
            sb.append("(");
            sb.append(campId);
            sb.append(",");
            sb.append(pi);
            sb.append("),");
        }
        sb.setCharAt(sb.length() - 1, ' ');
        return sb.toString();
    }

    public static void createProductInstanceHistory(EntityManager em, int productInstanceId) {
        if (BaseUtils.getBooleanProperty("env.product.instance.history.keep", false)) {
            log.debug("Deleting old product instance history");
            Query deleteOld = em.createNativeQuery("DELETE from product_instance_history where product_instance_id=? and INSERTED_DATETIME < NOW() - INTERVAL ? DAY");
            deleteOld.setParameter(1, productInstanceId);
            deleteOld.setParameter(2, BaseUtils.getIntProperty("env.product.instance.history.days.keep", 35));
            deleteOld.executeUpdate();
            em.flush();
            log.debug("Inserting product instance history");
            Query historyInsert = em.createNativeQuery(PI_HISTORY_INSERT);
            historyInsert.setParameter(1, productInstanceId);
            historyInsert.executeUpdate();
            em.flush();
            log.debug("Inserted product instance history");
        }
    }

    public static List<Object[]> getProductInstancesforKYCVerification(EntityManager em, int hours) {
        //Get product instances with KYC status unverified when 48Hours reached from creation day
        Query q = em.createNativeQuery("select distinct SI.PRODUCT_INSTANCE_ID, SI.SERVICE_INSTANCE_ID, CP.CUSTOMER_PROFILE_ID, SI.ACCOUNT_ID, SI.CREATED_DATETIME, CP.KYC_STATUS, SI.STATUS \n"
                + "from service_instance SI join customer_profile CP on SI.CUSTOMER_PROFILE_ID = CP.CUSTOMER_PROFILE_ID\n"
                + "where CP.CREATED_DATETIME < now() - interval ? hour\n"
                + "and CP.CREATED_DATETIME > now() - interval ? hour\n"
                + "and SI.SERVICE_SPECIFICATION_ID = 1\n"
                + "and SI.STATUS = 'AC' and (CP.KYC_STATUS like '%U%' or CP.KYC_STATUS like '%P%')");
        q.setParameter(1, hours);
        q.setParameter(2, hours + 2);
        return q.getResultList();
    }

    public static List<Object[]> getProductInstancesforNewCustomer(EntityManager em, int hours) {
        //Get product instances with KYC status verified when 24Hours reached from creation day
        Query q = em.createNativeQuery("select distinct CP.CUSTOMER_PROFILE_ID, PI.PRODUCT_INSTANCE_ID, CP.KYC_STATUS, PI.STATUS "
                + "from product_instance PI join customer_profile CP on CP.CUSTOMER_PROFILE_ID=PI.CUSTOMER_PROFILE_ID "
                + "join service_instance SI on SI.CUSTOMER_PROFILE_ID=PI.CUSTOMER_PROFILE_ID where PI.PRODUCT_INSTANCE_ID not in "
                + "(select distinct PRODUCT_INSTANCE_ID from service_instance si where si.SERVICE_SPECIFICATION_ID in (100,1)) "
                + "and PI.STATUS='AC' and (CP.KYC_STATUS like '%V%') and CP.CREATED_DATETIME > now() - interval ? hour");
        q.setParameter(1, hours);
        return q.getResultList();
    }

    public static List<Object[]> getNewCustomerActivatedSmileVoice(EntityManager em, int hours) {
        //Get product instances with KYC status verified when 24Hours reached from creation day
        Query q = em.createNativeQuery("select distinct CP.CUSTOMER_PROFILE_ID, PI.PRODUCT_INSTANCE_ID, CP.KYC_STATUS, PI.STATUS "
                + "from product_instance PI join customer_profile CP on CP.CUSTOMER_PROFILE_ID=PI.CUSTOMER_PROFILE_ID "
                + "join service_instance SI on SI.CUSTOMER_PROFILE_ID=PI.CUSTOMER_PROFILE_ID where PI.PRODUCT_INSTANCE_ID in "
                + "(select distinct PRODUCT_INSTANCE_ID from service_instance si where si.SERVICE_SPECIFICATION_ID = 100 and si.STATUS='AC' "
                + "and si.CREATED_DATETIME > now() - interval ? hour) and PI.STATUS='AC' and SI.STATUS = 'AC' and (CP.KYC_STATUS like '%V%')");
        q.setParameter(1, hours);
        return q.getResultList();
    }

    public static List<Object[]> getPreVisaExpiryCustomers(EntityManager em, int day) {
        Query q = em.createNativeQuery("select distinct CP.CUSTOMER_PROFILE_ID, CP.KYC_STATUS, PI.STATUS, CP.VISA_EXPIRY_DATE "
                + "from product_instance PI join customer_profile CP on CP.CUSTOMER_PROFILE_ID=PI.CUSTOMER_PROFILE_ID join service_instance SI on "
                + "SI.CUSTOMER_PROFILE_ID=PI.CUSTOMER_PROFILE_ID where PI.PRODUCT_INSTANCE_ID in (select distinct PRODUCT_INSTANCE_ID "
                + "from service_instance si where si.STATUS='AC') and PI.STATUS='AC' and CP.KYC_STATUS = 'V' and CP.VISA_EXPIRY_DATE = CURDATE() + (?);");
        q.setParameter(1, day);

        return q.getResultList();
    }

    public static BigDecimal doesCustomerVisaHasUpdated(EntityManager em, int productInstanceId) {
        Query q = em.createNativeQuery(BaseUtils.getProperty("env.sql.query.visa.date.updated"));
        q.setParameter(1, productInstanceId);

        try {
            return (BigDecimal) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public static void updateCustomerAddress(EntityManager em, Address address) {
        Query q = em.createNativeQuery("UPDATE address set LINE_1 = ?, LINE_2 = ?, ZONE = ?, TOWN = ?, CODE = ?, STATE = ?, COUNTRY = ? where CUSTOMER_PROFILE_ID = ?");
        q.setParameter(1, address.getLine1());
        q.setParameter(2, address.getLine2());
        q.setParameter(3, address.getZone());
        q.setParameter(4, address.getTown());
        q.setParameter(5, address.getCode());
        q.setParameter(6, address.getState());
        q.setParameter(7, address.getCountry());
        q.setParameter(8, address.getCustomerId());
        q.executeUpdate();
        em.flush();
    }

}
