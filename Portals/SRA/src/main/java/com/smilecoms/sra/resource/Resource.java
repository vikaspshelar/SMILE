/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.resource;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.sra.helpers.InsufficientPrivilegesException;
import com.smilecoms.sra.helpers.SRAException;
import com.smilecoms.sra.helpers.Token;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public abstract class Resource {

    private static final Logger log = LoggerFactory.getLogger(Resource.class);

    public SRAException processError(Exception ex) {
        log.debug("In processError for [{}]", ex.toString());
        if (ex instanceof SRAException) {
            log.debug("This is already an SRAException");
            return (SRAException) ex;
        }
        return new SRAException(ex.getMessage(),"BUSINESS");
    }
    
    public SRAException processError(String errMessage, String errorType, String errCode, Response.Status httpStatusCode) {
        SRAException ex = new SRAException(new Exception("Internal server error"));
        try{
            ex = new  SRAException(errMessage, errorType, errCode, httpStatusCode);
        }catch (Exception e){
            
        }
        return ex;
    }

    public void checkPermissions(Object resourceObj) throws InsufficientPrivilegesException {
        String resource = resourceObj.toString();
        Token callersToken = Token.getRequestsToken();
        if (log.isDebugEnabled()) {
            log.debug("About to check permissions for user [{}] accessing resource [{}]", callersToken.getUsername(), resource);
        }
        for (String friendlyRole : callersToken.getGroups()) {
            if (friendlyRole.equals("Customer")) {
                continue;
            }
            if (isRoleAllowedResource(friendlyRole, resource)) {
                if (log.isDebugEnabled()) {
                    log.debug("Sufficient permissions for user [{}] accessing resource [{}] using role [{}]", new Object[]{callersToken.getUsername(), resource, friendlyRole});
                }
                return;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Insufficient privileges for user [{}] accessing resource [{}]", callersToken.getUsername(), resource);
        }
        throw new InsufficientPrivilegesException(callersToken.getUsername(), resource);
    }

    private boolean isRoleAllowedResource(String role, String resource) {
        return getRolesPermissions(role).contains(resource);
    }

    private static final String RESOURCES_KEY_PREFIX = "env.portal.allowed.resources.";

    public Set<String> getRolesPermissions(String role) {
        return BaseUtils.getPropertyAsSet(RESOURCES_KEY_PREFIX + role);
    }

    long start;
    String label;

    protected void start(HttpServletRequest context) {
        this.label = Thread.currentThread().getStackTrace()[2].getMethodName();
        this.start = System.currentTimeMillis();
    }

    protected void end() {
        if (label != null) {
            long time = System.currentTimeMillis() - start;
            BaseUtils.addStatisticSample("SRA." + label, BaseUtils.STATISTIC_TYPE.latency, time);
        } else {
            log.warn("Ending SRA method without a start being called");
        }
    }

    protected static String getObjectAsJsonString(Object obj) {
        Gson gson = new Gson();
        String jsonString;
        if (obj instanceof JsonElement) {
            JsonElement je = (JsonElement) obj;
            jsonString = gson.toJson(je);
        } else {
            jsonString = gson.toJson(obj);
        }
        log.debug("Json String is [{}]", jsonString);
        return jsonString;
    }

    protected Map<String, String> getHeadersInfo(HttpServletRequest request) {

        Map<String, String> map = new HashMap<>();

        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }
    
    protected String getHeaderByName(HttpServletRequest request, String header) {
        return request.getHeader(header);
    }

}
