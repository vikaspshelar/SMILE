/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.beans.PropertyBean;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Web Service
 *
 * @author paul
 */
@Path("properties")
public class PropertyResource extends Resource {

    private static final Logger log = LoggerFactory.getLogger(PropertyResource.class);
    @Context
    private javax.servlet.http.HttpServletRequest request;
    private PropertyBean propertyBean;
    
    public PropertyResource() {
    }
    
    private PropertyResource(String name) {
        if (!BaseUtils.getPropertyAsSet("env.sra.allowed.properties").contains(name)) {
            throw new RuntimeException("env.sra.allowed.properties does not include property " + name);
        }
        propertyBean = PropertyBean.getPropertyByName(name);
    }
    
    @Path("{propName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PropertyResource getProperty(@PathParam("propName") String propName) {
        start(request);
        try {
            return new PropertyResource(propName);
        } catch (Exception ex) {
            throw processError(ex);
        } finally {
            end();
        }
    }
    
    public PropertyBean getProperty() {
        log.debug("In getProperty");
        return propertyBean;
    }

}
