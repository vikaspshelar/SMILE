/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.soapcache;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.util.HashUtils;
import com.smilecoms.commons.util.Stopwatch;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.namespace.NamespaceContext;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.slf4j.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author paul
 */
public class CacheManager {

    SOAPMessageContext context = null;
    private static final String CLASS = CacheManager.class.getName();
    private static final Logger log = LoggerFactory.getLogger(CLASS);
    private CacheToken cacheToken = null;
    private static final String CACHE_CONFIG_KEY_NAME = "SoapCacheConfig";
    private static final int INVALIDATION_LIFETIME = 3600 * 3; // must be longer than the longest cache time. 3 hours more than enough
    boolean inbound;
    private static final StringBuffer initLock = new StringBuffer("unlocked");
    private static Map<String, RequestCacheConfig> soapMethodConfigurationMap = null;
    RequestCacheConfig thisRequestsConfig = null;
    private static boolean inStartUp = true;
    private static final long startupTime = System.currentTimeMillis();
    private static long lastConfigLoad = 0;
    private static String lastConfigProp = "";
    public static final String SOAP_DOC_KEY = "soapdoc";

    public CacheManager(SOAPMessageContext context) throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("Entering constructor for CacheManager");
        }

        if (inStartUp) {
            if (System.currentTimeMillis() - startupTime < 10000) {
                if (log.isDebugEnabled()) {
                    log.debug("Cache manager has disabled caching for the first 10 seconds after server boot so that bootup can happen without caching");
                }
                throw NotCacheableException.getInstance();
            } else {
                log.warn("Cache Manager has now started caching!");
                inStartUp = false;
            }
        }

        if (soapMethodConfigurationMap == null || (System.currentTimeMillis() - lastConfigLoad) > 185000) {
            if (log.isDebugEnabled()) {
                log.debug("Performing initialisation of the cache configuration");
            }
            populateConfig();
        }

        this.context = context;
        try {
            inbound = !(Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            if (inbound) {
                if (log.isDebugEnabled()) {
                    log.debug("Cache manager is being constructed with an inbound soap request");
                }
                // This is a request object
                // Get the cache configuration for the request
                setRequestCacheConfiguration();
                // Look at the request and process it to ensure invalidation of cached data that could be modified by this request
                if (thisRequestsConfig.isApplyInvalidation()) {
                    setInvalidationData();
                }
                // Check if this request should be cached
                if (thisRequestsConfig.getCacheSecs() == 0) {
                    // 0 cache time means dont cache
                    if (log.isDebugEnabled()) {
                        log.debug("This request does have a cache configuration but only for invalidation as cache secs=0. Finished doing what we need to for this request.");
                    }
                    throw NotCacheableException.getInstance();
                }

                // Now look if there is a cached response
                if (log.isDebugEnabled()) {
                    log.debug("Going to look in the cache for a response to this request");
                }
                cacheToken = new CacheToken();
                cacheToken.setCacheKey(getCacheKeyForRequest());
                cacheToken.setCacheSecs(thisRequestsConfig.getCacheSecs());
                //Indicate that any data put in the cache for this request was retrieved now
                cacheToken.setDataTimestamp(System.currentTimeMillis());
                //Put the config into the context so its available in the return path
                context.put(CACHE_CONFIG_KEY_NAME, cacheToken);
                if (log.isDebugEnabled()) {
                    log.debug("Done doing all logic for constructing a cache manager for an inbound request");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cache manager is being constructed with an outbound soap request");
                }
                cacheToken = getCacheTokenFromResponse();
                if (log.isDebugEnabled()) {
                    log.debug("Done doing all logic for constructing a cache manager for an outbound request");
                }
            }
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Exiting constructor for CacheManager");
            }
        }
    }

    protected SOAPMessage getCachedResponse() {
        if (log.isDebugEnabled()) {
            log.debug("In getCachedResponse. Looking up response in cache with key " + cacheToken.getCacheKey());
        }
        SOAPMessage cachedResponseMessage;
        try {
            CachedObject co = (CachedObject) CacheHelper.getFromRemoteCache(cacheToken.getCacheKey());
            if (co == null) {
                // Nothing in cache
                if (log.isDebugEnabled()) {
                    log.debug("Finished getCachedResponse. There was nothing in the cache with key " + cacheToken.getCacheKey());
                }
                return null;
            }

            // There is something in cache, but is it still valid???
            // Convert byte array into a soap message
            cachedResponseMessage = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream((byte[]) co.getCacheData()));

            if (isCacheDataStale(co.getDataTimestamp(), cachedResponseMessage)) {
                // Remove it from the cache
                if (log.isDebugEnabled()) {
                    log.debug("Stale cache data found for key " + cacheToken.getCacheKey() + ". Removing response from cache");
                }
                CacheHelper.removeFromRemoteCache(cacheToken.getCacheKey());
                if (log.isDebugEnabled()) {
                    log.debug("Finished getCachedResponse cause cached data was stale");
                }
                return null;
            }

            if (log.isDebugEnabled()) {
                log.debug("Got message from cache using key [" + cacheToken.getCacheKey() + "]");
            }
        } catch (Exception ex) {
            log.warn("Error in getCachedResponse: " + ex.toString());
            cachedResponseMessage = null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished getCachedResponse. Returning cached response? " + (cachedResponseMessage != null));
        }
        return cachedResponseMessage;
    }

    protected boolean putResponseInCache() {
        if (log.isDebugEnabled()) {
            log.debug("Entering putResponseInCache");
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Writing the response message to a byte array so we can put it in remote cache");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            context.getMessage().writeTo(baos);
            byte[] toCache = baos.toByteArray();

            if (log.isDebugEnabled()) {
                log.debug("putInCache writing key [" + cacheToken.getCacheKey() + "] with cache value [" + toCache.toString() + "] for duration [" + cacheToken.getCacheSecs() + "sec]");
            }
            CacheHelper.putInRemoteCache(cacheToken.getCacheKey(), new CachedObject(toCache, cacheToken.getDataTimestamp()), cacheToken.getCacheSecs());
            if (log.isDebugEnabled()) {
                log.debug("putMessageInCache finished writing key [" + cacheToken.getCacheKey() + "]");
            }
        } catch (Exception ex) {
            log.warn("Error in putInCache: " + ex.toString());
            return false;
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Finished putResponseInCache");
            }
        }
        return true;
    }

    /*
     *
     * INTERNAL METHODS
     *
     */
    private String getCacheKeyForRequest() throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("In getCacheKeyForRequest");
        }
        //Make the md5 key
        String cacheKey = null;
        try {
            Document doc = getDocumentForKeyGeneration();
            cacheKey = HashUtils.md5(XMLUtils.doc2bytes(doc));
        } catch (Exception e) {
            log.warn("Cannot generate md5 key from XML document. No cache lookup will take place: " + e.toString());
            throw NotCacheableException.getInstance();
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished getCacheKeyForRequest. The cache key for this request is " + cacheKey);
        }
        return cacheKey;
    }

    private CacheToken getCacheTokenFromResponse() throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("In getCacheTokenFromResponse");
        }
        CacheToken cc = (CacheToken) context.get(CACHE_CONFIG_KEY_NAME);
        if (cc == null) {
            if (log.isDebugEnabled()) {
                log.debug("Finished getCacheTokenFromResponse. The request configuration indicated that this response should not be cached as there is no cache configuration in the context");
            }
            throw NotCacheableException.getInstance();
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished getCacheTokenFromResponse. There was a token in the context");
        }
        return cc;
    }

    private Document getDocumentForKeyGeneration() throws SOAPException {
        if (log.isDebugEnabled()) {
            log.debug("In getDocumentForKeyGeneration. Going to get a copy of the XML request");
        }
        // We cant remove data from the original request so we must copy it
        Document myDoc = (Document) context.getMessage().getSOAPBody().getOwnerDocument().cloneNode(true);
        if (log.isDebugEnabled()) {
            log.debug("Getting a list of xml elements that must be excluded when generating a hash of the request");
        }
        List<String> cacheKeyIgnoreList = thisRequestsConfig.getCacheKeyIgnoreList();
        for (String elementToRemove : cacheKeyIgnoreList) {

            // Remove elements that shouldnt be included in the md5 key
            int lastColon = elementToRemove.lastIndexOf(":");
            String ns = elementToRemove.substring(0, lastColon);
            String tag = elementToRemove.substring(lastColon + 1);
            if (log.isDebugEnabled()) {
                log.debug("Removing all tags with ns " + ns + " and id " + tag + " from xml document");
            }
            NodeList nodes = myDoc.getElementsByTagNameNS(ns, tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                nodes.item(i).getParentNode().removeChild(nodes.item(i));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished getDocumentForKeyGeneration");
        }
        return myDoc;
    }

    private String getRequestDocumentType() throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("In getRequestDocumentType");
            Stopwatch.start();
        }
        String docType = null;
        try {
            Document soapDoc = (Document) context.getMessage().getSOAPBody().getOwnerDocument();
            Node bodyNode = soapDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0);
            Node mainContentNode = null;
            for (int i = 0; i < bodyNode.getChildNodes().getLength(); i++) {
                Node child = bodyNode.getChildNodes().item(i);
                if (child.getLocalName() != null) {
                    mainContentNode = child;
                    break;
                }
            }
            if (mainContentNode == null) {
                log.debug("Cannot determine docType. Caching wont happen");
                docType = "null:null";
            } else {
                String localName = mainContentNode.getLocalName();
                docType = mainContentNode.getNamespaceURI() + ":" + localName;
                // Put soap doc type in context
                context.put(SOAP_DOC_KEY, localName);
            }

        } catch (Exception ex) {
            log.warn("Error: ", ex);
            log.warn("Finished getRequestDocumentType. Error in getRequestDocumentType. This soap request thus cant be cached:", ex);
            throw NotCacheableException.getInstance();
        }
        if (log.isDebugEnabled()) {
            Stopwatch.stop();
            log.debug("Finished getRequestDocumentType. Document type of request is " + docType + ". Time taken: " + Stopwatch.millisString());
        }
        return docType;
    }

    private List<String> getInvalidationResourceKeys(SOAPMessage request, SOAPMessage response) throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("In getInvalidationResourceKeysForRequest");
        }
        List<String> resourceKeysToReturn = new ArrayList();

        int keyCount = 0;
        int partCount = 0;

        try {
            Node requestBodyNode = null;
            Node responseBodyNode = null;

            List<ResourceKeyConfiguration> resourceKeyConfigurationList = thisRequestsConfig.getResourceKeyConfigurationList();
            if (log.isDebugEnabled()) {
                log.debug("This requests configuration stipulates that " + resourceKeyConfigurationList.size() + " resource keys are impacted by this request");
            }

            for (ResourceKeyConfiguration resourceKey : resourceKeyConfigurationList) {
                keyCount++;
                StringBuilder resourceKeyPlainString = new StringBuilder();
                partCount = 0;
                for (ResourceKeyPartConfiguration resourceKeyPart : resourceKey.getPartsList()) {
                    partCount++;
                    if (log.isDebugEnabled()) {
                        log.debug("Getting invalidation resource part " + partCount + " of invalidation resource key " + keyCount);
                    }
                    if (resourceKeyPart.getType() == ResourceKeyPartConfiguration.TYPE_XPATH) {
                        // XPath Query

                        if (resourceKey.getDocumentToExtractPartFrom() == ResourceKeyConfiguration.DOCUMENT_REQUEST) {
                            if (log.isDebugEnabled()) {
                                log.debug("Performing invalidation xquery on request document");
                            }
                            if (requestBodyNode == null) {
                                Document soapDoc = (Document) request.getSOAPBody().getOwnerDocument();
                                requestBodyNode = soapDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0);
                            }
                            Object result = resourceKeyPart.getXpathPart().evaluate(requestBodyNode, XPathConstants.STRING);
                            log.debug("XQuery result is [{}]", result);
                            resourceKeyPlainString.append((String) result);
                        } else if (resourceKey.getDocumentToExtractPartFrom() == ResourceKeyConfiguration.DOCUMENT_RESPONSE) {
                            if (log.isDebugEnabled()) {
                                log.debug("Performing invalidation xquery on response document");
                            }
                            if (responseBodyNode == null) {
                                Document soapDoc = (Document) response.getSOAPBody().getOwnerDocument();
                                responseBodyNode = soapDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body").item(0);
                            }
                            Object result = resourceKeyPart.getXpathPart().evaluate(responseBodyNode, XPathConstants.STRING);
                            log.debug("XQuery result is [{}]", result);
                            resourceKeyPlainString.append((String) result);
                        }

                    } else if (resourceKeyPart.getType() == ResourceKeyPartConfiguration.TYPE_STRING) {
                        //plain string
                        if (log.isDebugEnabled()) {
                            log.debug("Invalidation is a plain string: " + resourceKeyPart.getStrPart());
                        }
                        resourceKeyPlainString.append((String) resourceKeyPart.getStrPart());
                    } else {
                        log.warn("This resource invalidation data is invalid. It must be a String or XPathExpression. Ignoring. key " + keyCount);
                    }
                    log.debug("resourceKeyPlainString is now [{}]", resourceKeyPlainString);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Got a (pre md5) invalidation resource of : " + resourceKeyPlainString.toString());
                }
                String resourceKeyHash = getInvalidationHash(resourceKeyPlainString.toString());
                if (log.isDebugEnabled()) {
                    log.debug("Got a (post md5) invalidation resource of : " + resourceKeyHash + ". Adding it to the list");
                }
                resourceKeysToReturn.add(resourceKeyHash);
            }
        } catch (Exception ex) {
            log.warn("Error in getInvalidationResourceKeysForRequest(Key=" + keyCount + " Part=" + partCount + "): " + ex.toString() + ". This request will not use the cache");
            log.warn("Error: ", ex);
            throw NotCacheableException.getInstance();
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished getInvalidationResourceKeysForRequest. Returning list containing " + resourceKeysToReturn.size() + " md5 resource invalidation keys");
        }
        return resourceKeysToReturn;
    }

    /**
     * Checks if the data in the cache is still valid or not
     *
     * @return
     */
    private boolean isCacheDataStale(Long cacheDataTimestamp, SOAPMessage cacheResult) throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("In isCacheDataStale with timestamp " + cacheDataTimestamp + ". Going to get the invalidation resource keys for the request that this cached response applies to");
        }
        List<String> invalidationResourcekeys = getInvalidationResourceKeys(context.getMessage(), cacheResult);
        if (invalidationResourcekeys.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Finished isCacheDataStale. There are no invalidation resources to process. This cached response data is thus still fresh");
            }
            return false;
        }
        for (String invalidationResourcekey : invalidationResourcekeys) {
            if (log.isDebugEnabled()) {
                log.debug("Getting invalidation resource key " + invalidationResourcekey + " from remote cache");
            }
            Long staleFrom = (Long) CacheHelper.getFromRemoteCache(invalidationResourcekey);
            if (staleFrom == null) {
                // The cache is not stale as no changes have been made to the resource instance
                if (log.isDebugEnabled()) {
                    log.debug("Resource key " + invalidationResourcekey + " has no invalidation data so it must be fresh");
                }
                continue;
            }

            if (staleFrom >= cacheDataTimestamp) {
                // Something has changed since the cache data was obtained
                if (log.isDebugEnabled()) {
                    log.debug("Finished isCacheDataStale. Resource key " + invalidationResourcekey + " was last modified at " + staleFrom + " which is after " + cacheDataTimestamp + ". This cached response data is thus stale.");
                }
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Resource key " + invalidationResourcekey + " was last modified at " + staleFrom + " which is before " + cacheDataTimestamp + ". This cached response data is thus fresh... so far.");
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished isCacheDataStale. This cached response data is fresh");
        }
        return false;
    }

    /**
     * Inserts memory records of resource instances that have been updated
     */
    private void setInvalidationData() throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("In setInvalidationData. Going to get the resource keys that must be invalidated based on this request");
        }
        List<String> invalidationResourceKeys = getInvalidationResourceKeys(context.getMessage(), null);
        if (log.isDebugEnabled()) {
            log.debug("There are " + invalidationResourceKeys.size() + " invalidation resources for this request. Going to add invalidation entries for them");
        }

        Long lastModified = null;
        if (!invalidationResourceKeys.isEmpty()) {
            lastModified = (Long) System.currentTimeMillis();
        }

        for (String invalidationResourceKey : invalidationResourceKeys) {
            // Store the fact that this resource instance became invalid as of now
            if (log.isDebugEnabled()) {
                log.debug("Invalidating resource " + invalidationResourceKey + " for any cache older than now by adding a record in remote cache");
            }
            CacheHelper.putInRemoteCacheSync(invalidationResourceKey, lastModified, INVALIDATION_LIFETIME);
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished setInvalidationData");
        }
    }

    private void setRequestCacheConfiguration() throws NotCacheableException {
        if (log.isDebugEnabled()) {
            log.debug("In setRequestCacheConfiguration. Going to get the document type of the request");
        }
        String requestDocType = getRequestDocumentType();
        if (log.isDebugEnabled()) {
            log.debug("This request is of type " + requestDocType + ". Going to look in configuration map for this requests configuration");
        }
        thisRequestsConfig = soapMethodConfigurationMap.get(requestDocType);
        if (thisRequestsConfig == null) {
            //No cache configuration
            if (log.isDebugEnabled()) {
                log.debug("Finished setRequestCacheConfiguration. This request has no cache configuration and hence no further cache logic need be done.");
            }
            throw NotCacheableException.getInstance();
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished setRequestCacheConfiguration. Successfully set the cache configuration for this request of type " + requestDocType);
        }
    }

    private void populateConfig() throws NotCacheableException {

        // Dont even bother if properties are not available yet as one could end in an infinite loop
        // The call to get props below is the only one in SCA
        if (!BaseUtils.isPropsAvailable()) {
            throw NotCacheableException.getInstance();
        }

        synchronized (initLock) {
            if (initLock.toString().equals("locked")) {
                //Dont do if its being done
                if (soapMethodConfigurationMap == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cache configuration is being loaded currently but is not ready yet");
                    }
                    throw NotCacheableException.getInstance();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Cache configuration is being loaded currently but the old one can be used");
                    }
                    return;
                }
            }
            initLock.setLength(0);
            initLock.append("locked");
        }

        try {

            if (log.isDebugEnabled()) {
                log.debug("Entering populateConfig to initialise the cache configuration");
            }

            String thisConfigProp = BaseUtils.getProperty("global.sca.cache.config");
            if (thisConfigProp.equals(lastConfigProp)) {
                //No need to re-initialise
                if (log.isDebugEnabled()) {
                    log.debug("The configuration has not changed. No need to re-configure");
                }
                return;
            }



            XPath xpath = XPathFactory.newInstance().newXPath();
            NamespaceContext nsContext = new NamespaceContextMap("sca", "http://xml.smilecoms.com/schema/SCA");
            xpath.setNamespaceContext(nsContext);
            Map<String, RequestCacheConfig> soapMethodConfigurationMapTmp = new HashMap();
            String method;
            String ignore;
            String cacheSecs;
            String applyInvalidation;
            String resourceKey;
            RequestCacheConfig conf = null;
            StringTokenizer stValues = new StringTokenizer(thisConfigProp, "\r\n");

            while (stValues.hasMoreTokens()) {
                String line = stValues.nextToken();
                if (line.startsWith("method=")) {
                    //New function config
                    method = line.substring(7);
                    if (log.isDebugEnabled()) {
                        log.debug("Processing config for method " + method);
                    }
                    soapMethodConfigurationMapTmp.put(method, new RequestCacheConfig(method));
                    conf = soapMethodConfigurationMapTmp.get(method);
                }
                if (line.startsWith("cachesecs=")) {
                    cacheSecs = line.substring(10);
                    if (log.isDebugEnabled()) {
                        log.debug("Cache secs is " + cacheSecs);
                    }
                    conf.setCacheSecs(Integer.parseInt(cacheSecs));
                }
                if (line.startsWith("ignore=")) {
                    ignore = line.substring(7);
                    if (log.isDebugEnabled()) {
                        log.debug("Cache key will ignore value of element " + ignore);
                    }
                    conf.getCacheKeyIgnoreList().add(ignore);
                }
                if (line.startsWith("doesupdates=")) {
                    applyInvalidation = line.substring(12);
                    if (log.isDebugEnabled()) {
                        log.debug("Method does updates: " + applyInvalidation);
                    }
                    conf.setApplyInvalidation(applyInvalidation.equalsIgnoreCase("true") || applyInvalidation.equalsIgnoreCase("yes"));
                }
                if (line.contains("resourcekey=")) {
                    resourceKey = line.substring(line.indexOf("=") + 1);
                    if (log.isDebugEnabled()) {
                        log.debug("Resource Key to get: " + resourceKey);
                    }

                    ResourceKeyConfiguration newResourceKey = new ResourceKeyConfiguration();
                    if (line.startsWith("response")) {
                        if (conf.isApplyInvalidation()) {
                            log.error("A cache configuration for an update request cannot have invalidation resource keys to extract from a response! Key will be ignored");
                            continue;
                        }
                        newResourceKey.setApplyToResponse();
                        if (log.isDebugEnabled()) {
                            log.debug("Resource Key must be extracted from the response");
                        }
                    } else {
                        newResourceKey.setApplyToRequest();
                        if (log.isDebugEnabled()) {
                            log.debug("Resource Key must be extracted from the request");
                        }
                    }

                    StringTokenizer stResourceParts = new StringTokenizer(resourceKey, ",");
                    while (stResourceParts.hasMoreTokens()) {
                        String partStr = stResourceParts.nextToken();
                        if (partStr.contains("/")) {
                            //xpath query
                            if (log.isDebugEnabled()) {
                                log.debug("Resource Key Part is xquery: " + partStr);
                            }
                            ResourceKeyPartConfiguration part = new ResourceKeyPartConfiguration();
                            part.setXpathPart(xpath.compile(partStr));
                            newResourceKey.getPartsList().add(part);
                        } else {
                            //Plain string
                            if (log.isDebugEnabled()) {
                                log.debug("Resource Key Part is plain string: " + partStr);
                            }
                            ResourceKeyPartConfiguration part = new ResourceKeyPartConfiguration();
                            part.setStrPart(partStr);
                            newResourceKey.getPartsList().add(part);
                        }
                    }
                    conf.getResourceKeyConfigurationList().add(newResourceKey);
                }
            }

            /* e.g. config:

             #getCustomer
             method=http://xml.smilecoms.com/schema/SCA:CustomerSmileID
             cachesecs=60
             doesupdates=no
             ignore=http://xml.smilecoms.com/schema/SCA:TxID
             requestresourcekey=customer,//sca:CustomerSmileID/sca:SmileID
             responsesourcekey=customer,//sca:CustomerSmileID/sca:SmileIDInResp

             #modifyCustomer
             method=http://xml.smilecoms.com/schema/SCA:Customer
             cachesecs=0
             doesupdates=yes
             requestresourcekey=customer,//sca:Customer/sca:SmileID/sca:SmileID

             #modifyCustomerStatus
             method=http://xml.smilecoms.com/schema/SCA:NewCustomerStatus
             cachesecs=0
             doesupdates=yes
             requestresourcekey=customer,//sca:NewCustomerStatus/sca:SmileID/sca:SmileID

             #DoBalanceTransfer
             method=http://xml.smilecoms.com/schema/SCA:DoBalanceTransferData
             cachesecs=0
             doesupdates=yes
             resourcekey=customer,//sca:DoBalanceTransferData/sca:FromSmileID/sca:SmileID
             resourcekey=customer,//sca:DoBalanceTransferData/sca:ToSmileID/sca:SmileID
            
             */


            //Swap maps
            synchronized (initLock) {
                lastConfigLoad = System.currentTimeMillis();
                lastConfigProp = thisConfigProp;
                soapMethodConfigurationMap = null;
                soapMethodConfigurationMap = soapMethodConfigurationMapTmp;
            }

        } catch (Exception ex) {
            log.warn("Error setting up cache configuration: " + ex.toString());
        } finally {
            synchronized (initLock) {
                initLock.setLength(0);
                initLock.append("unlocked");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished populateConfig to initialise the cache configuration");
        }
    }

    public static String getInvalidationHash(String data) {
        return HashUtils.md5(data);
    }
}
