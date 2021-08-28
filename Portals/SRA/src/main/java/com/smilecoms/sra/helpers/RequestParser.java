/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class RequestParser {

    MultivaluedMap<String, String> formsParams;
    MultivaluedMap<String, String> queryParams;

    private static final Logger log = LoggerFactory.getLogger(RequestParser.class);

    public RequestParser(MultivaluedMap<String, String> formsParams, UriInfo uriInfo) {
        log.debug("In RequestParser for [{}] and [{}]", formsParams, uriInfo);
        this.formsParams = formsParams;
        this.queryParams = uriInfo.getQueryParameters();
    }
    
    public RequestParser(UriInfo uriInfo) {
        log.debug("In RequestParser for [{}] ", uriInfo);
        this.queryParams = uriInfo.getQueryParameters();
    }

    private String getParamFromEither(String name) {
        if (formsParams == null) {
            return queryParams.getFirst(name);
        }
        String val = formsParams.getFirst(name);
        if (val != null) {
            return val;
        }
        return queryParams.getFirst(name);
    }

    public String getParamAsString(String name) {
        return getParamFromEither(name);
    }

    public int getParamAsInt(String name) {
        return Integer.parseInt(getParamFromEither(name));
    }

    public int getParamAsInt(String name, int defaultValue) {
        String val = getParamFromEither(name);
        if (val == null) {
            return defaultValue;
        }
        return Integer.parseInt(val);
    }

    public long getParamAsLong(String name) {
        return Long.parseLong(getParamFromEither(name));
    }

    public boolean getParamAsBoolean(String name) {
        return Boolean.parseBoolean(getParamFromEither(name));
    }

    public long getParamAsLong(String name, long defaultValue) {
        String val = getParamFromEither(name);
        if (val == null) {
            return defaultValue;
        }
        return Long.parseLong(val);
    }

    public double getParamAsDouble(String name) {
        return Double.parseDouble(getParamFromEither(name));
    }

    public String getMethod() {
        String method = getParamFromEither("_method");
        if (method == null) {
            throw new RuntimeException("Request does not have _method parameter");
        }
        return method;
    }

}
