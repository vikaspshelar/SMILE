/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("organisations")
public class OrganisationsResource extends Resource {

    @Context
    private javax.servlet.http.HttpServletRequest request;
    private static final Logger log = LoggerFactory.getLogger(OrganisationsResource.class);

    @Path("{organisationId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OrganisationResource getOrganisation(@PathParam("organisationId") int organisationId) {
        start(request);
        try {
            return new OrganisationResource(organisationId);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }

}
