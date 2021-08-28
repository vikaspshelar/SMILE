/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.unitcredits.wrappers;

import com.smilecoms.bm.BalanceManager;
import com.smilecoms.bm.EventHelper;
import com.smilecoms.bm.charging.AccountFactory;
import com.smilecoms.bm.charging.IAccount;
import com.smilecoms.bm.charging.ServiceRules;
import com.smilecoms.bm.charging.ServiceSpecHelper;
import com.smilecoms.bm.db.model.ServiceInstance;
import com.smilecoms.bm.db.model.UnitCreditInstance;
import com.smilecoms.bm.db.model.UnitCreditSpecification;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.bm.rating.RatingResult;
import com.smilecoms.bm.unitcredits.UCChargeResult;
import com.smilecoms.bm.unitcredits.UnitCreditManager;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.beans.CustomerBean;
import com.smilecoms.commons.util.ExceptionManager;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.schema.bm.PlatformContext;
import com.smilecoms.xml.schema.bm.ProvisionUnitCreditLine;
import com.smilecoms.xml.schema.bm.ProvisionUnitCreditRequest;
import com.smilecoms.xml.schema.bm.RatingKey;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public abstract class BaseUnitCredit implements IUnitCredit {

    protected EntityManager em;
    protected UnitCreditInstance uci;
    protected UnitCreditSpecification ucs;
    protected List<IUnitCredit> listUCIsIn;
    protected boolean accountSupportsOOB;

    private static final Logger log = LoggerFactory.getLogger(BaseUnitCredit.class);

    @Override
    public void initialise(EntityManager em, UnitCreditInstance uci, UnitCreditSpecification ucs, List<IUnitCredit> listUCIsIn) {
        this.em = em;
        this.uci = uci;
        this.ucs = ucs;
        this.listUCIsIn = listUCIsIn;
    }

    /*
     * The cents deducted from the wallet (or paid for by POS) for this bundle, divided by the number of units in the bundle as set at provisioning time
     */
    @Override
    public BigDecimal getOOBUnitRate() {
        return uci.getRevenueCentsPerUnit();
    }

    @Override
    public boolean hasCurrentUnitsLeft() {
        return DAO.isPositive(getCurrentUnitsLeft());
    }

    @Override
    public boolean canTakeACharge() {
        return hasCurrentUnitsLeft();
    }

    @Override
    public boolean neverBeenUsed() {
        return (uci.getUnitsRemaining().compareTo(uci.getUnitsAtStart()) == 0);
    }

    @Override
    public boolean paidFor() {
        return DAO.isPositive(uci.getPOSCentsCharged());
    }

    @Override
    public BigDecimal getCurrentUnitsLeft() {
        return uci.getUnitsRemaining();
    }
    protected BigDecimal availableUnitsLeft;

    @Override
    public BigDecimal getAvailableUnitsLeft() {
        if (availableUnitsLeft == null) {
            availableUnitsLeft = DAO.getUnitCreditsAvailableUnits(em, uci);
        }
        return availableUnitsLeft;
    }

    @Override
    public boolean hasAvailableUnitsLeft() {
        return DAO.isPositive(getAvailableUnitsLeft());
    }

    @Override
    public String getUnitType() {
        return ucs.getUnitType();
    }

    @Override
    public UnitCreditSpecification getUnitCreditSpecification() {
        return ucs;
    }

    @Override
    public void doPostDepletionProcessing() {
        if (getBooleanPropertyFromConfig("DoNotSendDepletedWarningAtAll")) {
            return;
        }

        if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
            String warnOOB = getPropertyFromConfig("PostExpiryOrDepletedWarnOOBEventSubType");
            if (warnOOB != null) {
                try {
                    if (getAccount().getStatus() == 0) { //"Allows All Usages"
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PostExpiryOrDepletedWarnOOBEventSubType"));
                    }
                } catch (Exception ex) {
                    log.error("Error while trying to send event PostExpiryOrDepletedWarnOOBEventSubType on unit credit instance [{}],  error [{}]", uci.getUnitCreditInstanceId(),
                            ex);
                }
            }
        }

        boolean mustNotWarnIfOtherUCsExist = getBooleanPropertyFromConfig("NoLowWarningWhenOthersExist");
        if (mustNotWarnIfOtherUCsExist) {
            if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                if (getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays") > -1
                        && !DAO.isProductInstanceOlderThanPeriodInDays(em, uci.getProductInstanceId(), getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays"))) {
                    int days = getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays");
                    String subType = "DepletionEventSubType";
                    if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                        subType = "BonusDepletionEventSubType";
                    }
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1_" + days));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2_" + days));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "3_" + days));
                } else {
                    String subType = "DepletionEventSubType";
                    if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                        subType = "BonusDepletionEventSubType";
                    }
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "3"));
                }
            } else {
                log.debug("Not sending UC Depleated warning as other UCs with the same unit type exist");
            }
        } else {

            if (getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays") > -1
                    && !DAO.isProductInstanceOlderThanPeriodInDays(em, uci.getProductInstanceId(), getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays"))) {
                int days = getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays");
                String subType = "DepletionEventSubType";
                if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                    subType = "BonusDepletionEventSubType";
                }
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1_" + days));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2_" + days));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "3_" + days));
            } else {
                String subType = "DepletionEventSubType";
                if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                    subType = "BonusDepletionEventSubType";
                }
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1"));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2"));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "3"));
            }
        }
    }

    @Override
    public void doPostRolloverProcessing() {
        if (getBooleanPropertyFromConfig("DoNotSendRolloverNotificationsAtAll")) {
            return;
        }

        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("RolloverEventSubType1"), Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "RolloverCount"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("RolloverEventSubType2"), Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "RolloverCount"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("RolloverEventSubType3"), Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "RolloverCount"));
    }
    
    @Override
    public void doOnFirstUseProcessing() {
        if (getBooleanPropertyFromConfig("DoNotSendOnFirstUseNotificationsAtAll")) {
            return;
        }

        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OnFirstUseEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OnFirstUseEventSubType2"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OnFirstUseEventSubType3"));
    
    }
    

    @Override
    public void doPostEndDateProcessing() {
        if (getBooleanPropertyFromConfig("DoNotSendRolloverNotificationsAtAll")) {
            return;
        }

        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PostEndDateEventSubType1"), Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "RolloverCount"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PostEndDateEventSubType2"), Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "RolloverCount"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PostEndDateEventSubType3"), Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "RolloverCount"));
    }

    @Override
    public void doNoSpecialLevyBundleProcessing() {
        if (getBooleanPropertyFromConfig("DoNotSendMissingSpecialLevyWarningAtAll")) {
            return;
        }

        SimpleDateFormat howFrequent = new SimpleDateFormat("yyyyMMdd");

        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("MissingSpecialLevyBundleWarningSubType1"), howFrequent.format(new Date()));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("MissingSpecialLevyBundleWarningSubType2"), howFrequent.format(new Date()));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("MissingSpecialLevyBundleWarningSubType3"), howFrequent.format(new Date()));
    }

    public void doPostPeriodicLimitDepletionProcessing() {
        if (getBooleanPropertyFromConfig("DoNotSendDepletedWarningAtAll")) {
            return;
        }

        // int daysSinceUciGotPurchased = Utils.getDaysBetweenDates(uci.getPurchaseDate(), new Date());
        SimpleDateFormat howFrequent = new SimpleDateFormat("yyyyMMdd");

        boolean mustNotWarnIfOtherUCsExist = getBooleanPropertyFromConfig("NoLowWarningWhenOthersExist");
        if (mustNotWarnIfOtherUCsExist) {
            if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitDepletionEventSubType1"), howFrequent.format(new Date()));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitDepletionEventSubType2"), howFrequent.format(new Date()));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitDepletionEventSubType3"), howFrequent.format(new Date()));
            } else {
                log.debug("Not sending PeriodicLimit UC Depleated warning as other UCs with the same unit type exist");
            }
        } else {
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitDepletionEventSubType1"), howFrequent.format(new Date()));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitDepletionEventSubType2"), howFrequent.format(new Date()));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitDepletionEventSubType3"), howFrequent.format(new Date()));
        }
    }

    public void doPeriodicLimitPeriodStartProcessing() { //Alert when the period for a periodic limit bundle starts
        if (getBooleanPropertyFromConfig("DoNotSendPeriodStartWarningAtAll")) {
            return;
        }

        SimpleDateFormat howFrequent = new SimpleDateFormat("yyyyMMdd");

        boolean mustNotWarnIfOtherUCsExist = getBooleanPropertyFromConfig("NoPeriodStartWarningWhenOthersExist");
        if (mustNotWarnIfOtherUCsExist) {
            if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitPeriodStartEventSubType1"), howFrequent.format(new Date()));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitPeriodStartEventSubType2"), howFrequent.format(new Date()));
                EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitPeriodStartEventSubType3"), howFrequent.format(new Date()));
            } else {
                log.debug("Not sending PeriodicLimit UC Depleated warning as other UCs with the same unit type exist");
            }
        } else {
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitPeriodStartEventSubType1"), howFrequent.format(new Date()));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitPeriodStartEventSubType2"), howFrequent.format(new Date()));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PeriodicLimitPeriodStartEventSubType3"), howFrequent.format(new Date()));
        }
    }

    private void checkIfOnlyALittleLeftInUCI() {

        try {

            int firstWarningPercent = getIntPropertyFromConfig("FirstWarningPercent");
            int secondWarningPercent = getIntPropertyFromConfig("SecondWarningPercent");
            int thirdWarningPercent = getIntPropertyFromConfig("ThirdWarningPercent");
            int fourthWarningPercent = getIntPropertyFromConfig("FourthWarningPercent");
            int oobWarningPercent = getIntPropertyFromConfig("OOBWarningPercent");
            long unitsAtStartDiv100 = uci.getUnitsAtStart().longValue() / 100;
            long firstWarning = unitsAtStartDiv100 * firstWarningPercent;
            long secondWarning = unitsAtStartDiv100 * secondWarningPercent;
            long thirdWarning = unitsAtStartDiv100 * thirdWarningPercent;
            long fourthWarning = unitsAtStartDiv100 * fourthWarningPercent;
            long oobWarning = unitsAtStartDiv100 * oobWarningPercent;
            boolean mustNotWarnIfOtherUCsExist = getBooleanPropertyFromConfig("NoLowWarningWhenOthersExist");
            long unitsActuallyRemaining = uci.getUnitsRemaining().longValue();

            // Warn customers who have OOB enabled on their accounts before the last bundle depletes.
            if (oobWarningPercent != -1 && unitsActuallyRemaining <= oobWarning
                    && accountSupportsOOB) {
                if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OOBWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OOBWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OOBWarningEventSubType2"));
                }
            } else if (fourthWarningPercent != -1 && unitsActuallyRemaining <= fourthWarning) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType2"));
                }
            } else if (thirdWarningPercent != -1 && unitsActuallyRemaining <= thirdWarning) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType2"));
                }
            } else if (secondWarningPercent != -1 && unitsActuallyRemaining <= secondWarning) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType2"));
                }
            } else if (firstWarningPercent != -1 && unitsActuallyRemaining <= firstWarning) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType2"));
                }
            }
        } catch (Exception e) {
            log.warn("Error in checkIfOnlyALittleLeft", e);
        }

        try {

            long firstWarningAmount = getLongPropertyFromConfig("FirstWarningAmount");
            long secondWarningAmount = getLongPropertyFromConfig("SecondWarningAmount");
            long thirdWarningAmount = getLongPropertyFromConfig("ThirdWarningAmount");
            long fourthWarningAmount = getLongPropertyFromConfig("FourthWarningAmount");
            long oobWarningAmount = getLongPropertyFromConfig("OOBWarningAmount");
            boolean mustNotWarnIfOtherUCsExist = getBooleanPropertyFromConfig("NoLowWarningWhenOthersExist");
            long unitsActuallyRemaining = uci.getUnitsRemaining().longValue();

            // Warn customers who have OOB enabled on their accounts before the last bundle depletes.
            if (oobWarningAmount != -1 && unitsActuallyRemaining <= oobWarningAmount
                    && accountSupportsOOB) {

                if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {

                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OOBWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OOBWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("OOBWarningEventSubType2"));
                }

            } else if (fourthWarningAmount != -1 && unitsActuallyRemaining <= fourthWarningAmount) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FourthWarningEventSubType2"));
                }
            } else if (thirdWarningAmount != -1 && unitsActuallyRemaining <= thirdWarningAmount) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("ThirdWarningEventSubType2"));
                }
            } else if (secondWarningAmount != -1 && unitsActuallyRemaining <= secondWarningAmount) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("SecondWarningEventSubType2"));
                }
            } else if (firstWarningAmount != -1 && unitsActuallyRemaining <= firstWarningAmount) {
                if (mustNotWarnIfOtherUCsExist) {
                    if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType1"));
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType2"));
                    }
                } else {
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType1"));
                    EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("FirstWarningEventSubType2"));
                }
            }
        } catch (Exception e) {
            log.warn("Error in checkIfOnlyALittleLeft", e);
        }
    }

    private boolean imTheOnlyOneOfMyTypeWithUnitsInThisList() {
        if (listUCIsIn == null) {
            return true;
        }
        boolean ImAloneInThisList = true;
        boolean foundMyself = false;
        for (IUnitCredit uc : listUCIsIn) {
            if (uc.equals(this)) {
                log.debug("Found myself in the list. Now all UC's in the list would be used after me");
                foundMyself = true;
                continue;
            }
            if (!foundMyself) {
                continue;
            }
            if (uc.getUnitType().equals(this.getUnitType()) && DAO.isPositive(uc.getCurrentUnitsLeft())) {
                log.debug("Other UC in same list after me has same unit type as me and has units left. Will not send warning");
                ImAloneInThisList = false;
                break;
            } else {
                log.debug("Other UC in same list after me has different unit type than me or has no units left");
            }
        }
        return ImAloneInThisList;
    }

    @Override
    public void setUnitCreditUnitsRemaining(BigDecimal unitsRemaining) {
        availableUnitsLeft = null;
        Date now = new Date();

        if (uci.getCrossoverDate() == null && DAO.isZeroOrPositive(uci.getUnitsRemaining()) && DAO.isNegative(unitsRemaining)) {
            log.debug("Unit credit has crossed over into the negative. Setting crossoverdate");
            uci.setCrossoverDate(now);
        }
        if (unitsRemaining.compareTo(uci.getUnitsAtStart()) != 0) {
            if (uci.getFirstUsedDate() == null) {
                log.debug("Unit credit is being used for the first time. Setting firstuseddate");
                doOnFirstUseProcessing();
                uci.setFirstUsedDate(now);
            }
            uci.setLastUsedDate(now);
        }
        uci.setUnitsRemaining(unitsRemaining);
        em.persist(uci);
        checkForPostDepletionProcessing();
    }

    @Override
    public void setAuxCounter1(BigDecimal aux1) {
        uci.setAuxCounter1(aux1);
        em.persist(uci);

        checkForPeriodicLimitDepletionProcessing();

    }

    @Override
    public void setAuxCounter2(BigDecimal aux2) {
        uci.setAuxCounter2(aux2);
        em.persist(uci);
    }

    private static final BigDecimal window = new BigDecimal(-110000000); // 110MB check window to avoid writing every time charging happens when in the negative

    protected void checkForPostDepletionProcessing() {
        if (DAO.isZeroOrNegative(uci.getUnitsRemaining()) && uci.getUnitsRemaining().compareTo(window) > 0) {
            doPostDepletionProcessing();
        }
        checkIfOnlyALittleLeftInUCI();

    }

    protected void checkForPeriodicLimitDepletionProcessing() {

        BigDecimal periodicLimitUnits = getBigDecimalPropertyFromConfigZeroIfMissing("PeriodicLimitUnits");

        if (periodicLimitUnits.equals(BigDecimal.ZERO)) {
            return;
        }

        if (uci.getAuxCounter1() == null) {
            return;
        }

        BigDecimal periodicUnitsRemaining = periodicLimitUnits.subtract(uci.getAuxCounter1());

        if (DAO.isZeroOrNegative(periodicUnitsRemaining) && periodicUnitsRemaining.compareTo(window) > 0) {
            doPostPeriodicLimitDepletionProcessing();
        }
    }

    @Override
    public int getPriority() {
        int priority = ucs.getPriority();
        String dayOfWeekPriority = getPropertyFromConfig("UCDayOfWeekPriority");

        if (dayOfWeekPriority != null && !dayOfWeekPriority.isEmpty()) {
            Calendar et = Calendar.getInstance();
            int calDayOfWeek = et.get(Calendar.DAY_OF_WEEK);
            String[] priorityArray = dayOfWeekPriority.split(";");
            for (String dayPriority : priorityArray) {
                String[] dayPriorityArray = dayPriority.split(",");
                int cfgDayOfWeek = Integer.parseInt(dayPriorityArray[0]);
                int cfgPriority = Integer.parseInt(dayPriorityArray[1]);
                if (calDayOfWeek == cfgDayOfWeek) {
                    priority = cfgPriority;
                    log.debug("This priority [{}] is based on day of week [{}]", cfgPriority, cfgDayOfWeek);
                    break;
                }
            }
        }
        return priority;
    }

    @Override
    public String getUnitCreditName() {
        return ucs.getUnitCreditName();
    }

    @Override
    public BigDecimal getSpecUnits() {
        return new BigDecimal(ucs.getUnits());
    }

    @Override
    public BigDecimal getUnitsAtStart() {
        return uci.getUnitsAtStart();
    }

    @Override
    public String toString() {
        StringBuilder desc = new StringBuilder();
        desc.append("Id:");
        desc.append(uci.getUnitCreditInstanceId());
        desc.append(" SpecId:");
        desc.append(uci.getUnitCreditSpecificationId());
        desc.append(" Name:");
        desc.append(ucs.getUnitCreditName());
        desc.append(" AccId:");
        desc.append(uci.getAccountId());
        desc.append(" revenue cents/unit:");
        desc.append(uci.getRevenueCentsPerUnit());
        desc.append(" baseline cents/unit:");
        desc.append(uci.getBaselineCentsPerUnit());
        desc.append(" Expiry:");
        desc.append(uci.getExpiryDate());
        desc.append(" Purchased:");
        desc.append(uci.getPurchaseDate());
        desc.append(" Starts:");
        desc.append(uci.getStartDate());
        desc.append(" Current Units:");
        desc.append(getCurrentUnitsLeft());
        desc.append(" Available Units:");
        desc.append(getAvailableUnitsLeft());
        desc.append(" Priority:");
        desc.append(ucs.getPriority());
        desc.append(" Wrapper:");
        desc.append(ucs.getWrapperClass());
        desc.append(" Unit Type:");
        desc.append(getUnitType());
        desc.append(" PI id:");
        desc.append(uci.getProductInstanceId());

        return desc.toString();
    }

    @Override
    public Date getPurchaseDate() {
        return uci.getPurchaseDate();
    }

    @Override
    public Date getStartDate() {
        return uci.getStartDate();
    }

    @Override
    public Date getEndDate() {
        return uci.getEndDate();
    }

    @Override
    public int getUnitCreditInstanceId() {
        return uci.getUnitCreditInstanceId();
    }

    @Override
    public Date getExpiryDate() {
        return uci.getExpiryDate();
    }

    @Override
    public void provision(IAccount acc, int productInstanceId, Date startDate, boolean verifyOnly,
            String extTxid, double posCentsPaidEach, double posCentsDiscountEach, int saleLineId, String info) throws Exception {

        log.debug("In provision");
        checkProvisionRules(acc, startDate, verifyOnly, productInstanceId);
        /*
         If the bundle size is B bytes and the normal cost is N UGX and the discount amount is D UGX and the extra units is E bytes and the Free cents already included is F 
         Normal revenue rate: 
         (N-D)/(B+E) UGX per Byte
         Free revenue rate:
         (F + D + (E*N/B))/(B+E)
         */
        double units = ucs.getUnits();
        double normalRevenueRate;

        double baselineRate;
        if (getBooleanPropertyFromConfig("BaselineOnMarketPrice")) {
            baselineRate = (posCentsPaidEach + posCentsDiscountEach) / units;
        } else if (getBooleanPropertyFromConfig("BaselineOnPaidPrice")) {
            baselineRate = (posCentsPaidEach) / units;
        } else {
            int baselineCents = getIntPropertyFromConfig("BaselineCents");
            if (baselineCents > 0) {
                baselineRate = baselineCents / units;
            } else {
                // Default treatment is BaselineOnMarketPrice
                baselineRate = (posCentsPaidEach + posCentsDiscountEach) / units;
            }
        }

        if (BaseUtils.getBooleanProperty("env.bm.uc.revenue.recognition.nett", true)) {
            normalRevenueRate = (posCentsPaidEach) / units;
        } else {
            normalRevenueRate = (posCentsPaidEach + posCentsDiscountEach) / units;
        }

        int expireHour = getIntPropertyFromConfig("ExpiresAtThisHourOfDay");
        int startHour = getIntPropertyFromConfig("StartsAtThisHourOfDay");

        if (startDate == null) {
            startDate = new Date();
        }

        int startDelayInDays = getIntPropertyFromConfig("NumberOfDaysToDelayStartDate");
        if (startDelayInDays > 0) {
            Calendar calStarDate = Calendar.getInstance();
            calStarDate.add(Calendar.DATE, startDelayInDays);
            startDate = calStarDate.getTime();
        }

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        if (startHour >= 0 && expireHour >= 0) {

            if (startHour <= currentHour && currentHour > expireHour) { // Logic for bundles that must start at a specific hour of day.
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);
                cal.set(Calendar.HOUR_OF_DAY, startHour);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startDate = cal.getTime();
            }
        }

        if (getBooleanPropertyFromConfig("ExpiresAnyCurrentlyActiveOfSameSpec")) {
            // Get list of all active bundles on this account
            List<UnitCreditInstance> accountsCurrentUCs = DAO.getUnitCreditInstances(em, acc.getAccountId());
            for (UnitCreditInstance uciFromList : accountsCurrentUCs) {
                IUnitCredit ucOfSameType = UnitCreditManager.getWrappedUnitCreditInstance(em, uciFromList, null);
                if (ucOfSameType.getUnitCreditSpecification().getUnitCreditSpecificationId().equals(getUnitCreditSpecification().getUnitCreditSpecificationId())) {
                    uciFromList.setExpiryDate(new Date()); // Expire the bundle now.
                    em.persist(uciFromList);
                    em.flush();
                }
            }
        }

        if (getBooleanPropertyFromConfig("StartWhenLastOneOfSameSpecExpires")) {
            startDate = Utils.getMaxDate(startDate, DAO.getMaxExpiryDateByUnitCreditSpecId(em, acc.getAccountId(), getUnitCreditSpecification().getUnitCreditSpecificationId()));
            log.debug("StartWhenLastOneOfSameSpecExpires is true. Start date is [{}]", startDate);
        }

        if (getBooleanPropertyFromConfig("StartWhenLastOneOfSameSpecInSameSaleExpires")) { // As requested by Tanzania
            startDate = Utils.getMaxDate(startDate, DAO.getMaxExpiryDateByUnitCreditSpecIdAndSaleRowId(em, acc.getAccountId(), getUnitCreditSpecification().getUnitCreditSpecificationId(), saleLineId));
            log.debug("StartWhenLastOneOfSameSpecExpires is true. Start date is [{}]", startDate);
        }

        Date expDate = null;
        if (getBooleanPropertyFromConfig("ExpiryDateIsAvailableToDate")) {
            expDate = ucs.getAvailableTo();
        } else if (getBooleanPropertyFromConfig("ExpiryDateIsSameAsParent")) {
            // Get the parent's expiry date
            // Retrieve parent unit credit and get its expiry date;
            String strParentUciId = Utils.getValueFromCRDelimitedAVPString(info, "ParentUCI");

            if (strParentUciId != null) {
                expDate = DAO.getUnitCreditInstance(em, Integer.valueOf(strParentUciId)).getExpiryDate();
            } else {
                log.error("ExpiryDateIsSameAsParent is true, but no ParentUCI set on the Info field of unit credit instance, sale_row_id [{}]", saleLineId);
            }
        }

        // Set expiry date to the latest unit credit of type 
        String specIdsForExpiryDate = UnitCreditManager.getPropertyFromConfig(ucs, "SetExpiryDateToTheLatestOfSpecIds");
        if (specIdsForExpiryDate != null) {
            log.debug("SpecIds to be used for setting Expiry Date are [{}]", specIdsForExpiryDate);
            // Defaults to now() if no unit credit was found.
            expDate = DAO.getMaxExpiryDateByUnitCreditSpecIds(em, acc.getAccountId(), specIdsForExpiryDate);
        }

        if (expDate == null) { // We still do not have expiry date set, use standard logic.
            Calendar expiryDate = Calendar.getInstance();
            expiryDate.setTime(startDate);
            if (getBooleanPropertyFromConfig("ValidityIsMonths")) {
                expiryDate.add(Calendar.MONTH, ucs.getValidityDays());
            } else if (getBooleanPropertyFromConfig("ValidityIsHours")) {
                expiryDate.add(Calendar.HOUR, ucs.getValidityDays());
            } else {
                expiryDate.add(Calendar.DATE, ucs.getValidityDays());
            }
            expDate = expiryDate.getTime();
        }

        // Functionality requested by NG as part of HBT-8192
        Date enDate = null;
        boolean expiresAtSameTimeIfPurchasedBeforeHourOfDay = getBooleanPropertyFromConfig("ExpiresAtSameTimeIfPurchasedBeforeHourOfDay");

        int expireOnDay = getIntPropertyFromConfig("ExpiresOnThisDayOfWeek");

        if (expireOnDay > 0) {
            if (expireHour == -1) {
                expireHour = 0;
            }

            Calendar endTime = Calendar.getInstance(); // Set it to now
            currentHour = endTime.get(Calendar.HOUR_OF_DAY);
            int diff = expireOnDay - endTime.get(Calendar.DAY_OF_WEEK);

            if (diff <= 0) {
                diff += 7;
            }

            endTime.add(Calendar.DAY_OF_MONTH, diff);

            if ((expiresAtSameTimeIfPurchasedBeforeHourOfDay && (currentHour >= expireHour))
                    || !expiresAtSameTimeIfPurchasedBeforeHourOfDay) {
                endTime.set(Calendar.HOUR_OF_DAY, expireHour);
                endTime.set(Calendar.MINUTE, 0);
                endTime.set(Calendar.SECOND, 0);
                enDate = endTime.getTime();
            }
            expDate = enDate;

        } else if (expireHour >= 0) { // This bundle's validity must always be days.
            if (getBooleanPropertyFromConfig("ValidityIsMonths") || getBooleanPropertyFromConfig("ValidityIsHours")) {
                throw new Exception("Invalid validity period for bundle of this type - unit credit name " + ucs.getUnitCreditName());
            }

            Calendar endTime = Calendar.getInstance();
            currentHour = endTime.get(Calendar.HOUR_OF_DAY);

            endTime.set(Calendar.HOUR_OF_DAY, expireHour);
            endTime.set(Calendar.MINUTE, 0);
            endTime.set(Calendar.SECOND, 0);

            if (now.before(endTime) && ucs.getValidityDays() <= 1) { // Set the bundle to expire at 06:00 am today
                enDate = endTime.getTime();

            } else {

                if (expiresAtSameTimeIfPurchasedBeforeHourOfDay && currentHour < expireHour) {
                    endTime = Calendar.getInstance();
                    endTime.add(Calendar.DAY_OF_MONTH, ucs.getValidityDays());
                } else {
                    endTime.add(Calendar.DAY_OF_MONTH, ucs.getValidityDays());
                    endTime.set(Calendar.HOUR_OF_DAY, expireHour);
                    endTime.set(Calendar.MINUTE, 0);
                    endTime.set(Calendar.SECOND, 0);
                    enDate = endTime.getTime();
                }
                enDate = endTime.getTime();

            }

            expDate = enDate;

        } else if (getBooleanPropertyFromConfig("EndDateIsAvailableToDate")) {
            enDate = ucs.getAvailableTo();
        } else if (getBooleanPropertyFromConfig("EndDateIsSameAsParent")) {
            // Get the parent's end date
            // Retrieve parent unit credit and get its end date;
            String strParentUciId = Utils.getValueFromCRDelimitedAVPString(info, "ParentUCI");

            if (strParentUciId != null) {
                enDate = DAO.getUnitCreditInstance(em, Integer.valueOf(strParentUciId)).getEndDate();
            } else {
                log.error("EndDateIsSameAsParent is true, but no ParentUCI set on the Info field of unit credit specification [{}]", ucs.getUnitCreditSpecificationId());
            }
        }

        // Set expiry date to the latest unit credit of type 
        String specIdsForEndDate = UnitCreditManager.getPropertyFromConfig(ucs, "SetEndDateToTheLatestOfSpecIds");
        if (specIdsForEndDate != null) {
            log.debug("SpecIds to be used for setting End Date are [{}]", specIdsForEndDate);
            // Defaults to now() is no spec ids exists on this account.
            enDate = DAO.getMaxEndDateByUnitCreditSpecIds(em, acc.getAccountId(), specIdsForEndDate);
        }

        if (enDate == null) {
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(startDate);
            if (getBooleanPropertyFromConfig("ValidityIsMonths")) {
                endDate.add(Calendar.MONTH, ucs.getUsableDays());
            } else if (getBooleanPropertyFromConfig("ValidityIsHours")) {
                endDate.add(Calendar.HOUR, ucs.getUsableDays());
            } else {
                endDate.add(Calendar.DATE, ucs.getUsableDays());
            }
            enDate = endDate.getTime();
        }

        if (enDate.after(expDate)) {
            enDate = expDate;
        }

        Date revenueFirstDate = null;
        Date revenueLastDate = null;
        Double revenuePerDay = null;

        if (getBooleanPropertyFromConfig("NoRevenueOnUsage")) {
            log.debug("This unit credit does not recognise revenue as its used");
            if (getBooleanPropertyFromConfig("RecRevDaily")) {
                revenueFirstDate = startDate;
                revenueLastDate = expDate;
                long seconds = Utils.getSecondsBetweenDates(revenueFirstDate, revenueLastDate);
                revenuePerDay = normalRevenueRate * units / (seconds / 86400);
                if (log.isDebugEnabled()) {
                    log.debug("Revenue must be recognised daily starting on [{}] and ending on [{}] which is [{}] seconds at [{}] per day totalling [{}]",
                            revenueFirstDate, revenueLastDate, seconds, revenuePerDay, revenuePerDay * seconds / 86400);
                }
            }
            normalRevenueRate = 0;
            baselineRate = 0;
        }

        //For dynamic bonus bundles only
        String strDynamicSize = Utils.getValueFromCRDelimitedAVPString(info, "DynamicUCSizeInMB");
        if(strDynamicSize !=  null) {
            //Check if this is a Gift bundle
            if(getBooleanPropertyFromConfig("Gift"))  {
                //  This is a dynamic bonus bundle so ignore what is on the spec and use the size specified at runtime.
                // Convert MBs to bytes
                units = Double.valueOf(strDynamicSize) * 1000 * 1000;
            } else {
                throw new Exception ("The setting DynamicUCSize is only applicable to gift bundles.");
            }
        }
        
        uci = DAO.createUnitCreditInstance(em, ucs.getUnitCreditSpecificationId(), acc, expDate, units, normalRevenueRate, baselineRate,
                productInstanceId, posCentsPaidEach, posCentsDiscountEach, startDate, info, extTxid, saleLineId, enDate, revenueFirstDate, revenueLastDate, revenuePerDay);

        doPostProvisionProcessing(verifyOnly);
    }

    @Override
    public long getAccountId() {
        return uci.getAccountId();
    }

    @Override
    public int getProductInstanceId() {
        return uci.getProductInstanceId();
    }

    @Override
    public void do1WeekPreExpiryProcessing() {
        log.debug("UCI [{}] called for 1 week pre expiry processing", uci.getUnitCreditInstanceId());
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1WeekPreExpiryEventSubType"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1WeekPreExpiryEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1WeekPreExpiryEventSubType2"));
    }

    @Override
    public void doCheckUnitCreditConstraints() {
        log.debug("UCI [{}] called for 1check bundle constraints", uci.getUnitCreditInstanceId());
    }

    @Override
    public void doPostExpiryProcessing() {
        log.debug("UCI [{}] called for post expiry processing", uci.getUnitCreditInstanceId());

        if (getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays") > -1
                && !DAO.isProductInstanceOlderThanPeriodInDays(em, uci.getProductInstanceId(), getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays"))) {
            int days = getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays");
            String subType = "PostExpiryEventSubType";
            if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                subType = "BonusPostExpiryEventSubType";
            }
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "_" + days));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1_" + days));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2_" + days));
        } else {
            String subType = "PostExpiryEventSubType";
            if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                subType = "BonusPostExpiryEventSubType";
            }
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2"));
        }

        if (imTheOnlyOneOfMyTypeWithUnitsInThisList()) {
            String warnOOB = getPropertyFromConfig("PostExpiryOrDepletedWarnOOBEventSubType");
            if (warnOOB != null) {
                try {
                    if (getAccount().getStatus() == 0) { //"Allows All Usages"
                        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("PostExpiryOrDepletedWarnOOBEventSubType"));
                    }
                } catch (Exception ex) {
                    log.error("Error while trying to send event PostExpiryOrDepletedWarnOOBEventSubType on unit credit instance [{}],  error [{}]", uci.getUnitCreditInstanceId(),
                            ex);
                }
            }
        }

        String stickyDPIRules = getPropertyFromConfig("StickyDPIRules");
        if (stickyDPIRules != null) {

            try {
                boolean foundAnother = false;
                for (UnitCreditInstance otherUCIs : DAO.getAccountsActiveUnitCredits(em, getAccount())) {
                    if (otherUCIs.getUnitCreditSpecificationId() == this.getUnitCreditSpecification().getUnitCreditSpecificationId()) {
                        foundAnother = true;
                        break;
                    }
                }
                if (!foundAnother) {
                    log.debug("This UC has sticky DPI rules that must be removed [{}]", stickyDPIRules);
                    ServiceInstance serviceInstance = DAO.getDataServiceInstanceForProductInstance(em, uci.getProductInstanceId());
                    ServiceRules rules = new ServiceRules();
                    rules.setSystemDefinedDpiRulesToRemove(Utils.getSetFromCommaDelimitedString(stickyDPIRules));
                    ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
                } else {
                    log.debug("Found another UC with same spec so not removing sticky rules");
                }
            } catch (Exception e) {
                log.warn("Error removing sticky dpi rules", e);
                new ExceptionManager(log).reportError(e);
            }
        }
    }

    @Override
    public void do30DaysPreExpiryProcessing() {
        log.debug("UCI [{}] called for 30 days pre expiry processing", uci.getUnitCreditInstanceId());
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("30DaysPreExpiryEventSubType"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("30DaysPreExpiryEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("30DaysPreExpiryEventSubType2"));
    }

    @Override
    public void do3DaysPreExpiryProcessing() {
        log.debug("UCI [{}] called for 3 days pre expiry processing", uci.getUnitCreditInstanceId());
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("3DaysPreExpiryEventSubType"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("3DaysPreExpiryEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("3DaysPreExpiryEventSubType2"));
    }

    @Override
    public void do1DayPreExpiryProcessing() {
        log.debug("UCI [{}] called for 1 day pre expiry processing", uci.getUnitCreditInstanceId());
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1DayPreExpiryEventSubType"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1DayPreExpiryEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("1DayPreExpiryEventSubType2"));
    }

    @Override
    public void do4HourPreExpiryProcessing() {
        log.debug("UCI [{}] called for 4 Hours pre expiry processing", uci.getUnitCreditInstanceId());
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("4HoursPreExpiryEventSubType"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("4HoursPreExpiryEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("4HoursPreExpiryEventSubType2"));
    }

    @Override
    public void do8HourPreExpiryProcessing() {
        log.debug("UCI [{}] called for 8 Hours pre expiry processing", uci.getUnitCreditInstanceId());
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("8HoursPreExpiryEventSubType"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("8HoursPreExpiryEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("8HoursPreExpiryEventSubType2"));
    }

    @Override
    public void do2DaysPreExpiryProcessing() {
        log.debug("UCI [{}] called for 2 days pre expiry processing", uci.getUnitCreditInstanceId());
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("2DaysPreExpiryEventSubType"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("2DaysPreExpiryEventSubType1"));
        EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig("2DaysPreExpiryEventSubType2"));
    }

    @Override
    public boolean supportsLastResortProcessing() {
        return false;
    }

    @Override
    public String getInfoValue(String attribute) {
        List<String> avpList = Utils.getListFromCRDelimitedString(uci.getInfo());
        String lookup = attribute + "=";
        for (String avp : avpList) {
            if (avp.startsWith(lookup)) {
                String[] bits = avp.split("=", 2);
                if (bits.length == 1) {
                    return "";
                }
                return bits[1];
            }
        }
        return null;
    }

    @Override
    public UCChargeResult charge(
            RatingKey ratingKey,
            BigDecimal unitsToCharge,
            BigDecimal originalUnitsToCharge,
            IAccount acc,
            RatingResult ratingResult,
            boolean isLastResort,
            String unitType,
            Date eventTimetamp) {

        if (neverBeenUsed() && DAO.isPositive(unitsToCharge) && getBooleanPropertyFromConfig("ExpiryBaseOnFirstUsed")) {
            extendExpiryAndEndDate(ucs.getValidityDays(), true, false, false, null);
        }

        if (acc != null) {
            accountSupportsOOB = acc.supportsOperationType(IAccount.ACCOUNT_OPERATION_TYPE.MONETARY_CHARGE);
        } else {
            accountSupportsOOB = false;
        }

        UCChargeResult res = new UCChargeResult();
        res.setUnitCreditInstanceId(getUnitCreditInstanceId());
        
        res.setPaidForUsage((DAO.isPositive(uci.getPOSCentsCharged()) && DAO.isPositive(uci.getUnitsRemaining()))
                                || (getBooleanPropertyFromConfig("AlwaysSetUsageAsPaidForUsage"))); //As requested by UG - Kyookya bonus bundles must be marked as revenue generating even though they were given away for free

        return res;
    }

    @Override
    public void setInfoValue(String attribute, String value) {
        if (value == null) {
            return;
        }
        uci.setInfo(Utils.setValueInCRDelimitedAVPString(uci.getInfo(), attribute, value));
        em.persist(uci);
    }
    public static String infoDateFormat = "dd/MMM/yyyy:HH:mm:ss Z";

    @Override
    public void extendExpiryAndEndDate(int count, boolean fromNow, boolean forceDays, boolean writeOriginalToInfo, Date actualDate) {
        log.debug("Expiry Date will be updated on uci [{}] by [{}]units or to date [{}]", new Object[]{uci.getUnitCreditInstanceId(), count, actualDate});

        if (writeOriginalToInfo) {
            setInfoValue("OriginalExpiry", Utils.getDateAsString(uci.getExpiryDate(), infoDateFormat, null));
        }

        String rolloverCount = Utils.getValueFromCRDelimitedAVPString(uci.getInfo(), "RolloverCount");

        if (rolloverCount == null || rolloverCount.isEmpty()) {
            setInfoValue("RolloverCount", "0");
        } else {
            int iRolloverCount = Integer.valueOf(rolloverCount) + 1;
            setInfoValue("RolloverCount", String.valueOf(iRolloverCount));
        }

        Date newDate;
        if (actualDate != null) {
            newDate = actualDate;
        } else {
            Calendar expiryDateToUse = Calendar.getInstance();
            if (fromNow) {
                log.debug("Expiry date is being extended from now");
            } else {
                expiryDateToUse.setTime(uci.getExpiryDate());
                log.debug("Expiry date is being extended from the existing expiry date of [{}]", uci.getExpiryDate());
            }

            if (!forceDays && getBooleanPropertyFromConfig("ValidityIsMonths")) {
                expiryDateToUse.add(Calendar.MONTH, count);
            } else if (!forceDays && getBooleanPropertyFromConfig("ValidityIsHours")) {
                expiryDateToUse.add(Calendar.HOUR, count);
            } else {
                expiryDateToUse.add(Calendar.DATE, count);
            }
            newDate = expiryDateToUse.getTime();
        }
        uci.setExpiryDate(newDate);
        uci.setEndDate(newDate);
        em.persist(uci);
        log.debug("This unit credits expiry date and end has now been set to [{}]", newDate);
    }

    @Override
    public IAccount getAccount() throws Exception {
        return AccountFactory.getAccount(em, uci.getAccountId(), true);
    }

    @Override
    public void checkProvisionRules(IAccount acc, Date startDate, boolean verifyOnly, int productInstanceId) throws Exception {
        boolean onlyOneAllowedPerPI = getBooleanPropertyFromConfig("OneAllowedPerPIAtAnyTime");
        int hoursBetweenPurchase = getIntPropertyFromConfig("MinHoursBetweenPurchasing");
        int groupIdOfBundleBeingPurchased = getIntPropertyFromConfig("GroupId");

        if (hoursBetweenPurchase > 0 || onlyOneAllowedPerPI) {
            Date mustBeBeforeDt = null;
            if (onlyOneAllowedPerPI) {
                log.debug("Only one UC of this type is allowed on account [{}] and PI [{}]", acc.getAccountId(), productInstanceId);
            }
            if (hoursBetweenPurchase > 0) {
                Calendar mustBeBefore = Calendar.getInstance();
                mustBeBefore.add(Calendar.MINUTE, hoursBetweenPurchase * -60);
                mustBeBeforeDt = mustBeBefore.getTime();
                log.debug("This UC can only be purchased after a gap of [{}] hours from when the same type was last purchased on this service. It must thus have been last purchased before [{}]", hoursBetweenPurchase, mustBeBeforeDt);
            }

            List<UnitCreditInstance> accountsCurrentUCs = DAO.getUnitCreditInstances(em, acc.getAccountId());
            for (UnitCreditInstance uciFromList : accountsCurrentUCs) {
                if (uciFromList.getProductInstanceId() == productInstanceId) {
                    UnitCreditSpecification thisUCS = DAO.getUnitCreditSpecification(em, uciFromList.getUnitCreditSpecificationId());
                    int groupIdOfExistingBundle = UnitCreditManager.getIntPropertyFromConfig(thisUCS, "GroupId");
                    log.debug("The UC being provisionined has a group Id of [{}] and this existing bundle has a group id of [{}]", groupIdOfBundleBeingPurchased, groupIdOfExistingBundle);
                    if (groupIdOfExistingBundle == groupIdOfBundleBeingPurchased && mustBeBeforeDt != null && uciFromList.getPurchaseDate().after(mustBeBeforeDt)) {
                        throw new Exception("This unit credit cannot be purchased this soon after it was last purchased -- wait time is " + hoursBetweenPurchase + " hours");
                    }
                    if (onlyOneAllowedPerPI && uciFromList.getUnitCreditSpecificationId() == this.getUnitCreditSpecification().getUnitCreditSpecificationId()) {
                        throw new Exception("Cannot have more than one of these unit credits at any point in time");
                    }
                }
            }
        }

        if (productInstanceId == 0) {
            log.debug("This provisioning allows sharing. Checking for max product instance rules");
            int maxSharing = getIntPropertyFromConfig("MaxSharing");

            if (maxSharing > 0) {
                log.debug("Only [{}] product instances can share from this unit credit");
                long cnt = DAO.getCountOfProductInstancesOnAccountThatCouldUseUnitCreditSpec(em, acc.getAccountId(), ucs.getUnitCreditSpecificationId());
                if (cnt > maxSharing) {
                    throw new Exception("Too many products under the account for unit credit sharing -- " + cnt + " is greater than " + maxSharing);
                }
            }
        }

        if ((!verifyOnly && getBooleanPropertyFromConfig("EnforcePurchaseRolesOnProvisioning"))
                || (verifyOnly && getBooleanPropertyFromConfig("EnforcePurchaseRolesOnVerify"))) {
            checkProvisionRoles(acc);
        }
    }

    @Override
    public void doPostProvisionProcessing(boolean verifyOnly) throws Exception {
        if (verifyOnly) {
            log.debug("Only verifying provisioning can happen so will only verify doExtraUCProvisioning(...)");
            doExtraUCProvisioning(verifyOnly);
            return;
        }
        log.debug("In doPostProvisionProcessing");
        // Send event if terms and conditions are required
        String termsAndConditionsKey = getPropertyFromConfig("TermsAndConditionsResourceKey");
        if (termsAndConditionsKey != null) {
            EventHelper.sendTermsAndConditionsEvent(this, termsAndConditionsKey);
        }

        if (getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays") > -1
                && !DAO.isProductInstanceOlderThanPeriodInDays(em, uci.getProductInstanceId(), getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays"))) {
            int days = getIntPropertyFromConfig("UCMessagesForPIsYoungerThanDays");

            String subType = "ProvisionedEventSubType";
            if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                subType = "BonusProvisionedEventSubType";
            }

            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1_" + days));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2_" + days));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "3_" + days));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "4_" + days));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "5_" + days));
        } else {
            String subType = "ProvisionedEventSubType";
            if (Utils.getBooleanValueFromCRDelimitedAVPString(uci.getInfo(), "BonusBundle")) {
                subType = "BonusProvisionedEventSubType";
            }
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "1"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "2"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "3"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "4"));
            EventHelper.sendUnitCreditEvent(this, getPropertyFromConfig(subType + "5"));
        }

        doBonusBundleUCProvisioning();
        doExtraUCProvisioning(false);
        doExpireOtherUCsOnProvisioning();
        doRollOverProcessing();

        String stickyDPIRules = getPropertyFromConfig("StickyDPIRules");
        if (stickyDPIRules != null) {
            log.debug("This UC has sticky DPI rules that must be added [{}]", stickyDPIRules);
            ServiceInstance serviceInstance = DAO.getDataServiceInstanceForProductInstance(em, uci.getProductInstanceId());
            ServiceRules rules = new ServiceRules();
            rules.setSystemDefinedDpiRulesToAdd(Utils.getSetFromCommaDelimitedString(stickyDPIRules));
            ServiceSpecHelper.applyServiceRules(serviceInstance, rules);
        }
    }

    private void doBonusBundleUCProvisioning() throws Exception {

        // Where the account has a bonous bundle on it that implies that you get free bundles added when buying a bundle of a type
        List<UnitCreditInstance> accountsCurrentUCs = DAO.getUnitCreditInstances(em, getAccountId());
        for (UnitCreditInstance uciFromList : accountsCurrentUCs) {
            IUnitCredit ucOfTypeBonusBundleUnitCredit = UnitCreditManager.getWrappedUnitCreditInstance(em, uciFromList, null);
            if (!ucOfTypeBonusBundleUnitCredit.getUnitCreditSpecification().getWrapperClass().equals("BonusBundleUnitCredit")) {
                log.debug("UC is not a BonusBundleUnitCredit");
                continue;
            }
            log.debug("This is a BonusBundleUnitCredit");
            if (uciFromList.getStartDate().after(new Date())) {
                log.debug("UC has not started yet");
                continue;
            }
            if (uciFromList.getProductInstanceId() != getProductInstanceId()) {
                log.debug("UC is for a different PI");
                continue;
            }
            int unitsLeft = ucOfTypeBonusBundleUnitCredit.getCurrentUnitsLeft().intValue();
            log.debug("This uc has [{}] units left", unitsLeft);
            if (unitsLeft <= 0) {
                continue;
            }
            log.debug("It has units left");

            String listBonusSpecIds = ucOfTypeBonusBundleUnitCredit.getPropertyFromConfig("BonusForUC" + getUnitCreditSpecification().getUnitCreditSpecificationId());
            if (listBonusSpecIds == null || listBonusSpecIds.isEmpty()) {
                continue;

            }
            String[] bonusSpecIds = listBonusSpecIds.split(",");
            if (bonusSpecIds.length <= 0) {
                continue;
            }
            for (String stringBonusSpecId : bonusSpecIds) {
                int bonusSpecId = Integer.parseInt(stringBonusSpecId);
                // Do in same transaction with same txid so it will be added and rolled back and reversed with the parent
                // Make sure these UC's price is zero unless you really want to charge the customers airtime
                log.debug("This UC can get a bonus with spec Id [{}]", bonusSpecId);
                ucOfTypeBonusBundleUnitCredit.setUnitCreditUnitsRemaining(ucOfTypeBonusBundleUnitCredit.getCurrentUnitsLeft().subtract(new BigDecimal(1)));

                //If we are in IFRS mode, do not provision the bundle, just continue.
                if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                    continue;
                }

                if (getUnitCreditSpecification().getUnitCreditSpecificationId() == bonusSpecId) {
                    log.warn("This UC [{}] cannot give a bonus of the same type [{}]", getUnitCreditSpecification().getUnitCreditSpecificationId(), bonusSpecId);
                    continue;
                }

                ProvisionUnitCreditRequest provReq = new ProvisionUnitCreditRequest();
                provReq.setSkipUniqueTest(true);
                provReq.setPlatformContext(new PlatformContext());
                // Make sure that for this BonusBundle with this remaining units, it can only be used once
                provReq.getPlatformContext().setTxId(uci.getExtTxid());
                provReq.getProvisionUnitCreditLines().add(new ProvisionUnitCreditLine());
                provReq.getProvisionUnitCreditLines().get(0).setAccountId(uci.getAccountId());
                provReq.getProvisionUnitCreditLines().get(0).setNumberToProvision(1);
                provReq.getProvisionUnitCreditLines().get(0).setUnitCreditSpecificationId(bonusSpecId);
                provReq.getProvisionUnitCreditLines().get(0).setProductInstanceId(getProductInstanceId());
                provReq.getProvisionUnitCreditLines().get(0).setStartDate(Utils.getDateAsXMLGregorianCalendar(uci.getStartDate()));
                provReq.getProvisionUnitCreditLines().get(0).setInfo("BonusBundle=true");
                provReq.setSaleLineId(uci.getSaleRowId());
                log.debug("Calling BM to provision unit credit [{}] on account [{}]", bonusSpecId, getAccountId());
                new BalanceManager().provisionUnitCredit(em, provReq);
                log.debug("Finished provisioning a free UC due to the account having a BonusBundleUnitCredit");

            }
            if (ucOfTypeBonusBundleUnitCredit.getBooleanPropertyFromConfig("AllowMultipleBonuses")) {
                log.debug("This bonus allows more bonuses to follow on");
            } else {
                break;
            }
        }
    }
    
    private void doExpireOtherUCsOnProvisioning() throws Exception { //Expire the bundles that are not allowed to run concurrently with the current bundle.
        String specIdsToExpire = getPropertyFromConfig("UnitCreditSpecIdsToExpireOnProvisioning");
        if (specIdsToExpire == null) {
            return;
        }
        
        List<UnitCreditInstance> accountsCurrentUCs = DAO.getUnitCreditInstances(em, getAccountId());
        
        String[] specIds = specIdsToExpire.split(",");
        for (String specId : specIds) {
            specId = specId.trim();
            if (specId.isEmpty()) {
                continue;
            }
            int iSpecIdToExpire = Integer.valueOf(specId);
            if (iSpecIdToExpire <= 0) {
                continue;
            }
            
            for (UnitCreditInstance uciFromList : accountsCurrentUCs) {
                IUnitCredit uciToExpire = UnitCreditManager.getWrappedUnitCreditInstance(em, uciFromList, null);
                if(uciToExpire.getUnitCreditSpecification().getUnitCreditSpecificationId() == iSpecIdToExpire) {
                    // Expire the bundle here
                    uciFromList.setExpiryDate(new Date());
                    em.persist(uciFromList);
                }
            }
        }
        
    }
            

    private void doExtraUCProvisioning(boolean verifyOnly) throws Exception {
        // Provision one bundle but automatically get another one - e.g. provision data and get free onnet voice
        String extraSpecIds = getPropertyFromConfig("ExtraUnitCreditSpecId");
        if (extraSpecIds == null) {
            return;
        }

        if (BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false) && !verifyOnly) { //Do not process extra unit credits when in IFRS since they are automatically added to the sale.
            return;
        }

        //Check if extra unit credit are restricted to certain products?
        String extraSpecIdsExcludedFromDate = getPropertyFromConfig("ExtraUnitCreditSpecIdExcludedForPIsCreatedAfter");
        if (extraSpecIdsExcludedFromDate != null) {
            Date latestProductCreatedDatetime = DAO.getAccountsLatestProductInstanceCreatedDate(em, uci.getAccountId());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Date excludeFromDate = sdf.parse(extraSpecIdsExcludedFromDate);

            if (verifyOnly) {
                throw new Exception("Product instance excluded from receiving extra unit credits -- exclusion start date is " + extraSpecIdsExcludedFromDate);
            }

            if (latestProductCreatedDatetime.after(excludeFromDate)) { // Do not provision extra unit credits if the latest product on this account id is after the cutoff date.
                return;
            }
        }

        log.debug("extraSpecIds is [{}]", extraSpecIds);
        String[] specIds = extraSpecIds.split(",");
        for (String specId : specIds) {

            specId = specId.trim();
            if (specId.isEmpty()) {
                continue;
            }
            int extraUCSpecId = Integer.valueOf(specId);
            if (extraUCSpecId <= 0) {
                continue;
            }
            UnitCreditSpecification ucspec = DAO.getUnitCreditSpecification(em, extraUCSpecId);

            int piId = DAO.getLastActiveOrTempDeactiveProductInstanceIdForAccountAndUnitCreditSpec(em, getAccountId(), extraUCSpecId);
            if (piId == -1) {
                log.debug("Extra UC spec cannot apply to the account. Skipping");
                if (verifyOnly && BaseUtils.getBooleanProperty("env.pos.ifrs15.enabled", false)) {
                    throw new Exception("Extra UC spec cannot apply to the account. Skipping -- unit credit specid " + extraUCSpecId);
                }
                continue;
            }

            try {
                boolean blackListed = false;
                String blackListSpecIds = UnitCreditManager.getPropertyFromConfig(ucspec, "DontProvisionAsExtraIfHasSpecId");
                if (blackListSpecIds != null) {
                    log.debug("blackListSpecIds is [{}]", blackListSpecIds);
                    String[] blackListSpecIdsArray = blackListSpecIds.split(",");
                    for (String blSpecId : blackListSpecIdsArray) {
                        blSpecId = blSpecId.trim();
                        if (blSpecId.isEmpty()) {
                            continue;
                        }
                        int blUCSpecId = Integer.valueOf(blSpecId);
                        List<UnitCreditInstance> uciList = DAO.getUnitCreditInstances(em, uci.getAccountId());
                        for (UnitCreditInstance uciExisting : uciList) {
                            if (uciExisting.getUnitCreditSpecificationId() == blUCSpecId
                                    && uciExisting.getProductInstanceId() == getProductInstanceId()) {
                                log.debug("Account and product instance already has a spec id of [{}]", blUCSpecId);
                                if (verifyOnly) {
                                    throw new Exception("Account and product instance already has extra spec id -- account id " + uci.getAccountId() + " product instance " + getProductInstanceId());
                                }
                                blackListed = true;
                                break;
                            }
                        }
                        if (blackListed) {
                            break;
                        }
                    }
                }
                if (blackListed) {
                    continue;
                }
            } catch (Exception e) {
                log.warn("Error doing new blacklist processing: ", e);
            }

            if (!Utils.isBetween(new Date(), ucspec.getAvailableFrom(), ucspec.getAvailableTo())) {
                log.debug("Skipping provisioning extra unit credit as its not currently available");
                if (verifyOnly) {
                    throw new Exception("Extra unit credit is not currently available -- unit credit spec id " + ucspec.getUnitCreditSpecificationId());
                }
                continue;
            }
            int numberToProvision = getIntPropertyFromConfig("SpecId" + extraUCSpecId + "Qty");
            if (numberToProvision == -1) {
                numberToProvision = 1;
            }
            int gap = getIntPropertyFromConfig("SpecId" + extraUCSpecId + "Gap");
            if (gap == -1) {
                gap = 0;
            }
            int initialDelayDays = getIntPropertyFromConfig("SpecId" + extraUCSpecId + "Delay");
            if (initialDelayDays == -1) {
                initialDelayDays = 0;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(uci.getStartDate());
            cal.add(Calendar.DATE, initialDelayDays);

            log.debug("NumberToProvision [{}] Gap [{}]", numberToProvision, gap);
            // Do in same transaction with same txid so it will be added and rolled back and reversed with the parent
            // Make sure these UC's price is zero unless you really want to charge the customers airtime
            log.debug("Extra UC spec with id [{}] must be provisioned", extraUCSpecId);

            if (verifyOnly) {
                log.debug("Only doing verify, return successfully.");
                return;
            } else {
                ProvisionUnitCreditRequest provReq = new ProvisionUnitCreditRequest();
                provReq.setSkipUniqueTest(true);
                provReq.setPlatformContext(new PlatformContext());
                provReq.getPlatformContext().setTxId(uci.getExtTxid());
                provReq.getProvisionUnitCreditLines().add(new ProvisionUnitCreditLine());
                provReq.getProvisionUnitCreditLines().get(0).setAccountId(uci.getAccountId());
                provReq.getProvisionUnitCreditLines().get(0).setDaysGapBetweenStart(gap);
                provReq.getProvisionUnitCreditLines().get(0).setNumberToProvision(numberToProvision);
                provReq.getProvisionUnitCreditLines().get(0).setUnitCreditSpecificationId(extraUCSpecId);
                provReq.getProvisionUnitCreditLines().get(0).setProductInstanceId(getProductInstanceId());
                provReq.getProvisionUnitCreditLines().get(0).setStartDate(Utils.getDateAsXMLGregorianCalendar(cal.getTime()));
                provReq.setSaleLineId(uci.getSaleRowId());
                provReq.getProvisionUnitCreditLines().get(0).setInfo(Utils.setValueInCRDelimitedAVPString(
                        provReq.getProvisionUnitCreditLines().get(0).getInfo(), "ParentUCI", String.valueOf(uci.getUnitCreditInstanceId())));
                log.debug("Calling BM to provision unit credit [{}] on account [{}]", extraUCSpecId, getAccountId());
                new BalanceManager().provisionUnitCredit(em, provReq);
                log.debug("Finished provisioning a extra UC");
            }
        }
    }

    private void doRollOverProcessing() throws Exception {
        log.debug("In doRollOverProcessing");
        String rolloverSpecIds = getPropertyFromConfig("RollOverSpecIds");
        String rolloverCampaignSpecIds = getPropertyFromConfig("RollOverCampaignSpecIds");
        if (rolloverSpecIds == null && rolloverCampaignSpecIds == null) {
            return;
        }
        log.debug("rolloverSpecIds is [{}]", rolloverSpecIds);
        log.debug("rolloverCampaignSpecIds is [{}]", rolloverCampaignSpecIds);
        if(rolloverSpecIds != null && rolloverCampaignSpecIds != null){
            log.debug("rolloverCampaignSpecIds exist with rolloverSpecIds");
            return;
        }
        Set<Integer> rolloverSpecs = new HashSet<>();
        for (String specId : rolloverSpecIds.split(",")) {
            specId = specId.trim();
            if (specId.isEmpty()) {
                continue;
            }
            rolloverSpecs.add(Integer.valueOf(specId));
        }
        
        boolean isCampaignRollover = false;
        if(rolloverCampaignSpecIds != null){
            rolloverSpecs = new HashSet<>();
            for (String specId : rolloverSpecIds.split(",")) {
            specId = specId.trim();
            if (specId.isEmpty()) {
                continue;
            }
            rolloverSpecs.add(Integer.valueOf(specId));
           }
           isCampaignRollover = true;
        }

        List<UnitCreditInstance> possibleRolloverList;
        if (BaseUtils.getBooleanProperty("env.bm.rollover.at.product.instance.level", true)) {
            log.debug("Rollover should be considered on all the unit credits on the same product instance (and account)");
                possibleRolloverList = DAO.getAccountsActiveUnitCredits(em, getAccount())
                    .stream()
                    .filter(u -> u.getProductInstanceId() == getProductInstanceId())
                    .collect(Collectors.toList());
        } else if (BaseUtils.getBooleanProperty("env.bm.rollover.at.customer.level", false)) {
            log.debug("Rollover should be considered on all the unit credits on the same customer");
            if (getProductInstanceId() == 0) {
                log.debug("Product instance ID is zero so no UC can be rolled over at customer level");
                return;
            }
            possibleRolloverList = DAO.getCustomersActiveUnitCredits(em, getProductInstanceId());
        } else if (BaseUtils.getBooleanProperty("env.bm.rollover.at.account.level", true)) {
            log.debug("Rollover should be considered on all the unit credits on the same account");
            possibleRolloverList = DAO.getAccountsActiveUnitCredits(em, getAccount());
        } else {
            log.debug("There is nothing that can be rolled over");
            return;
        }

        for (UnitCreditInstance existingUCI : possibleRolloverList) {
            log.debug("Looking at UCI id [{}] and getUnitCreditInstanceId [{}]", existingUCI.getUnitCreditInstanceId(), getUnitCreditInstanceId());
            if (DAO.isZeroOrNegative(existingUCI.getUnitsRemaining()) || !rolloverSpecs.contains(existingUCI.getUnitCreditSpecificationId())
                    || existingUCI.getUnitCreditInstanceId() == getUnitCreditInstanceId()) {
                log.debug("not doing rollover for "+existingUCI.getUnitCreditInstanceId()+ " rolloverSpecs = "+rolloverSpecs+ " getUnitCreditInstanceId = "+getUnitCreditInstanceId());
                continue;
            }
            String split = Utils.getValueFromCRDelimitedAVPString(existingUCI.getInfo(), "Split");
            if (split != null && split.equals("true")) {
                log.debug("This UC is from a bundle split so cannot roll over");
                continue;
            }
            IUnitCredit uc = UnitCreditManager.getWrappedUnitCreditInstance(em, existingUCI, null);

            if (uc.getBooleanPropertyFromConfig("OnlyRolloverOnce") && Utils.getValueFromCRDelimitedAVPString(existingUCI.getInfo(), "OriginalExpiry") != null) {
                log.debug("Unit credit can only be rolled over once");
                continue;
            }

            Date newDate = getExpiryDate(); // The default to roll over to

            boolean  isDefaultDate = true;
            if (uc.getBooleanPropertyFromConfig("RolloverToCurrentExpiry") && !isCampaignRollover) {
                log.debug(uc.getUnitCreditName()+" is RolloverToCurrentExpiry");
                isDefaultDate = false;
                // Roll over to its expiry date
                newDate = uc.getExpiryDate();
                if (existingUCI.getExpiryDate().after(newDate)) {
                    log.debug("Expiry is already after the new date");
                    continue;
                }
            }
            if (uc.getBooleanPropertyFromConfig("RolloverToCurrentEndDate")  && !isCampaignRollover) {
                isDefaultDate = false;
                // Roll over to its end date
                log.debug(uc.getUnitCreditName()+" rolling over to end date "+getEndDate());
                newDate = getEndDate();
                if(existingUCI.getEndDate().after(newDate)){
                    log.debug("Enddate is already after the new date");
                    continue;
                }
            }  
            if(isDefaultDate && (existingUCI.getExpiryDate().after(newDate)) && !isCampaignRollover) {
                log.debug("Default Expiry is already after the new date");
                    continue;
            }
            

            log.debug("Going to extend [{}] to [{}]", existingUCI.getExpiryDate(), newDate);
            uc.extendExpiryAndEndDate(0, false, false, true, newDate);
            uc.doPostRolloverProcessing();
        }

    }

    @Override
    public void split(long targetAccountId, BigDecimal units, int targetProductInstanceId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getIntPropertyFromConfig(String propName) {
        return UnitCreditManager.getIntPropertyFromConfig(ucs, propName);
    }

    @Override
    public long getLongPropertyFromConfig(String propName) {
        return UnitCreditManager.getLongPropertyFromConfig(ucs, propName);
    }

    public double getDoublePropertyFromConfig(String propName) {
        return UnitCreditManager.getDoublePropertyFromConfig(ucs, propName);
    }

    public BigDecimal getBigDecimalPropertyFromConfigZeroIfMissing(String propName) {
        return UnitCreditManager.getBigDecimalPropertyFromConfigZeroIfMissing(ucs, propName);
    }

    public BigDecimal getBigDecimalPropertyFromConfig(String propName) {
        return UnitCreditManager.getBigDecimalPropertyFromConfig(ucs, propName);
    }

    @Override
    public boolean getBooleanPropertyFromConfig(String propName) {
        return UnitCreditManager.getBooleanPropertyFromConfig(ucs, propName);
    }

    @Override
    public String getPropertyFromConfig(String propName) {
        return UnitCreditManager.getPropertyFromConfig(ucs, propName);
    }

    @Override
    public UnitCreditInstance getUnitCreditInstance() {
        return uci;
    }

    private void checkProvisionRoles(IAccount acc) throws Exception {
        log.debug("Checking if provisioning roles allows owner of account [{}] to get the UC");
        CustomerBean cb = CustomerBean.getCustomerByAccountNumber(acc.getAccountId(), StCustomerLookupVerbosity.CUSTOMER);
        log.debug("The account is owned by customer Id [{}]", cb.getCustomerId());
        List<String> customersRoles = cb.getSecurityGroups();
        List<String> ucPurchaseRoles = Utils.getListFromCRDelimitedString(getUnitCreditSpecification().getPurchaseRoles());
        if (!Utils.listsIntersect(customersRoles, ucPurchaseRoles)) {
            throw new Exception("Unit credit is not allowed on this account due to a role mismatch");
        }
    }

    @Override
    public boolean ignoreWhenNoUnitsLeft() {
        return getBooleanPropertyFromConfig("IgnoreWhenNoUnitsLeft");
    }
    
    

}
