/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountList;
import com.smilecoms.commons.sca.Customer;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductInstanceList;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.ServiceInstanceList;
import com.smilecoms.commons.sca.ServiceSpecification;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.StCustomerLookupVerbosity;
import com.smilecoms.commons.sca.StServiceInstanceLookupVerbosity;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.sra.model.ModifyUserDefinedAttributesRequest;
import com.smilecoms.sra.model.UserDefinedServiceAttributes;
import com.smilecoms.commons.sca.beans.ServiceBean;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import net.sourceforge.stripes.action.Resolution;

/**
 *
 * @author lesiba
 */
@Path("services")
public class ServicesResource extends Resource {

    @Context
    private javax.servlet.http.HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{serviceInstanceId}")
    public ServiceResource getServiceInstance(@PathParam("serviceInstanceId") int serviceInstanceId) {
        start(request);
        try {
            return new ServiceResource(serviceInstanceId);
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }

    @PUT
    @Path("/{serviceInstanceId}/attributes")
    @Produces(MediaType.APPLICATION_JSON)
    public ServiceResource modifyUserDefinedAttributes(@PathParam("serviceInstanceId") int serviceInstanceId, MultivaluedMap<String, String> formParams, @Context UriInfo uriInfo) {
        start(request);
        try {
            Map<String, String> attributes = new HashMap<>();
            for (Entry<String, List<String>> entry : formParams.entrySet()) {                                
                attributes.put(entry.getKey(), entry.getValue().get(0));
            }
            return new ServiceResource(ServiceBean.changeAttributes(serviceInstanceId, attributes, true));
        } catch (Exception e) {
            throw processError(e);
        } finally {
            end();
        }
    }
    
    
     /**
     * API to Modify customer Service.
     * @param reqBody     
     * @return 
     */
    @Path("/{serviceInstanceId}/modify/attributes")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Object modifyUserDefinedAttributes(@PathParam("serviceInstanceId") int serviceInstanceId, ModifyUserDefinedAttributesRequest reqBody) {    
        start(request);
        try {    
            
            List<String> internetSpeeds = BaseUtils.getPropertyAsList("env.scp.slider.speed.config");
            
            boolean speedMatches = false;
            for (String speed : internetSpeeds) {
                double configSpdBitsPerSec = Double.parseDouble(speed);
                double convertedSpeed = (configSpdBitsPerSec / (1024 * 1024));
                if (convertedSpeed == Double.parseDouble(reqBody.getUserDefinedInternetUpDownlinkSpeed())) {
                    speedMatches = true;               
                    break;
                }
            }

            if (!speedMatches) {

                throw processError(new Exception("Invalid download/upload speed."));
            }        
            String userSpeed = String.valueOf(Double.parseDouble(reqBody.getUserDefinedInternetUpDownlinkSpeed())*1024*1024);
            
            Map<String, String> attributes = new HashMap<>();
            attributes.put("UserDefinedInternetDownlinkSpeed", userSpeed);
            attributes.put("UserDefinedInternetUplinkSpeed", userSpeed);
            attributes.put("UserDefinedDPIRules", reqBody.getUserDefinedDPIRules());   
            
            ServiceBean srvResource = ServiceBean.changeAttributes(serviceInstanceId, attributes, true);
            
            
            ServiceInstance userServiceInstance = SCAWrapper.getUserSpecificInstance().getServiceInstance(srvResource.getServiceInstanceId(), StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
            ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(userServiceInstance.getServiceSpecificationId());
            
            String userDefinedDPIRules="";
            String availableUserDPIRules = "";
            double currentBitsperSec=0;
            for(AVP avp: srvResource.getAVPs()) {
                
                        
                if (avp.getAttribute().equals("AvailableUserDefinedDPIRules")) {
                    availableUserDPIRules = avp.getValue();
                }
                
                if (avp.getAttribute().equals("UserDefinedDPIRules")) {
                        userDefinedDPIRules = avp.getValue();
                }
                
                if (avp.getAttribute().equals("UserDefinedInternetDownlinkSpeed")) {            
                    
                    if (!avp.getValue().isEmpty()) {
                        currentBitsperSec = Double.parseDouble(avp.getValue());
                    } else {

                        for (AVP ssAvp : ss.getAVPs()) {
                            if (ssAvp.getAttribute().equals("PCRFGxConfig")) {

                                List<String> pcrfConfig = Utils.getListFromCRDelimitedString(ssAvp.getValue());
                                for (String serviceDefaultInternetSpeed : pcrfConfig) {
                                    String[] prop = serviceDefaultInternetSpeed.split("=");

                                    if (prop[0].equals("DefaultBearerApnAmbrDownlink")) {                                  
                                        currentBitsperSec = Double.parseDouble(prop[1]);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    //break;
                }
            }
            
            List<String> allowedSpeeds = BaseUtils.getPropertyAsList("env.scp.slider.speed.config");
            List sliderSpeedList = new ArrayList<>();
            double closestSpeed = 0;
            for (String speed : allowedSpeeds) {               
                double configSpdBitsPerSec = Double.parseDouble(speed);
                double convertedSpeed = (configSpdBitsPerSec / (1024 * 1024)); //TODO 1024 should be based on property
                sliderSpeedList.add((int) convertedSpeed);
                if (configSpdBitsPerSec >= currentBitsperSec && closestSpeed == 0) {
                    closestSpeed = convertedSpeed;                    
                }
            }            
            
            int downAndUplinkInternetSpeed = (int) closestSpeed;
            
            UserDefinedServiceAttributes serviceAttributes= new UserDefinedServiceAttributes();            
            serviceAttributes.setCurrentBitsperSec(downAndUplinkInternetSpeed);
            serviceAttributes.setAvailableDPIRules(availableUserDPIRules);
            serviceAttributes.setUserDefinedDPIRules(userDefinedDPIRules);
            serviceAttributes.setAllowedSpeedList(sliderSpeedList);
            
            return serviceAttributes;
        } catch (Exception e) {
            throw processError(new Exception("Failed to process."+ e.getMessage().toString()));
        } finally {
            end();
        }
    }
    
    /**
     * API to Modify customer Service.
     * @param serviceInstanceId     
     * @return 
     */
    @Path("/{serviceInstanceId}/get/attributes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)    
    public Object getUserDefinedAttributes(@PathParam("serviceInstanceId") int serviceInstanceId) {    
        start(request);
        try {     
            String userDefinedDPIRules="";
            double currentBitsperSec = 0;
            int serviceId = 0;
            ServiceInstance userServiceInstance = new ServiceInstance();
            
            
            serviceId = serviceInstanceId;
            
            userServiceInstance = SCAWrapper.getUserSpecificInstance().getServiceInstance(serviceId, StServiceInstanceLookupVerbosity.MAIN_SVCAVP);
            
            ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(userServiceInstance.getServiceSpecificationId());
            String availableUserDPIRules = "";
            for (AVP siAvp : userServiceInstance.getAVPs()) {
                if (siAvp.getAttribute().equals("UserDefinedInternetDownlinkSpeed")) {            
                    
                    if (!siAvp.getValue().isEmpty()) {
                        currentBitsperSec = Double.parseDouble(siAvp.getValue());
                    } else {

                        for (AVP ssAvp : ss.getAVPs()) {
                            if (ssAvp.getAttribute().equals("PCRFGxConfig")) {

                                List<String> pcrfConfig = Utils.getListFromCRDelimitedString(ssAvp.getValue());
                                for (String serviceDefaultInternetSpeed : pcrfConfig) {
                                    String[] prop = serviceDefaultInternetSpeed.split("=");

                                    if (prop[0].equals("DefaultBearerApnAmbrDownlink")) {                                  
                                        currentBitsperSec = Double.parseDouble(prop[1]);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    //break;
                }
                
                if (siAvp.getAttribute().equals("UserDefinedDPIRules")) {
                    if (!siAvp.getValue().isEmpty()) {                       
                        userDefinedDPIRules = siAvp.getValue();
                    } 
                }
                
                 if (siAvp.getAttribute().equals("AvailableUserDefinedDPIRules")) {
                    availableUserDPIRules = siAvp.getValue();
                }
            }

            List<String> internetSpeeds = BaseUtils.getPropertyAsList("env.scp.slider.speed.config");
            List sliderSpeedList = new ArrayList<>();
            double closestSpeed = 0;
            for (String speed : internetSpeeds) {               
                double configSpdBitsPerSec = Double.parseDouble(speed);
                double convertedSpeed = (configSpdBitsPerSec / (1024 * 1024)); //TODO 1024 should be based on property
                sliderSpeedList.add((int) convertedSpeed);
                if (configSpdBitsPerSec >= currentBitsperSec && closestSpeed == 0) {
                    closestSpeed = convertedSpeed;                    
                }
            }
            int downAndUplinkInternetSpeed = (int) closestSpeed;
            
            UserDefinedServiceAttributes serviceAttributes= new UserDefinedServiceAttributes();
            serviceAttributes.setCurrentBitsperSec(downAndUplinkInternetSpeed);
            serviceAttributes.setAvailableDPIRules(availableUserDPIRules);
            serviceAttributes.setUserDefinedDPIRules(userDefinedDPIRules);
            serviceAttributes.setAllowedSpeedList(sliderSpeedList);
                    
            return serviceAttributes;

            
        } catch (Exception e) {
            throw processError(new Exception("Failed to process."+ e.getMessage().toString()));
        } finally {
            end();
        }
    }
    
    
    private boolean isSIDeactivatedOrSIMCard(ServiceInstance si) {
        boolean ret = false;
        ServiceSpecification ss = NonUserSpecificCachedDataHelper.getServiceSpecification(si.getServiceSpecificationId());
        if (si.getStatus().equals("TD") || ss.getName().equals("SIM Card")) {
            ret = true;
        }
        return ret;
    }

    private boolean checkIfStaffOrPartnerService(int serviceSpecificationId) {
        return (serviceSpecificationId >= 1000);
    }
}
