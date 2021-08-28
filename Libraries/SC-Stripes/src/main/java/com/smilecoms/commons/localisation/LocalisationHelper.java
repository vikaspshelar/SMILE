package com.smilecoms.commons.localisation;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.UpdatePropertyRequest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.slf4j.*;

/**
 * Helper class for all common localisation utilities
 *
 * @author PCB
 */
public class LocalisationHelper {

    private static final Logger log = LoggerFactory.getLogger(LocalisationHelper.class);
    private static final Map<String, SmileResourceBundle> bundleCache = new ConcurrentHashMap<>();
    private static final Lock lock = new ReentrantLock();

    
    public static void init() {
        getBundle(getDefaultLocale());
        log.warn("Localisation is ready!");
    }
    /**
     * Returns a SmileResourceBundle from a cache (is ones in the cache, or else
     * creates one) for the given locale
     *
     * @param locale
     * @return ResourceBundle
     * @throws java.util.MissingResourceException
     */
    public static ResourceBundle getBundle(Locale locale) throws MissingResourceException {
        checkIfMustClearCache();
        String locString = locale.toString();
        log.debug("In LocalisationHelper.getBundle for locale: [{}]", locString);
        SmileResourceBundle bundle = bundleCache.get(locString);
        boolean gotLock = false;
        if (bundle == null) {
            try {
                log.debug("Getting lock to create a new bundle for locale [{}]", locString);
                gotLock = lock.tryLock();
                log.debug("Did get lock to create a new bundle: [{}]", gotLock);
                if (!gotLock) {
                    throw new RuntimeException("Localisation not ready");
                }
                bundle = bundleCache.get(locString);
                if (bundle == null) {
                    log.debug("No bundle in cache for locale [{}]", locString);
                    bundle = new SmileResourceBundle(locString);
                    bundleCache.put(locString, bundle);
                    log.debug("Put bundle in cache for locale [{}]", locString);
                }
            } finally {
                if (gotLock) {
                    lock.unlock();
                }
            }
        }
        return bundle;
    }

    /**
     * Refresh all the bundles in the cache
     */
    private static void refreshAllBundleCachesSync() {
        synchronized (bundleCache) {
            Iterator<String> it = bundleCache.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                SmileResourceBundle bundle = bundleCache.get(key);
                bundle.refreshCache();
            }
        }
    }

    public static void refreshAllBundleCaches(boolean allJVMs) {
        if (allJVMs) {
            log.debug("Calling SCA to modify property env.resources.refresh.check.string so strings will be refreshed");
            UpdatePropertyRequest req = new UpdatePropertyRequest();
            req.setClient("default");
            req.setPropertyName("env.resources.refresh.check.string");
            req.setPropertyValue(java.util.UUID.randomUUID().toString());
            // No need to refresh this ones cache twice
            propAtLastRefresh = req.getPropertyValue();
            SCAWrapper.getAdminInstance().updateProperty(req);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                refreshAllBundleCachesSync();
            }
        }).start();
    }
    private static String propAtLastRefresh = null;

    private static void checkIfMustClearCache() {
        if (BaseUtils.isPropsAvailable()) {
            if (propAtLastRefresh == null) {
                propAtLastRefresh = BaseUtils.getProperty("env.resources.refresh.check.string");
            } else if (!propAtLastRefresh.equals(BaseUtils.getProperty("env.resources.refresh.check.string"))) {
                propAtLastRefresh = BaseUtils.getProperty("env.resources.refresh.check.string");
                log.warn("env.resources.refresh.check.string has changed so resource bundles are being refreshed");
                refreshAllBundleCaches(false);
            }
        }
    }

    /**
     * Get a SmileResourceBundle for the given String locale. Uses
     * getLocale(locale).
     *
     * @param loc as a String
     * @return SmileResourceBundle
     */
    public static SmileResourceBundle getBundle(String loc) {
        checkIfMustClearCache();
        String[] locArray = loc.split("_");
        SmileResourceBundle ret = null;
        if (locArray.length == 1) {
            ret = (SmileResourceBundle) getBundle(new Locale(locArray[0]));
        }

        if (locArray.length == 2) {
            ret = (SmileResourceBundle) getBundle(new Locale(locArray[0], locArray[1]));
        }

        if (locArray.length == 3) {
            ret = (SmileResourceBundle) getBundle(new Locale(locArray[0], locArray[1], locArray[2]));
        }
        return ret;
    }

    /**
     * Returns the locale for a language code using the property
     * env.locale.country.for.language to get the country for the language
     *
     * @param language
     * @return Locale for the language
     */
    public static Locale getLocaleForLanguage(String language) {

        if (language == null) {
            // prevent recursive loop if getDefaultLanguage() returns null
            throw new java.lang.NullPointerException();
        }
        // language is in short notation e.g. en
        Locale locale;
        String country = null;
        try {
            country = BaseUtils.getProperty("env.locale.country.for.language." + language);
        } catch (Exception e) {
            log.debug("Error getting locale: ", e.toString());
        }
        if (country == null) {
            if (log.isDebugEnabled()) {
                log.debug("The language " + language + " has been requested but is not supported as there is no property env.locale.country.for.language." + language + " The default language from env.default.portal.language will be used instead");
            }
            // There is no configured country for this language, so use default locale
            return getDefaultLocale();
        }
        locale = new Locale(language, country);
        return locale;
    }

    /**
     * Return the default locale generated by getting the default language and
     * then calling getLocaleForLanguage
     *
     * @return Default Locale
     */
    public static Locale getDefaultLocale() {
        return getLocaleForLanguage(getDefaultLanguage());
    }

    /**
     * Returns value of property env.default.portal.language
     *
     * @return language code
     */
    public static String getDefaultLanguage() {
        return BaseUtils.getProperty("env.default.portal.language");
    }

    /**
     * Get all the locales available using getAllowedLanguages and then calling
     * getLocaleForLanguage on each one
     *
     * @return a List of Locales
     */
    public static List<Locale> getAllowedLocales() {
        List<Locale> ret = new ArrayList<>();
        Iterator<String> it = getAllowedLanguages().iterator();
        while (it.hasNext()) {
            String language = it.next();
            ret.add(getLocaleForLanguage(language));
        }
        return ret;
    }

    /**
     * Returns list for property env.portal.languages.available
     *
     * @return List of strings
     */
    public static List<String> getAllowedLanguages() {
        return BaseUtils.getPropertyAsList("env.portal.languages.available");
    }

    /**
     * Returns the localised string for the given locale and key
     *
     * @param locale
     * @param key
     * @return localised string
     */
    public static String getLocalisedString(Locale locale, String key) {
        ResourceBundle b = getBundle(locale);
        return b.getString(key);
    }

    /**
     * Returns the localised string for the given locale and key with parameters
     *
     * @param locale
     * @param key
     * @param parameter
     * @return localised string
     */
    public static String getLocalisedString(Locale locale, String key, Object... parameter) {
        ResourceBundle b = getBundle(locale);
        String message = b.getString(key);
        message = MessageFormat.format(escapeCurlyBraces(message), parameter);
        message = doMaths(message);
        message = evaluateString(message);
        return message;
    }

    public static String getLocalisedStringAllowingDuplicatePlaceholders(Locale locale, String key, Object... parameter) {
        ResourceBundle b = getBundle(locale);
        String message = b.getString(key);
        // Our version allows for duplicate placeholders but is not very fast
        message = format(escapeCurlyBraces(message), parameter);
        message = doMaths(message);
        message = evaluateString(message);
        return message;
    }
    
    private static String format(String msg, Object... arguments) {
        int index = 0;
        for (Object s : arguments) {
            msg = msg.replaceAll("\\{" + index + "\\}", s.toString());
            index++;
        }
        return msg;
    }

    private static String escapeCurlyBraces(String msg) {
        if (msg.contains("'{'")) {
            // temp hack
            return msg;
        }
        log.debug("Before replacement [{}]", msg);
        String val = msg.replace("{", "'{'").replace("}", "'}'").replaceAll("'\\{'(\\d+)'\\}'", "{$1}");
        log.debug("After replacement [{}]", val);
        return val;
    }

    private static final Pattern DIVIDE_PATTERN = Pattern.compile("divide\\((.*)\\)");

    private static String doMaths(String s) {

        try {
            int mathsLoc = s.indexOf("divide");
            if (mathsLoc == -1) {
                return s;
            }
            log.debug("Before maths: [{}]", s);
            Matcher m = DIVIDE_PATTERN.matcher(s);
            while (m.find()) {
                String divideSum = m.group();
                String basics = divideSum.replace("divide(", "").replace(")", "");
                log.debug("Basics is [{}]", basics);
                double numerator = Double.parseDouble(basics.split("\\,")[0]);
                double denominator = Double.parseDouble(basics.split("\\,")[1]);
                String result = Long.toString((long) (numerator / denominator));
                log.debug("Replacing [{}] with [{}]", divideSum, result);
                s = s.replace(divideSum, result);
            }

        } catch (Exception e) {
            log.warn("Error doing divide:", e);
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "Localisation", "Error doing divide in localisation: " + s);
            return s;
        }
        log.debug("returning: [{}]", s);
        return s;
    }

    public static String evaluateString(String expr) {

        int scriptLoc = expr.indexOf("<JAVASCRIPT>");
        if (scriptLoc == -1) {
            return expr;
        }
        log.debug("Evaluating expression [{}]", expr);
        if (expr.contains("'{'")) {
            expr = expr.replace("'{'", "{").replace("'}'", "}");
        }
        String[] bits = expr.split("\\<JAVASCRIPT\\>");
        StringBuilder sb = new StringBuilder();
        ScriptEngine engine = null;
        for (String bit : bits) {
            if (bit.contains("</JAVASCRIPT>")) {
                String[] subbits = bit.split("\\</JAVASCRIPT\\>");
                if (engine == null) {
                    engine = new ScriptEngineManager().getEngineByName("JavaScript");
                }
                String res;
                String expression = subbits[0];
                try {
                    engine.eval(expression);
                    res = engine.get("text").toString();
                } catch (Exception e) {
                    res = e.toString();
                }
                sb.append(res);
                if (subbits.length > 1) {
                    sb.append(subbits[1]);
                }
            } else {
                sb.append(bit);
            }

        }
        return sb.toString();

    }
}
