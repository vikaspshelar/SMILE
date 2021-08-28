/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.rra.tcra.controller;

import com.smilecoms.commons.sca.Event;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.rra.common.Utilities;
import com.smilecoms.rra.model.RegistrationImageResponse;
import com.smilecoms.rra.service.TCRADataService;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author abhilash
 */
@RestController
@RequestMapping("/tcra/csis")
public class TCRARestController {
    private static final Logger log = LoggerFactory.getLogger(TCRARestController.class);
    
    @Autowired
    private TCRADataService dataService;
    
    @RequestMapping(value = "/registrationresult", method = RequestMethod.POST, consumes = {MediaType.TEXT_XML})
    public Response processRegistrationResult(@RequestBody String registrationResult) 
    {
        Utilities utilities = new Utilities();
        try {
                String eventKey = utilities.extractValue("<operatorConversationId>(.*)</operatorConversationId>",registrationResult);;

                if (eventKey.isEmpty()) {
                    Response.status(Response.Status.BAD_REQUEST).build();
                }
                
                Event event = new Event();
                event.setEventType("TCRA_CSIS");
                event.setEventSubType("RegistrationResult");
                event.setEventData(registrationResult);
                event.setEventKey(eventKey);                

                try {
                    SCAWrapper.getAdminInstance().createEvent(event);
                } catch (Exception ex) {
                    log.warn("Failed to create event for CSIS via SCA call, cause: {}", ex.toString());
                }       
            } catch (Exception ex) {
                log.error("Error while processing the processRegistrationResult:"+ex);
            }
        return Response.status(Response.Status.OK).build();
    }
    
    @RequestMapping(value = "/imagequery", method = RequestMethod.GET, consumes = {MediaType.TEXT_XML}, produces = {MediaType.TEXT_XML})
    public RegistrationImageResponse getCustomerImages(@RequestBody String requestData, HttpServletResponse resp) throws IOException 
    {   
        Utilities utilities = new Utilities();
        
        String iccid = utilities.extractValue("<iccid>(.*)</iccid>",requestData);
        String msisdn = utilities.extractValue("<msisdn>(.*)</msisdn>",requestData);
        boolean iccidFlag = (!iccid.equals("") || !iccid.isEmpty());
        boolean msisdnFlag = (!msisdn.equals("") || !msisdn.isEmpty());
        
        log.info("ICCID is: "+iccid+" and MSISDN is: "+msisdn);
        
        if(!iccidFlag && !msisdnFlag)
        {
            resp.sendError(400, "Both MSISDN And ICCID values are missing in request");
        }        
        
        return dataService.getImageData(iccid,msisdn);
    }
    
    @RequestMapping(value = "/savesimapprovaldata", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON})
    public Response saveSimApprovalData(@RequestBody String registrationResult) 
    {
        Utilities utilities = new Utilities();
        try {
            
                
                String eventKey = utilities.extractValue("<operatorConversationId>(.*)</operatorConversationId>",registrationResult);;

                if (eventKey.isEmpty()) {
                    Response.status(Response.Status.BAD_REQUEST).build();
                }
                
                Event event = new Event();
                event.setEventType("TCRA_CSIS");
                event.setEventSubType("RegistrationResult");
                event.setEventData(registrationResult);
                event.setEventKey(eventKey);                

                try {
                    SCAWrapper.getAdminInstance().createEvent(event);
                } catch (Exception ex) {
                    log.warn("Failed to create event for CSIS via SCA call, cause: {}", ex.toString());
                }       
            } catch (Exception ex) {
                log.error("Error while processing the processRegistrationResult:"+ex);
            }
        return Response.status(Response.Status.OK).build();
    }
}
