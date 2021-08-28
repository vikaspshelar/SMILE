/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.beans.CampaignBean;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("campaigns")
public class CampaignsResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(CampaignsResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("{campaign}/optin")
    @GET
    public Response optInToCampaign(@PathParam("campaign") String campaignName, @QueryParam("ui") String unique, @QueryParam("piid") int productInstanceId, @QueryParam("info") String info, @QueryParam("hash") String hash) {
        start(request);
        try {
            CampaignBean campaign = new CampaignBean(campaignName, productInstanceId, unique, info);
            String redirectTo = campaign.processURLOptIn(hash);
            return Response.status(Response.Status.TEMPORARY_REDIRECT)
                    .header(HttpHeaders.LOCATION, redirectTo)
                    .build();
        } catch (Exception ex) {
            processError(ex);
        } finally {
            end();
        }
        return Response.status(Response.Status.TEMPORARY_REDIRECT)
                .header(HttpHeaders.LOCATION, BaseUtils.getProperty("env.campaign.redirect.err." + campaignName, "https://smilecoms.com?ok=false"))
                .build();
    }

}
