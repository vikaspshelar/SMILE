/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pvs.db.op;

import com.smilecoms.pvs.PrepaidVoucherSystem.STRIP_STATUS;
import com.smilecoms.pvs.db.model.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class DAO {

    private static final Logger log = LoggerFactory.getLogger(DAO.class);

    public static boolean unredeemedStripExists(EntityManager em, String encryptedCodeInHex) {
        Query q = em.createNativeQuery("SELECT count(*) FROM prepaid_strip PS WHERE PS.ENCRYPTED_PIN_HEX = ? AND PS.STATUS != ?");
        q.setParameter(1, encryptedCodeInHex);
        q.setParameter(2, STRIP_STATUS.RE.toString());
        Long cnt = (Long) q.getSingleResult();
        if (cnt > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static PrepaidStrip getUnredeemedStrip(EntityManager em, String encryptedPIN) {
        Query q = em.createNativeQuery("SELECT * FROM prepaid_strip WHERE ENCRYPTED_PIN_HEX = ? AND STATUS = ?", PrepaidStrip.class);
        q.setParameter(1, encryptedPIN);
        q.setParameter(2, STRIP_STATUS.DC.toString());
        return (PrepaidStrip) q.getSingleResult();
    }

    public static PrepaidStrip getStrip(EntityManager em, int stripId) {
        Query q = em.createNativeQuery("SELECT * FROM prepaid_strip WHERE PREPAID_STRIP_ID = ?", PrepaidStrip.class);
        q.setParameter(1, stripId);
        return (PrepaidStrip) q.getSingleResult();
    }

    public static PrepaidStrip getStripUsingEncryptedPINHex(EntityManager em, String stripPinHex) {
        Query q = em.createNativeQuery("SELECT * FROM prepaid_strip WHERE ENCRYPTED_PIN_HEX = ?", PrepaidStrip.class);
        q.setParameter(1, stripPinHex);
        return (PrepaidStrip) q.getSingleResult();
    }

    
    public static PrepaidVoucherSystemLock getVoucherLockForAccount(EntityManager em, long accountId) {
        Query q = em.createNativeQuery("SELECT * FROM prepaid_voucher_system_lock PVL WHERE PVL.ACCOUNT_ID = ?", PrepaidVoucherSystemLock.class);
        q.setParameter(1, accountId);
        return (PrepaidVoucherSystemLock) q.getSingleResult();
    }

    public static int getStripCountForInvoiceDataAndValue(EntityManager em, String invoiceData, double stripValueCents) {
        Query q = em.createNativeQuery("SELECT count(*) FROM prepaid_strip WHERE INVOICE_DATA = ? and VALUE_CENTS = ?");
        q.setParameter(1, invoiceData);
        q.setParameter(2, stripValueCents);
        Long cnt = (Long) q.getSingleResult();
        return cnt.intValue();
    }
    
   public static int deletePrepaidVoucherLock(EntityManager em, long accountId) {
        Query q = em.createNativeQuery("delete from prepaid_voucher_system_lock where account_id=?;");
        q.setParameter(1, accountId);
        return q.executeUpdate();
    }
    
    public static int getStripCountForIdRange(EntityManager em, int startingPrepaidStripId, int endingPrepaidStripId) {
         Query q = em.createNativeQuery("SELECT count(*) from prepaid_strip WHERE PREPAID_STRIP_ID >= ? AND PREPAID_STRIP_ID <= ? ");
        q.setParameter(1, startingPrepaidStripId);
        q.setParameter(2, endingPrepaidStripId);
        Long cnt = (Long) q.getSingleResult();
        return cnt.intValue();
    }

    public static int getStripCountForSaleIdAndStatusFilter(EntityManager em, String  saleId, String statusFilter) {
         Query q = em.createNativeQuery("select count(*) from prepaid_strip WHERE INVOICE_DATA=? and status in (" + statusFilter + ");");
        q.setParameter(1, saleId);
        Long cnt = (Long) q.getSingleResult();
        return cnt.intValue();
    }
    
    public static int getStripCountForStartingIdAndBoxSize(EntityManager em, int startingPrepaidStripId, int boxSize) {
        Query q = em.createNativeQuery("select count(*) from " +
                    "    (select prepaid_strip_id from prepaid_strip where prepaid_strip_id >= ? order by prepaid_strip_id asc limit ?) as T1");
        q.setParameter(1, startingPrepaidStripId);
        q.setParameter(2, boxSize);
        Long lastStripId = (Long) q.getSingleResult();
        return lastStripId.intValue();
    }

    public static int getLastStripIdInTheRangeForBox(EntityManager em, int startingPrepaidStripId, int boxSize) {
        Query q = em.createNativeQuery("select max(prepaid_strip_id) from " +
                    "    (select prepaid_strip_id from prepaid_strip where prepaid_strip_id >= ? order by prepaid_strip_id asc limit ?) as T1");
        q.setParameter(1, startingPrepaidStripId);
        q.setParameter(2, boxSize);
        Integer lastStripId = (Integer) q.getSingleResult();
        return lastStripId.intValue();
    }
    
    public static void bulkUpdate(EntityManager em, int startid, int endid, STRIP_STATUS newStatus, String invoiceData) throws Exception {
        // First verify that startid and endid exist

        Query q = em.createNativeQuery("SELECT count(*) from prepaid_strip WHERE PREPAID_STRIP_ID = ? OR PREPAID_STRIP_ID = ?");
        q.setParameter(1, startid);
        q.setParameter(2, endid);
        Long cnt = (Long) q.getSingleResult();
        if ((startid != endid) && cnt.intValue() != 2) {
            throw new Exception("Invalid start or end serial number");
        }
        if ((startid == endid) && cnt.intValue() != 1) {
            throw new Exception("Invalid start or end serial number");
        }
        if (startid > endid) {
            throw new Exception("Invalid start or end serial number");
        }
        // Now check how many strips are in this range
        q = em.createNativeQuery("SELECT STATUS, VALUE_CENTS, count(*) from prepaid_strip WHERE PREPAID_STRIP_ID >= ? AND PREPAID_STRIP_ID <= ? GROUP BY STATUS, VALUE_CENTS");
        q.setParameter(1, startid);
        q.setParameter(2, endid);
        List statuses = q.getResultList();
        if (statuses.isEmpty()) {
            throw new Exception("Bulk update changes no strips");
        }
        if (statuses.size() > 1) {
            throw new Exception("All existing strips in a bulk update must have the same status and value");
        }
        // by now there must be one status. Validate the change is ok
        Object[] row = (Object[]) statuses.get(0);
        String currentStatus = (String) row[0];
        Long rows = (Long) row[2];
        validateStatus(currentStatus, newStatus.toString());

        q = em.createNativeQuery("UPDATE prepaid_strip SET STATUS=?, INVOICE_DATA = ? where PREPAID_STRIP_ID >= ? AND PREPAID_STRIP_ID <= ? AND STATUS=?");
        q.setParameter(1, newStatus.toString());
        q.setParameter(2, invoiceData);
        q.setParameter(3, startid);
        q.setParameter(4, endid);
        q.setParameter(5, currentStatus);
        int rowsUpdated = q.executeUpdate();
        if (rows.intValue() != rowsUpdated) {
            try {
                InitialContext ic = new InitialContext();
                SessionContext sctxLookup = (SessionContext) ic.lookup("java:comp/EJBContext");
                sctxLookup.setRollbackOnly();
            } catch (Exception e) {
                log.error("Error trying to setrollbackonly on CMP transaction", e);
            }
            throw new Exception("Incorrect number of rows updated");
        }
    }

    public static void validateStatus(String oldstatus, String newstatus) throws Exception {
        log.debug("validateStatus current is [{}] and new is [{}]", oldstatus, newstatus);
        STRIP_STATUS newStatus = STRIP_STATUS.valueOf(newstatus);
        if (oldstatus == null) {
            if (newStatus.equals(STRIP_STATUS.GE)) {
                return;
            } else {
                throw new Exception("Invalid Prepaid Strip Status Change");
            }
        }

        STRIP_STATUS currentStatus = STRIP_STATUS.valueOf(oldstatus);

        switch (currentStatus) {
            case GE:
                if (!newStatus.equals(STRIP_STATUS.EX)) {
                    throw new Exception("Invalid Prepaid Strip Status Change");
                }
                break;
            case EX:
                if (!newStatus.equals(STRIP_STATUS.WH)) {
                    throw new Exception("Invalid Prepaid Strip Status Change");
                }
                break;
            case WH:
                if (!newStatus.equals(STRIP_STATUS.DC)) {
                    throw new Exception("Invalid Prepaid Strip Status Change");
                }
                break;
            case DC:
                if (!newStatus.equals(STRIP_STATUS.RI)) {
                    throw new Exception("Invalid Prepaid Strip Status Change");
                }
                break;
            case RI:
                if (!newStatus.equals(STRIP_STATUS.RE)) {
                    throw new Exception("Invalid Prepaid Strip Status Change");
                }
                break;
            case RE:
                if (!newStatus.equals(STRIP_STATUS.RE)) {
                    throw new Exception("Invalid Prepaid Strip Status Change");
                }
                break;
        }
    }

    public static List<PrepaidStrip> getPrepaidStripsForInvoiceAndStatuss(EntityManager em, int saleId, String status) {
        Query q = em.createNativeQuery(
                "select  prepaid_strip.* from prepaid_strip where invoice_data=? and status=?;", PrepaidStrip.class); 
        q.setParameter(1, saleId);
        q.setParameter(2, status);
        return q.getResultList();
    }
    
    public static Map <Integer, String> getUnitCreditNames(EntityManager em) {
        
        Map <Integer, String> map = new HashMap<>();
        
        Query q = em.createNativeQuery("select unit_credit_specification_id, unit_credit_name from unit_credit_specification");
        
        long start = System.currentTimeMillis();
        
        List <Object[]> ucs = q.getResultList();
        
        for(Object [] us : ucs) {
            map.put((Integer) us[0], (String) us[1]);
        }
        
        return map;
    }
    
    public static boolean checkUnitCreditSpecExists(EntityManager em, int specId) {

        Query q = em.createNativeQuery("select count(*) from unit_credit_specification where unit_credit_specification_id=?"
                + " and configuration like '%AllowVoucherCreation=true%'"
                + " and available_to > now()");

        q.setParameter(1, specId);
        Long value = (Long) q.getSingleResult();
        return value.intValue() > 0;
    }

    public static int getLastStripIdGenerated(EntityManager em) {
        Query q = em.createNativeQuery("select max(prepaid_strip_id) from prepaid_strip");
        Integer lastStripId = (Integer) q.getSingleResult();
        return lastStripId;
    }
}