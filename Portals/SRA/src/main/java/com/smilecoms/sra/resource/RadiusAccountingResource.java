/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.SCADelegate;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.direct.bm.ChargingData;
import com.smilecoms.commons.sca.direct.bm.ChargingRequest;
import com.smilecoms.commons.sca.direct.bm.ChargingResult;
import com.smilecoms.commons.sca.direct.bm.PlatformContext;
import com.smilecoms.commons.sca.direct.bm.RatingKey;
import com.smilecoms.commons.sca.direct.bm.ServiceInstanceIdentifier;
import com.smilecoms.commons.sca.direct.bm.UsedServiceUnit;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sra.helpers.RequestParser;
import com.smilecoms.sra.helpers.SRAException;
import com.smilecoms.sra.helpers.radius.RadiusChargingData;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.*;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("radiusaccounting")
public class RadiusAccountingResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(RadiusAccountingResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public String account(@Context UriInfo uriInfo, RadiusChargingData rcd) {
        start(request);
        try {
            checkPermissions(Permissions.SRA_RADIUS);
            log.debug("Account status type: [{}]", rcd.getAcctStatusType() );
            if (rcd.getAcctStatusType().getIntValue() == 7 || rcd.getAcctStatusType().getIntValue() == 8) {
                RequestParser parser = new RequestParser(uriInfo);
                String mac = parser.getParamAsString("m");
                if (mac == null) {
                    log.warn("MAC is null");
                    throw new SRAException(Response.Status.UNAUTHORIZED);
                }
                mac = mac.toUpperCase();
                log.debug("This is an accounting on request for gateway [{}]", mac);
                if (!BaseUtils.getPropertyAsSet("env.sra.wifi.gateway.macs").contains(mac)) {
                    throw new SRAException(Response.Status.UNAUTHORIZED);
                }
                log.debug("Gateway is known");

            } else {

                ChargingRequest cr = new ChargingRequest();
                ChargingData cd = new ChargingData();
                cr.setPlatformContext(new PlatformContext());
                cr.getPlatformContext().setTenant(SCADelegate.ADMIN_TENANT);
                cr.getChargingData().add(cd);

                cd.setChargingDataIndex(0);
                cd.setDescription("WiFi");
                cd.setEventTimestamp(Utils.getDateAsXMLGregorianCalendar(new Date()));
                cd.setIPAddress(rcd.getFramedIPAddress().getValue());
                cd.setLocation("");
                cd.setRatingKey(new RatingKey());
                cd.getRatingKey().setFrom("");
                cd.getRatingKey().setTo("");
                cd.getRatingKey().setServiceCode("WIFI");
                cd.setServiceInstanceIdentifier(new ServiceInstanceIdentifier());
                cd.getServiceInstanceIdentifier().setIdentifierType("WIFI_IP");
                cd.getServiceInstanceIdentifier().setIdentifier(rcd.getFramedIPAddress().getValue());
                cd.setSessionId(rcd.getAcctUniqueSessionId().getValue());
                cd.setUserEquipment("MAC=" + rcd.getCalledStationId().getValue());
                cd.setUsedServiceUnit(new UsedServiceUnit());
                cd.getUsedServiceUnit().setUnitType("OCTET");
                cd.getUsedServiceUnit().setUnitQuantity(rcd.getAcctInputOctets().getBigDecimalValue().add(rcd.getAcctOutputOctets().getBigDecimalValue()));
                cd.getUsedServiceUnit().setTotalSessionUnits(true);
                if (rcd.getAcctStatusType().getIntValue() == 2) {
                    cd.getUsedServiceUnit().setTerminationCode(rcd.getAcctTerminateCause().getValue());
                }
                ChargingResult res = SCAWrapper.getAdminInstance().rateAndBill_Direct(cr);
                String err = res.getGrantedServiceUnits().get(0).getErrorCode();
                if (err != null) {
                    log.debug("Error charging [{}]", err);
                    throw new SRAException(Response.Status.UNAUTHORIZED);
                }
            }
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return "{}";
    }

}
