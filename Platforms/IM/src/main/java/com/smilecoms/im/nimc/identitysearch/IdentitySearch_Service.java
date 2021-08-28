package com.smilecoms.im.nimc.identitysearch;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 3.1.11
 * 2021-01-12T07:29:21.951+02:00
 * Generated source version: 3.1.11
 * 
 */
@WebServiceClient(name = "IdentitySearchService", 
                  wsdlLocation = "http://172.16.0.20:8080/nvs/IdentitySearch?wsdl",
                  targetNamespace = "http://IdentitySearch.nimc/") 
public class IdentitySearch_Service extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://IdentitySearch.nimc/", "IdentitySearch");
    public final static QName IdentitySearchPort = new QName("http://IdentitySearch.nimc/", "IdentitySearchPort");
    static {
        URL url = null;
        try {
            url = new URL("http://172.16.0.20:8080/nvs/IdentitySearch?wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(IdentitySearch_Service.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "http://172.16.0.20:8080/nvs/IdentitySearch?wsdl");
        }
        WSDL_LOCATION = url;
    }

    public IdentitySearch_Service(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public IdentitySearch_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public IdentitySearch_Service() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    public IdentitySearch_Service(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public IdentitySearch_Service(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public IdentitySearch_Service(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }    




    /**
     *
     * @return
     *     returns IdentitySearch
     */
    @WebEndpoint(name = "IdentitySearchPort")
    public IdentitySearch getIdentitySearchPort() {
        return super.getPort(IdentitySearchPort, IdentitySearch.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns IdentitySearch
     */
    @WebEndpoint(name = "IdentitySearchPort")
    public IdentitySearch getIdentitySearchPort(WebServiceFeature... features) {
        return super.getPort(IdentitySearchPort, IdentitySearch.class, features);
    }

}