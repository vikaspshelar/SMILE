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
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author paul
 */
public interface IUnitCredit {

    public void initialise(EntityManager em, UnitCreditInstance uci, UnitCreditSpecification ucs, List<IUnitCredit> listUCIsIn);

    UCReserveResult reserve(
            RatingKey ratingKey,
            BigDecimal unitsToReserve,
            BigDecimal originalUnitsToReserve,
            IAccount acc,
            String sessionId,
            Date eventTimetamp,
            byte[] request,
            int reservationSecs,
            RatingResult ratingResult,
            ServiceInstance serviceInstance,
            boolean isLastResort,
            boolean checkOnly,
            String unitType,
            String location
    );

    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge,
            BigDecimal originalUnitsToCharge,
            IAccount acc,
            RatingResult ratingResult,
            boolean isLastResort,
            String unitType,
            Date eventTimetamp);

    public boolean canTakeACharge();

    public boolean hasCurrentUnitsLeft();

    public boolean supportsLastResortProcessing();

    public boolean hasAvailableUnitsLeft();

    public boolean neverBeenUsed();

    public boolean paidFor();

    public BigDecimal getCurrentUnitsLeft();

    public BigDecimal getAvailableUnitsLeft();

    public BigDecimal getSpecUnits();

    public BigDecimal getUnitsAtStart();

    public int getPriority();

    public UnitCreditSpecification getUnitCreditSpecification();

    public String getUnitCreditName();

    public String getUnitType();

    public void setUnitCreditUnitsRemaining(BigDecimal unitsRemaining);

    public void setAuxCounter1(BigDecimal aux1);

    public void setAuxCounter2(BigDecimal aux2);

    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception;

    public void checkProvisionRules(IAccount acc, Date startDate, boolean verifyOnly, int productInstanceId) throws Exception;

    public void doPostProvisionProcessing(boolean verifyOnly) throws Exception;
    
    
    public Date getPurchaseDate();

    public Date getStartDate();

    public Date getEndDate();

    public Date getExpiryDate();

    public int getUnitCreditInstanceId();

    public int getProductInstanceId();

    public long getAccountId();

    public String getInfoValue(String attribute);

    public void setInfoValue(String attribute, String value);

    public void extendExpiryAndEndDate(int count, boolean fromNow, boolean forceDays, boolean writeOriginalToInfo, Date actualDate);

    public IAccount getAccount() throws Exception;

    // Lifecycle events
    public void doPostDepletionProcessing();

    // May be called more than once
    public void doPostExpiryProcessing();
    
    public void doNoSpecialLevyBundleProcessing();

    public void doPostEndDateProcessing();

    public void do1WeekPreExpiryProcessing();

    public void do30DaysPreExpiryProcessing();

    public void do1DayPreExpiryProcessing();

    public void do2DaysPreExpiryProcessing();

    public void do3DaysPreExpiryProcessing();

    public void do4HourPreExpiryProcessing();

    public void do8HourPreExpiryProcessing();

    public void doPostRolloverProcessing();

    public void doCheckUnitCreditConstraints();

    public void doOnFirstUseProcessing();
    
    public BigDecimal getOOBUnitRate();

    public void split(long targetAccountId, BigDecimal units, int targetProductInstanceId) throws Exception;

    public String getPropertyFromConfig(String prop);

    public boolean getBooleanPropertyFromConfig(String propName);

    public UnitCreditInstance getUnitCreditInstance();

    public int getIntPropertyFromConfig(String propName);

    public long getLongPropertyFromConfig(String propName);

    public boolean ignoreWhenNoUnitsLeft();
  
}
