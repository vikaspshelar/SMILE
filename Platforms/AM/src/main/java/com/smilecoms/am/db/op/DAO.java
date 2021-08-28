/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.am.db.op;

import com.smilecoms.am.db.model.AvailableIPAddress;
import com.smilecoms.am.db.model.AvailableNumber;
import com.smilecoms.am.db.model.MnpPortRequest;
import com.smilecoms.am.db.model.Photograph;
import com.smilecoms.am.db.model.RingfencedNumber;
import com.smilecoms.am.np.MnpHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DAO {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);

    public static List<AvailableNumber> getNumbersFromTable(EntityManager em, int priceLimitCents, String pattern, int custId, int orgId, Date releasedBefore) {
        Query query;
        if (releasedBefore == null) {
            releasedBefore = new Date();
        }
        if (custId == -1 && orgId == -1) {
            log.debug("Ignoring ownership");
            query = em.createNativeQuery("select * from available_number where issued=0 and price_cents <= ? and IMPU like ? and (released_datetime is null or released_datetime <= ?) order by IMPU asc LIMIT 500", AvailableNumber.class);
            query.setParameter(1, priceLimitCents);
            query.setParameter(2, pattern);
            query.setParameter(3, releasedBefore);
        } else {
            query = em.createNativeQuery("select * from available_number where issued=0 and price_cents <= ? and IMPU like ? and owned_by_customer_profile_id = ? and owned_by_organisation_id = ?"
                    + " and (released_datetime is null or released_datetime <= ?) and ICCID is null order by IMPU asc LIMIT 500", AvailableNumber.class);
            query.setParameter(1, priceLimitCents);
            query.setParameter(2, pattern);
            query.setParameter(3, custId);
            query.setParameter(4, orgId);
            query.setParameter(5, releasedBefore);
        }
        return (List<AvailableNumber>) query.getResultList();
    }

    public static List<AvailableNumber> getNumberFromTableByICCID(EntityManager em, int priceLimitCents, String iccid, Date releasedBefore) {
        Query query;
        if (releasedBefore == null) {
            releasedBefore = new Date();
        }
        query = em.createNativeQuery("select * from available_number where price_cents <= ? and ICCID = ? and (released_datetime is null or released_datetime <= ?) order by IMPU asc LIMIT 1", AvailableNumber.class);
        query.setParameter(1, priceLimitCents);
        query.setParameter(2, iccid);
        query.setParameter(3, releasedBefore);

        return (List<AvailableNumber>) query.getResultList();
    }

    public static int makeNumberIssued(EntityManager em, String number) {
        Query query = em.createNativeQuery("UPDATE available_number a set a.issued = 1, a.issued_datetime = now() WHERE a.IMPU = ? and a.issued = 0");
        query.setParameter(1, number);
        return query.executeUpdate();
    }

    public static List<String> getIssuedNumbers(EntityManager em, String pattern, int count) {
        Query query = em.createNativeQuery("SELECT number FROM available_number WHERE issued = 1 AND IMPU like ? limit ?", String.class);
        query.setParameter(1, pattern);
        query.setParameter(2, count);
        return query.getResultList();
    }

    public static List<AvailableIPAddress> getIPsFromTable(EntityManager em, String impi, String region, String apnList) {

        List<AvailableIPAddress> availableIPAddresses = null;

        Query query = em.createNativeQuery("select * from available_ip_address where issued=0 and last_impi=? and region=? and apn=? order by IP_ADDRESS asc LIMIT 500", AvailableIPAddress.class);
        query.setParameter(1, impi);
        query.setParameter(2, region);
        query.setParameter(3, apnList);

        availableIPAddresses = (List<AvailableIPAddress>) query.getResultList();
        if (availableIPAddresses.isEmpty()) {
            query = em.createNativeQuery("select * from available_ip_address where issued=0 and (released_datetime is null or released_datetime < now() - interval ? day) and region=? and apn=? order by IP_ADDRESS asc LIMIT 500", AvailableIPAddress.class);
            query.setParameter(1, BaseUtils.getIntProperty("global.static.ip.hold.days"));
            query.setParameter(2, region);
            query.setParameter(3, apnList);
            availableIPAddresses = (List<AvailableIPAddress>) query.getResultList();
        }
        return availableIPAddresses;
    }

    public static int makeIPIssued(EntityManager em, String ip, String impi) {
        Query query = em.createNativeQuery("UPDATE available_ip_address a set a.issued = 1, a.issued_datetime = now(), a.last_impi = ? WHERE a.ip_address = ? and a.issued = 0");
        query.setParameter(1, impi);
        query.setParameter(2, ip);
        return query.executeUpdate();
    }

    public static int makeIPFree(EntityManager em, String ip, String impi) {

        Query query = em.createNativeQuery("UPDATE available_ip_address a set a.issued = 0, a.released_datetime = now(), a.issued_datetime = null, a.last_impi = ? WHERE a.ip_address = ?");
        query.setParameter(1, impi);
        query.setParameter(2, ip);
        return query.executeUpdate();
    }

    public static List<String> getIssuedIPs(EntityManager em, String pattern, int count) {
        Query query = em.createNativeQuery("SELECT ip_address FROM available_ip_address WHERE issued = 1 AND ip_address like ? limit ?", String.class);
        query.setParameter(1, pattern);
        query.setParameter(2, count);
        return query.getResultList();
    }

    public static MnpPortRequest savePortingRequestToDB(EntityManager em, MnpPortRequest dbPortInRequest) throws Exception {

        try {
            dbPortInRequest.setLastModified(new Date()); // Set last modified to now, always.

            MnpPortRequest existingRecord = em.find(MnpPortRequest.class, dbPortInRequest.getMnpPortRequestPK());

            if (existingRecord == null) {// Create new record
                em.persist(dbPortInRequest);
                em.flush();
                em.refresh(dbPortInRequest);
            } else {
                // Update existing
                MnpHelper.synchMnpPortRequests(dbPortInRequest, existingRecord);
                existingRecord.setLastModified(new Date());
                em.merge(existingRecord);
                em.flush();
            }
        } catch (Exception ex) {
            log.error("Error while saving porting request to DB", ex);
            throw ex;
        } finally {
        }

        return dbPortInRequest;

    }

    public static RingfencedNumber getRingfencedNumber(EntityManager em, long number) {
        return em.find(RingfencedNumber.class, number);
    }

    public static void addRingfencedNumber(EntityManager em, RingfencedNumber rfNumber) {
        try {
            RingfencedNumber existingRecord = em.find(RingfencedNumber.class, rfNumber.getNumber());

            if (existingRecord == null) {// Create new record
                em.persist(rfNumber);
                em.flush();
            }

        } catch (javax.persistence.PersistenceException ex) {
            log.error("Error while saving ringfence number {} to  database [{}].", rfNumber.getNumber(), ex.getMessage());

        }
    }

    public static void removeRingfencedNumber(EntityManager em, long number) {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.ringfenced_number WHERE NUMBER = ?");
        q.setParameter(1, number);
        q.executeUpdate();
        em.flush();
    }

    public static void insertAvailableNumber(EntityManager em, AvailableNumber availableNumber) {

        Query q = em.createNativeQuery("INSERT INTO available_number (IMPU, ISSUED, ISSUED_DATETIME, PRICE_CENTS, OWNED_BY_CUSTOMER_PROFILE_ID, OWNED_BY_ORGANISATION_ID, RELEASED_DATETIME) "
                + " VALUES (?, 0, null, 0, ?, ?, null)"
                + " ON DUPLICATE KEY UPDATE "
                + " ISSUED=0, ISSUED_DATETIME=null, RELEASED_DATETIME=null, OWNED_BY_CUSTOMER_PROFILE_ID= ?, OWNED_BY_ORGANISATION_ID= ?");

        q.setParameter(1, availableNumber.getIMPU());
        q.setParameter(2, availableNumber.getOwnedByCustomerProfileId());
        q.setParameter(3, availableNumber.getOwnedByOrganisationId());
        q.setParameter(4, availableNumber.getOwnedByCustomerProfileId());
        q.setParameter(5, availableNumber.getOwnedByOrganisationId());
        q.executeUpdate();
        em.flush();

    }

    public static MnpPortRequest getMnpPortRequest(EntityManager em, String portingOrderId, String portingDirection) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.mnp_port_request WHERE PORTING_ORDER_ID = ? and PORTING_DIRECTION = ?", MnpPortRequest.class);
        q.setParameter(1, portingOrderId);
        q.setParameter(2, portingDirection);
        try {
            return (MnpPortRequest) q.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    /* public static MnpPortIRequestState addPortInRequestState(EntityManager em, Long portingOrderId, Long messageId, String senderId, IPortState state) {
        MnpPortInRequestState dbPortingState = new MnpPortInRequestState();
        dbPortingState.setPortingOrderId(portingOrderId);
        dbPortingState.setMessageId(messageId);
        dbPortingState.setPortingState(state);
        dbPortingState.setSenderId(senderId);
        dbPortingState.setEventDatetime(new Date());

        em.persist(dbPortingState);
        em.flush();
        em.refresh(dbPortingState);

        return dbPortingState;
    } */

 /*
    public static IPortState getCurrentState(EntityManager em, String portingOrderId) throws Exception {
        // Retrieve the current porting request
        MnpPortRequest mnpPortRequest = getMnpPortRequest(em, portingOrderId);
        return    mnpPortRequest.getNpState();
    }*/
    public static Collection<MnpPortRequest> getPortOrdersByPortOrderId(EntityManager em, String portOrderId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.mnp_port_request WHERE PORTING_ORDER_ID = ?", MnpPortRequest.class);
        q.setParameter(1, portOrderId);
        return q.getResultList();
    }

    public static Collection<MnpPortRequest> getPortOrdersByPortOrderIdAndPortingDirection(EntityManager em, String portOrderId, String portingDirection) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.mnp_port_request WHERE PORTING_ORDER_ID = ? and PORTING_DIRECTION = ?", MnpPortRequest.class);

        q.setParameter(1, portOrderId);
        q.setParameter(2, portingDirection);

        return q.getResultList();
    }

    // Search by CustomerProfileId, PortingDirection, PortingState and ProcessingState
    public static Collection<MnpPortRequest> getPortOrdersByCustomerProfileIdPortingDirectionPortingState(
            EntityManager em, int customerProfileId, String portingDirection,
            String portingState) {

        Query q = em.createNativeQuery("SELECT * FROM SmileDB.mnp_port_request WHERE CUSTOMER_PROFILE_ID = ? and PORTING_DIRECTION = ? AND NP_STATE= ?", MnpPortRequest.class);

        q.setParameter(1, customerProfileId);
        q.setParameter(2, portingDirection);
        q.setParameter(3, portingState);
        return q.getResultList();
    }

    // Search by CustomerProfileId, PortingDirection, PortingState and ProcessingState
    public static Collection<MnpPortRequest> getPortOrdersByOrganisationIdPortingDirectionPortingState(
            EntityManager em, int customerProfileId, String portingDirection,
            String portingState) {

        Query q = em.createNativeQuery("SELECT * FROM SmileDB.mnp_port_request WHERE ORGANISATION_ID = ? and PORTING_DIRECTION = ? AND NP_STATE= ?", MnpPortRequest.class);

        q.setParameter(1, customerProfileId);
        q.setParameter(2, portingDirection);
        q.setParameter(3, portingState);
        return q.getResultList();
    }

    public static Collection<MnpPortRequest> getPortOrdersByCustomerProfileId(EntityManager em, int customerProfileId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.mnp_port_request WHERE CUSTOMER_PROFILE_ID = ?", MnpPortRequest.class);
        q.setParameter(1, customerProfileId);
        return q.getResultList();
    }

    public static Collection<MnpPortRequest> getPortOrdersByOrganisationId(EntityManager em, int customerProfileId) {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.mnp_port_request WHERE ORGANISATION_ID = ?", MnpPortRequest.class);
        q.setParameter(1, customerProfileId);
        return q.getResultList();
    }

    public static List<com.smilecoms.xml.schema.am.Photograph> getPortOrderForms(EntityManager em, String portingOrderId) throws Exception {
        Query q = em.createNativeQuery("SELECT * FROM SmileDB.photograph WHERE PORTING_ORDER_ID=?", Photograph.class);
        q.setParameter(1, portingOrderId);
        List<Photograph> photos = q.getResultList();
        List<com.smilecoms.xml.schema.am.Photograph> ret = new ArrayList<>();
        for (Photograph photo : photos) {
            com.smilecoms.xml.schema.am.Photograph cp = new com.smilecoms.xml.schema.am.Photograph();
            cp.setPhotoGuid(photo.getPhotoGuid());
            cp.setPhotoType(photo.getPhotoType());
            cp.setData(Utils.encodeBase64(photo.getData()));
            ret.add(cp);
            log.debug("Got a photo from DB with GUID [{}]", cp.getPhotoGuid());
        }
        return ret;
    }

    public static void setPortOrderForms(EntityManager em, List<com.smilecoms.xml.schema.am.Photograph> contractDocuments, String portingOrderId) throws Exception {
        Query q = em.createNativeQuery("DELETE FROM SmileDB.photograph WHERE PORTING_ORDER_ID = ?");
        q.setParameter(1, portingOrderId);
        q.executeUpdate();
        for (com.smilecoms.xml.schema.am.Photograph p : contractDocuments) {
            if (!p.getPhotoGuid().isEmpty() && !p.getPhotoType().isEmpty()) {
                Photograph photo = new Photograph();
                photo.setPortingOrderId(portingOrderId);
                photo.setPhotoGuid(p.getPhotoGuid());
                photo.setPhotoType(p.getPhotoType());
                if (p.getData() == null || p.getData().length() < 1000) {
                    throw new Exception("Invalid/empty photograph -- " + photo.getPhotoType());
                }
                photo.setData(Utils.decodeBase64(p.getData()));
                photo.setPhotoHash(HashUtils.md5(photo.getData()));
                Query q2 = em.createNativeQuery("select concat(ifnull(porting_order_id,''), ifnull(customer_profile_id,''),ifnull(organisation_id,'')) FROM SmileDB.photograph WHERE PHOTO_HASH = ? limit 1");
                q2.setParameter(1, photo.getPhotoHash());
                List res = q2.getResultList();
                if (!res.isEmpty()) {
                    log.error("Duplicate photograph -- {} exists on a port order with id {}.", photo.getPhotoType(), res.get(0));
                }
                em.persist(photo);
            }
        }
        em.flush();
    }
}
