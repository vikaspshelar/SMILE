/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.soapcache;

import java.io.Serializable;

/**
 *
 * @author paul
 */
public class CachedObject implements Serializable {

    private Object cacheData = null;
    private long dataTimestamp;

    public CachedObject(Object o, long ts) {
        this.cacheData = o;
        this.dataTimestamp = ts;
    }
    public Object getCacheData() {
        return cacheData;
    }

    public void setCacheData(Object cacheData) {
        this.cacheData = cacheData;
    }

    public long getDataTimestamp() {
        return dataTimestamp;
    }

    public void setDataTimestamp(long dataTimestamp) {
        this.dataTimestamp = dataTimestamp;
    }



}
