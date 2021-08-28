/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.sra.helpers.SRAException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.*;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("radiusauthorisation")
public class RadiusAuthorisationResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(RadiusAuthorisationResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public String authorise(@QueryParam("u") String userName, @QueryParam("m") String mac, @QueryParam("ip") String ip) {
        start(request);
        try {
            checkPermissions(Permissions.SRA_RADIUS);

            ServiceInstanceQuery siq = new ServiceInstanceQuery();
            siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN);
            siq.setIdentifierType("WIFI_MAC");
            siq.setIdentifier(mac);
            ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
            log.debug("We have SI [{}]", si.getServiceInstanceId());

            this.ip = ip;
            this.mac = mac;
            this.userName = userName;
        } catch (SCABusinessError e) {
            log.debug("SI does not exist", e);
            throw new SRAException(Response.Status.UNAUTHORIZED);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return "{}";
    }
    
    private String userName;
    private String mac;
    private String ip;
    
//    public RadiusAVP getMac() {
//        return new RadiusAVP(RadiusAVP.DATA_TYPE.text, mac);
//    }
    
//    @JsonProperty("User-Name")
//    public RadiusAVP getUserName() {
//        return new RadiusAVP(RadiusAVP.DATA_TYPE.text, userName);
//    }
//
//    public RadiusAVP getMac() {
//        return new RadiusAVP(RadiusAVP.DATA_TYPE.text, mac);
//    }
}
