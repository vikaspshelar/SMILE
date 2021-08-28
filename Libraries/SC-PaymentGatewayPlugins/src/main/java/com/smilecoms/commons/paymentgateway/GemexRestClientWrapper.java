/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import javax.ws.rs.core.EntityTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class GemexRestClientWrapper {

    private Client client = null;
    private final String username;
    private final String password;
    private final String url;
    private static final Logger log = LoggerFactory.getLogger(GemexRestClientWrapper.class);

    public GemexRestClientWrapper(String url, String username, String password, int connectTimeout, int readTimeout) {
        this.url = url;
        this.username = username;
        this.password = password;
        try {
            URLConnectionClientHandler cc = getProxy();
            client = new Client(cc);
            client.setConnectTimeout(connectTimeout);
            client.setReadTimeout(readTimeout);
        } catch (Exception ex) {
            log.warn("Error occured on GemexRestClientWrapper constructor {}", ex.toString());
        }

        //client.addFilter(new HTTPBasicAuthFilter(username, password)); Username and password is passed as part of the resource URL as instructed by Parth
        //https://smileug.gemexsystems.com/GemexService/GemexService.svc/help
    }

    public String getTransactionDetails(String orderId) throws Exception {
        //https://smileug.gemexsystems.com/GemexService/GemexService.svc/GetOrderDetails/ORDERID/USERNAME/PASSWORD
        String targetResource = url + orderId + "/" + username + "/" + password;
        WebResource resource = client.resource(targetResource);
        log.debug("Calling Gemex REST resource: {}", targetResource);
        ClientResponse response = resource.get(ClientResponse.class);
        log.debug("Finished calling Gemex REST resource");
        EntityTag e = response.getEntityTag();
        String entity = response.getEntity(String.class);
        log.debug("Response is: {}", entity);
        return entity;
    }

    private static URLConnectionClientHandler getProxy() {
        log.debug("In getProxy.");
        URLConnectionClientHandler conHandler = new URLConnectionClientHandler(new HttpURLConnectionFactory() {
            HttpURLConnection con = null;

            @Override
            public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
                //http_proxy=http://UGBUK-SQUID1.it.ug.smilecoms.com:8800
                String proxCon = System.getenv("HTTP_PROXY");
                log.debug("Found value for env HTTP_PROXY: {}", proxCon);
                if (proxCon != null) {
                    String[] poxyConArray = proxCon.split(":");
                    String host = poxyConArray[1].substring(2);
                    int port = Integer.parseInt(poxyConArray[2]);
                    log.debug("SQUID is on host[{}] and listening on port[{}]", host, port);

                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
                    con = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    con = (HttpURLConnection) url.openConnection();
                }
                return con;
            }
        });
        log.debug("Exiting getProxy.");
        return conHandler;
    }

}
