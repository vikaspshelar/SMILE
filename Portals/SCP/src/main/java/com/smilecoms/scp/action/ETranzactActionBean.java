/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.scp.action;

import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.scp.helpers.etranzact.TransactionData;
import com.smilecoms.scp.helpers.etranzact.ETransactionManager;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;

/**
 *
 * @author sabelo
 */
public class ETranzactActionBean extends SmileActionBean {

    @DefaultHandler
    public Resolution processTransaction() {
        try {
            TransactionData data = ETransactionManager.commitTransaction(
                    Integer.parseInt(getParameter("SUCCESS")),
                    getParameter("FINAL_CHECKSUM"),
                    getParameter("TRANSACTION_ID"));
            ForwardResolution res = (ForwardResolution) getDDForwardResolution(AccountActionBean.class, "retrieveAccount", "etranzact.completed.successfully");
            res.addParameter("accountQuery.accountId", data.getRecipientAccountId());
            return res;
        } catch (Exception e) {
            log.warn("Error: ", e);
            return getDDForwardResolution(CustomerActionBean.class, "retrieveCustomerDefault");
        }
    }
    private TransactionData eTranzactData;

    public TransactionData getETranzactData() {
        return eTranzactData;
    }

    public Resolution confirmEtranzact() {
        try {
            eTranzactData = ETransactionManager.startTransaction(
                    Double.parseDouble(getParameter("eTranzactMajorCurrencyUnits")) * 100, 
                    getAccount().getAccountId(),
                    getRequest());
        } catch (Exception e) {
            log.warn("Error: ", e);
        }
        return getDDForwardResolution("/account/etranzact_confirm.jsp");
    }
}
