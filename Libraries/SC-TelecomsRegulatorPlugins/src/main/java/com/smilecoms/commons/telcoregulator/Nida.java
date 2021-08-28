/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.telcoregulator;

//import com.smilecoms.commons.base.BaseUtils;
//import com.smilecoms.commons.telcoregulator.helpers.CryptoUtil;
import com.smilecoms.commons.telcoregulator.helpers.RegulatorSOAPHandler;
import com.smilecoms.commons.telcoregulator.nida.GatewayService;
import com.smilecoms.commons.telcoregulator.nida.IGatewayService;
/*import com.smilecoms.commons.telcoregulator.nida.core.RequestBaseCryptoInfoPayload;
import com.smilecoms.commons.telcoregulator.nida.core.RequestBaseRequestBodySectionBase;
import com.smilecoms.commons.telcoregulator.nida.core.RequestHeaderSectionBase;
import com.smilecoms.commons.telcoregulator.nida.transactions.GetBasicDemographicDataRequest;
import com.smilecoms.commons.telcoregulator.nida.transactions.GetBasicDemographicDataResponse;*/
//import com.smilecoms.commons.util.Utils;
import java.net.URL;
import java.util.Arrays;
import java.util.StringTokenizer;
//import java.util.Date;
//import javax.crypto.spec.IvParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.AddressingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class Nida {

    private static IGatewayService cachedNidaProxy = null;
    private static String proxyConfig = null;
    private static final Logger log = LoggerFactory.getLogger(Nida.class);
    private static final String ALGORITHM_AES = "AES";
    /**
     * Minimum number of bytes to raise the key material to before generating a
     * key.
     */
    private static final int MIN_KEY_BYTES = 256;//128;

    @SuppressWarnings("restriction")
    public static IGatewayService getNidaProxy() {
        //String proxyConfigNow = BaseUtils.getProperty("env.regulator.nida.config");
        String proxyConfigNow = "NidaWSURL=https://nacis01/CIG/GatewayService.svc?wsdl\n"
                + "OldNidaWSURL=https://nacis01/CIG/GatewayService.svc\n"
                + "SmileIrisCertsKeyStoreFileLocation=/var/smile/install/scripts/NIDA/client.jks\n"
                + "SmileIrisCertAliasName=smile_key\n"
                + "SmileIrisSigningPropertiesFileLocation=client_sign.properties\n"
                + "CIGCertsKeyStoreFileLocation=/var/smile/install/scripts/NIDA/CIGSecurity.p12\n"
                + "CIGMessagingPublicCertFileLocation=/var/smile/install/scripts/NIDA/CIGSecurity.cer\n"
                + "CigIrisCertAliasName=CIGSecurity's IRIS ID\n"
                + "KeyStorePassword=nlp774\n"
                + "TrustStorePassword=changeit\n"
                + "namespace=http://tempuri.org/\n"
                + "name=GatewayService\n"
                + "location=https://nacis01/CIG/GatewayService.svc";

        if (proxyConfig != null && !proxyConfigNow.equals(proxyConfig)) {
            cachedNidaProxy = null;
        }
        if (cachedNidaProxy != null) {
            return cachedNidaProxy;
        }
        
        /*
        
        
        
        public static class Addressing2004SoapHandler implements SOAPHandler<SOAPMessageContext>
            {
                private String mEndpoint;

                public Addressing2004SoapHandler(String endpoint) {
                    super();
                    mEndpoint = endpoint;
                }

                public Set<QName> getHeaders()
                {
                    Set<QName> retval = new HashSet<QName>();
                    retval.add(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "Action"));
                    retval.add(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "MessageID"));
                    retval.add(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "ReplyTo"));
                    retval.add(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "To"));
                    return retval;
                }

                public boolean handleMessage(SOAPMessageContext messageContext)
                {
                    Boolean outboundProperty = (Boolean)messageContext.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);

                    if (outboundProperty.booleanValue()) {
                        try {
                            messageContext.put(MessageContext.HTTP_REQUEST_HEADERS, Collections.singletonMap("Content-Type",Collections.singletonList("application/soap+xml; charset=utf-8")));
                            SOAPMessage message = messageContext.getMessage();
                            if(message.getSOAPPart().getEnvelope().getHeader() == null) {
                                    message.getSOAPPart().getEnvelope().addHeader();
                            }
                            SOAPHeader header = message.getSOAPPart().getEnvelope().getHeader();
                            SOAPHeaderElement actionElement = header.addHeaderElement(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "Action"));
                            actionElement.setMustUnderstand(true);
                            String action = (String)messageContext.get("javax.xml.ws.soap.http.soapaction.uri");
                            messageContext.put("javax.xml.ws.soap.http.soapaction.uri", null);
                            actionElement.addTextNode(action);
                            header.addHeaderElement(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "MessageID")).addTextNode("uuid:" + UUID.randomUUID().toString());
                            SOAPHeaderElement replyToElement = header.addHeaderElement(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "ReplyTo"));
                            SOAPElement addressElement = replyToElement.addChildElement(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "Address"));
                            addressElement.addTextNode("http://www.w3.org/2004/08/addressing/anonymous");
                            SOAPHeaderElement toElement = header.addHeaderElement(new QName("http://schemas.xmlsoap.org/ws/2004/08/addressing", "To"));
                            toElement.setMustUnderstand(true);
                            String endpoint = (String)messageContext.get("javax.xml.ws.service.endpoint.address");
                            toElement.addTextNode(endpoint);
                        }
                        catch(SOAPException ex) {
                        }
                    }

                    return true;
                }

                public boolean handleFault(SOAPMessageContext messageContext)
                {
                        return true;
                }
                public void close(MessageContext messageContext)
                {
                }
            }
        
        
        
        
        
            public class test {
            public static void main(String[] args) {
                MyService service = new MyService();  
                IMyService proxy = service.getMyService();  
                javax.xml.ws.Binding binding = ((BindingProvider)proxy).getBinding();
                List<Handler> handlerList = binding.getHandlerChain();
                handlerList.add(new Addressing2004SoapHandler(endpoint));
                binding.setHandlerChain(handlerList);
                Map<String, Object> requestContext = ((BindingProvider)proxy).getRequestContext();
                requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://192.168.0.5:1234/services/MyService");
                proxy.Ping("Foo");
            }
        }
        */
        log.warn("Creating Nida webservice proxy");
        proxyConfig = proxyConfigNow;
        String targetNamespace = getValueFromCRDelimitedAVPString(proxyConfig, "namespace");//the targetNamespace attribute in the WSDL's <wsdl:definitions> element.
        String serviceName = getValueFromCRDelimitedAVPString(proxyConfig, "name");//the name attribute in the WSDL's <wsdl:definitions> element.
        String endpointAddress = getValueFromCRDelimitedAVPString(proxyConfig, "location");//the location attribute in the WSDL's <soap:address> element

        log.debug("Going to create soap client to query nida, web service config: namespace[{}], name[{}], location[{}]", new Object[]{targetNamespace, serviceName, endpointAddress});

        URL url = Nida.class.getResource("/GatewayService.svc.wsdl");
        //GatewayService nidaWService = new GatewayService(url, new QName(targetNamespace, serviceName));
        GatewayService nidaWService = new GatewayService(url, new QName(targetNamespace, serviceName), new javax.xml.ws.soap.AddressingFeature(true, true));
        
        
        
        /*
        IMyService proxy = service.getMyService(new com.sun.xml.ws.developer.MemberSubmissionAddressingFeature(true, true) );
            //com.sun.xml.ws.developer.MemberSubmissionAddressingFeature(true, true) );
            <!-- https://mvnrepository.com/artifact/com.sun.xml.ws/jaxws-rt -->
            <dependency>
                <groupId>com.sun.xml.ws</groupId>
                <artifactId>jaxws-rt</artifactId>
                <version>2.2.7</version>
            </dependency>


        
        */
        
        
        //javax.xml.ws.soap.AddressingFeature featureA = new AddressingFeature();
        //cachedNidaProxy = nidaWService.getCigTxnEndpoint(featureA);

        log.warn("Adding binding configs on proxy");
        ((BindingProvider) cachedNidaProxy).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        ((BindingProvider) cachedNidaProxy).getRequestContext().put("javax.xml.ws.client.connectionTimeout", 5000);
        ((BindingProvider) cachedNidaProxy).getRequestContext().put("javax.xml.ws.client.receiveTimeout", 50000);
        ((BindingProvider) cachedNidaProxy).getBinding().setHandlerChain(Arrays.<Handler>asList(new RegulatorSOAPHandler()));
        log.warn("Returning proxy");
        return cachedNidaProxy;
    }

    public static String getValueFromCRDelimitedAVPString(String crDelimitedList, String key) {
        if (crDelimitedList == null) {
            return null;
        }
        StringTokenizer stValues = new StringTokenizer(crDelimitedList, "\r\n");
        while (stValues.hasMoreTokens()) {
            String val = stValues.nextToken();
            if (!val.isEmpty()) {
                String[] propVal = val.split("=", 2);
                if (propVal[0].trim().equalsIgnoreCase(key)) {
                    if (propVal.length == 1) {
                        return "";
                    } else {
                        return propVal[1].trim();
                    }
                }
            }
        }
        return null;
    }


    /*public static void queryBasicDemographic() throws Exception {
        IGatewayService gs = getNidaProxy();
        GetBasicDemographicDataRequest iRequest = new GetBasicDemographicDataRequest();
        RequestHeaderSectionBase head = new RequestHeaderSectionBase();
        head.setClientNameOrIP(getCIGProxy());
        head.setId(Utils.getUUID());
        head.setUserId("SMILE");
        head.setTimeStamp(Utils.getDateAsXMLGregorianCalendar(new Date()));
        iRequest.setHeader(head);

        RequestBaseRequestBodySectionBase body = new RequestBaseRequestBodySectionBase();
        String payload = "<Payload><NIN>19801123141080000115</NIN></Payload>";
        RequestBaseCryptoInfoPayload baseCryptoPayload = new RequestBaseCryptoInfoPayload();

        CryptoUtil.initKeyStoreProps();
        // somehow we need to distribute this key
        byte[] salt = CryptoUtil.getSalt();
        SecretKeySpec key = CryptoUtil.createKey("nlp774", salt);
        IvParameterSpec iv = CryptoUtil.createIV(16);

        String encrypted = CryptoUtil.getEncryptedMessage(payload, key, iv);
        CryptoUtil.encryptNidaAESCryptoParameters(CryptoUtil.getCipherForNidaAESCryptoParameters(), key, iv, baseCryptoPayload);//baseCryptoPayload :: Allows us to share the key
        body.setCryptoInfo(baseCryptoPayload);

        body.setPayload(encrypted);
        body.setSignature(CryptoUtil.getSignature(encrypted, "Smile"));//Digital signature generated with Stakeholder Private Key

        iRequest.setBody(body);

        GetBasicDemographicDataResponse queryBasicDemographicResponse = gs.queryBasicDemographic(iRequest);

    }*/
    public static String getCIGProxy() {
        //http_proxy=http://UGBUK-SQUID1.it.ug.smilecoms.com:8800
        String proxCon = System.getenv("CIG_PROXY");
        log.debug("Found value for env CIG_PROXY: {}", proxCon);
        if (proxCon != null) {
            String[] poxyConArray = proxCon.split(":");
            String host = poxyConArray[1].substring(2);
            int port = Integer.parseInt(poxyConArray[2]);
            log.debug("SQUID is on host[{}] and listening on port[{}]", host, port);
        }
        return "10.24.64.252";
    }

}
