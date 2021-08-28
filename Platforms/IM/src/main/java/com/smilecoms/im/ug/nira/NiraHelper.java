/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.ug.nira;



import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.BaseListener;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.ThirdPartyInterfaceFacadeNew;
import de.muehlbauer.tidis.thirdparty.pilatus.server.facade.ThirdPartyInterfaceNewWSService;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.apache.cxf.ws.security.wss4j.DefaultCryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mukosi
 */
public class NiraHelper {
    
    private static final Logger log = LoggerFactory.getLogger(NiraHelper.class);
    
    public static final String NIRA_CARD_STATUS_VALID =  "Valid";
    public static final String TRANSACTION_STATUS_OK = "Ok";
    
    public static Properties props = null;
   
    public static void initialise() {
        props = new Properties();
        try {            
            log.warn("Here are the Nira properties as contained in env.nira.config\n");
            log.warn(BaseUtils.getProperty("env.nira.config"));
            props.load(BaseUtils.getPropertyAsStream("env.nira.config"));
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.nira.config: ", ex);
        }
    }
    
    
    public static ThirdPartyInterfaceFacadeNew getNiraConnection(String urlProperty)  throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NIDA web service");
        
        try {
            
            
            if(urlProperty == null) {
                urlProperty = "NiraWSURL";
            }
                                                
            CXFBusFactory bf = new CXFBusFactory();
            // URL busFile = Client.class.getResource("/wssec.xml");
            Bus bus = bf.createBus();
            BusFactory.setDefaultBus(bus);
            
            //- Log4jLogger sss;
            
            // URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
            LoggingFeature loggingFeature = new LoggingFeature();
            loggingFeature.setPrettyLogging(true);
            // ClientProxy.getClient(npcdbServicePort).getEndpoint().getActiveFeatures().add(loggingFeature);
            
            URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
            
            ThirdPartyInterfaceNewWSService niraService = new ThirdPartyInterfaceNewWSService(url,  
                    new QName("http://facade.server.pilatus.thirdparty.tidis.muehlbauer.de/", "ThirdPartyInterfaceNewWSService"));
            // IGatewayService npcdbServicePort = (IGatewayService) nidaService.getCigTxnEndpoint(new AddressingFeature(true, true));
            ThirdPartyInterfaceFacadeNew npcdbServicePort = (ThirdPartyInterfaceFacadeNew) niraService.getThirdPartyInterfaceNewWSPort(loggingFeature);
            ((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, NiraHelper.props.getProperty(urlProperty));
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


            httpClientPolicy.setConnectionTimeout(Integer.valueOf(NiraHelper.props.getProperty("NiraConnectionTimeout", "60")) * 1000);
            httpClientPolicy.setReceiveTimeout(Integer.valueOf(NiraHelper.props.getProperty("NiraReceiveTimeout", "60")) * 1000);
            httpClientPolicy.setAllowChunking(false);
            httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE); 
            
            String proxyHost = NiraHelper.props.getProperty("ProxyHost", null); // System.getenv("HTTP_PROXY"); 
            int proxyPort = Integer.parseInt(NiraHelper.props.getProperty("ProxyPort", "8800")); // System.getenv("HTTP_PROXY"); 
            
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
            log.error("Error while trying to connect to NIRA [{}]", ex.getMessage());
            throw ex;
        }  
    }          
    
    
    public static ThirdPartyInterfaceFacadeNew getNiraConnectionWorked()  throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NIDA web service");
        
        try {                                
            CXFBusFactory bf = new CXFBusFactory();
            // URL busFile = Client.class.getResource("/wssec.xml");
            Bus bus = bf.createBus();
            BusFactory.setDefaultBus(bus);
            
            //Log4jLogger sss;
            
            // URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
            LoggingFeature loggingFeature = new LoggingFeature();
            loggingFeature.setPrettyLogging(true);
            // ClientProxy.getClient(npcdbServicePort).getEndpoint().getActiveFeatures().add(loggingFeature);
            
            URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
            
            /*JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(ThirdPartyInterfaceNewWSService.class); //the service SEI
            factory.setAddress(NiraHelper.props.getProperty("NiraWSURL")); */
 
            
            
            ThirdPartyInterfaceNewWSService niraService = new ThirdPartyInterfaceNewWSService(url,  
                    new QName("http://facade.server.pilatus.thirdparty.tidis.muehlbauer.de/", "ThirdPartyInterfaceNewWSService"));
            // IGatewayService npcdbServicePort = (IGatewayService) nidaService.getCigTxnEndpoint(new AddressingFeature(true, true));
            ThirdPartyInterfaceFacadeNew npcdbServicePort = (ThirdPartyInterfaceFacadeNew) niraService.getThirdPartyInterfaceNewWSPort(loggingFeature);
            ((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, NiraHelper.props.getProperty("NiraWSURL"));
            
            
            HashMap outProps = new HashMap();
            
            org.apache.cxf.endpoint.Client client = ClientProxy.getClient(npcdbServicePort);
                    
            // Client client = org.apache.cxf.frontend.ClientProxy.getClient(npcdbServicePort);
            Endpoint cxfEndpoint = client.getEndpoint();
            
            // LoggingFeature loggingFeature = new LoggingFeature();
            // loggingFeature.setPrettyLogging(true);
            // ClientProxy.getClient(npcdbServicePort).getEndpoint().getActiveFeatures().add(loggingFeature);
                     
           
            
            HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
            
            HTTPClientPolicy httpClientPolicy = httpConduit.getClient(); // new HTTPClientPolicy();
            httpClientPolicy.setContentType("application/soap+xml");
            httpClientPolicy.setAutoRedirect(true);
            
            httpClientPolicy.setConnectionTimeout(Integer.valueOf(NiraHelper.props.getProperty("NiraConnectionTimeout", "60")) * 1000);
            httpClientPolicy.setReceiveTimeout(Integer.valueOf(NiraHelper.props.getProperty("NiraReceiveTimeout", "60")) * 1000);
            httpClientPolicy.setAllowChunking(false);
            httpClientPolicy.setConnection(ConnectionType.KEEP_ALIVE); 
            
            httpConduit.setClient(httpClientPolicy); 
           
            /*
            Question:   can you also check the soap version you need?   SOAP 1.1 calls for 
            setting it to text/xml whild SOAP 1.2 uses application/soap+xml.     If the 
            services is requiring application/soap+xml then I kind of expect it would also 
            
            */
            
            
            // set the username and password
            Map ctx = ((BindingProvider) npcdbServicePort).getRequestContext();
            
            ctx.put("ws-security.username", "libuser");
            ctx.put("ws-security.password", "books");
            
            outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
            outProps.put(WSHandlerConstants.USER, NiraHelper.props.getProperty("NiraUsername"));
            outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
            // Automatically adds a Base64 encoded message nonce and a created timestamp
            //outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_CREATED, WSConstants.CREATED_LN); 
            //outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_NONCE, WSConstants.NONCE_LN); 
            // outProps.put(WSHandlerConstants., WSConstants.CREATED_LN + " " + WSConstants.NONCE_LN); 
            outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_CREATED, "true");
            outProps.put(WSHandlerConstants.ADD_USERNAMETOKEN_NONCE, "true");
            
            outProps.put(WSHandlerConstants.MUST_UNDERSTAND, "0");
            
            // You must implement the PasswordCallback class yourself. See Glen's page mentioned above for how 
            outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, "com.smilecoms.im.ug.nira.ClientPasswordCallback");

            WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
            cxfEndpoint.getOutInterceptors().add(wssOut);
            
            LoggingOutInterceptor loi = new LoggingOutInterceptor(); 
            loi.setPrettyLogging(true); 
            LoggingInInterceptor lii = new LoggingInInterceptor(); 
            lii.setPrettyLogging(true); 
            
            cxfEndpoint.getInInterceptors().add(lii);
            cxfEndpoint.getOutInterceptors().add(loi);
            cxfEndpoint.getOutInterceptors().add(wssOut);
            
            // org.apache.cxf.feature.LoggingFeature loggingFeature = new org.apache.cxf.feature.LoggingFeature();
            // loggingFeature.setPrettyLogging(true);
            // client.getEndpoint().getActiveFeatures().add(loggingFeature);
            // factory                
            
            return npcdbServicePort; 
            
            
        } catch (Exception ex) {
            log.error("Error while trying to connect to NIRA [{}]", ex.getMessage());
            throw ex;
        }  
    }          
    
    public static ThirdPartyInterfaceFacadeNew getNiraConnectionOLD()  throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NIDA web service");
        
        try {
            
            //Log4jLogger sss;
            
            // URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
            
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(ThirdPartyInterfaceFacadeNew.class); //the service SEI
            factory.setAddress(NiraHelper.props.getProperty("NiraWSURL"));
 
            
            
            // ThirdPartyInterfaceFacadeNew clientPort = (ThirdPartyInterfaceFacadeNew) factory.create();
            
            ThirdPartyInterfaceFacadeNew npcdbServicePort = (ThirdPartyInterfaceFacadeNew) factory.create();
            
            HashMap outProps = new HashMap();
            Client client = org.apache.cxf.frontend.ClientProxy.getClient(npcdbServicePort);
            Endpoint cxfEndpoint = client.getEndpoint();
            
            // LoggingFeature loggingFeature = new LoggingFeature();
            // loggingFeature.setPrettyLogging(true);
            // ClientProxy.getClient(npcdbServicePort).getEndpoint().getActiveFeatures().add(loggingFeature);
                     
           
            
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
            outProps.put(WSHandlerConstants.USER, NiraHelper.props.getProperty("NiraUsername"));
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
            // cxfEndpoint.getOutInterceptors().add(wssOut);
            
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
            log.error("Error while trying to connect to NIRA [{}]", ex.getMessage());
            throw ex;
        }  
    }  
    
    public static ThirdPartyInterfaceFacadeNew getNiraConnection_Works()  throws Exception { //Invokes web service at the NPC DB and bring back resutls
        log.debug("Going to create soap client to call NIDA web service");
        
        try {
            
            
            URL url = NiraHelper.class.getResource("/ThirdPartyInterfaceNewWS_Updated.wsdl");
            
            /*JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(ThirdPartyInterfaceNewWSService.class); //the service SEI
            factory.setAddress(NiraHelper.props.getProperty("NiraWSURL")); */
 
            ThirdPartyInterfaceNewWSService niraService = new ThirdPartyInterfaceNewWSService(url);
            ThirdPartyInterfaceFacadeNew npcdbServicePort = (ThirdPartyInterfaceFacadeNew) niraService.getThirdPartyInterfaceNewWSPort();
            ((BindingProvider) npcdbServicePort).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, NiraHelper.props.getProperty("NiraWSURL"));
            
            
            // ThirdPartyInterfaceFacadeNew clientPort = (ThirdPartyInterfaceFacadeNew) factory.create();
            
            HashMap outProps = new HashMap();
            Client client = org.apache.cxf.frontend.ClientProxy.getClient(npcdbServicePort);
            Endpoint cxfEndpoint = client.getEndpoint();
            
            // LoggingFeature loggingFeature = new LoggingFeature();
            // loggingFeature.setPrettyLogging(true);
            // ClientProxy.getClient(npcdbServicePort).getEndpoint().getActiveFeatures().add(loggingFeature);
                     
           
            
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
            outProps.put(WSHandlerConstants.USER, NiraHelper.props.getProperty("NiraUsername"));
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
            cxfEndpoint.getOutInterceptors().add(wssOut);
            
            LoggingOutInterceptor loi = new LoggingOutInterceptor(); 
            loi.setPrettyLogging(true); 
            LoggingInInterceptor lii = new LoggingInInterceptor(); 
            lii.setPrettyLogging(true); 
            
            cxfEndpoint.getOutInterceptors().add(loi); 
            cxfEndpoint.getInInterceptors().add(lii);
            cxfEndpoint.getInFaultInterceptors().add(lii);
            
            org.apache.cxf.feature.LoggingFeature loggingFeature = new org.apache.cxf.feature.LoggingFeature();
            loggingFeature.setPrettyLogging(true);
            client.getEndpoint().getActiveFeatures().add(loggingFeature);
                            
            
            return npcdbServicePort; 
            
            
        } catch (Exception ex) {
            log.error("Error while trying to connect to NIRA [{}]", ex.getMessage());
            throw ex;
        }  
    }   
    
   
}
