/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sep.helpers;

/**
 *
 * @author abhilash
 */
import com.smilecoms.commons.base.BaseUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import com.sun.jersey.client.urlconnection.HttpURLConnectionFactory;


public class ConnectionFactory implements HttpURLConnectionFactory {

    Proxy proxy;

    private void initializeProxy() {
        String proxyHost = BaseUtils.getProperty("env.http.proxy.hostIP", "10.24.66.15");
        int proxyPort = BaseUtils.getIntProperty("env.http.proxy.port", 8800);
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    }

    public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        initializeProxy();
        return (HttpURLConnection) url.openConnection(proxy);
    }
}