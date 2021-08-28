/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.soapcache;

/**
 *
 * @author paul
 */
public class NotCacheableException extends java.lang.Exception {

    private static NotCacheableException me = new NotCacheableException();
    private NotCacheableException() {
    }
    
    public static NotCacheableException getInstance() {
        return me;
    }
    
    @Override
    public String toString() {
        return "That request is not configured to be cacheable or an error occured processing the request for caching";
    }
}
