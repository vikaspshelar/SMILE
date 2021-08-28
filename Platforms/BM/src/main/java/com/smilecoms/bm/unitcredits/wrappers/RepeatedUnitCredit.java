/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UCReserveResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author paul
 *
 * Provisions one or more of another unit credit so that they kick in one after
 * the other
 *
 */
public class RepeatedUnitCredit implements IUnitCredit {

    private static final Logger log = LoggerFactory.getLogger(RepeatedUnitCredit.class);
    private EntityManager em;
    private UnitCreditSpecification ucs;
    private Date purchaseDate = null;

    @Override
    public void initialise(EntityManager em, UnitCreditInstance uci, UnitCreditSpecification ucs, List<IUnitCredit> listUCIsIn) {
        this.em = em;
        this.ucs = ucs;
    }

    @Override
    public boolean canTakeACharge() {
        return false;
    }

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {
        int actualUCSpecId = getIntPropertyFromConfig("UnitCreditSpecificationIdToProvision");
        int numberToProvision = getIntPropertyFromConfig("Count");
        int daysGap = getIntPropertyFromConfig("StartDaysGap");
        log.debug("Repeated unit credit is count to provision [{}] instances of UC spec Id [{}]", numberToProvision, actualUCSpecId);
        startDate = new Date();
        for (int i = 0; i < numberToProvision; i++) {
            IUnitCredit uc = UnitCreditManager.provisionUnitCredit(
                    em,
                    actualUCSpecId,
                    productInstanceId,
                    acc,
                    startDate,
                    verifyOnly,
                    extTxid,
                    posCentsPaidEach / numberToProvision,
                    posCentsDiscountEach / numberToProvision,
                    saleLineId,
                    info);
            if (daysGap >= 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(uc.getStartDate());
                cal.add(Calendar.DATE, daysGap);
                startDate = cal.getTime();
            } else {
                // Next one starts when last one expires
                startDate = uc.getExpiryDate();
            }
            purchaseDate = uc.getPurchaseDate();
        }

    }

    @Override
    public void doPostProvisionProcessing(boolean verifyOnly) {
    }

    @Override
    public Date getPurchaseDate() {
        return (purchaseDate == null ? new Date() : purchaseDate);
    }

    @Override
    public int getUnitCreditInstanceId() {
        return 0;
    }

    @Override
    public String getUnitCreditName() {
        return ucs.getUnitCreditName();
    }

    /*
     * 
     * NOTHING BELOW HERE SHOULD EVER BE CALLED BECAUSE THEY ARE ONLY NEEDED IF AN INSTANCE IS CREATED
     * 
     */
    @Override
    public UCReserveResult reserve(RatingKey ratingKey, BigDecimal unitsToReserve, BigDecimal originalUnitsToReserve, IAccount acc, String sessionId, Date eventTimetamp, byte[] request, int reservationSecs, RatingResult ratingResult, ServiceInstance serviceInstance, boolean isLastResort, boolean checkOnly, String unitType, String location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge,
            BigDecimal originalUnitsToCharge,
            IAccount acc,
            RatingResult ratingResult,
            boolean isLastResort, String unitType, Date eventTimetamp) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasCurrentUnitsLeft() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UnitCreditSpecification getUnitCreditSpecification() {
        return ucs;
    }

    @Override
    public boolean hasAvailableUnitsLeft() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigDecimal getCurrentUnitsLeft() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigDecimal getAvailableUnitsLeft() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPriority() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getUnitType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date getStartDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public Date getEndDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date getExpiryDate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getProductInstanceId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getAccountId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void do1WeekPreExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void doPostExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void doPostDepletionProcessing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void do30DaysPreExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void do3DaysPreExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void do2DaysPreExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void setAuxCounter1(BigDecimal aux1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void setAuxCounter2(BigDecimal aux2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void do1DayPreExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportsLastResortProcessing() {
        return false;
    }

    @Override
    public String getInfoValue(String attribute) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setInfoValue(String attribute, String value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean paidFor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean neverBeenUsed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void extendExpiryAndEndDate(int days, boolean fromNow, boolean forceDays, boolean writeOriginalToInfo, Date actualDate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigDecimal getSpecUnits() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IAccount getAccount() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void checkProvisionRules(IAccount acc, Date startDate, boolean verifyOnly, int productInstanceId) throws Exception {

    }

    @Override
    public BigDecimal getOOBUnitRate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setUnitCreditUnitsRemaining(BigDecimal unitsRemaining) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void split(long targetAccountId, BigDecimal units, int targetProductInstanceId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UnitCreditInstance getUnitCreditInstance() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getIntPropertyFromConfig(String propName) {
        String val = getPropertyFromConfig(propName);
        if (val == null) {
            return -1;
        }
        return Integer.parseInt(val);
    }

    @Override
    public long getLongPropertyFromConfig(String propName) {
        String val = getPropertyFromConfig(propName);
        if (val == null) {
            return -1;
        }
        return Long.parseLong(val);
    }

    public double getDoublePropertyFromConfig(String propName) {
        String val = getPropertyFromConfig(propName);
        if (val == null) {
            return -1;
        }
        return Double.parseDouble(val);
    }

    public BigDecimal getBigDecimalPropertyFromConfigZeroIfMissing(String propName) {
        String val = getPropertyFromConfig(propName);
        if (val == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Double.parseDouble(val));
    }

    public BigDecimal getBigDecimalPropertyFromConfig(String propName) {
        return BigDecimal.valueOf(getDoublePropertyFromConfig(propName));
    }

    @Override
    public boolean getBooleanPropertyFromConfig(String propName) {
        String val = getPropertyFromConfig(propName);
        if (val == null) {
            return false;
        }
        return val.equalsIgnoreCase("true");
    }

    private Map<String, String> propCache = null;

    @Override
    public String getPropertyFromConfig(String propName) {
        if (propCache == null) {
            propCache = new HashMap<>();
            StringTokenizer stValues = new StringTokenizer(ucs.getConfiguration(), "\r\n");
            while (stValues.hasMoreTokens()) {
                String row = stValues.nextToken();
                if (!row.isEmpty()) {
                    String[] bits = row.split("=");
                    String name = bits[0].trim();
                    String value;
                    if (bits.length == 1) {
                        value = "";
                    } else {
                        value = bits[1].trim();
                    }
                    propCache.put(name, value);
                }
            }
        }
        return propCache.get(propName);
    }

    @Override
    public BigDecimal getUnitsAtStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean ignoreWhenNoUnitsLeft() {
        return true;
    }
    
    @Override
    public void doCheckUnitCreditConstraints() {
    }

    

    @Override
    public void do4HourPreExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void do8HourPreExpiryProcessing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void doPostRolloverProcessing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void doPostEndDateProcessing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void doNoSpecialLevyBundleProcessing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void doOnFirstUseProcessing() {
        
    }
    
}
