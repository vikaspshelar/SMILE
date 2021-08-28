/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.beans.OrganisationBean;
import org.slf4j.*;

/**
 * REST Web Service
 *
 * @author paul
 */
public class OrganisationResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(OrganisationResource.class);
    private OrganisationBean organisation;

    public OrganisationResource() {
    }
    
    public OrganisationResource(int organisationId)   {
        organisation = OrganisationBean.getOrganisationById(organisationId);
    }
    
    public OrganisationBean getOrganisation() {
        log.debug("In getOrganisation");
        return organisation;
    }
    
    public void setOrganisation(OrganisationBean organisation) {
        this.organisation = organisation;
    }
}
