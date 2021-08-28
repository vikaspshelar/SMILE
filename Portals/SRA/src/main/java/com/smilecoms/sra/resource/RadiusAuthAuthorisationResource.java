/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.CustomerQuery;
import com.smilecoms.commons.sca.SCABusinessError;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceQuery;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.helpers.Permissions;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.sra.helpers.SRAException;
import com.smilecoms.sra.helpers.radius.AccessResponse;
import com.smilecoms.sra.helpers.radius.DiameterAgentHost;
import com.smilecoms.sra.helpers.radius.SubscriptionData;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
@Path("radius-authenticator")
public class RadiusAuthAuthorisationResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(RadiusAuthAuthorisationResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;

    @Path("")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @JsonIgnore
    public AccessResponse authauthorisation(@QueryParam("username") String userName, @QueryParam("password") String pwd) {
        start(request);
        AccessResponse ar = new AccessResponse();
        try {
            checkPermissions(Permissions.SRA_RADIUS);
            CustomerQuery customerQuery = new CustomerQuery();
            customerQuery.setResultLimit(1);
            customerQuery.setVerbosity(StCustomerLookupVerbosity.CUSTOMER);

            String naiID = Utils.makeNAIIdentityFromUsername(userName);
            ServiceInstanceQuery siq = new ServiceInstanceQuery();
            siq.setVerbosity(StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
            siq.setIdentifierType("END_USER_NAI");
            siq.setIdentifier(naiID);
            log.debug("Retrieving WLAN SI using username [{}] identifier type [{}]", naiID, "END_USER_NAI");
            ServiceInstance si = SCAWrapper.getAdminInstance().getServiceInstance(siq);
            log.debug("We have WLAN SI [{}]", si.getServiceInstanceId());

            String avpPassword = getPasswordAVPforWifiSvc(si.getAVPs());
            if (avpPassword.isEmpty()) {
                throw new Exception("WLAN service password not set");
            }
            
            //make a hash
            String password = Utils.oneWayHash(pwd);
            //String password = Codec.stringToEncryptedHexString(pwd);
            SubscriptionData subscriptionData = new SubscriptionData();

            if (!authoriseWifiService(password, avpPassword)) {
                log.debug("Invalid credentials for WLAN service");
                subscriptionData.setSubscriberStatus("ACCESS REJECT");
                subscriptionData.setSubscriptionIdData(userName);
                subscriptionData.setSubscriptionIdType("");
                ar.setSubscriptionData(subscriptionData);
                return ar;
            }
            log.debug("All is good with this service");
            subscriptionData.setSubscriberStatus("ACCESS ACCEPT");
            subscriptionData.setSubscriptionIdData(naiID);
            subscriptionData.setSubscriptionIdType("END_USER_NAI");
            
            ar.setSubscriptionData(subscriptionData);
            setDiameterHosts(ar);

        } catch (SCABusinessError e) {
            log.debug("SI does not exist", e);
            throw new SRAException(Response.Status.UNAUTHORIZED);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
        return ar;
    }

    private void setDiameterHosts(AccessResponse ar) {
        Set<String> diamHostsConfig = BaseUtils.getPropertyAsSet("env.sra.wlan.service.diameter.hosts");
        for (String hostConfig : diamHostsConfig) {
            DiameterAgentHost diameterAgentHost = new DiameterAgentHost();
            String host = hostConfig.split(Pattern.quote("|"))[0]; //pcrf1|za.smilecoms.com|16777224
            String realm = hostConfig.split(Pattern.quote("|"))[1];
            String appId = hostConfig.split(Pattern.quote("|"))[2];
            diameterAgentHost.setName(host);
            diameterAgentHost.setRealm(realm);
            diameterAgentHost.setAuthAppId(appId.split(","));
            ar.getDiameterAgentHosts().add(diameterAgentHost);
        }
    }

    private String getPasswordAVPforWifiSvc(List<AVP> avPs) {
        if (avPs == null) {
            return "";
        }
        String pass = "";
        for (AVP avp : avPs) {
            if (avp != null && avp.getAttribute() != null) {
                if (avp.getAttribute().equalsIgnoreCase("NAIPassword")) {
                    pass = avp.getValue();
                    break;
                }
            }
        }
        return pass;
    }

    private boolean authoriseWifiService(String hashedPassword, String wifiAVPHashedPassword) {
        if (BaseUtils.getBooleanProperty("env.sra.wlanservice.passwordcheck.disabled", false)) {
            return true;
        }
        log.debug("Checking if [{}] matches [{}]", hashedPassword, wifiAVPHashedPassword);
        if (wifiAVPHashedPassword.equals(hashedPassword)) {
            log.debug("Password is correct");
            return true;
        }
        return false;
    }
}
