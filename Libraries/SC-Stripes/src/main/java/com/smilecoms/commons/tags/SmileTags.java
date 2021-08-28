package com.smilecoms.commons.tags;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.localisation.LocalisationHelper;
import com.smilecoms.commons.sca.AVP;
import com.smilecoms.commons.sca.Account;
import com.smilecoms.commons.sca.AccountQuery;
import com.smilecoms.commons.sca.Photograph;
import com.smilecoms.commons.sca.ProductInstance;
import com.smilecoms.commons.sca.ProductServiceInstanceMapping;
import com.smilecoms.commons.sca.SCAWrapper;
import com.smilecoms.commons.sca.ServiceInstance;
import com.smilecoms.commons.sca.StAccountLookupVerbosity;
import com.smilecoms.commons.sca.UnitCreditInstance;
import com.smilecoms.commons.sca.UnitCreditSpecification;
import com.smilecoms.commons.sca.beans.NonUserSpecificCachedDataHelper;
import com.smilecoms.commons.sca.beans.UserSpecificCachedDataHelper;
import com.smilecoms.commons.stripes.InsufficientPrivilegesException;
import com.smilecoms.commons.stripes.SmileActionBean;
import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.util.Utils;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.XMLGregorianCalendar;
import net.sourceforge.stripes.action.ActionBeanContext;
import org.slf4j.*;

/**
 * Enter description of class
 *
 */
public class SmileTags {

    private static final Logger log = LoggerFactory.getLogger(SmileTags.class);
    private static final String BLANK = "";
    private static final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
    private static final DecimalFormatSymbols commaFormatSymbol = new DecimalFormatSymbols();

    static {
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
        commaFormatSymbol.setDecimalSeparator('.');
        commaFormatSymbol.setGroupingSeparator(',');
    }
    private static final SimpleDateFormat sdfLong = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final SimpleDateFormat sdfShort = new SimpleDateFormat("yyyy/MM/dd");
    private static final DateFormat timeFormat = new SimpleDateFormat("k'h'mm'm'ss's'");
    private static final DecimalFormat BigNumber = new DecimalFormat("#,##0", formatSymbols);
    private static final DecimalFormat BigDecimal = new DecimalFormat("#,##0.0000000000", formatSymbols);

    public static String getLocaleDisplayName(String localeName) {
        String[] localeArray = localeName.split("_");
        Locale locale = new Locale(localeArray[0], localeArray[1]);
        return locale.getDisplayName();
    }

    public static String retrieveQueueID(String queueString) {
        return queueString.substring(queueString.indexOf("<") + 1, queueString.indexOf(">"));
    }

    public static String retrieveQueueName(String queueString) {
        return queueString.substring(0, queueString.indexOf("<"));
    }

    public static String getLocalisedString(String resourceKey, String locale) {
        return LocalisationHelper.getLocalisedString(new Locale(locale), resourceKey);
    }

    public static int getListSize(Collection list) {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public static int getTimeZoneOffsetHours() {
        Calendar cal = Calendar.getInstance();
        return (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000 * 60);
    }

    public static String getPhotoGuid(List<Photograph> photos, String type) throws Exception {
        if (photos == null || photos.isEmpty()) {
            return "";
        }
        for (Photograph photo : photos) {
            if (photo.getPhotoType().equals(type)) {
                return photo.getPhotoGuid();
            }
        }
        return "";
    }

    public static int getListSize(List list, String methodName, String value) throws Exception {
        int ret = 0;
        for (Object item : list) {
            Method m = item.getClass().getMethod(methodName);
            Object val = m.invoke(item);
            String valAsString;
            if (val instanceof String) {
                valAsString = (String) val;
            } else {
                // Seems like the return value is not a string. Could be an enum. Try calling the method name "value" on it
                Method mOnEnum = val.getClass().getMethod("value");
                valAsString = (String) mOnEnum.invoke(val);
            }
            if (valAsString.equalsIgnoreCase(value)) {
                ret++;
            }
        }
        return ret;
    }

    public static Object getEntryInList(List list, String methodName, int value) throws Exception {
        for (Object item : list) {
            Method m = item.getClass().getMethod(methodName);
            Integer val = (Integer) m.invoke(item);
            // If response is null, then it must be filtered out
            if (val == null) {
                break;
            }
            if (val == value) {
                return item;
            }
        }
        return null;
    }

    public static List filterList(List list, String methodName, String value) throws Exception {
        List<Object> retList = new ArrayList<>();
        if (list == null) {
            return null;
        }
        for (Object item : list) {
            String[] methodNames = methodName.split("\\|");
            String[] values = value.split("\\|\\|");

            boolean flag = true;
            // Must match all filter criteria for it to remain in the list
            for (int i = 0; i < methodNames.length; i++) {
                methodName = methodNames[i];
                value = values[i];

                Method m = item.getClass().getMethod(methodName);
                Object val = m.invoke(item);
                // If response is null, then it must be filtered out
                if (val == null) {
                    break;
                }
                String valAsString;
                if (val instanceof String) {
                    valAsString = (String) val;
                } else {
                    // Seems like the return value is not a string. Could be an enum. Try calling the method name "value" on it
                    Method mOnEnum = val.getClass().getMethod("value");
                    valAsString = (String) mOnEnum.invoke(val);
                }
                if (!matches(valAsString, value)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                retList.add(item);
            }
        }
        return retList;
    }

    @SuppressWarnings("unchecked")
    public static List orderList(List list, String getterToOrderBy, String order) throws Exception {
        if (list == null) {
            return null;
        }
        Comparator comparator = new DynamicComparator(getterToOrderBy, order);
        Collections.sort(list, comparator);
        return list;
    }

    private static class DynamicComparator implements Comparator {

        private final String getterToOrderBy;
        private final String order;
        private static final String DESC = "desc";
        private Method m = null;

        public DynamicComparator(String getterToOrderBy, String order) {
            this.getterToOrderBy = getterToOrderBy;
            this.order = order;
        }

        @Override
        public int compare(Object o1, Object o2) {
            // Ok so, o1 and o2 are both objects in a list. We need to get the result of calling the method "getterToOrderBy" on each object and compare them
            int ret = 0;
            if (m == null) {
                try {
                    m = o1.getClass().getMethod(getterToOrderBy);
                } catch (Exception ex) {
                    log.error("Error", ex);
                }
            }
            try {
                Object o1Data = m.invoke(o1);
                Object o2Data = m.invoke(o2);
                if (o1Data instanceof String) {
                    ret = ((String) o1Data).compareTo((String) o2Data);
                } else if (o1Data instanceof XMLGregorianCalendar) {
                    ret = ((XMLGregorianCalendar) o1Data).compare((XMLGregorianCalendar) o2Data);
                } else if (o1Data instanceof Integer) {
                    ret = ((Integer) o1Data).compareTo((Integer) o2Data);
                } else if (o1Data instanceof Double) {
                    ret = ((Double) o1Data).compareTo((Double) o2Data);
                }
            } catch (Exception ex) {
                log.error("Error", ex);
            }

            if (order.equalsIgnoreCase(DESC)) {
                ret *= -1;
            }
            return ret;
        }
    }

    public static int add(int x, int y) {
        return x + y;
    }

    /**
     * Format the xml calendar into human readable string.
     *
     * @param d XMLGregorianCalendar
     * @return Formatted string.
     */
    public static String formatDateLong(XMLGregorianCalendar d) {
        if (d == null || d.toGregorianCalendar().getTime().getTime() == 0) {
            return BLANK;
        }
        String ret = sdfLong.format(d.toGregorianCalendar().getTime());
        return ret;
    }

    /**
     * Format the xml calendar into human readable string.
     *
     * @param d XMLGregorianCalendar
     * @return Formatted string.
     */
    public static String formatDateShort(XMLGregorianCalendar d) {
        if (d == null || d.toGregorianCalendar().getTime().getTime() == 0) {
            return BLANK;
        }
        String ret = sdfShort.format(d.toGregorianCalendar().getTime());
        return ret;
    }

    public static String getLastPart(String s, int chars) {
        if (s.length() > chars) {
            return s.substring(s.length() - chars);
        }
        return s;
    }

    public static String formatBigNumber(double d) {
        return BigNumber.format(d);
    }

    public static String formatBigDecimal(double d) {
        return BigDecimal.format(d);
    }

    public static String formatURI(String uri) {
        if (uri == null) {
            return "";
        }
        return uri.split("@")[0];
    }

    public static long minutesAgo(javax.xml.datatype.XMLGregorianCalendar date) {
        Date now = new Date();
        long diff = now.getTime() - date.toGregorianCalendar().getTime().getTime();
        return diff / 60000;
    }

    /**
     * Format the calendar into human readable string in form of Month Year
     *
     * @param d GregorianCalendar
     * @return Formatted string.
     */
    public static String formatDateShortMonthYear(XMLGregorianCalendar d) {
        if (d == null) {
            return BLANK;
        }

        String monthString;
        switch (Integer.valueOf(d.getMonth())) {
            case 1:
                monthString = "January";
                break;
            case 2:
                monthString = "February";
                break;
            case 3:
                monthString = "March";
                break;
            case 4:
                monthString = "April";
                break;
            case 5:
                monthString = "May";
                break;
            case 6:
                monthString = "June";
                break;
            case 7:
                monthString = "July";
                break;
            case 8:
                monthString = "August";
                break;
            case 9:
                monthString = "September";
                break;
            case 10:
                monthString = "October";
                break;
            case 11:
                monthString = "November";
                break;
            case 12:
                monthString = "December";
                break;
            default:
                monthString = "Invalid month";
                break;
        }
        String ret = monthString + " " + String.valueOf(d.getYear());
        return ret;
    }

    /**
     * Format the xml calendar into time since epoch long
     *
     * @param d XMLGregorianCalendar
     * @return time since epoch long.
     */
    public static Long convertXMLGregorianCalendarToTimestamp(XMLGregorianCalendar d) {

        Date date = Utils.getXMLGregorianCalendarAsDate(d, new java.util.Date());
        if (d == null) {
            return (long) 0;
        }
        return date.getTime();
    }

    /**
     * Format the time in seconds into human readable string.
     *
     * @param seconds Time in seconds.
     * @return Formatted string.
     */
    public static String formatTime(int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(seconds * 1000);
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        String ret = timeFormat.format(calendar.getTime());
        if (2 % 2 == 0) {
        }
        // remove leading 24h
        if (ret.startsWith("24h")) {
            ret = ret.substring(3);
        }
        return ret;
    }

    public static int getStringLength(String string) {
        int length = 0;
        if (string != null) {
            length = string.length();
        }
        return length;
    }

    /**
     * Format the xml date into a java Date object.
     *
     * @param d XMLGregorianCalendar
     * @return Java Date object.
     */
    public static Date getJavaDate(XMLGregorianCalendar d) {
        return d.toGregorianCalendar().getTime();
    }

    // PCB 202 - Round down when displaying to users so there is no confusion when they cant buy something cause their balance is rounded up when displayed
    public static String convertCentsToCurrencyShort(double minorUnit) {
        return Utils.convertCentsToCurrencyShort(minorUnit);
    }

    public static String convertCentsToCurrencyLong(double minorUnit) {
        return Utils.convertCentsToCurrencyLong(minorUnit);
    }

    public static double convertCentsToLong(double minorUnit) {
        return (double) (Math.floor(minorUnit) / 100.0);
    }

    public static double convertCentsToLongRoundUp(double minorUnit) {
        return (double) (Math.ceil(minorUnit) / 100.0);
    }

    public static double convertCentsToLongRoundHalfEven(double minorUnit) {
        return Utils.round(minorUnit / 100.0, 2);
    }

    public static String convertCentsToCurrencyLongRoundHalfEven(double minorUnit) {
        return Utils.convertCentsToCurrencyLongRoundHalfEven(minorUnit);
    }

    public static String convertCentsToSpecifiedCurrencyLongRoundHalfEven(String currency, double minorUnit) {
        return Utils.convertCentsToSpecifiedCurrencyLongRoundHalfEven(currency, minorUnit);
    }

    /**
     * Get the phone number from the SIP URI.
     *
     * @param uri The number in URI format ie. 087xxxx
     * @return PhoneNumber
     */
    public static String getPhoneNumberFromSipURI(String uri) {
        return Utils.getFriendlyPhoneNumber(uri);
    }

    public static String getStringFromRemoteCache(String key) {
        return (String) CacheHelper.getFromRemoteCache(key);
    }

    public static String getProperty(String key) {
        return BaseUtils.getProperty(key);
    }

    public static String getPropertyWithDefault(String key, String defaultVal) {
        return BaseUtils.getProperty(key, defaultVal);
    }

    public static List<String> getPropertyAsList(String key) {
        return BaseUtils.getPropertyAsList(key);
    }

    public static Set<String> getPropertyAsSet(String key) {
        return BaseUtils.getPropertyAsSet(key);
    }

    public static boolean setContains(Set set, Object obj) {
        return set.contains(String.valueOf(obj));
    }

    public static Set<String> getRoleBasedSubsetOfPropertyAsSet(String key, HttpServletRequest request) {
        Set<String> ret = new TreeSet<>();
        for (String row : BaseUtils.getPropertyAsList(key)) {
            if (!row.contains("=")) {
                ret.add(row); // Default to allowing everyone
                continue;
            }
            String[] bits = row.split("\\=");
            String val = bits[0];
            String roles = bits[1];
            String[] rolesList = roles.split("\\,");
            log.debug("Value [{}] is accessible by [{}]", val, rolesList);
            for (String role : rolesList) {
                role = role.trim();
                if (request.isUserInRole(Utils.getUnfriendlyRoleName(role))) {
                    ret.add(val);
                    break;
                }
            }
        }
        log.debug("Returning values [{}]", ret);
        return ret;
    }

    /**
     * Return the first property the user is allowed or the first allowed by
     * everyone
     *
     * @param key
     * @param request
     * @return
     */
    public static String getRoleBasedProperty(String key, HttpServletRequest request) {
        for (String row : BaseUtils.getPropertyAsList(key)) {
            String[] bits = row.split("\\=", 2);
            if (bits.length != 2) {
                continue;
            }
            String val = bits[1];
            String roles = bits[0];
            String[] rolesList = roles.split("\\,");
            log.debug("Value [{}] is accessible by [{}]", val, rolesList);
            for (String role : rolesList) {
                role = role.trim();
                if (role.equalsIgnoreCase("default") || request.isUserInRole(Utils.getUnfriendlyRoleName(role))) {
                    return val;
                }
            }
        }
        return null;
    }

    public static boolean hasPermissions(String resourceObj, ActionBeanContext context) {
        SmileActionBean sab = new SmileActionBean();
        sab.setContext(context);
        try {
            sab.checkPermissions(resourceObj);
            return true;
        } catch (InsufficientPrivilegesException e) {
            return false;
        }
    }

    public static String getKYCStatus(ProductInstance pi) {
        if (!BaseUtils.getProperty("env.locale.country.for.language.en").equals("UG")) {
            return "";
        }
        boolean foundASIMService = false;
        for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
            ServiceInstance si = m.getServiceInstance();
            if (!foundASIMService && si.getServiceSpecificationId() == 1) {
                foundASIMService = true;
            }
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("KYCStatus")) {
                    return "KYC:" + avp.getValue();
                }
            }
        }
        if (foundASIMService) {
            return "KYC:Missing";
        }
        return "";
    }

    public static boolean isProductInstanceMissingKYC(ProductInstance pi) {
        if (!BaseUtils.getProperty("env.locale.country.for.language.en").equals("UG")) {
            return false;
        }
        boolean foundASIMService = false;
        for (ProductServiceInstanceMapping m : pi.getProductServiceInstanceMappings()) {
            ServiceInstance si = m.getServiceInstance();
            if (!foundASIMService && si.getServiceSpecificationId() == 1) {
                foundASIMService = true;
            }
            for (AVP avp : si.getAVPs()) {
                if (avp.getAttribute().equals("KYCStatus")) {
                    if (avp.getValue().equals("Complete")) {
                        return false;
                    }
                }
            }
        }
        return foundASIMService;
    }

    public static List<String[]> getPropertyFromSQL(String key) {
        return BaseUtils.getPropertyFromSQL(key);
    }

    public static String getSubProperty(String propName, String subKey) {
        String prop = null;
        try {
            prop = BaseUtils.getSubProperty(propName, subKey);;
        } catch (Exception ex) {
            log.debug("Subproperty does not exist, error: {}", ex.toString());
        }
        return prop;
    }

    public static List<List<String>> getDelimitedPropertyAsList(String key) {

        List<String> lstProperty = BaseUtils.getPropertyAsList(key);
        List<List<String>> lstResults = new ArrayList<>();
        StringTokenizer st;

        List<String> stArray;

        for (String prop : lstProperty) {
            st = new StringTokenizer(prop, "|");
            stArray = new ArrayList();
            stArray.add(st.nextToken());
            stArray.add(st.nextToken());
            lstResults.add(stArray);
        }

        return lstResults;
    }

    public static String getPropertyValueFromList(String propKey, String valueKey) {

        List<String> lstProperty = BaseUtils.getPropertyAsList(propKey);
        StringTokenizer st;

        for (String prop : lstProperty) {
            st = new StringTokenizer(prop, "=");
            if (st.nextToken().equals(valueKey)) {
                return st.nextToken();
            }
        }
        return "{no mapping found}";
    }

    public static String getDelimitedPropertyValueMapping(String propKey, String valueKey) {

        List<String> lstProperty = BaseUtils.getPropertyAsList(propKey);
        StringTokenizer st;

        for (String prop : lstProperty) {
            st = new StringTokenizer(prop, "|");
            if (st.nextToken().equals(valueKey)) {
                return st.nextToken();
            }
        }
        return "{no mapping found}";
    }

    public static String encodeURL(String url, HttpServletResponse response) {
        return response.encodeURL(url);
    }

    public static String capitalize(String s) {
        return Utils.capitalize(s);
    }

    public static String toString(long l) {
        return String.valueOf(l);
    }

    public static String concat(String s1, String s2) {
        return s1 + s2;
    }

    public static String concat3(String s1, String s2, String s3) {
        return s1 + s2 + s3;
    }

    private static boolean matches(String field, String regex) {
        return Utils.matchesWithPatternCache(field, regex);
    }

    public static long getDaysAfterEpoch(XMLGregorianCalendar theDate) {
        Date d;
        if (theDate == null) {
            d = new Date();
        } else {
            d = theDate.toGregorianCalendar().getTime();
        }
        long days = d.getTime() / (1000 * 3600 * 24);
        return days;
    }

    public static String getPageXtoYofZ(int pageStart, int pageSize, int numberOfMessages) throws Exception {
        String output = "";

        int x = pageStart;
        if (numberOfMessages > 0) {
            x++;
        }
        int z = numberOfMessages;
        int y = pageStart + pageSize;
        if (y > z) {
            y = z;
        }
        return LocalisationHelper.getLocalisedString(LocalisationHelper.getDefaultLocale(), "results.xtoyofz", String.valueOf(x), String.valueOf(y), String.valueOf(z));
    }

    public static String convertDMStoDeg(String gps) {
        try {
            double[] degPair = Utils.convertDMSToDegree(gps);
            return String.valueOf(degPair[0]) + "," + String.valueOf(degPair[1]);
        } catch (Exception e) {
            return "";
        }
    }

    public static String displayVolumeAsString(double units, String unitType) {
        return Utils.displayVolumeAsString(units, unitType);
    }

    public static String displayVolumeAsStringWithCommaGroupingSeparator(double units, String unitType) {
        return Utils.displayVolumeAsStringWithCommaGroupingSeparator(units, unitType);
    }

    public static String displayVolumeAsStringInGB(double units, String unitType) {
        return Utils.displayVolumeAsStringInGB(units, unitType);
    }

    public static String convertCentsToCurrencyShortWithCommaGroupingSeparator(double minorUnit) {
        double majorUnit = (double) (Math.floor(minorUnit) / 100.0);
        DecimalFormat myFormatter = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.shortformat"));
        return myFormatter.format(majorUnit);
    }

    public static String convertCentsToCurrencyLongWithCommaGroupingSeparator(double minorUnit) {
        double majorUnit = (double) (Math.floor(minorUnit) / 100.0);
        DecimalFormat myFormatter = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.longformat"));
        return myFormatter.format(majorUnit);
    }
    private static Map<String, String> validationMap = null;
    private static long validationMapLastRefreshed = 0;

    public static String getValidationRule(String fieldName, String defaultValidation) {

        Map<String, String> validations = getValidations();
        if (validations == null) {
            return defaultValidation;
        }
        String validation = validations.get(fieldName);
        if (validation == null) {
            return defaultValidation;
        } else {
            return validation;
        }
    }

    private static Map<String, String> getValidations() {
        long now = System.currentTimeMillis();
        if (validationMap == null || now - validationMapLastRefreshed > 120000) {
            validationMapLastRefreshed = now;
            Map<String, String> tmpValidationMap = new ConcurrentHashMap();
            try {
                List<String> validationRules = BaseUtils.getPropertyAsList("env.sep.validation.rules");
                for (String rule : validationRules) {
                    int index = rule.indexOf("=");
                    String fName = rule.substring(0, index);
                    String validation = rule.substring(index + 1);
                    tmpValidationMap.put(fName, validation);
                }
            } catch (Exception e) {
                log.debug("There is no property env.sep.validation.rules. Default validations will be used");
            }
            validationMap = tmpValidationMap;
        }
        return validationMap;
    }

    public static String abbreviate(String longString, int len) {
        if (longString.length() <= len) {
            return longString;
        }
        return longString.substring(0, len) + "...";

    }

    public static String breakUp(String longString, int maxLengthOfBits) {
        StringBuilder sb = new StringBuilder();
        int nonSpaceCharsWrittenInARow = 0;
        for (char c : longString.toCharArray()) {
            if (nonSpaceCharsWrittenInARow == maxLengthOfBits) {
                nonSpaceCharsWrittenInARow = 0;
                sb.append(" ");
            }
            sb.append(c);
            if (c == ' ' || c == '\r' || c == '\n') {
                // We have a breaking character so can reset nonSpaceCharsWrittenInARow
                nonSpaceCharsWrittenInARow = 0;
            } else {
                nonSpaceCharsWrittenInARow++;
            }
        }
        return sb.toString();
    }

    public static String getObjectAsJsonString(Object obj) {
        Gson gson = new Gson();
        String jsonString = "";
        if (obj instanceof JsonElement) {
            JsonElement je = (JsonElement) obj;//maintains the original object state, prevents array object being encapsulated with new json object/element
            jsonString = gson.toJson(je);
        } else {
            jsonString = gson.toJson(obj);
        }
        log.debug("Json String is [{}]", jsonString);
        return jsonString;
    }

    public static boolean isBitOn(long l, int index) {
        return (l & (long) Math.pow(2, index)) != 0;
    }

    public static String getValueFromCRDelimitedAVPString(String crDelimitedList, String propName) {
        return Utils.getValueFromCRDelimitedAVPString(crDelimitedList, propName);
    }

    public static List<String> getListFromCommaDelimitedString(String commaDelimitedList) {
        return Utils.getListFromCommaDelimitedString(commaDelimitedList);
    }

    public static int getDaysBetweenDates(Date startDate, Date endDate) {
        return Utils.getDaysBetweenDates(startDate, endDate);
    }

    public static double getBundleBalance(long accountId) {
        double bundle = 0.00d;
        Date now = new Date();

        AccountQuery aq = new AccountQuery();
        aq.setAccountId((long) accountId);
        aq.setVerbosity(StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);
        Account acc = SCAWrapper.getUserSpecificInstance().getAccount(aq);

        for (UnitCreditInstance uci : acc.getUnitCreditInstances()) {
            UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId());
            // Only include data that is not from some unlimited bundles and is currently available for use
            if (ucs.getUnitType().equalsIgnoreCase("byte") && (!ucs.getConfiguration().contains("DisplayBalance=false") && (!ucs.getConfiguration().contains("DisplayUnitsType=sec") && !ucs.getConfiguration().contains("DisplayUnitsType=minute")))
                        && Utils.getJavaDate(uci.getStartDate()).before(now)
                        && uci.getAvailableUnitsRemaining() > 0) {
                    bundle += uci.getAvailableUnitsRemaining();
                }
        }
        return bundle;
    }

    public static boolean containsUnlimitedBundle(long accountId) {
        boolean isUnlimitedBundle = false;
        Date now = new Date();

        Account acc = UserSpecificCachedDataHelper.getAccount(accountId, StAccountLookupVerbosity.ACCOUNT_UNITCREDITS_RESERVATIONS);

        for (UnitCreditInstance uci : acc.getUnitCreditInstances()) {
            UnitCreditSpecification ucs = NonUserSpecificCachedDataHelper.getUnitCreditSpecification(uci.getUnitCreditSpecificationId());
            // Only include data that is from some unlimited bundles and is currently available for use
            if ((ucs.getWrapperClass().equals("DustUnitCredit") || ucs.getWrapperClass().equals("CorporateAccessUnitCredit") || ucs.getWrapperClass().contains("Unlimited"))
                    && ucs.getUnitType().equalsIgnoreCase("byte")
                    && Utils.getJavaDate(uci.getStartDate()).before(now)) {
                isUnlimitedBundle = true;
                break;
            }
        }
        return isUnlimitedBundle;
    }

    public static int getTimeLeftBasedOnUnitsUsedFromBaseline(long bps, double unitsUsed, String unitsUsedType, double baseLine, String baseUnitsType, String timeUnit) {
        log.debug("pbs [{}], units used [{}], units used type [{}], baseline units [{}], baseline unit type [{}], time unit [{}]",
                new Object[]{bps, unitsUsed, unitsUsedType, baseLine, baseUnitsType, timeUnit});
        double usedUnitsInBits;
        switch (unitsUsedType) {
            case "byte":
                usedUnitsInBits = unitsUsed * 8;
                break;
            case "MB":
                usedUnitsInBits = unitsUsed * 1024 * 1024 * 8;
                break;
            case "GB":
                usedUnitsInBits = unitsUsed * 1024 * 1024 * 1024 * 8;
                break;
            default:
                usedUnitsInBits = unitsUsed;//Assuming its OCTECT
        }

        double baseUnitsInBits;
        switch (baseUnitsType) {
            case "byte":
                baseUnitsInBits = baseLine * 8;
                break;
            case "MB":
                baseUnitsInBits = baseLine * 1024 * 1024 * 8;
                break;
            case "GB":
                baseUnitsInBits = baseLine * 1024 * 1024 * 1024 * 8;
                break;
            default:
                baseUnitsInBits = baseLine;//Assuming its OCTECT
        }

        double bitsLeft = baseUnitsInBits - usedUnitsInBits;
        log.debug("baleline units in bits [{}] - units used in bits [{}] gives [{}]", new Object[]{baseUnitsInBits, usedUnitsInBits, bitsLeft});

        //e.g time = ( bits / bits/sec) :: 'bits' units will cancel each other and what is left is the 'sec' unit
        Double timeLeftIn;

        if (timeUnit.equalsIgnoreCase("second")) {
            // get seconds
            timeLeftIn = (bitsLeft / bps);
        } else if (timeUnit.equalsIgnoreCase("minute")) {
            // get minutes
            timeLeftIn = (bitsLeft / bps) / 60;
        } else {
            // get hours
            timeLeftIn = (bitsLeft / bps) / (60 * 60);
        }
        log.debug("Returning {} units {}", timeLeftIn, timeUnit
        );
        return timeLeftIn.intValue();
    }

    public static double getListTotalAsDoubleWithFilter(List list, String filterMethodName, String value, String filterTotalMethodName) throws Exception {
        double ret = 0;
        for (Object item : list) {
            Method m = item.getClass().getMethod(filterMethodName);
            Object val = m.invoke(item);
            String valAsString = "";
            if (val instanceof String) {
                valAsString = (String) val;
            }
            if (valAsString.equalsIgnoreCase(value)) {
                Method m2 = item.getClass().getMethod(filterTotalMethodName);
                Double val2 = (Double) m2.invoke(item);
                ret += val2;
            }
        }
        return ret;
    }

    public static String getUnitCreditNameByItemNumber(String itemNumber) {
        try {
            UnitCreditSpecification spec = NonUserSpecificCachedDataHelper.getUnitCreditSpecificationByItemNumber(itemNumber);
            return spec.getName();
        } catch (Exception e) {
            log.debug("Error in getUnitCreditNameByItemNumber [{}]", e.toString());
            return itemNumber + " - Not a unit credit";
        }
    }

    public static String getItemNameByItemNumber(String itemNumber) {
        // TODO
        return "";
    }

    @SuppressWarnings("unchecked")
    public static List orderUnitCreditListByNamePopularity(List list, String sectionName) throws Exception {
        if (list == null) {
            return null;
        }
        Comparator comparator = new UnitCreditPerSectionComparator(sectionName);
        Collections.sort(list, comparator);
        return list;
    }

    private static class UnitCreditPerSectionComparator implements Comparator {

        private final Set<String> sectionsOrder = BaseUtils.getPropertyAsSet("env.scp.unitcredit.persection.order");
        private String sectionName = "";
        private Method m = null;

        public UnitCreditPerSectionComparator(String sectionName) {
            this.sectionName = sectionName;
        }

        @Override
        public int compare(Object o1, Object o2) {
            // Ok so, o1 and o2 are both objects in a list. We need to get the result of calling the method "getName" on each object and compare them
            int ret = 1;
            if (m == null) {
                try {
                    m = o1.getClass().getMethod("getName");
                } catch (Exception ex) {
                    log.error("Error in UnitCreditPerSectionComparator", ex);
                }
            }
            try {

                String o1Data = (String) m.invoke(o1);
                String o2Data = (String) m.invoke(o2);

                int o1Position = 1000;
                int o2Position = 1000;

                for (String orderConfig : sectionsOrder) {
                    String[] bitsConfig = orderConfig.split("=");//3=5GB|anytime
                    String[] sectionBundleNameConfig = bitsConfig[1].split(Pattern.quote("|"));//5GB|anytime

                    if (o1Data.equals(sectionBundleNameConfig[0]) && sectionName.equals(sectionBundleNameConfig[1])) {
                        log.debug("UnitCreditPerSectionComparator1 TO MATCH CONFIG_BUN[{}], LIST_BUN[{}, CONFIG_SECT[{}], LIST_SECT[{}]", new Object[]{sectionBundleNameConfig[0], o1Data, sectionBundleNameConfig[1], sectionName});
                        o1Position = Integer.parseInt(bitsConfig[0]);
                        break;
                    }
                }

                for (String orderConfig : sectionsOrder) {
                    String[] bitsConfig = orderConfig.split("=");//3=5GB|anytime
                    String[] sectionBundleNameConfig = bitsConfig[1].split(Pattern.quote("|"));//5GB|anytime

                    if (o2Data.equals(sectionBundleNameConfig[0]) && sectionName.equals(sectionBundleNameConfig[1])) {
                        log.debug("UnitCreditPerSectionComparator2 TO MATCH CONFIG_BUN[{}], LIST_BUN[{}, CONFIG_SECT[{}], LIST_SECT[{}]", new Object[]{sectionBundleNameConfig[0], o2Data, sectionBundleNameConfig[1], sectionName});
                        o2Position = Integer.parseInt(bitsConfig[0]);
                        break;
                    }
                }

                ret = ((Integer) o1Position).compareTo((Integer) o2Position);
            } catch (Exception ex) {
                log.error("Some error occured:: ", ex);
            }
            return ret;
        }
    }

}
