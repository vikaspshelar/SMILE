/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.sra.model.RechargeOffer;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * REST Web Service
 *
 * @author XolaniM
 */
@Path("offer")
public class RechargeOfferResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(RechargeOfferResource.class);
      
    @Path("/create")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOffer(RechargeOffer rechargeOffer ) {
 
        String output = rechargeOffer.toString();
        log.warn("Received request to create offer [{}]", output );
        
        String resp = "Offer for subscription: " + rechargeOffer.getCampaignId() + " created successfully.";
        
        return Response.status(201).entity(resp).build();
    } 
/*    
    @Path("/notify")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendSMS(OTPMessage otpNotification) throws Exception {        
        String resp = "";
        String from = otpNotification.getFrom();
        String to = otpNotification.getTo();
        String body = otpNotification.getBody();
        
        ShortMessage sm = new ShortMessage();
        sm.setBody(body);
        sm.setFrom(from);
        sm.setTo(to);
        
        if (!sm.getFrom().isEmpty() && !sm.getTo().isEmpty()) {
            log.warn("Sending notification sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
            try {
                SCAWrapper.getAdminInstance().sendShortMessage(sm);
                resp = "SMS Sent";
            } catch (Exception ex) {
                resp = "SMS send failed." + ex.getMessage();
                log.warn("Failed to send notification sms from [{}] to [{}] text [{}]", new Object[]{from, to, body});
            }
        }
        return Response.status(200).entity(resp).build();
    }
    */
}
