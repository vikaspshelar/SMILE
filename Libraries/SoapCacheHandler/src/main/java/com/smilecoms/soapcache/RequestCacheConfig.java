/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.soapcache;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author paul
 */
public class RequestCacheConfig {

    private List <ResourceKeyConfiguration> resourceKeyConfigurationList = new ArrayList();
    private List<String> cacheKeyIgnoreList = new ArrayList();
    private String requestDocType = null;
    private int cacheSecs = 60;
    private boolean applyInvalidation;

    RequestCacheConfig(String docType) {
        requestDocType = docType;
    }


    public List<String> getCacheKeyIgnoreList() {
        return cacheKeyIgnoreList;
    }

    public int getCacheSecs() {
        return cacheSecs;
    }

    public String getRequestDocType() {
        return requestDocType;
    }

    public void setCacheSecs(int cacheSecs) {
        this.cacheSecs = cacheSecs;
    }

    public boolean isApplyInvalidation() {
        return applyInvalidation;
    }

    public void setApplyInvalidation(boolean applyInvalidation) {
        this.applyInvalidation = applyInvalidation;
    }

    public List<ResourceKeyConfiguration> getResourceKeyConfigurationList() {
        return resourceKeyConfigurationList;
    }

    
}
