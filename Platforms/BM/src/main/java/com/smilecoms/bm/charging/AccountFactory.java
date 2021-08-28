/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.bm.db.model.Account;
import com.smilecoms.bm.db.op.DAO;
import com.smilecoms.commons.base.BaseUtils;
import javax.persistence.EntityManager;

/**
 *
 * @author paul
 */
public class AccountFactory {

    public static enum ACCOUNT_TYPE {

        PREPAID
    };

    public static IAccount getAccount(EntityManager em, long accountId, boolean readOnly) throws Exception {
        return new PrepaidAccount(em, accountId, readOnly);
    }

    /**
     * Can add more account types and hence implementations of IAccount as we
     * progress
     *
     * @param em
     * @param type
     * @return
     * @throws java.lang.Exception
     */
    public static IAccount createAccount(EntityManager em, ACCOUNT_TYPE type) throws Exception {
        if (type.equals(ACCOUNT_TYPE.PREPAID)) {
            Account dbAcc = DAO.createAccount(em, BaseUtils.getIntProperty("env.bm.account.default.status", 8));
            return new PrepaidAccount(em, dbAcc.getAccountId(), false);
        }
        throw new java.lang.UnsupportedOperationException();
    }
}
