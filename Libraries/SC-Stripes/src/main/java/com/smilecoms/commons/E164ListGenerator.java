/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Number;
import com.smilecoms.commons.sca.NumberList;
import com.smilecoms.commons.sca.NumbersQuery;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author paul
 */
public class E164ListGenerator implements IListGenerator {

    @Override
    public List<String> getList() {
        NumbersQuery q = new NumbersQuery();
        q.setResultLimit(BaseUtils.getIntProperty("env.e164.number.list.size", 20));
        q.setPriceLimitCents(0);
        // Dont list any numbers owned by somebody
        q.setOwnedByCustomerProfileId(0);
        q.setOwnedByOrganisationId(0);
        NumberList list = SCAWrapper.getUserSpecificInstance().getAvailableNumbers(q);
        List<String> ret = new ArrayList();
        for (Number num : list.getNumbers()) {
            ret.add(Utils.getFriendlyPhoneNumber(num.getIMPU()));
        }
        return ret;
    }
}
