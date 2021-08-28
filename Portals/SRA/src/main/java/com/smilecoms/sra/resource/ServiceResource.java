/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.sca.beans.ServiceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lesiba
 */
public class ServiceResource  extends Resource {
    private static final Logger log = LoggerFactory.getLogger(ServiceResource.class);
    private ServiceBean service;
    
    public ServiceResource() {
    }
    
    public ServiceResource(int serviceInstanceId){
        service = ServiceBean.getServiceInstanceById(serviceInstanceId);
    }
    public ServiceResource(ServiceBean service){
        this.service = service;
    }
    
    public ServiceBean getService(){
        log.debug("In getService");
        return service;
    }
    
    public void setService(ServiceBean service){
        this.service = service;
    }
}
