/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.soapcache;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.slf4j.*;

/**
 *
 * @author paul
 */
public class Cache {

    private static final String CLASS = Cache.class.getName();
    private static Logger log = LoggerFactory.getLogger(CLASS);

    protected static boolean getMessageFromCache(SOAPMessageContext requestMessageContext) {
        if (log.isDebugEnabled()) {
            log.debug("In getMessageFromCache. Getting the cache configuration for the request type.");
        }
        CacheManager cacheManager;
        try {
            cacheManager = new CacheManager(requestMessageContext);
        } catch (NotCacheableException nce) {
            // A request like this is not cacheable
            if (log.isDebugEnabled()) {
                log.debug("Returning from getMessageFromCache. The request does not have a cache configuration.");
            }
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("The request does have a cache configuration. Checking if there is a cached response.");
        }
        SOAPMessage cachedResponseMessage = cacheManager.getCachedResponse();
        if (cachedResponseMessage != null) {
            requestMessageContext.setMessage(cachedResponseMessage);
            if (log.isDebugEnabled()) {
                log.debug("Returning from getMessageFromCache. The request has a response from cache. Thats great News!");
            }
            return true;
        }
        // No result in cache
        if (log.isDebugEnabled()) {
            log.debug("Returning from getMessageFromCache. The request does not have a response in cache. What a pitty!");
        }
        return false;
    }

    protected static boolean putMessageInCache(SOAPMessageContext responseMessageContext) {
        if (log.isDebugEnabled()) {
            log.debug("In putMessageInCache. Getting the cache configuration from the context put in when the request was processed.");
        }
        boolean ret = false;
        try {
            CacheManager cacheManager = new CacheManager(responseMessageContext);
            if (log.isDebugEnabled()) {
                log.debug("This response had cache configuration info in the context. Now going to get cacheManager to put the response in the cache.");
            }
            ret = cacheManager.putResponseInCache();
            if (log.isDebugEnabled()) {
                log.debug("Successfully put the response in the cache.");
            }
        } catch (NotCacheableException ex) {
            if (log.isDebugEnabled()) {
                log.debug("This response object does not have a cache configuration and hence should not be put in the cache");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Returning from putMessageInCache. Put in cache: " + ret);
        }
        return ret;
    }
}
