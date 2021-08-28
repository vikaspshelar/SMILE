/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.model;

import com.smilecoms.commons.sca.Account;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rajeshkumar
 */
public class ShareUnitCreditResponse {

    private long accountId;
    private double availableBalanceInCents;
    private double currentBalanceInCents;
    private List<UnitCreditInstance> unitCreditInstances;
    private String extraInfo;

    public ShareUnitCreditResponse(Account acc) {
        this.accountId = acc.getAccountId();
        this.availableBalanceInCents = acc.getAvailableBalanceInCents();
        this.currentBalanceInCents = acc.getCurrentBalanceInCents();
        unitCreditInstances = new ArrayList<>();
        for (com.smilecoms.commons.sca.UnitCreditInstance uci : acc.getUnitCreditInstances()) {
            UnitCreditInstance ucins = new UnitCreditInstance();
            ucins.setAccountId(uci.getAccountId());
            ucins.setAvailableUnitsRemaining(uci.getAvailableUnitsRemaining());
            ucins.setCurrentUnitsRemaining(uci.getCurrentUnitsRemaining());
            ucins.setUnitCreditInstanceId(uci.getUnitCreditInstanceId());
            ucins.setUnitCreditSpecificationId(uci.getUnitCreditSpecificationId());
            ucins.setProductInstanceId(uci.getProductInstanceId());
            ucins.setName(uci.getName());
            ucins.setUnitType(uci.getUnitType());
            unitCreditInstances.add(ucins);
        }
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public double getAvailableBalanceInCents() {
        return availableBalanceInCents;
    }

    public void setAvailableBalanceInCents(double availableBalanceInCents) {
        this.availableBalanceInCents = availableBalanceInCents;
    }

    public double getCurrentBalanceInCents() {
        return currentBalanceInCents;
    }

    public void setCurrentBalanceInCents(double currentBalanceInCents) {
        this.currentBalanceInCents = currentBalanceInCents;
    }

    public List<UnitCreditInstance> getUnitCreditInstances() {
        return unitCreditInstances;
    }

    public void setUnitCreditInstances(List<UnitCreditInstance> unitCreditInstances) {
        this.unitCreditInstances = unitCreditInstances;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }
}
