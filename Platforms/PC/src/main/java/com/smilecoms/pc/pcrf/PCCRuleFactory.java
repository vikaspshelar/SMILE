/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pc.pcrf;

import com.smilecoms.pc.pcrf.api.model.XMLPCCRule;
import com.smilecoms.pc.pcrf.db.model.DBPCCRule;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richard
 */
public class PCCRuleFactory {
    
    private static final Logger log = LoggerFactory.getLogger(AFSessionFactory.class);

    public static List<? extends PCCRule> getPCCRules(String dataModel) {
        if (dataModel.equals("HZ")) {
            return new ArrayList<XMLPCCRule>();
        } else if (dataModel.equals("DB")) {
            return new ArrayList<DBPCCRule>();
        } else {
            log.warn("Check env.pcrf.data.model - should be HZ or DB only");
            return null;
        }
    }
    
}
