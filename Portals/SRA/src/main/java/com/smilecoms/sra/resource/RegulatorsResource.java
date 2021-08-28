/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.sra.helpers.UCCHelper;
import com.smilecoms.sra.model.ValidateRefugeeRequest;
import com.smilecoms.sra.model.CeirResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author sabza
 */
@Path("regulators")
public class RegulatorsResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(RegulatorsResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("{name: \btcra\b}/{house: \bcsis\b}/registrationresult")
    @POST
    @Consumes(MediaType.TEXT_XML)
    public Response processRegistrationResult(@Context UriInfo uriInfo, String registrationResult) {
        start(request);
        try {
            String PROPVAL_DELIM = "<operatorConversationId>";
            String PROPVAL_DELIM_END = "</operatorConversationId>";
            int PROPVAL_DELIM_LEN = PROPVAL_DELIM.length();
            int propStart = registrationResult.indexOf(PROPVAL_DELIM);
            int propEnd = registrationResult.indexOf(PROPVAL_DELIM_END);
            String eventKey = "";

            Event event = new Event();
            event.setEventType("TCRA_CSIS");
            event.setEventSubType("RegistrationResult");
            event.setEventData(registrationResult);

            if (propStart != -1 && propEnd != -1) {
                eventKey = registrationResult.substring(propStart + PROPVAL_DELIM_LEN, propEnd);
                log.debug("Got CSIS data [{}] event key [{}]", registrationResult, eventKey);
                event.setEventKey(eventKey);
            }

            try {
                SCAWrapper.getAdminInstance().createEvent(event);
            } catch (Exception ex) {
                log.warn("Failed to create event for CSIS via SCA call, cause: {}", ex.toString());
            }

            if (eventKey.isEmpty()) {
                Response.status(Response.Status.BAD_REQUEST).build();
            }

        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return Response.status(Response.Status.OK).build();
    }
    
    @POST
    @Path("validaterefugee")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validateRefugee(ValidateRefugeeRequest req){
        log.debug("validateRefugee request received.");    
        boolean isValid = UCCHelper.validateRefugee(req);
        if(isValid){
            return Response.status(Response.Status.OK).build();
        }
        else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }               
    }

    @GET
    @Path("NTWKD")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDetailsForCEIR(@Context UriInfo uriInfo){
        log.debug("Inside getDetailsForCEIR with URI:"+uriInfo.getPath());
        Response response = null;
        String data="";
        CeirResponse cresp = new CeirResponse();
        
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters(true);
        String version=queryParams.getFirst("VRSN");
        String tid=queryParams.getFirst("TID");
        String rdat=queryParams.getFirst("RDAT");
        String imsi=queryParams.getFirst("IMSI");
        
        cresp.setVRSN(version);
        cresp.setTID(tid);
        cresp.setMTON("1");
        cresp.setMNPI("1");
        
        switch (Integer.parseInt(version)) {
            case 1:
                if(Integer.parseInt(rdat)==1)
                {
                    String sqlQuery = BaseUtils.getProperty("env.ceir.msisdn.query", "");
                    data=UCCHelper.getDataFromDB(sqlQuery,imsi);
                    cresp.setMSDN(data.replaceAll("tel:\\+", ""));
                    response=Response.status(Response.Status.OK).entity(cresp.toString()).build();
                } else if(Integer.parseInt(rdat)!=0) {
                    response=Response.status(Response.Status.OK).entity("RDAT has improper value").build();
                } else {
                    response=Response.status(Response.Status.OK).entity(cresp.toString()).build();
                }
                break;
            case 2:
                char rdatArray[] = rdat.toCharArray();
                if(rdatArray.length==3)
                {
                    if(rdatArray[0]=='1')
                    {
                        String sqlQuery = BaseUtils.getProperty("env.ceir.msisdn.query", "");
                        data=UCCHelper.getDataFromDB(sqlQuery,imsi);
                        cresp.setMSDN(data.replaceAll("tel:\\+", ""));                        
                    }
                    if(rdatArray[1]=='1')
                    {
                        String sqlQuery = BaseUtils.getProperty("env.ceir.latest.location.query", "");
                        data=UCCHelper.getDataFromDB(sqlQuery,imsi);
                        cresp.setCELL(UCCHelper.makeCellId(data));
                    }
                    cresp.setRNDN("NA");
                    log.info("Msisdn is:"+cresp.getMSDN()+" and Cell id is:"+cresp.getCELL());
                    response=Response.status(Response.Status.OK).entity(cresp.toString()).build();
                } else {
                    response=Response.status(Response.Status.OK).entity("RDAT has wrong number of characters").build();
                }   break;
            default:
                log.error("Invalid Version submitted in the request");
                response=Response.status(Response.Status.BAD_REQUEST).entity("Invalid Version Value Submitted").build();
        }
        return response;
    }
}