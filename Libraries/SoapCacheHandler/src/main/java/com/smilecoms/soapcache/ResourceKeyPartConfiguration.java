/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.soapcache;

import javax.xml.xpath.XPathExpression;

/**
 *
 * @author paul
 */
public class ResourceKeyPartConfiguration {
    public static final int TYPE_STRING = 1;
    public static final int TYPE_XPATH = 2;
    private int type;
    private String strPart = null;
    private XPathExpression xpathPart = null;


    public int getType() {
        return type;
    }
    public String getStrPart() {
        return strPart;
    }

    public void setStrPart(String strPart) {
        type = TYPE_STRING;
        this.strPart = strPart;
    }

    public XPathExpression getXpathPart() {
        return xpathPart;
    }

    public void setXpathPart(XPathExpression xpathPart) {
        type = TYPE_XPATH;
        this.xpathPart = xpathPart;
    }



}
