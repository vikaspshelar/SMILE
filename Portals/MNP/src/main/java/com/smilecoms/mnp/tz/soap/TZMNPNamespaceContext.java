/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mnp.tz.soap;

import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 *
 * @author mukosi
 */
public class TZMNPNamespaceContext implements NamespaceContext {

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) throw new NullPointerException("Null prefix");
        else if ("sp".equals(prefix)) return "http://np.systor.st/sp";
        else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
        return XMLConstants.NULL_NS_URI;
    }

    // This method isn't necessary for XPath processing.
    @Override
    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    // This method isn't necessary for XPath processing either.
    @Override
    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
    
}