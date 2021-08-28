/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.action;

import com.smilecoms.commons.sca.OrganisationList;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.StOrganisationLookupVerbosity;
import com.smilecoms.commons.stripes.SmileActionBean;
import java.io.StringReader;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;

/**
 *
 * @author paul
 */
public class AutoCompleteActionBean extends SmileActionBean {
    
    @DontValidate
    public Resolution getOrganisationsJSON() {
        try {
            log.debug("Getting orgs matching name [{}]", this.getOrganisationQuery().getOrganisationName());
            getOrganisationQuery().setVerbosity(StOrganisationLookupVerbosity.MAIN);
            getOrganisationQuery().setResultLimit(50);
            OrganisationList orgs = SCAWrapper.getUserSpecificInstance().getOrganisations(getOrganisationQuery());
            log.debug("Got [{}] orgs", orgs.getNumberOfOrganisations());
            Resolution res = getJSONResolution(orgs);
            return res;
        } catch (Exception e) {
            Resolution res = new StreamingResolution("text", new StringReader(e.toString()));
            this.getResponse().setStatus(500);
            return res;
        }
    }
    
}
