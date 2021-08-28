package com.smilecoms.commons.localisation;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.Resource;
import com.smilecoms.commons.sca.ResourceList;
import com.smilecoms.commons.sca.SCAString;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.util.Utils;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.*;

/**
 * Our implementation of a resource bundle that retrieves the resource strings
 * from SCA (SCA in turn uses Property Manager)
 *
 * @author PCB
 */
public class SmileResourceBundle extends java.util.ResourceBundle {

    private static final Logger log = LoggerFactory.getLogger(SmileResourceBundle.class.getName());
    private Map<String, String> keys = new ConcurrentHashMap<>();
    private final String locale;
    private static final String ACTION = ".action.";
    private static final String SPACE = " ";
    private static final String EMPTY = "";
    private static final String NULL = "null\\.";
    private static final String DOT = ".";
    private static final int ACTION_LEN = ACTION.length();
    private SmileResourceBundle fallBackBundle = null;
    private boolean hasFallBackBundle = false;

    /**
     * Constructor that populates Map with resource strings
     *
     * @param locale
     */
    public SmileResourceBundle(String locale) {
        this.locale = locale;
        int lastUnderscore = locale.lastIndexOf("_");
        if (lastUnderscore > 0) {
            //is not just a language so it does have a fallback
            String fallBackLocale = locale.substring(0, lastUnderscore);
            fallBackBundle = LocalisationHelper.getBundle(fallBackLocale);
            if (fallBackBundle != null) {
                hasFallBackBundle = true;
            }
        }
        refreshCache();
    }

    /**
     * Clear out and then populate the Hashmap of resource keys and values
     */
    protected final void refreshCache() {
        log.debug("RefreshCache: Populating resource bundle data into hashmap for locale [{}]", locale);
        Map<String, String> newKeys = new HashMap<>();
        SCAString loc = new SCAString();
        loc.setString(locale);
        ResourceList list = SCAWrapper.getAdminInstance().getResourceList(loc);
        String resources = Utils.unzip(list.getResources());
        String[] bits = resources.split("\\|");
        boolean onKey = true;
        String key = null;
        String value = null;
        for (String bit : bits) {
            if (onKey) {
                key = bit.toLowerCase();
                onKey = false;
            } else {
                value = doPropertyReplacements(bit);
                newKeys.put(key, value);
                onKey = true;
            }
            if (log.isDebugEnabled()) {
                log.debug("Key [{}] Value [{}]", key, value);
            }
        }
        keys = newKeys;
        log.debug("RefreshCache: Finished populating resource bundle data into hashmap for locale [{}]", locale);
    }
    private static final Pattern PROP_PATTERN = Pattern.compile("<env\\.[a-zA-Z0-9\\.]+>");

    private static String doPropertyReplacements(String s) {
        try {
            int propLoc = s.indexOf("<env.");
            if (propLoc == -1) {
                return s;
            }
            log.debug("Before prop: [{}]", s);
            Matcher m = PROP_PATTERN.matcher(s);
            while (m.find()) {
                String propNameWithBrackets = m.group();
                String propName = propNameWithBrackets.replace("<", "").replace(">", "");
                log.debug("Prop name is [{}]", propName);
                String prop = BaseUtils.getProperty(propName);
                log.debug("Replacing [{}] with [{}]", propNameWithBrackets, prop);
                s = s.replace(propNameWithBrackets, prop);
            }

        } catch (Exception e) {
            log.warn("Error doing property replacement:", e);
            return s;
        }
        log.debug("returning: [{}]", s);
        return s;
    }

    private String cleanKey(String key) {
        key = key.toLowerCase();
        int startFrom = key.indexOf(ACTION);
        if (startFrom != -1) {
            key = key.substring(startFrom + ACTION_LEN);
        }
        key = key.replaceAll(SPACE, DOT);
        key = key.replaceAll(NULL, EMPTY);
        return key;
    }

    /**
     * Just to help performance
     *
     * @param key
     * @return Object
     */
    private Object handleGetObjectWithCleanKey(String key) {
        String ret = keys.get(key);
        if (ret != null) {
            return ret;
        }
        //This bundle doesn't have it - try fallback bundle                
        if (hasFallBackBundle) {
            ret = (String) fallBackBundle.handleGetObjectWithCleanKey(key);
        } else {
            // Still null, and has no fallback bundle so add it
            ret = "???" + key + "???";
            addKey(key);
        }
        return ret;
    }

    /**
     * Return the resource value for the key. Do some stuff to try and get a
     * match for various ways that stripes sometimes sends keys. If all else
     * fails, return the key with 3 question marks around it and add the key
     * into resource table via call to SCA
     *
     * @param key
     * @return The resource value
     */
    @Override
    protected Object handleGetObject(String key) {
        key = cleanKey(key);
        Object ret = handleGetObjectWithCleanKey(key);
        if (log.isDebugEnabled()) {
            log.debug("SmileResourceBundle returning string [" + ret.toString() + "] for key [" + key + "] and locale [" + this.locale + "]");
        }
        return ret;
    }

    /**
     * Not implemented
     *
     * @return error
     */
    @Override
    public Enumeration<String> getKeys() {
        throw new java.lang.UnsupportedOperationException("SmileResourceBundle: getKeys not implemented yet");
    }

    /**
     * Add a key into the resource table via a call to SCA. Used by
     * handleGetObject
     *
     * @param key
     */
    private void addKey(String key) {
        // Only add resources if the properties setting allow for this (only want to add properties in development environments)
        if (!BaseUtils.getBooleanProperty("env.localisation.add.unknown.resources")) {
            return;
        }
        try {
            log.warn("Adding key " + key + " for locale " + locale + " to resources table by calling addResource on SCA");
            Resource r = new Resource();
            r.setKey(key);
            r.setLocale(this.locale);
            r.setValue("?" + key + "?");
            SCAWrapper.getAdminInstance().addResource(r);
        } catch (Exception e) {
            log.warn("Error adding key to resources table via SCA:" + e.toString());
        }
    }
}
