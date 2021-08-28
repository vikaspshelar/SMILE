/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca.beans;

import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountHistoryQuery;
import com.smilecoms.commons.sca.AccountSummaryQuery;
import com.smilecoms.commons.sca.BalanceTransferData;
import com.smilecoms.commons.sca.PrepaidStrip;
import com.smilecoms.commons.sca.PrepaidStripQuery;
import com.smilecoms.commons.sca.PrepaidStripRedemptionData;
import com.smilecoms.commons.sca.PurchaseUnitCreditRequest;
import com.smilecoms.commons.sca.Reservation;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StAccountHistoryVerbosity;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StAccountSummaryVerbosity;
import com.smilecoms.commons.sca.VoucherLockForAccount;
import com.smilecoms.commons.sca.VoucherLockForAccountQuery;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public final class AccountBean extends BaseBean {


    
    private static final Logger log = LoggerFactory.getLogger(AccountBean.class);
    private com.smilecoms.commons.sca.Account scaAccount;
    private com.smilecoms.commons.sca.AccountHistory accountHistory;
    private com.smilecoms.commons.sca.AccountSummary accountSummary;

    public AccountBean() {
        scaAccount = new Account();
    }
    
    public static AccountBean getAccountById(long accountId) {
        log.debug("Getting Account by Id [{}]", accountId);
        return new AccountBean(UserSpecificCachedDataHelper.getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS));
        //return new AccountBean(UserSpecificCachedDataHelper.getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS));
    }
    
    public static AccountBean getAccountWithHistoryAndSummary(long accountId, Date dateFrom, Date dateTo, int resultLimit) { 
        com.smilecoms.commons.sca.Account acc = UserSpecificCachedDataHelper.getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);
        
        AccountHistoryQuery ahq = new AccountHistoryQuery();
        ahq.setAccountId(accountId);
        ahq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        ahq.setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        ahq.setResultLimit(resultLimit);
        ahq.setVerbosity(StAccountHistoryVerbosity.RECORDS);
        com.smilecoms.commons.sca.AccountHistory ah = SCAWrapper.getUserSpecificInstance().getAccountHistory(ahq);     
        
        com.smilecoms.commons.sca.AccountSummaryQuery asq = new AccountSummaryQuery();        
        int gap = Utils.getDaysBetweenDates(dateFrom, dateTo);
        if(gap > 1 && gap < 30){
            asq.setVerbosity(StAccountSummaryVerbosity.DAILY);
        }else if(gap > 30){
            asq.setVerbosity(StAccountSummaryVerbosity.MONTHLY);
        }else if(gap <=1) {
            asq.setVerbosity(StAccountSummaryVerbosity.HOURLY);
        }        
        asq.setAccountId(accountId);
        asq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        asq.setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));        
        com.smilecoms.commons.sca.AccountSummary accountSummary = SCAWrapper.getUserSpecificInstance().getAccountSummary(asq);       
        
        return new AccountBean(acc, accountSummary, ah);
    }
    
    public static AccountBean getAccountWithHistory(long accountId, Date dateFrom, Date dateTo, int resultLimit) { 
        com.smilecoms.commons.sca.Account acc = UserSpecificCachedDataHelper.getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);
        AccountHistoryQuery ahq = new AccountHistoryQuery();
        ahq.setAccountId(accountId);
        ahq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        ahq.setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));
        ahq.setResultLimit(resultLimit);
        ahq.setVerbosity(StAccountHistoryVerbosity.RECORDS);
        com.smilecoms.commons.sca.AccountHistory ah = SCAWrapper.getUserSpecificInstance().getAccountHistory(ahq);        
        return new AccountBean(acc, ah);
    }    
    
    public static AccountBean getAccountWithSummary(long accountId, Date dateFrom, Date dateTo){
        com.smilecoms.commons.sca.Account acc = UserSpecificCachedDataHelper.getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);
        com.smilecoms.commons.sca.AccountSummaryQuery asq = new AccountSummaryQuery();        
        int gap = Utils.getDaysBetweenDates(dateFrom, dateTo);
        if(gap > 1 && gap < 30){
            asq.setVerbosity(StAccountSummaryVerbosity.DAILY);
        }else if(gap > 30){
            asq.setVerbosity(StAccountSummaryVerbosity.MONTHLY);
        }else if(gap <=1) {
            asq.setVerbosity(StAccountSummaryVerbosity.HOURLY);
        }        
        asq.setAccountId(accountId);
        asq.setDateFrom(Utils.getDateAsXMLGregorianCalendar(dateFrom));
        asq.setDateTo(Utils.getDateAsXMLGregorianCalendar(dateTo));        
        com.smilecoms.commons.sca.AccountSummary accountSummary = SCAWrapper.getUserSpecificInstance().getAccountSummary(asq);       
        
        return new AccountBean(acc, accountSummary);
    }
    
    public static AccountBean provisionUnitCredit(long accountId, int unitCreditSpecificationId, int productInstanceId, int numberToProvision, int daysGapBetweenStart) {
        PurchaseUnitCreditRequest pucr = new PurchaseUnitCreditRequest();
        pucr.setAccountId(accountId);
        pucr.setNumberToPurchase(numberToProvision);
        pucr.setDaysGapBetweenStart(daysGapBetweenStart);
        pucr.setPaidByAccountId(accountId);
        pucr.setProductInstanceId(productInstanceId);
        pucr.setUnitCreditSpecificationId(unitCreditSpecificationId);
        SCAWrapper.getUserSpecificInstance().purchaseUnitCredit(pucr);
        return getAccountById(accountId);
    }
    
    public static AccountBean modifyAccount(AccountBean account) {
        SCAWrapper.getUserSpecificInstance().modifyAccount(account.scaAccount);
        return getAccountById(account.getAccountId());
    }

    public static AccountBean doBalanceTransfer(long fromAccountId, long toAccountId, double amountInCents) {
        BalanceTransferData btd = new BalanceTransferData();
        btd.setAmountInCents(amountInCents);
        btd.setSaleId(0);
        btd.setSourceAccountId(fromAccountId);
        btd.setTargetAccountId(toAccountId);
        SCAWrapper.getUserSpecificInstance().transferBalance(btd);
        return getAccountById(fromAccountId);
    }
    
     public static AccountBean doBalanceTransfer(long fromAccountId, String toPhoneNumber, double amountInCents) {
        ServiceBean si =  ServiceBean.getServiceInstanceByIMPU(toPhoneNumber);
        BalanceTransferData btd = new BalanceTransferData();
        btd.setAmountInCents(amountInCents);
        btd.setSaleId(0);
        btd.setSourceAccountId(fromAccountId);
        btd.setTargetAccountId(si.getAccountId());
        SCAWrapper.getUserSpecificInstance().transferBalance(btd);
        return getAccountById(fromAccountId);
    }
     
    public static AccountBean redeemVoucher(long thirdPartyAccountId, long accountId, String voucherCode) {
        PrepaidStripRedemptionData psrd = new PrepaidStripRedemptionData();
        psrd.setAccountId(accountId);
        psrd.setRedeemedByAccountId(thirdPartyAccountId);
        psrd.setEncryptedPIN(Codec.stringToEncryptedHexString(voucherCode));
        SCAWrapper.getUserSpecificInstance().redeemPrepaidStrip(psrd);
        return getAccountById(accountId);
    }
    
    public static PrepaidStrip getVoucher(String pin, long redeemingAccountId) {
        PrepaidStripQuery psq = new PrepaidStripQuery();
        psq.setEncryptedPINHex(Codec.stringToEncryptedHexString(pin));
        
        return SCAWrapper.getUserSpecificInstance().getPrepaidStrip(psq);
    }
    
    public static VoucherLockForAccount getVoucherLockForAccount(long accountId) {
        VoucherLockForAccountQuery vlaQ = new VoucherLockForAccountQuery();
        vlaQ.setAccountId(accountId);
        return SCAWrapper.getUserSpecificInstance().getVoucherLockForAccount(vlaQ);
    }
    
    private AccountBean(com.smilecoms.commons.sca.Account scaAccount) {        
        this(scaAccount, new com.smilecoms.commons.sca.AccountSummary(), new com.smilecoms.commons.sca.AccountHistory());
    }
    
    
    private AccountBean(com.smilecoms.commons.sca.Account scaAccount, com.smilecoms.commons.sca.AccountSummary accountSummary){
        this(scaAccount, accountSummary, new com.smilecoms.commons.sca.AccountHistory());
    }
    
    private AccountBean(com.smilecoms.commons.sca.Account scaAccount, com.smilecoms.commons.sca.AccountHistory accountHistory){
        this(scaAccount, new com.smilecoms.commons.sca.AccountSummary(), accountHistory);
    }
    
    private AccountBean(com.smilecoms.commons.sca.Account scaAccount, com.smilecoms.commons.sca.AccountSummary accountSummary, com.smilecoms.commons.sca.AccountHistory accountHistory){
        this.scaAccount = scaAccount;
        this.accountSummary = accountSummary;
        this.accountHistory = accountHistory;
    }

    @XmlElement
    public long getAccountId() {
        return scaAccount.getAccountId();
    }
    
    public void setAccountId(long  accountId) {
        scaAccount.setAccountId(accountId);
    }

    @XmlElement
    public Integer getStatus() {
        return scaAccount.getStatus();
    }

    public void setStatus(int status) {
        scaAccount.setStatus(status);
    }
    
    @XmlElement
    public double getCurrentBalanceInCents() {
        return scaAccount.getCurrentBalanceInCents();
    }

    @XmlElement
    public double getAvailableBalanceInCents() {
        return scaAccount.getAvailableBalanceInCents();
    }

    @XmlElement
    public List<UnitCreditBean> getUnitCredits() {
        return UnitCreditBean.wrap(scaAccount.getUnitCreditInstances());
    }

    @XmlElement
    public List<Reservation> getReservations() {
        return scaAccount.getReservations();
    }

    @XmlElement
    public com.smilecoms.commons.sca.AccountHistory getAccountHistory() {
        return accountHistory;
    }
    
    @XmlElement
    public com.smilecoms.commons.sca.AccountSummary getAccountSummary(){
        return accountSummary;
    }
    
}
