/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.beans.AccountBean;
import java.util.Date;
import org.slf4j.*;

/**
 * REST Web Service
 *
 * @author paul
 */
public class AccountResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(AccountResource.class);
    private AccountBean account;

    
    public AccountResource() {
    }
    
    public AccountResource(long accountId)   {
        account = AccountBean.getAccountById(accountId);
    }
    
    public AccountResource(AccountBean account)   {
        this.account = account;
    }
    
    public static AccountResource getAccountResourceWithHistory(long accountId, Date dateFrom, Date dateTo, int resultLimit){
        return new AccountResource(AccountBean.getAccountWithHistory(accountId, dateFrom, dateTo, resultLimit));        
    }
    
    public static AccountResource getAccountResourceWithSummary(long accountId, Date dateFrom, Date dateTo){
        return new AccountResource(AccountBean.getAccountWithSummary(accountId,dateFrom, dateTo));
    }
    
    public static AccountResource getAccountResourceWithHistoryAndSummary(long accountId, Date dateFrom, Date dateTo, int resultLimit){
        return new AccountResource(AccountBean.getAccountWithHistoryAndSummary(accountId,dateFrom, dateTo, resultLimit));
    }
    
    public AccountBean getAccount() {
        log.debug("In getAccount");
        return account;
    }
    
    public void setAccount(AccountBean account) {
        this.account = account;
    }
}
