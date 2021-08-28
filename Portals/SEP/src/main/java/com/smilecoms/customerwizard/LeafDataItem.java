/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.customerwizard;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author paul
 */
public class LeafDataItem {
    
    public enum DATA_ITEM_TYPE {
        TT_FIXED_FIELD,
        TT_USER_FIELD,
        TT_DROPDOWN_FIELD,
        TT_EXPLANATION,
        WEB_LINK,
        EXPLANATION
    }
    
    private DATA_ITEM_TYPE myType;
    private String attribute;
    private String value;
    
    public LeafDataItem(DATA_ITEM_TYPE type, String attribute, String value) {
        myType = type;
        this.attribute = attribute;
        this.value = value;
    }
    
    public String getAttribute() {
        return attribute;
    }
    
    public String getValue() {
        return value;
    }
    
    public List<String> getValueAsList() {
        String[] bits = value.split(Pattern.quote(","));
        List<String> toReturn = new ArrayList<String>();
        for (String bit : bits) {
            toReturn.add(bit.trim());
        }
        return toReturn;
    }
    
    public String getType() {
        return myType.toString();
    }
    
}
