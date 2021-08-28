/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.beans.TTIssueBean;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.sra.helpers.RequestParser;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
@Path("tickets")
public class TTIssuesResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(TTIssuesResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;
    
    @Path("{ticketId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TTIssueResource getTicket(@PathParam("ticketId") String ticketId) {
        start(request);
        try {
            return new TTIssueResource(ticketId);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public TTIssueResource createNewTicket(MultivaluedMap<String, String> formParams, @Context UriInfo uriInfo) {
        start(request);
        try {
            RequestParser parser = new RequestParser(formParams, uriInfo);
            switch (parser.getMethod()) {
                case "createTroubleTicket":
                    return new TTIssueResource(TTIssueBean.createTroubleTicket(parser.getParamAsInt("customerId"), parser.getParamAsString("description"), parser.getParamAsString("category")));
                default:
                    throw new Exception("Invalid method type -- " + parser.getMethod());
            }

        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
}
