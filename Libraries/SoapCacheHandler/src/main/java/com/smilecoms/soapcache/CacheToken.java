/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.soapcache;

/**
 *
 * @author paul
 */
public class CacheToken {

    private int cacheSecs;
    private String cacheKey;
    private long dataTimestamp;
    
    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public int getCacheSecs() {
        return cacheSecs;
    }

    public void setCacheSecs(int cacheSecs) {
        this.cacheSecs = cacheSecs;
    }

    public long getDataTimestamp() {
        return dataTimestamp;
    }

    public void setDataTimestamp(long dataTimestamp) {
        this.dataTimestamp = dataTimestamp;
    }




}
