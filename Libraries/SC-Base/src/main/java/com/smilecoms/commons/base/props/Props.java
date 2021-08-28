package com.smilecoms.commons.base.props;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.helpers.BaseHelper;
import com.smilecoms.commons.base.sd.ServiceDiscoveryAgent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.*;

/**
 * Helper class to retrieve constants from SCA. Retrieving a property is as
 * simple as calling Prop.getProperty("some.property.name"). To make this call
 * however requires some initial configuration:<br/> - Your JVM needs to have 2
 * system properties configured by passing in -D parameters. The first is
 * -DSCA_BOOTSTRAP_HOST and must point to a server hosting the SCA web service.
 * e.g. -DSCA_BOOTSTRAP_HOST=http://localhost:18181 This property tells smile
 * commons where to fetch the properties from. The second property is
 * -DSCA_CLIENT_ID and is used to identify the client requesting the property
 * e.g. -DSCA_CLIENT_ID=dev_server_1 The property management system finds the
 * client id in the property_mapping table and gets the properties version for
 * that client. The version and property name is then used to get the applicable
 * property. This allows for a property to have different values depending on
 * who wants the property - useful for cluster management etc.
 *
 * @author PCB
 */
public class Props {

    // Public constants
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final String MUST_REFRESH_STRING = "true</MustRefresh>";
    private static final String PROPVAL_DELIM = "<PropertyValue>";
    private static final int PROPVAL_DELIM_LEN = PROPVAL_DELIM.length();
    private static final String PROPVAL_DELIM_END = "</PropertyValue>";
    private static final String PROPVAL_DELIM_BLANK = "<PropertyValue/>";
    //Post data for get property
    private static final String SOAP_POST_DATA_BEGIN = "<soapenv:Envelope xsi:schemaLocation=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
            + " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:sca=\"http://xml.smilecoms.com/schema/SCA\">"
            + "<soapenv:Body>";
    private static final String SOAP_POST_DATA_END = "</soapenv:Body></soapenv:Envelope>";
    private static final String PROPERTY_POST_DATA_BEGIN = SOAP_POST_DATA_BEGIN
            + "<sca:PropertyRequest><sca:SCAContext><sca:TxId></sca:TxId><sca:Tenant>ad</sca:Tenant><sca:OriginatingIdentity></sca:OriginatingIdentity><sca:OriginatingIP>0.0.0.0</sca:OriginatingIP></sca:SCAContext><sca:PropertyName>";
    private static final String PROPERTY_POST_DATA_USE_CACHE = "</sca:PropertyName><sca:SkipCache>false</sca:SkipCache>";
    private static final String PROPERTY_POST_DATA_DONT_USE_CACHE = "</sca:PropertyName><sca:SkipCache>true</sca:SkipCache>";
    private static final String PROPERTY_POST_DATA_END = "<sca:Client>" + BaseUtils.SCA_CLIENT_ID + "</sca:Client></sca:PropertyRequest>" + SOAP_POST_DATA_END;
    private static final String ISSTALE_POST_DATA_BEGIN = SOAP_POST_DATA_BEGIN + "<sca:CacheLastRefreshed><sca:SCAContext><sca:TxId></sca:TxId><sca:Tenant>ad</sca:Tenant><sca:OriginatingIdentity></sca:OriginatingIdentity><sca:OriginatingIP>0.0.0.0</sca:OriginatingIP></sca:SCAContext><sca:DateTime>";
    private static final String ISSTALE_POST_DATA_END = "</sca:DateTime></sca:CacheLastRefreshed>" + SOAP_POST_DATA_END;
    private static final Random random = new Random();
    //Logging
    private static final String CLASS = Props.class.getName();
    private static final Logger log = LoggerFactory.getLogger(CLASS);
    //Cache of property values
    private static Map<String, String> props = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, Set<String>> setProps = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> mapProps = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, List<String>> listProps = new java.util.concurrent.ConcurrentHashMap<>();
    //New cache used for refreshing
    private static Map<String, String> newProps = null;
    //Last version of the map used as a fallback if an error occurs getting a property
    private static Map<String, String> oldProps = null;
    //Static constants not retrieved from SCA    
    //Last time the cache was cleared
    private static Date lastCleared = new Date();
    // Background thread name prefix
    private static final String NULL_PROPERTY_STRING = "_n_";
    private static boolean isAvailable = false;
    private static final CharSequence PMERROR = "<ErrorCode>PM-0001</ErrorCode>";

    private static void logCurrentLevels() {
        try {
            log.warn("************************* CURRENT LOGGING LEVELS *****************************");
            LogManager lm = LogManager.getLogManager();
            Enumeration names = lm.getLoggerNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                java.util.logging.Logger logger = lm.getLogger(name);
                if (logger != null) {
                    Level level = logger.getLevel();
                    if (level != null) {
                        log.warn("Logger : " + name + " : " + level.toString());
                    }
                } else {
                    log.warn("For some reason logger [{}] is null", name);
                }
            }
        } catch (Throwable e) {
            log.warn("Error listing logging levels : " + e.toString());
            log.warn("Error: ", e);
        }
    }

    public static void setLoggingLevels() {
        if (log.isDebugEnabled()) {
            log.debug("ENTERING setLoggingLevels");
        }
        logCurrentLevels();
        List<String> levels = BaseUtils.getPropertyAsList("env.logging.levels");
        if (levels == null || levels.isEmpty()) {
            log.warn("No logging levels are configured using env.logging.levels");
            return;
        }

        log.warn("Logging levels are being changed to : \n" + BaseUtils.getProperty("env.logging.levels"));
        String loggerString = null;
        String level = null;
        try {
            StringTokenizer stLevel;
            for (String setting : levels) {
                try {
                    stLevel = new StringTokenizer(setting, "=");
                    loggerString = stLevel.nextToken();
                    level = stLevel.nextToken();
                    setLoggingLevel(loggerString, level);
                } catch (Exception e) {
                    log.warn("Error setting logging levels for log [" + loggerString + "] to level [" + level + "] : " + e.toString());
                }
            }
        } catch (Throwable e) {
            log.warn("Error setting logging levels for log [" + loggerString + "] to level [" + level + "] : " + e.toString());
        } finally {
            logCurrentLevels();
            if (log.isDebugEnabled()) {
                log.debug("EXITING setLoggingLevels");
            }
        }
    }

    private static void setLoggingLevel(String logger, String level) {
        if (log.isDebugEnabled()) {
            log.debug("ENTERING setLoggingLevel. Logger : " + logger + " Level : " + level);
        }
        java.util.logging.Logger l = java.util.logging.Logger.getLogger(logger);
        l.setLevel(Level.parse(level));
        if (log.isDebugEnabled()) {
            log.debug("EXITING setLoggingLevel");
        }
    }

    /**
     * Clear the cache. Cant just clear the map cause then there would be no
     * properties available to call SCA. Thus get the properties required to
     * call SCA into a new map and swap the old map with the new one and store
     * the old one in case sca is down and properties cant be fetched
     */
    public static void clearCache() {
        if (log.isDebugEnabled()) {
            log.debug("ENTERING clearCache");
        }

        newProps = new ConcurrentHashMap<>();
        for (Object propKeyObj : props.keySet()) {
            try {
                String propKey = (String) propKeyObj;
                log.debug("Refreshing property [{}] in cache", propKey);
                String val = getPropertyInternal(propKey, false);
                if (val.length() > 100) {
                    log.warn("Cache cleared. New value for property [{}] is [{}]...", propKey, val.substring(0, 100));
                } else {
                    log.warn("Cache cleared. New value for property [{}] is [{}]", propKey, val);
                }
                newProps.put(propKey, val);
            } catch (Exception e) {
                log.warn("Error refreshing property in cache: ", e);
            }
        }
        // Store the properties in the old Props map so we can use them if SCA is down after clearing the cache
        oldProps = null;
        oldProps = props;
        props = newProps;
        setProps.clear();
        mapProps.clear();
        listProps.clear();
        setLoggingLevels();
        if (log.isDebugEnabled()) {
            log.debug("EXITING clearCache");
        }
    }

    /**
     * Get a property as a List Used for properties whos values are carriage
     * return <cr><lf> delimited strings
     *
     * @param prop The property field name
     * @return List The property values in a list. If the property does not
     * exist, null will be returned
     */
    public static List<String> getPropertyAsList(String prop) {

        List<String> list = listProps.get(prop);
        if (list == null) {
            String value = getProperty(prop);
            if (value == null) {
                return null;
            }
            list = new ArrayList<>();
            StringTokenizer stValues = new StringTokenizer(value, "\r\n");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (val.length() != 0) {
                    list.add(val);
                }
            }
            listProps.put(prop, list);
        }
        return list;
    }

    /* Get a property as a Set Used for properties whos values are carriage return <cr><lf> delimited strings
     *
     * @param prop The property field name
     * @return Set The property values in a list. If the property does not exist, null will be returned
     */
    public static Set<String> getPropertyAsSet(String prop) {

        Set<String> set = setProps.get(prop);
        if (set == null) {
            String value = getProperty(prop);
            if (value == null) {
                return null;
            }
            set = new HashSet<>();
            StringTokenizer stValues = new StringTokenizer(value, "\r\n");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (val.length() != 0) {
                    set.add(val);
                }
            }
            setProps.put(prop, set);
        }
        return set;
    }

    public static Map<String, String> getPropertyAsMap(String prop) {
        Map<String, String> map = mapProps.get(prop);
        if (map == null) {
            String value = getProperty(prop);
            if (value == null) {
                return null;
            }
            map = new HashMap<>();
            StringTokenizer stValues = new StringTokenizer(value, "\r\n");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                String[] bits = val.split("=");
                if (bits.length == 2) {
                    map.put(bits[0].trim(), bits[1].trim());
                }
            }
            mapProps.put(prop, map);
        }

        return map;
    }

    /* Get a property as a List of string arrays based on a SQL query as the property value
     *
     * @param prop The property field name
     * @return List The property values in a list of String arrays. If the property does not exist, null will be returned
     */
    public static List<String[]> getPropertyFromSQL(String prop) {
        String rows = BaseUtils.getProperty(prop);
        if (rows == null) {
            return null;
        }
        String[] rowsAsArray = rows.split("_newrow_");

        List<String[]> ret = new ArrayList<>();
        for (String row : rowsAsArray) {
            String[] rowAsArray = row.split("\\|");
            int index = 0;
            for (String entry : rowAsArray) {
                rowAsArray[index] = entry.replaceAll("_pipe_", "|");
                index++;
            }
            ret.add(rowAsArray);
        }
        return ret;
    }

    public static Set<String> getPropertyFromSQLAsSet(String prop) {
        String rows = BaseUtils.getProperty(prop);
        if (rows == null) {
            return null;
        }
        String[] rowsAsArray = rows.split("_newrow_");

        Set<String> ret = new HashSet<>();
        for (String row : rowsAsArray) {
            String[] rowAsArray = row.split("\\|");
            ret.add(rowAsArray[0].replaceAll("_pipe_", "|"));
        }
        return ret;
    }

    public static List<String[]> getPropertyFromSQLWithoutCache(String prop) {
        String rows = BaseUtils.getPropertyWithoutCache(prop);
        if (rows == null) {
            return null;
        }
        String[] rowsAsArray = rows.split("_newrow_");

        List<String[]> ret = new ArrayList<>();
        for (String row : rowsAsArray) {
            String[] rowAsArray = row.split("\\|");
            int index = 0;
            for (String entry : rowAsArray) {
                rowAsArray[index] = entry.replaceAll("_pipe_", "|");
                index++;
            }
            ret.add(rowAsArray);
        }
        return ret;
    }

    public static Set<String> getPropertyFromSQLAsSetWithoutCache(String prop) {
        String rows = BaseUtils.getPropertyWithoutCache(prop);
        if (rows == null) {
            return null;
        }
        String[] rowsAsArray = rows.split("_newrow_");

        Set<String> ret = new HashSet<>();
        for (String row : rowsAsArray) {
            String[] rowAsArray = row.split("\\|");
            ret.add(rowAsArray[0].replaceAll("_pipe_", "|"));
        }
        return ret;
    }

    /**
     * Get a property as a String. First check if one is in the Map (cache). If
     * not it gets it from SCA and puts it in the map Throws a
     * PropertyFetchException if there was an error looking up a property or the
     * property was not found
     *
     * @param prop The property field name
     * @return String The property value. If the property does not exist, a
     * PropertyFetchException will be thrown
     */
    public static String getProperty(String prop) {
        String ret = props.get(prop);
        if (ret == null) {
            //not in cache
            log.debug("Property [{}] is not in cache", prop);
            ret = getPropertyInternal(prop, false);
            // Add into cache
            props.put(prop, ret);
        }
        //Check if its String _n_ and if so throw exception
        if (ret.equals(NULL_PROPERTY_STRING)) {
            throw new PropertyFetchException("Property [" + prop + "] for client [" + BaseUtils.SCA_CLIENT_ID + "] does not exist");
        }
        return ret;
    }

    public static String getPropertyWithoutCache(String prop) {
        String ret = getPropertyInternal(prop, true);
        // Add into cache
        props.put(prop, ret);
        //Check if its String _n_ and if so throw exception
        if (ret.equals(NULL_PROPERTY_STRING)) {
            throw new PropertyFetchException("Property [" + prop + "] for client [" + BaseUtils.SCA_CLIENT_ID + "] does not exist");
        }
        return ret;
    }

    public static String getProperty(String prop, String defaultValue) {
        try {
            return getProperty(prop);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getIntProperty(String prop, int defaultValue) {
        try {
            return getIntProperty(prop);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty(String prop, boolean defaultValue) {
        try {
            return getBooleanProperty(prop);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double getDoubleProperty(String prop, double defaultValue) {
        try {
            return getDoubleProperty(prop);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static BigDecimal getDecimalProperty(String prop, BigDecimal defaultValue) {
        try {
            return getDecimalProperty(prop);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get a property as a String from SCA
     *
     * @param prop The property field name
     * @return String The property value. If the property does not exist, String
     * _n_ will be returned. If calling SCA fails then null will be returned
     */
    private static String getPropertyInternal(String prop, boolean skipCache) {

        log.debug("In getPropertyInternal [{}]", prop);

        if (!isAvailable) {
            throw new PropertyFetchException("Property Framework is not ready yet. Possibly SCA or PM is not available");
        }
        String ret = null;
        try {
            ret = getSCAProperty(prop, skipCache);
            if (ret == null) {
                // Called SCA successfully and the property did not exist. 
                // Return _n_ as a string so that SCA wont be called every time this Property is requested
                log.debug("Could not get a property called [" + prop + "] returning _n_");
                ret = NULL_PROPERTY_STRING;
            }
        } catch (PropertyFetchException ex) {
            throw ex;
        } catch (Exception e) {
            log.warn("Could not get property [" + prop + "] from SCA : " + e.toString() + ". Going to try and return the previous version in the old cache");
            // Try the old cache
            if (oldProps != null) {
                ret = oldProps.get(prop);
            }
            if (ret == null) {
                log.warn("Still could not get property [" + prop + "]. Couldn't even get it from the previous property cache. Throwing PropertyFetchException");
                throw new PropertyFetchException("Error getting property " + prop + " from SCA. Error msg:" + e.toString());
            } else {
                log.warn("Got the previous version of property " + prop + ". Returning value " + ret);
            }
        }
        return ret;
    }

    /**
     * Get a property as an int
     *
     * @param prop The property field name
     * @return int The property value as a int. If the property does not exist,
     * a PropertyFetchException will be thrown
     */
    public static int getIntProperty(String prop) {
        return Integer.parseInt(getProperty(prop).trim());
    }

    public static BigDecimal getDecimalProperty(String prop) {
        return BigDecimal.valueOf(Double.parseDouble(getProperty(prop).trim()));
    }

    public static double getDoubleProperty(String prop) {
        return Double.parseDouble(getProperty(prop).trim());
    }

    /**
     * Get a property as a boolean
     *
     * @param prop The property field name
     * @return int The property value as a boolean. If the property does not
     * exist, a PropertyFetchException will be thrown
     */
    public static boolean getBooleanProperty(String prop) {
        String val = getProperty(prop).trim();
        return val.equalsIgnoreCase("true");
    }

    /**
     * Call SCA to see if any properties or version mappings have changed since
     * the cache was last flushed. If any have changed, the clearCache is called
     */
    public static boolean checkIfMustClearCache() {
        if (log.isDebugEnabled()) {
            log.debug("ENTERING checkIfMustClearCache");
        }
        String ret = null;
        boolean didPropsChange = false;
        try {
            String dateString = formatter.format(lastCleared);
            ret = doXMLPost(ServiceDiscoveryAgent.getInstance().getAvailableService("SCA").getURL(), ISSTALE_POST_DATA_BEGIN + dateString + ISSTALE_POST_DATA_END);
        } catch (Exception e) {
            log.warn("Error checking if we must clear the cache: " + e.toString());
        }
        if (ret != null && ret.contains(MUST_REFRESH_STRING)) {
            clearCache();
            lastCleared = new Date();
            didPropsChange = true;
        }
        if (log.isDebugEnabled()) {
            log.debug("EXITING checkIfMustClearCache");
        }
        return didPropsChange;
    }

    /**
     * This function is used to post given data to a specified URL.
     *
     * @param strURL the URL to which the data should be POSTed.
     * @param data the data to be POSTed.
     * @return the response received from the POST operation.
     */
    protected static String doXMLPost(String strURL, String data) {
        String ret = null;
        PostMethod post = null;
        HttpClient httpclient = null;
        try {
            // Get file to be posted
            post = new PostMethod(strURL);
            RequestEntity entity = new ByteArrayRequestEntity(data.getBytes(), "text/xml; charset=utf-8");

            post.setRequestEntity(entity);
            // Get HTTP client
            httpclient = new HttpClient();

            // use  SimpleHttpConnectionManager cause one can close its ports and prevent close-wait states
            httpclient.setHttpConnectionManager(new SimpleHttpConnectionManager());
            HttpMethodRetryHandler myretryhandler = new HttpMethodRetryHandler() {
                @Override
                public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
                    // do not retry
                    return false;
                }
            };

            HttpParams httpParams = new HttpClientParams();
            httpParams.setParameter(HttpClientParams.RETRY_HANDLER, myretryhandler);
            httpParams.setIntParameter(HttpClientParams.SO_TIMEOUT, 5000);
            httpParams.setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
            httpParams.setParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, Boolean.FALSE);
            httpclient.setParams((HttpClientParams) httpParams);
            httpclient.getHostConfiguration().setProxyHost(null);
            if (log.isDebugEnabled()) {
                log.debug("About to post to method...");
            }
            int result = httpclient.executeMethod(post);
            log.debug("Result code is [{}]", result);
            String resultBody = null;
            if (post.getResponseBodyAsStream() != null) {
                resultBody = StringEscapeUtils.unescapeXml(BaseHelper.parseStreamToString(post.getResponseBodyAsStream(), "UTF-8"));
            }
            // Display status code
            if (log.isDebugEnabled()) {
                log.debug("Finished post to method");
                log.debug("HTTP Post Response status code: [{}]", result);
                log.debug("Response body: ");
                log.debug(resultBody);
            }

            if (result == 200 && resultBody != null) {
                ret = resultBody;
            } else {
                ret = "Non 200 Response: " + result;
                if (resultBody != null) {
                    ret += " [" + resultBody + "]";
                }
            }

        } catch (Exception e) {
            log.warn("Error posting to sca at address " + strURL + " : " + e.toString());
            ret = "Error posting to " + strURL + " : " + e.toString();
        } finally {
            // Release current connection to the connection pool once you are done
            try {
                if (post != null) {
                    post.releaseConnection();
                }
            } catch (Exception ex) {
                log.warn("Error releasing http post connection:" + ex.toString());
            }
            try {
                if (httpclient != null) {
                    ((SimpleHttpConnectionManager) httpclient.getHttpConnectionManager()).shutdown();
                }
            } catch (Exception ex) {
                log.warn("Error releasing http client connection:" + ex.toString());
            }
        }
        return ret;
    }

    private static String getSCAProperty(String prop, boolean skipCache) throws Exception {
        log.debug("In getSCAProperty [{}]", prop);
        String endPoint = ServiceDiscoveryAgent.getInstance().getServiceBestEffort("SCA").getURL();
        String propVal = doXMLPost(endPoint, PROPERTY_POST_DATA_BEGIN + prop + (skipCache ? PROPERTY_POST_DATA_DONT_USE_CACHE : PROPERTY_POST_DATA_USE_CACHE) + PROPERTY_POST_DATA_END);
        log.debug("Got value [{}] for prop [{}]", propVal, prop);
        if (propVal == null) {
            throw new PropertyFetchException("No response from SCA or SCA is down -- null from XML Post. Property requested: " + prop);
        }
        if (propVal.contains(PMERROR)) {
            //TODO - need to prevent a property from having this and messing things up...
            // Property does not exist
            return null;
        }

        int propStart = propVal.indexOf(PROPVAL_DELIM);
        int propEnd = propVal.indexOf(PROPVAL_DELIM_END);

        if (propStart != -1 && propEnd != -1) {
            propVal = propVal.substring(propStart + PROPVAL_DELIM_LEN, propEnd);
            if (!isAvailable) {
                // Flag that properties are available and working
                isAvailable = true;
                log.warn("Smile property framework is now available!");
                setLoggingLevels();
            }
            log.debug("getSCAProperty [{}] returning [{}]", prop, propVal);
            return propVal;
        } else if (propVal.contains(PROPVAL_DELIM_BLANK)) {
            return "";
        } else {
            
            throw new PropertyFetchException("Problem retrieving property.  System returned [" + propVal + "] when requesting property: " + prop);
        }
    }

    public static boolean testForPropsAvailability() {
        if (!isAvailable) {
            // Props aren't available, lets try and get one
            try {
                log.warn("Smile property framework is not available yet. Testing again...");
                Props.getSCAProperty("global.properties.test", false);
            } catch (Exception e) {
                String eString = e.toString();
                if (eString.contains("No service is available for -- PM")) {
                    log.warn("Error testing for props availability - the SCA being connected to has no PM endpoints available");
                } else {
                    log.warn("Error testing for props availability [{}]", e.toString());
                }
            }
        }
        return isAvailable;
    }

    public static boolean isAvailable() {
        return isAvailable;
    }

    public static InputStream getPropertyAsStream(String prop) {
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(getProperty(prop).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new PropertyFetchException("Property [" + prop + "] for client [" + BaseUtils.SCA_CLIENT_ID + "] does not exist");
        }
        return is;
    }

}
