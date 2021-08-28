/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.bpel;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.sca.EntityAuthorisationException;
import com.smilecoms.commons.sca.SCAAuthenticatorImpl;
import com.smilecoms.commons.util.Stopwatch;
import com.smilecoms.commons.util.Utils;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author paul
 */
public class BPELUtils {
    //Logging

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BPELUtils.class);

    public static long getSecondsSinceEpoch() {
        log.debug("In getSecondsSinceEpoch");
        long ret = System.currentTimeMillis() / 1000;
        log.debug("Finished getSecondsSinceEpoch. Returning [{}]", ret);
        return ret;
    }

    public static long getSecondsBetweenNowAndDate(String dstr) {
        // Date example: 2013-07-18T12:33:20.000+02:00
        log.debug("In getSecondsBetweenNowAndDate for date [{}]", dstr);
        Date d;
        try {
            d = sdfGregorianCal.parse(dstr);
        } catch (Exception e) {
            log.warn("Error parsing data: [{}]", e.toString());
            return Long.MAX_VALUE;
        }
        log.debug("Parsed date is. [{}]", d.toString());
        long now = System.currentTimeMillis() / 1000;
        Calendar c = Calendar.getInstance();

        long then = d.getTime() / 1000;
        long ret = now - then;
        log.debug("Finished getSecondsBetweenNowAndDate. Returning [{}]", ret);
        return ret;
    }

    public static String getPhoneNumberFromSIPURI(String uri) {
        return Utils.getPhoneNumberFromSIPURI(uri);
    }

    public static int getPhoneNumberLengthFromSIPURI(String uri) {
        return Utils.getPhoneNumberFromSIPURI(uri).length();
    }

    public static String getPublicIdentityForPhoneNumber(String phoneNumber) {
        return Utils.getPublicIdentityForPhoneNumber(phoneNumber);
    }

    public static String getAVPValueAsString(Node allAvpsNode, String avpName) {
        String ret = null;
        log.debug("getAVPValueAsString: AVPs are [{}] AVP to search for is [{}]", new Object[]{allAvpsNode.getTextContent(), avpName});
        NodeList allAVPs = allAvpsNode.getChildNodes();
        int avpCount = allAVPs.getLength();
        log.debug("There are [{}] avps in this node list", avpCount);
        for (int i = 0; i < avpCount; i++) {
            Node avpNode = allAVPs.item(i);
            AVP avp = getAVPFromNode(avpNode);
            if (avp.isPopulated() && avp.attribute.equalsIgnoreCase(avpName)) {
                ret = avp.value;
                break;
            }
        }
        return ret;
    }

    public static String getAVPValueAsStringEmptyIfMissing(Node allAvpsNode, String avpName) {
        String ret = getAVPValueAsString(allAvpsNode, avpName);
        if (ret == null) {
            ret = "";
        }
        return ret;
    }

    public static String removeTerminatingCommaFromString(String string) {
        if (string.endsWith(",")) {
            string = string.substring(0, string.length() - 1);
        }
        return string;

    }

    public static int getStringSplitArraySize(String avpString, String token) {
        String[] splitAvpString = avpString.split(token);
        return splitAvpString.length;
    }

    public static String getStringSplitArrayAtIndex(String avpString, String token, Double index) {
        String[] splitAvpString = avpString.split(token);
        return splitAvpString[index.intValue()];
    }

    public static Number getAVPValueAsNumber(Node allAvpsNode, String avpName) {
        String valAsString = getAVPValueAsString(allAvpsNode, avpName);
        if (valAsString == null) {
            return null;
        } else {
            return new BigDecimal(valAsString);
        }
    }

    private static AVP getAVPFromNode(Node avpNode) {
            log.debug("In getAVPFromNode for  [{}]", avpNode.getLocalName());

        AVP avp = new AVP();
        //Loop though  all elements of the AVP and find the attribute and value
        NodeList avpElements = avpNode.getChildNodes();
        int avpElementCount = avpElements.getLength();
            log.debug("There are [{}] elements in this AVP", avpElementCount);
        for (int i = 0; i < avpElementCount; i++) {
            Node element = avpElements.item(i);
            String elementName = element.getLocalName();
            if (elementName == null) {
                continue;
            }
            String elementValue = element.getTextContent();
            if (elementValue == null) {
                continue;
            }

                log.debug("This element has Name [{}] and Value [{}]", new Object[]{elementName, elementValue});
            if (elementName.equals("Attribute")) {
                avp.attribute = elementValue;
            }
            if (elementName.equals("Value")) {
                avp.value = elementValue;
            }
            if (avp.isPopulated()) {
                    log.debug("AVP has been populated - breaking from loop");
                break;
            }
        }

            log.debug("AVP has attribute name [{}] and value [{}]", new Object[]{avp.attribute, avp.value});

        return avp;
    }

    public static String hexStringToEncryptedHexString(String plaintext) {
        return Codec.hexStringToEncryptedHexString(plaintext);
    }

    public static String getProperty(String property) {
        return BaseUtils.getProperty(property);
    }

    public static String getProperty(String property, String defaultValue) {
        return BaseUtils.getProperty(property, defaultValue);
    }

    public static boolean doesPropertySetContain(String property, String value) {
        Set<String> vals = null;
        try {
            vals = BaseUtils.getPropertyAsSet(property);
        } catch (Exception e) {
        }
        if (vals == null) {
            return false;
        }
        return vals.contains(value);
    }

    public static String getSubProperty(String property, String subProperty) {
        return BaseUtils.getSubProperty(property, subProperty);
    }

    private static final SimpleDateFormat sdfLong = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final SimpleDateFormat sdfGregorianCal = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss"); //2012-09-12T18:40:36.000+02:00

    public static String formatDateLong(String d) {
        try {
            return sdfLong.format(sdfGregorianCal.parse(d));
        } catch (Exception e) {
            log.warn("Error parsing date: {}", e.toString());
            return "Unknown";
        }

    }

    public static String sendTrapToOpsManagement(String msg) {
        try {
            BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "SCA", msg);
        } catch (Exception e) {
            log.warn("Error in sendTrapToOpsManagement: {}", e.toString());
        }
        return "true";
    }
    private static final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();

    static {
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
    }

    public static String convertCentsToCurrencyLong(Double cents) {
        DecimalFormat CurrencyLong = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.longformat"), formatSymbols);
        double majorUnit = cents / 100.0;
        return CurrencyLong.format(majorUnit);
    }

    public static String obviscate(String data, String type) {
        if (!BaseUtils.getBooleanPropertyFailFast("global.sca.check.permissions", true)) {
            log.warn("checkPermissions is turned off. Returning no obviscation");
            return data;
        }
        return SCAAuthenticatorImpl.obviscate(data, type);
    }

    public static String checkPermissions(Node requestContext, String entityType, Double entityKey) throws EntityAuthorisationException {
        return checkPermissions(requestContext, entityType, String.valueOf(entityKey.longValue()));
    }

    public static String checkPermissions(Node requestContext, String entityType, String entityKey) throws EntityAuthorisationException {

        if (!BaseUtils.getBooleanPropertyFailFast("global.sca.check.permissions", true)) {
            log.warn("checkPermissions is turned off. Returning ALL_OK");
            return SCAAuthenticatorImpl.ALL_OK;
        }

        if (log.isDebugEnabled()) {
            Stopwatch.start();
        }
        NodeList contextNodes = requestContext.getChildNodes();
        Set<String> callersRoles = new HashSet();
        String methodName = null;
        String originatingIdentity = null;
        int contextCount = contextNodes.getLength();
        for (int i = 0; i < contextCount; i++) {
            Node element = contextNodes.item(i);
            String elementName = element.getLocalName();
            if (elementName == null) {
                continue;
            }
            switch (elementName) {
                case "Roles":
                    callersRoles.add(element.getTextContent());
                        log.debug("Caller has role [{}]", element.getTextContent());
                    break;
                case "Method":
                    methodName = element.getTextContent();
                        log.debug("Method name is [{}]", methodName);
                    break;
                case "OriginatingIdentity":
                    originatingIdentity = element.getTextContent();
                        log.debug("Originating identity is [{}]", originatingIdentity);
                    break;
            }
        }

        String res = SCAAuthenticatorImpl.checkEntityAccess(originatingIdentity, callersRoles, methodName, entityType, entityKey);
        if (log.isDebugEnabled()) {
            Stopwatch.stop();
            log.debug("Permission check result is [{}] and took [{}]", new Object[]{res, Stopwatch.millisString()});
        }
        return res;

    }

    public static int binaryAnd(Double a, Double b) {
        int ret = a.intValue() & b.intValue();
        return ret;
    }

    public static String changeNamespace(String src, String fromNameSpace, String toNameSpace) throws Exception {
        String ret = src.replaceFirst(fromNameSpace, toNameSpace);
        if (fromNameSpace.equals("http://xml.smilecoms.com/schema/SCA")) {
            ret = ret.replaceFirst("<SCAContext>", "<PlatformContext>");
            ret = ret.replaceFirst("</SCAContext>", "</PlatformContext>");
            ret = ret.replaceAll("<Roles>.*</Roles>", "");
            ret = ret.replaceFirst("<Method>.*</Method>", "");
        } else {
            ret = ret.replaceFirst("<PlatformContext>", "<SCAContext>");
            ret = ret.replaceFirst("</PlatformContext>", "</SCAContext>");
        }
        return ret;
    }
    
    
    public static String getPropertyFromConfig(String crConfig, String propName) {

        Map<String, String> config = new HashMap<>();
            StringTokenizer stValues = new StringTokenizer(crConfig, "\r\n");
            while (stValues.hasMoreTokens()) {
                String row = stValues.nextToken();
                if (!row.isEmpty()) {
                    String[] bits = row.split("=");
                    String name = bits[0].trim();
                    String value;
                    if (bits.length == 1) {
                        value = "";
                    } else {
                        value = bits[1].trim();
                    }
                    config.put(name, value);
                }
            }
        return config.get(propName);
    }
  
}

class AVP {
    public boolean isPopulated() {
        return (attribute != null && value != null);
    }
    public String attribute;
    public String value;
}
