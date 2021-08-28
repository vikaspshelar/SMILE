/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms.helpers;

import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.message.SIPMessage;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sip.header.Header;
import javax.sip.message.Request;
import org.slf4j.*;

/**
 *
 * @author Jason
 */
public class SipUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SipUtils.class);
    
    private static final Pattern sipURIPattern = Pattern.compile("<(.*)>");
    private static final Pattern iscMarkPattern = Pattern.compile(".*<(sip:iscmark.*)>.*");
    private static final Pattern scscfAddressPatttern = Pattern.compile("^Route:.*<(.*)>");
    
    public static String getIMSPublicIdentity(Header header) 
    {
        String headerString = header.toString();
        String ret = null;
        
        Matcher m = sipURIPattern.matcher("");
        m.reset(headerString);
        
        if (m.find()) {
            ret = m.group(1);
        }
        
        return ret;
    }
    
    public static String getSIPContentAsString(Request request) {
        return new String(request.getRawContent());
    }
    
    public static String getISCMarkFromRoute(Request request) 
    {
        String ret = null;
        Matcher m = iscMarkPattern.matcher("");
                
        SIPMessage sipMessage = (SIPMessage)request;
        RouteList routeList = sipMessage.getRouteHeaders();
        
        if (routeList == null) {
            return null;
        }
        
        List<Route> routes = routeList.getHeaderList();
        
        for (Route route : routes) {
            m.reset(route.getHeaderValue());
            if (m.find()) {
                ret = m.group(1);
            }
        }
        return ret;
    }
    
    public static String getSCSCFFromRoute(Request request) 
    {
        String ret = null;
        
        String routeHeaderString = request.getHeader(SIPHeader.ROUTE).toString();
        
        Matcher m = scscfAddressPatttern.matcher("");
        m.reset(routeHeaderString);
        
        if (m.find()) {
            ret = m.group(1);
        }
        
        return ret;
    }
   
    
}
