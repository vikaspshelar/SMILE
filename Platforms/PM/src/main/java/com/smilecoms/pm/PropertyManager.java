/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.pm;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.platform.SmileWebService;
import com.smilecoms.commons.util.Codec;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.commons.util.Utils;
import com.smilecoms.xml.pm.PMError;
import com.smilecoms.xml.pm.PMSoap;
import com.smilecoms.xml.schema.pm.Done;
import com.smilecoms.xml.schema.pm.FlushCacheRequest;
import com.smilecoms.xml.schema.pm.GeneralQueryRequest;
import com.smilecoms.xml.schema.pm.GeneralQueryResponse;
import com.smilecoms.xml.schema.pm.PlatformString;
import com.smilecoms.xml.schema.pm.PropertyListRequest;
import com.smilecoms.xml.schema.pm.PropertyListResponse;
import com.smilecoms.xml.schema.pm.PropertyResponse;
import com.smilecoms.xml.schema.pm.ReplaceWithPropertiesRequest;
import com.smilecoms.xml.schema.pm.ResourceList;
import com.smilecoms.xml.schema.pm.UpdatePropertyRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * The property manager is used to manage and return configuration values used
 * by systems across Smiles architecture. Whereever possible, configuration
 * management (constants and properties) values are stored centrally in the
 * SmileDB in a table called property. This allows for central management and
 * simplified updating of properties across Smiles systems. At time of writing,
 * only the java infrastructure utilises the property functionality, but the
 * intent is that all platforms centralize on this single point of configuration
 * management.<br/>
 * <br/>
 * Here are some key points about properties:<br/>
 * 1) A property consists of 3 things: Its key e.g.
 * global.sca.timeout.connectmillis , its version e.g. production , and its
 * value e.g. 10000 <br/>
 * 2) The key is the thing you use to lookup the property and its value is
 * obviously the properties value. The version relates to which version of the
 * property should be returned. Lets say the property is the IP address of a DB
 * server and we want one client to use one database and another to use a
 * different one. This would be difficult to do with a central property list if
 * a property could only have a single value. The version allows us to keep
 * multiple values for a property and return the one for the version requested.
 * <br/>
 * 3) All callers into the property management platform have a client ID (the
 * value of -DSCA_CLIENT_ID passed into the JVM for java stuff). We use this
 * client ID to lookup the version of properties that should be returned to that
 * client. For our 2 databases example, we would thus configure that client ID
 * server1 has version X while client ID of server2 has Y version and then put 2
 * values for the IP address property - one for version X and one for version
 * Y.<br/>
 * 4) In order to avoid having to configure a property for every version in the
 * environment, we have a special version called "default". If a property value
 * has not been configured for the specific version, then the default version of
 * the property is returned.
 * <br/><br/>
 * The configuration of properties consists of modifying 2 tables in SmileDB.
 * The first is the property_mapping table. This table simply stores a mapping
 * of client ID's to version name. E.g. client ID "SEPServer1" could have
 * property version "ProductionPortal". Then whenever SEPServer1 asks for the
 * value of a property, the value for version "ProductionPortal" is returned, or
 * the default version if one for "ProductionPortal" doesn't exist. The second
 * table is called property. It stores the property key, property value and
 * version, along with a description of the property.<br/>
 * <br/>
 * The properties are cached extensively so as to avoid roundtrips when clients
 * need properties. The cache timeout is configured as a property itself
 * "global.sca.property.cache.stalecheckseconds". This duration is the maximum
 * time that a properties old version would be returned if it had been updated
 * in the database. PM checks if properties have been updated by looking at the
 * last_modified timestamp in the property table. If any one of these is greater
 * than the last time a property client cleared its cache, then PM tells the
 * client that its cache is stale. The easiest way to refresh all property
 * caches is to run this sql statement against SmileDB: "update property set
 * last_modified=now()"<br/>
 * <br/>
 * The naming convention for properw falty keys is as follows:
 * [env|global].some.descriptive.name. That is, every property either starts
 * with "env" or "global". The reason for this is to make it clear which
 * properties are expected/intended to be changed in an Opco for its specific
 * environment. "env" properties are fine to change in an OpCo, while"global"
 * properties should be the same across all OpCos and their values were set by
 * the development team and should NEVER be changed unless requested by the dev
 * team.<br/>
 * <br/>
 * The final functionality of the property manager is to store all of the
 * localisation strings used by the applications to make them multilingual. An
 * example could be the enterprise portal landing page. The text "Smile
 * Enterprise Portal" is not hardcoded in the portal code - none of the text is
 * in fact. Instead the portal just renders the text for "title". "title" is
 * then replaced with the version of the portals title for the language of the
 * user browsing the page. These strings are again cached heavily, and the
 * values are stored and returned from the "resource" table. This table stores
 * the locale (e.g. en_ZA for south african english), the key and the value for
 * the string. The cache of the resources is managed seperately from that
 * controlled by "global.sca.property.cache.stalecheckseconds" so dont expect
 * changes in that table to be reflected within
 * "global.sca.property.cache.stalecheckseconds" seconds. Instead the cache is
 * only cleared out when a call is made to code on the server to clear out the
 * servers cache. This call is a development process and not required by
 * operations. The reason this cache is not cleared out often is due to its
 * enormous size and impact on performance when the new strings are pulled from
 * the database. The call can be triggered on SEP by selecting the "Refresh
 * Strings" option on the menu. The strings are then refreshed in the cache for
 * the server that was being accessed.
 *
 * @author PCB
 */
@WebService(serviceName = "PM", portName = "PMSoap", endpointInterface = "com.smilecoms.xml.pm.PMSoap", targetNamespace = "http://xml.smilecoms.com/PM", wsdlLocation = "PMServiceDefinition.wsdl")
@Stateless
@HandlerChain(file = "/handler.xml")
public class PropertyManager extends SmileWebService implements PMSoap {

    @javax.annotation.Resource
    javax.xml.ws.WebServiceContext wsctx;
    @PersistenceContext(unitName = "PMPU")
    private EntityManager em;
    private static final String SQL_QUERY_GET_VALUE = "SELECT P FROM Property P WHERE P.propertyPK.propertyName = :key AND P.propertyPK.propertyVersion = :version";
    private static final String SQL_QUERY_UPDATE_VALUE = "UPDATE property SET LAST_MODIFIED = ?, PROPERTY_VALUE = ? WHERE PROPERTY_NAME = ? AND PROPERTY_VERSION = ?";
    private static final String SQL_QUERY_INSERT_VALUE = "insert into property values(?,?,'default',now(),'',now())";
    private static final String SQL_QUERY_GET_RESOURCE_LIST = "SELECT R FROM Resource R WHERE R.resourcePK.locale = :locale AND R.status = 'ac'";
    private static final String SQL_QUERY_LAST_MODIFIED_PROP_DATE = "SELECT MAX(P.lastModified) FROM Property P";
    private static final String SQL_QUERY_UPDATE_LAST_MODIFIED = "UPDATE property set LAST_MODIFIED=?";
    private static final String DEFAULT = "default";
    private static final String ACTIVE_RESOURCE = "ac";
    private static final String NEW_RESOURCE = "new";
    private static final Map<String, String> propertyCache = new ConcurrentHashMap<>();
    private static Date cacheLastRefreshed = new Date();
    private static long lastStaleCheckAgainstDB = 0;
    private static final long gap = 3000;
    private static Date cachedLastModified = null;
    private static final String GLOBAL_PROPERTY_PREFIX = "<global.";
    private static final String ENV_PROPERTY_PREFIX = "<env.";
    private static final String ERROR_CODE_PROP_DNE = "PM-0001";
    private static final String ERROR_CODE_PROP_OTHER = "PM-0002";
    private static final String ERROR_CODE_PROP_SQL_ISSUE = "PM-0003";

    /**
     * Returns a property value for the requested property key and client ID.
     * The property is retrieved by calling getCachedProperty on this class so
     * that the property will be rerieved out a cache as opposed to doing a DB
     * roundtrip.
     *
     * @param propertyRequest with the property key and client ID
     * @return PropertyResponse object with the requested property value for the
     * clients version
     * @throws com.smilecoms.xml.pm.PMError
     */
    @Override
    public com.smilecoms.xml.schema.pm.PropertyResponse getProperty(com.smilecoms.xml.schema.pm.PropertyRequest propertyRequest) throws PMError {
        setContext(propertyRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart("Property: " + propertyRequest.getPropertyName() + " Client: " + propertyRequest.getClient());
        }

        refreshLocalCacheIfNecessary();

        PropertyResponse ret = new PropertyResponse();
        try {
            ret.setPropertyValue(getCachedProperty(propertyRequest.getPropertyName(), propertyRequest.getClient(), propertyRequest.isSkipCache()));
        } catch (PMError e) {
            throw e;
        } finally {
            if (log.isDebugEnabled()) {
                logEnd(propertyRequest.getPropertyName() + "=" + ret.getPropertyValue());
            }
        }
        return ret;
    }

    /**
     * Returns the string with all properties recursively replaced with their
     * values
     *
     * @param replaceWithProperties
     * @return Replaced String
     * @throws com.smilecoms.xml.pm.PMError
     */
    @Override
    public PropertyResponse replaceWithProperties(ReplaceWithPropertiesRequest replaceWithProperties) throws PMError {
        setContext(replaceWithProperties, wsctx);
        if (log.isDebugEnabled()) {
            logStart("String: " + replaceWithProperties.getPropertyString() + " Client: " + replaceWithProperties.getClient());
        }
        PropertyResponse ret = new PropertyResponse();
        try {
            ret.setPropertyValue(replaceNestedProperties(replaceWithProperties.getPropertyString(), replaceWithProperties.getClient()));
        } catch (PMError e) {
            throw e;
        } finally {
            if (log.isDebugEnabled()) {
                logEnd(replaceWithProperties.getPropertyString() + "=" + ret.getPropertyValue());
            }
        }
        return ret;
    }

    /**
     * Returns a property value as a string from the property cache or adds it
     * to the cache and returns it if its not already in the cache. Calls
     * getDBProperty to get the property if its not in the cache.
     *
     * @param propName the property key
     * @param client the client ID to get the property for (the client ID is
     * used to know what version to return)
     * @return Property value as a String
     * @throws com.smilecoms.xml.pm.PMError
     */
    private String getCachedProperty(String propName, String client, boolean skipCache) throws PMError {
        if (client == null || client.isEmpty()) {
            log.debug("Client id passed in was blank or null so will use default");
            client = DEFAULT;
        }
        String ret = null;
        String key = propName + "--" + client;

        if (!skipCache) {
            ret = propertyCache.get(key);
        }
        if (ret == null) {
            ret = replaceNestedProperties(getDBProperty(propName, client), client);
            if (ret.startsWith("SQL:")) {
                log.debug("This property request is for a query based property");
                String bitafterSQL = ret.substring(4);
                String[] bits = bitafterSQL.split("=", 2);
                ret = getQueryBasedProperty(bits[0], bits[1]);
            }
            propertyCache.put(key, ret);
        }
        return ret;
    }

    /**
     * Utility method to make a complex String object from a normal String.
     *
     * @param s String to put in the complex object
     * @return The resulting complex object
     */
    public com.smilecoms.xml.schema.pm.PlatformString makePlatformString(String s) {
        com.smilecoms.xml.schema.pm.PlatformString ret = new com.smilecoms.xml.schema.pm.PlatformString();
        ret.setString(s);
        ret.setPlatformContext((com.smilecoms.xml.schema.pm.PlatformContext) getContext());
        return ret;
    }

    /**
     * Utility method to make a complex Integer object from a int.
     *
     * @param i Integer to put in the complex object
     * @return The resulting complex object
     */
    public com.smilecoms.xml.schema.pm.PlatformInteger makePlatformInteger(int i) {
        com.smilecoms.xml.schema.pm.PlatformInteger ret = new com.smilecoms.xml.schema.pm.PlatformInteger();
        ret.setInteger(i);
        ret.setPlatformContext((com.smilecoms.xml.schema.pm.PlatformContext) getContext());
        return ret;
    }

    /**
     * Utility method to make a complex boolean object from a simple boolean
     *
     * @param b boolean to put in the complex object
     * @return The resulting complex object
     */
    public com.smilecoms.xml.schema.pm.PlatformBoolean makePlatformBoolean(boolean b) {
        com.smilecoms.xml.schema.pm.PlatformBoolean ret = new com.smilecoms.xml.schema.pm.PlatformBoolean();
        ret.setBoolean(b);
        ret.setPlatformContext((com.smilecoms.xml.schema.pm.PlatformContext) getContext());
        return ret;
    }

    /**
     * Returns a cached version (no more than 10s old) of the date when a row in
     * property or property_mapping was last changed. Used by isStale to let a
     * client know if the property cache they have is older than the last date
     * when a change is made. It allows clients to keep a property cache and be
     * sure that their cache is cleaned out when a property changes on the
     * database
     *
     * @return Date when a property was last changed
     */
    private Date getCachedLastModifiedDate() {
        long nowMillis = System.currentTimeMillis();
        if (nowMillis > (lastStaleCheckAgainstDB + gap)) {
            // No need to check all the time... rather cache a result for at least 10 seconds to avoid lots of DB access
            if (log.isDebugEnabled()) {
                log.debug("Running query to get max property date");
            }
            Query q = em.createQuery(SQL_QUERY_LAST_MODIFIED_PROP_DATE);
            Date maxProp = (Date) q.getSingleResult();
            Date now = new Date();
            if (maxProp.after(now)) {
                // For some reason the date is in the future. Set it to current date to prevent constantly getting new properties
                log.warn("Property table last modified is in the future. Going to set it to the current date");
                q = em.createNativeQuery(SQL_QUERY_UPDATE_LAST_MODIFIED);
                q.setParameter(1, now);
                q.executeUpdate();
                maxProp = now;
            }
            // Return greatest date
            cachedLastModified = maxProp;
            lastStaleCheckAgainstDB = nowMillis;
        }
        if (log.isDebugEnabled()) {
            log.debug("Max property modified date is: [{}]", cachedLastModified.toString());
        }
        return cachedLastModified;
    }

    private void refreshLocalCacheIfNecessary() {
        Date maxPropDateinDB = getCachedLastModifiedDate();
        if (cacheLastRefreshed.before(maxPropDateinDB)) {
            // Must refresh cache
            if (log.isDebugEnabled()) {
                log.debug("Refreshing PM property cache as the DB has been updated at [{}] while the cache was last refreshed at [{}]", new Object[]{maxPropDateinDB.toString(), cacheLastRefreshed.toString()});
            }
            propertyCache.clear();
            cacheLastRefreshed = new Date();
        } else if (log.isDebugEnabled()) {
            log.debug("PM's property cache is still fresh (was last cleared at [{}]) so no need to clear it", cacheLastRefreshed.toString());
            log.debug("The cache has [{}] items in it", propertyCache.size());
        }
    }

    /**
     * Lets the caller know if any properties have changed in the property and
     * property_mapping table since they last refreshed their cache. Enables
     * clients to keep a cache and to call into SCA and check if they must
     * refresh their cache. PM also keeps its own cache of properties to enhance
     * its performance for clients that cant use a cache themselves (e.g. BPEL
     * flows). This method also triggers PM to internally see if it should clear
     * out its own cache.
     *
     * @param staleTest Contains the date when a client last cleared out their
     * cache
     * @return boolean indicating if their cache is stale or not
     * @throws com.smilecoms.xml.pm.PMError
     */
    @Override
    public com.smilecoms.xml.schema.pm.PlatformBoolean isStale(com.smilecoms.xml.schema.pm.PlatformDateTime staleTest) throws PMError {
        setContext(staleTest, wsctx);
        if (log.isDebugEnabled()) {
            logStart(staleTest.getDateTime().toString());
        }

        refreshLocalCacheIfNecessary();

        // Get a cached version of the date when a property was last changed in the DB
        Date maxPropDateinDB = getCachedLastModifiedDate();

        // Now check if clients cache is stale
        boolean isStale = false;
        if (Utils.getJavaDate(staleTest.getDateTime()).before(maxPropDateinDB)) {
            isStale = true;
        }
        if (log.isDebugEnabled()) {
            logEnd("Returning " + isStale);
        }

        return makePlatformBoolean(isStale);
    }

    /**
     * Returns the property version configured for a JVM client ID or an error
     * if one is not configured
     *
     * @param client The JVM client ID
     * @return String indicating the version
     * @throws com.smilecoms.xml.pm.PMError
     */
    private String getClientsVersion(String client) throws PMError {
        if (log.isDebugEnabled()) {
            logStart(client);
        }
        String version = "default";
        try {
            PropertyMapping mapping = em.find(PropertyMapping.class, client);
            version = mapping.getPropertyVersion();
        } catch (Exception e) {
            log.debug("Could not find a property version for client [{}] : [{}]. Using default", new Object[]{client, e.toString()});
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("Returning version " + version + " for client " + client);
            }
        }
        return version;
    }

    /**
     * Returns a property value from the property table for the requested
     * property key and client ID. Uses getClientsVersion() to get the version
     * configured for the client ID passed in. If a property is not configured
     * for the requested key and version, then the default version is assumed
     * and looked up instead as a fallback mechanism.
     *
     * @param key Property key
     * @param client Clienty ID of the requesting JVM
     * @return the property value as a string
     * @throws com.smilecoms.xml.pm.PMError
     */
    private String getDBProperty(String key, String client) throws PMError {
        if (log.isDebugEnabled()) {
            logStart("Property " + key + " and client " + client);
        }
        String version = null;
        try {
            version = getClientsVersion(client);
        } catch (Exception e) {
            // Dont use processError as its too heavyweight for this error
            com.smilecoms.xml.schema.pm.PMError faultInfo = new com.smilecoms.xml.schema.pm.PMError();
            faultInfo.setErrorCode(ERROR_CODE_PROP_OTHER);
            faultInfo.setErrorDesc(e.toString());
            faultInfo.setErrorType("system");
            throw new PMError(e.toString(), faultInfo);
        }

        String ret = null;
        Query q = em.createQuery(SQL_QUERY_GET_VALUE);

        Property p;
        try {
            q.setParameter("key", key);
            q.setParameter("version", version);
            p = (Property) q.getSingleResult();
            ret = p.getPropertyValue();
        } catch (NoResultException nr) {
            // No result for the clients version, so try default version
            if (log.isDebugEnabled()) {
                log.debug("No result for that clients version so trying default");
            }
            q.setParameter("key", key);
            q.setParameter("version", DEFAULT);
            try {
                p = (Property) q.getSingleResult();
                ret = p.getPropertyValue();
                if (log.isDebugEnabled()) {
                    log.debug("Found a default fallback property");
                }
            } catch (Exception e) {
                // Dont use processError as its too heavyweight for this error
                com.smilecoms.xml.schema.pm.PMError faultInfo = new com.smilecoms.xml.schema.pm.PMError();
                faultInfo.setErrorCode(ERROR_CODE_PROP_DNE);
                faultInfo.setErrorDesc("Property [" + key + "] does not exist");
                faultInfo.setErrorType("business");
                throw new PMError("Property [" + key + "] does not exist", faultInfo);
            }
        } catch (Exception e) {
            // Dont use processError as its too heavyweight for this error
            com.smilecoms.xml.schema.pm.PMError faultInfo = new com.smilecoms.xml.schema.pm.PMError();
            faultInfo.setErrorCode(ERROR_CODE_PROP_OTHER);
            faultInfo.setErrorDesc(e.toString());
            faultInfo.setErrorType("system");
            throw new PMError(e.toString(), faultInfo);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("Returning value " + ret);
            }
        }
        return decrypt(ret);
    }

    private String replaceNestedProperties(String prop, String client) throws PMError {
        String subPropertyKey = getSubPropertyKey(prop);
        String replacedProp = prop;
        while (subPropertyKey != null) {
            // The property has a nested property key
            String nestedPropValue = getDBProperty(subPropertyKey, client);
            if (log.isDebugEnabled()) {
                log.debug("Replacing property key [{}] in [{}] with [{}]", new Object[]{subPropertyKey, replacedProp, nestedPropValue});
            }

            if (nestedPropValue.startsWith("SQL:")) {
                log.debug("This nested prop value is for a query based property");
                String bitafterSQL = nestedPropValue.substring(4);
                String[] bits = bitafterSQL.split("=", 2);
                String deepLyingSubPropertyKey = getSubPropertyKey(bits[1]);//Just go one level deep to see if there is a sub-property in there as well
                String deepLyingReplacedProp = bits[1];

                if (deepLyingSubPropertyKey != null) {
                    String deepLyingNestedPropValue = getDBProperty(deepLyingSubPropertyKey, client);
                    if (log.isDebugEnabled()) {
                        log.debug("Replacing deep lying property key [{}] in [{}] with [{}]", new Object[]{deepLyingSubPropertyKey, deepLyingReplacedProp, deepLyingNestedPropValue});
                    }
                    deepLyingReplacedProp = deepLyingReplacedProp.replaceAll("<" + deepLyingSubPropertyKey + ">", Matcher.quoteReplacement(deepLyingNestedPropValue));
                }
                nestedPropValue = getQueryBasedProperty(bits[0], deepLyingReplacedProp);
            }
            replacedProp = replacedProp.replaceAll("<" + subPropertyKey + ">", Matcher.quoteReplacement(nestedPropValue));
            if (log.isDebugEnabled()) {
                log.debug("Property value is now [{}]", replacedProp);
            }
            subPropertyKey = getSubPropertyKey(replacedProp);
        }
        return replacedProp;
    }

    private String getSubPropertyKey(String s) {
        String ret = null;
        if (s.contains(GLOBAL_PROPERTY_PREFIX)) {
            int keyStart = s.indexOf(GLOBAL_PROPERTY_PREFIX) + 1;
            int keyEnd = s.indexOf(">", keyStart);
            ret = s.substring(keyStart, keyEnd);
        } else if (s.contains(ENV_PROPERTY_PREFIX)) {
            int keyStart = s.indexOf(ENV_PROPERTY_PREFIX) + 1;
            int keyEnd = s.indexOf(">", keyStart);
            ret = s.substring(keyStart, keyEnd);
        }
        if (log.isDebugEnabled()) {
            if (ret != null) {
                log.debug("Found a nested property key of [{}] in property value [{}]", new Object[]{ret, s});
            } else {
                log.debug("Did not find a nested property key in property value [{}]", s);
            }
        }
        return ret;
    }

    /**
     * Returns a list of property values when passed in a list of property keys
     * and a client ID. Uses getCachedProperty() for each property in the list
     * i.e. is checks in the cache first before returning each property
     *
     * @param propertyListRequest A list of property keys and a client ID
     * @return List of property values in the same order as the request list
     * @throws com.smilecoms.xml.pm.PMError
     */
    @Override
    public PropertyListResponse getPropertyList(PropertyListRequest propertyListRequest) throws PMError {
        setContext(propertyListRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart("getPropertyList");
        }

        refreshLocalCacheIfNecessary();

        PropertyListResponse ret = new PropertyListResponse();
        try {
            Iterator<String> properties = propertyListRequest.getPropertyNames().iterator();
            String client = propertyListRequest.getClient();
            List<String> retList = ret.getPropertyValues();
            while (properties.hasNext()) {
                String propKey = properties.next();
                String propValue = getCachedProperty(propKey, client, propertyListRequest.isSkipCache());
                retList.add(propValue);
            }
        } catch (PMError e) {
            throw e;
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("getPropertyList");
            }
        }
        return ret;
    }

    /**
     * Adds a row into the resource table or changes its status to ac if its not
     * new
     *
     * @param newResource the data to insert
     * @return Done
     * @throws com.smilecoms.xml.pm.PMError
     */
    @Override
    public com.smilecoms.xml.schema.pm.Done addResource(com.smilecoms.xml.schema.pm.Resource newResource) throws PMError {
        setContext(newResource, wsctx);
        if (log.isDebugEnabled()) {
            logStart("addResource");
        }
        try {
            ResourcePK pk = new ResourcePK();
            pk.setResourceKey(newResource.getKey());
            pk.setLocale(newResource.getLocale());
            boolean exists = true;
            try {
                Resource r = JPAUtils.findAndThrowENFE(em, Resource.class, pk);
                // It exists so update the status to active if its not new
                if (!r.getStatus().equals(NEW_RESOURCE)) {
                    r.setStatus(ACTIVE_RESOURCE);
                    em.persist(r);
                    if (log.isDebugEnabled()) {
                        log.debug("Updated resource to status ac : key=[{}] locale=[{}]", new Object[]{newResource.getKey(), newResource.getLocale()});
                    }
                }
            } catch (EntityNotFoundException e) {
                exists = false;
            }
            if (!exists) {
                Resource r = new Resource();
                r.setResourcePK(pk);
                r.setValue(newResource.getValue());
                r.setStatus(NEW_RESOURCE);
                em.persist(r);
                if (log.isDebugEnabled()) {
                    log.debug("Added resource : key=[{}] locale=[{}]", new Object[]{newResource.getKey(), newResource.getLocale()});
                }
            }
        } catch (Exception e) {
            throw processError(PMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("addResource");
            }
        }
        return makeDone();
    }

    /**
     * Utility method to make a complex boolean object with value TRUE.
     *
     * @return The resulting complex object
     */
    private com.smilecoms.xml.schema.pm.Done makeDone() {
        com.smilecoms.xml.schema.pm.Done done = new com.smilecoms.xml.schema.pm.Done();
        done.setDone(com.smilecoms.xml.schema.pm.StDone.TRUE);
        return done;
    }

    /**
     * Returns a list of all of the resource keys and values for a given locale
     * from the resource table.
     *
     * @param locale e.g. (en_ZA)
     * @return A list of all the resource keys and values from the resource
     * table for the given locale
     * @throws com.smilecoms.xml.pm.PMError
     */
    @SuppressWarnings("unchecked")
    @Override
    public ResourceList getResourceList(PlatformString locale) throws PMError {
        setContext(locale, wsctx);
        if (log.isDebugEnabled()) {
            logStart("getResourceList");
        }
        ResourceList list = new ResourceList();
        try {
            Query q = em.createQuery(SQL_QUERY_GET_RESOURCE_LIST);
            q.setParameter("locale", locale.getString());
            List<Resource> DBList = q.getResultList();
            int cnt = 0;
            StringBuilder resources = new StringBuilder();
            for (Resource res : DBList) {
                resources.append(res.getResourcePK().getResourceKey());
                resources.append("|");
                resources.append(res.getValue());
                resources.append("|");
                cnt++;
            }
            list.setResources(Utils.zip(resources.toString()));
            if (log.isDebugEnabled()) {
                log.debug("Returning [{}] resources for locale [{}]", cnt, locale.getString());
            }
        } catch (Exception e) {
            throw processError(PMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("getResourceList");
            }
        }
        return list;
    }

    @Override
    public Done isUp(String isUpRequest) throws PMError {
        getCachedProperty("global.properties.test", "default", false);
        return makeDone();
    }

    @Override
    public Done flushCache(FlushCacheRequest flushCacheRequest) throws PMError {
        setContext(flushCacheRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart("flushCache");
        }
        try {
            Query q = em.createNativeQuery(SQL_QUERY_UPDATE_LAST_MODIFIED);
            Date now = new Date();
            q.setParameter(1, now);
            q.executeUpdate();
            cachedLastModified = now;
        } catch (Exception e) {
            throw processError(PMError.class, e);
        } finally {
            if (log.isDebugEnabled()) {
                logEnd("flushCache");
            }
        }
        return makeDone();
    }

    private String getQueryBasedProperty(String dsName, String query) throws PMError {
        Connection conn = null;
        PreparedStatement ps = null;
        String result;
        try {
            conn = getConnection(dsName);
            ps = conn.prepareStatement(query);
            long start = 0;
            if (log.isDebugEnabled()) {
                log.debug("About to run property query: " + query + " on datasource name " + dsName);
                start = System.currentTimeMillis();
            }

            ResultSet rs = ps.executeQuery();
            if (log.isDebugEnabled()) {
                log.debug("Finished running property query. Query took " + (System.currentTimeMillis() - start) + "ms");
            }

            int columnCount = ps.getMetaData().getColumnCount();
            log.debug("There are [{}] columns", columnCount);
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    Object o = rs.getObject(i);
                    if (o == null) {
                        o = "null";
                    }
                    sb.append(o.toString().replaceAll("\\|", "_pipe_"));
                    if (i != columnCount) {
                        sb.append("|");
                    }
                }
                if (!rs.isLast()) {
                    sb.append("_newrow_");
                }
            }

            result = sb.toString();
        } catch (Exception e) {
            log.warn("Error running property query: " + query + ". " + e.toString());
            // Dont use processError as its too heavyweight for this error
            com.smilecoms.xml.schema.pm.PMError faultInfo = new com.smilecoms.xml.schema.pm.PMError();
            faultInfo.setErrorCode(ERROR_CODE_PROP_SQL_ISSUE);
            faultInfo.setErrorDesc("SQL query for property has an issue: " + e.toString());
            faultInfo.setErrorType("system");
            throw new PMError("SQL query for property has an issue: " + e.toString(), faultInfo);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
        }
        return result;
    }

    private Connection getConnection(String dsName) throws Exception {
        return JPAUtils.getNonJTAConnection(dsName);
    }

    private String decrypt(String prop) {
        if (prop.startsWith("Encrypted:")) {
            prop = prop.substring(10);
            return Codec.encryptedHexStringToDecryptedString(prop);
        } else {
            return prop;
        }
    }

    @Override
    public Done updateProperty(UpdatePropertyRequest updatePropertyRequest) throws PMError {
        setContext(updatePropertyRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart("updateProperty");
        }

        Query q;
        Date now = new Date();
        try {
            q = em.createNativeQuery(SQL_QUERY_UPDATE_VALUE);
            String version = getClientsVersion(updatePropertyRequest.getClient());
            q.setParameter(1, now);
            q.setParameter(2, updatePropertyRequest.getPropertyValue());
            q.setParameter(3, updatePropertyRequest.getPropertyName());
            q.setParameter(4, version);
            log.debug("Updating property {{}} version [{}] with value [{}]", new Object[]{updatePropertyRequest.getPropertyName(), version, updatePropertyRequest.getPropertyValue()});
            int updated = q.executeUpdate();

            if (updated < 1) {
                //lets try default property
                q.setParameter(4, DEFAULT);
                updated = q.executeUpdate();
                if (updated < 1) {
                    log.debug("No property to update. Going to insert property");
                    q = em.createNativeQuery(SQL_QUERY_INSERT_VALUE);
                    q.setParameter(1, updatePropertyRequest.getPropertyName());
                    q.setParameter(2, updatePropertyRequest.getPropertyValue());
                    q.executeUpdate();
                }
            }
            cachedLastModified = now;
        } catch (Exception e) {
            throw processError(PMError.class, e);
        } finally {
            logEnd("updateProperty");
        }

        return makeDone();
    }
    private static final DecimalFormat df = new DecimalFormat("0.00");

    @Override
    public GeneralQueryResponse runGeneralQuery(GeneralQueryRequest generalQueryRequest) throws PMError {
        setContext(generalQueryRequest, wsctx);
        if (log.isDebugEnabled()) {
            logStart("runGeneralQuery");
        }

        GeneralQueryResponse resp = new GeneralQueryResponse();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            String dataSourceName = null;
            String query = "";
            String requestedQuery = generalQueryRequest.getQueryName();
            log.debug("General query [{}]", requestedQuery);
            Set<String> queryConfigs = BaseUtils.getPropertyAsSet("env.pm.generalquery.config");
            for (String configRow : queryConfigs) {
                String[] bits = configRow.split("\\|");
                String queryName = bits[0].trim();
                if (queryName.equals(requestedQuery)) {
                    dataSourceName = bits[1];
                    for (int i = 2; i < bits.length; i++) {
                        query = query + bits[i] + "|";
                    }
                    query = query.substring(0, query.length() - 1);
                    log.debug("Found the requested general query. Connection [{}] and query [{}]", dataSourceName, query);
                    break;
                }
            }
            if (dataSourceName == null || query.isEmpty()) {
                throw new Exception("Invalid general query -- " + requestedQuery);
            }

            conn = getConnection(dataSourceName);
            ps = conn.prepareStatement(query);
            int i = 0;
            for (String param : generalQueryRequest.getParameters()) {
                log.debug("Adding a query paramater [{}]", param);
                i++;
                if (Utils.isNumeric(param) && param.length() < 14) {
                    // Dont treat a long number like an imei as a number
                    try {
                        ps.setLong(i, Long.parseLong(param));
                    } catch (Exception e) {
                        log.debug("Couldnt set value as a number. Going to try as a string");
                        ps.setString(i, param);
                    }
                } else {
                    ps.setString(i, param);
                }
            }

            ResultSet rs = ps.executeQuery();
            int columns = rs.getMetaData().getColumnCount();
            StringBuilder sb = new StringBuilder();
            for (int c = 1; c <= columns; c++) {
                sb.append(rs.getMetaData().getColumnName(c));
                sb.append("^");
            }
            // Remove trailing ^
            sb.deleteCharAt(sb.length() - 1);
            sb.append("#");
            int cnt = 0;
            int max = BaseUtils.getIntProperty("env.pm.generalquery.max.results", 10000);
            while (rs.next() && cnt < max) {
                cnt++;
                for (int c = 1; c <= columns; c++) {
                    Object entry = rs.getObject(c);
                    if (entry == null) {
                        entry = "null";
                    }
                    if (entry instanceof Float || entry instanceof Double) {
                        log.debug("This is a number with decimal places");
                        sb.append(df.format(entry).replaceAll("\\^", requestedQuery).replaceAll("#", ""));
                    } else {
                        log.debug("This is not a number with decimal places");
                        sb.append(entry.toString().replaceAll("\\^", requestedQuery).replaceAll("#", ""));
                    }

                    sb.append("^");
                }
                // Remove trailing ^
                sb.deleteCharAt(sb.length() - 1);
                sb.append("#");
            }
            // Remove trailing #
            sb.deleteCharAt(sb.length() - 1);

            log.debug("Query result string is [{}]", sb);

            resp.setBase64CompressedResult(Utils.zip(sb.toString()));

        } catch (Exception e) {
            throw processError(PMError.class, e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
            }
            logEnd("runGeneralQuery");
        }

        return resp;
    }
}
