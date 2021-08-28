/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.sca;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.slf4j.Logger;

/**
 *
 * @author paul
 */
public class AVPListAccessor {
    
    private final Map<String, String> avpMap;
    private Map<String, Map<String, String>> subAvpMaps;
    
    public AVPListAccessor(List<AVP> avpList) {
        avpMap = new HashMap<>();
        for (AVP avp : avpList) {
            avpMap.put(avp.getAttribute(), avp.getValue());
        }
    }
    
    public String getValueAsString(String attribute) {
        return avpMap.get(attribute);
    }
    
    public int getValueAsInt(String attribute) {
        String val = getValueAsString(attribute);
	if (val == null || val.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(val);
    }
    
    public String getSubValueAsString(String attribute, String subAttribute) {
        Map<String, String> subMap = getSubAvpMap(attribute);
        if (subMap == null) {
            return null;            
        }
        return subMap.get(subAttribute);
    }
    
    private Map<String, String> getSubAvpMap(String attribute) {
        if (subAvpMaps == null) {
            subAvpMaps = new HashMap<>();
        }
        Map<String, String> subMap = subAvpMaps.get(attribute);
        if (subMap == null) {
            subMap = new HashMap<>();
            String value = getValueAsString(attribute);
            if (value == null) {
                return null;
            }
            // Split value on crlf and then = to get all the avps
            StringTokenizer stValues = new StringTokenizer(value, "\r\n");
            while (stValues.hasMoreTokens()) {
                String line = stValues.nextToken();
                if (!line.startsWith("#") && line.contains("=")) {
                    String[] subAVP = line.split("=");
                    subMap.put(subAVP[0], subAVP[1]);
                }
            }
            subAvpMaps.put(attribute, subMap);
        }
        return subMap;
    }
    
    public int getSubValueAsInt(String attribute, String subAttribute) {
        String val = getSubValueAsString(attribute, subAttribute);
	if (val == null || val.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(getSubValueAsString(attribute, subAttribute));
    }
    
    public void logValues(Logger log) {
        if (log.isDebugEnabled()) {
            for (String attribute : avpMap.keySet()) {
                log.debug("[{}]=[{}]", attribute, avpMap.get(attribute));
            }
        }
    }
    private Map<String, String> defaultSubMap = null;
    
    public void setDefaultAttribute(String defaultAttribute) {
        defaultSubMap = getSubAvpMap(defaultAttribute);
    }
    
    public int getSubValueAsInt(String subAttribute) {
        String val = getSubValueAsString(subAttribute);
	if (val == null || val.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(getSubValueAsString(subAttribute));
    }
    
    public String getSubValueAsString(String subAttribute) {
        if (defaultSubMap == null) {
            return null;
        }
        return defaultSubMap.get(subAttribute);
    }
}
