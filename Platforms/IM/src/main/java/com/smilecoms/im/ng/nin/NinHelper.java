/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.ng.nin;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.im.ug.nira.UsernameTokenInterceptor;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import com.smilecoms.im.nimc.identitysearch.IdentitySearch_Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import com.smilecoms.im.nimc.identitysearch.IdentitySearch;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

/**
 *
 * @author bhaskarhg
 */
public class NinHelper {

    private static final Logger log = LoggerFactory.getLogger(NinHelper.class);

    public static final String NIRA_CARD_STATUS_VALID = "Valid";
    public static final String TRANSACTION_STATUS_OK = "Ok";

    public static Properties props = null;

    public static String NinWSURL = "http://172.16.0.20:8080/nvs/IdentitySearch?wsdl";

    public static void initialise() {
        props = new Properties();
        try {
            log.warn("Here are the Nin properties as contained in env.nin.config\n");
            log.warn(BaseUtils.getProperty("env.nin.config"));
            props.load(BaseUtils.getPropertyAsStream("env.nin.config"));
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.nin.config: ", ex);
        }
    }

    public static IdentitySearch getNinConnection(String urlProperty) throws Exception {
        log.debug("Going to create soap client to call NIN web service");
        try {

            if (urlProperty == null) {
                urlProperty = "NinWSURL";
            }

//            CXFBusFactory bf = new CXFBusFactory();
//            // URL busFile = Client.class.getResource("/wssec.xml");
//            Bus bus = bf.createBus();
//            BusFactory.setDefaultBus(bus);
//
//            //- Log4jLogger sss;
//            // URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
//            LoggingFeature loggingFeature = new LoggingFeature();
//            loggingFeature.setPrettyLogging(true);
            // ClientProxy.getClient(npcdbServicePort).getEndpoint().getActiveFeatures().add(loggingFeature);

            //URL url = NinHelper.class.getResource("/IdentitySearch.wsdl");
            URL url = new URL(NinWSURL);

            /*JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(ThirdPartyInterfaceNewWSService.class); //the service SEI
            factory.setAddress(NiraHelper.props.getProperty("NiraWSURL")); */
            IdentitySearch_Service ninService = new IdentitySearch_Service(url, new QName("http://IdentitySearch.nimc/", "IdentitySearch"));

            IdentitySearch npcdbServicePort = (IdentitySearch) ninService.getIdentitySearchPort();
            ((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, NinWSURL);
            //((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, NinHelper.props.getProperty(urlProperty));
            ((BindingProvider) npcdbServicePort).getRequestContext().put("org.apache.cxf.http.no_io_exceptions", "true");

            HashMap outProps = new HashMap();

            org.apache.cxf.endpoint.Client client = ClientProxy.getClient(npcdbServicePort);

            // Client client = org.apache.cxf.frontend.ClientProxy.getClient(npcdbServicePort);
            Endpoint cxfEndpoint = client.getEndpoint();

            HTTPConduit httpConduit = (HTTPConduit) client.getConduit();

            HTTPClientPolicy httpClientPolicy = httpConduit.getClient(); // new HTTPClientPolicy();
            httpClientPolicy.setContentType("application/soap+xml");
            httpClientPolicy.setAutoRedirect(true);

            TLSClientParameters tlsCP = new TLSClientParameters();
            // other TLS/SSL configuration like setting up TrustManagers
            tlsCP.setDisableCNCheck(true);
            httpConduit.setTlsClientParameters(tlsCP);

            //httpClientPolicy.setConnectionTimeout(Integer.valueOf("60") * 1000);
            //httpClientPolicy.setReceiveTimeout(Integer.valueOf("60") * 1000);
            httpClientPolicy.setConnectionTimeout(Integer.valueOf(NinHelper.props.getProperty("NinConnectionTimeout", "60")) * 1000);
            httpClientPolicy.setReceiveTimeout(Integer.valueOf(NinHelper.props.getProperty("NinReceiveTimeout", "60")) * 1000);
            httpClientPolicy.setAllowChunking(false);
            httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);

            //String proxyHost = "10.28.66.15"; // System.getenv("HTTP_PROXY"); 
            //int proxyPort = 8800; // System.getenv("HTTP_PROXY"); 
            String proxyHost = NinHelper.props.getProperty("ProxyHost", null); // System.getenv("HTTP_PROXY"); 
            int proxyPort = Integer.parseInt(NinHelper.props.getProperty("ProxyPort", "8800")); // System.getenv("HTTP_PROXY"); 

            httpClientPolicy.setProxyServer(proxyHost);
            httpClientPolicy.setProxyServerPort(proxyPort);
            httpClientPolicy.setProxyServerType(ProxyServerType.HTTP);

            httpConduit.setClient(httpClientPolicy);

            LoggingOutInterceptor loi = new LoggingOutInterceptor();
            loi.setPrettyLogging(true);
            LoggingInInterceptor lii = new LoggingInInterceptor();
            lii.setPrettyLogging(true);

            cxfEndpoint.getInInterceptors().add(lii);
            cxfEndpoint.getOutInterceptors().add(loi);
            cxfEndpoint.getOutInterceptors().add(new UsernameTokenInterceptor());

            return npcdbServicePort;

        } catch (Exception ex) {
            log.error("Error while trying to connect to NIN [{}]", ex.getMessage());
            throw ex;
        }
    }
    
    public static IdentitySearch getNinaConnectionOLD() throws Exception { //Invokes web service at the NPC DB and bring back resutls
       // log.debug("Going to create soap client to call NIDA web service");

        try {

            //Log4jLogger sss;
            // URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
            
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(IdentitySearch.class); //the service SEI
            factory.setAddress(NinWSURL);
            
            // ThirdPartyInterfaceFacadeNew clientPort = (ThirdPartyInterfaceFacadeNew) factory.create();
            IdentitySearch npcdbServicePort = (IdentitySearch) factory.create();
            
            HashMap outProps = new HashMap();
            Client client = org.apache.cxf.frontend.ClientProxy.getClient(npcdbServicePort);
            Endpoint cxfEndpoint = client.getEndpoint();
            
            /*LoggingFeature loggingFeature = new LoggingFeature();
            loggingFeature.setPrettyLogging(true);
            ClientProxy.getClient(npcdbServicePort).getEndpoint().getActiveFeatures().add(loggingFeature);*/
            HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
            
            HTTPClientPolicy httpClientPolicy = httpConduit.getClient(); // new HTTPClientPolicy();
            httpClientPolicy.setContentType("application/soap+xml");
            httpClientPolicy.setAutoRedirect(true);
            
            httpClientPolicy.setConnectionTimeout(60000);
            httpClientPolicy.setReceiveTimeout(60000);
            httpClientPolicy.setAllowChunking(false);
            // httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
            
            httpConduit.setClient(httpClientPolicy);
            
            /*
            Question:   can you also check the soap version you need?   SOAP 1.1 calls for
            setting it to text/xml whild SOAP 1.2 uses application/soap+xml.     If the
            services is requiring application/soap+xml then I kind of expect it would also

             */
            
            outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
            
            outProps.put(WSHandlerConstants.USER, "usertest1a");
            
            outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
            
            // Automatically adds a Base64 encoded message nonce and a created timestamp
            //outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_CREATED, WSConstants.CREATED_LN);
            //outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_NONCE, WSConstants.NONCE_LN);
            // outProps.put(WSHandlerConstants., WSConstants.CREATED_LN + " " + WSConstants.NONCE_LN);
            
            outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_CREATED, "true");
            
            outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_NONCE, "true");
            
            outProps.put(WSHandlerConstants.MUST_UNDERSTAND, "false");
            
            // You must implement the PasswordCallback class yourself. See Glen's page mentioned above for how
            outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, "com.smilecoms.im.ug.nira.ClientPasswordCallback");
            
            WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
            //cxfEndpoint.getOutInterceptors().add(wssOut);
         
            LoggingOutInterceptor loi = new LoggingOutInterceptor();
            loi.setPrettyLogging(true);
            LoggingInInterceptor lii = new LoggingInInterceptor();
            lii.setPrettyLogging(true);

            factory.getInInterceptors().add(lii);
            factory.getOutInterceptors().add(loi);
            factory.getOutInterceptors().add(wssOut);

            // org.apache.cxf.feature.LoggingFeature loggingFeature = new org.apache.cxf.feature.LoggingFeature();
            // loggingFeature.setPrettyLogging(true);
            // client.getEndpoint().getActiveFeatures().add(loggingFeature);
            // factory
            return npcdbServicePort;

        } catch (Exception ex) {
            //log.error("Error while trying to connect to NIRA [{}]", ex.getMessage());
            throw ex;
        }
    }

    public static String getPasswordHash(String pwd) {

        try {
            MessageDigest m = MessageDigest.getInstance("sha-256");
            m.update(pwd.getBytes(), 0, pwd.length());

            return new BigInteger(1, m.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
