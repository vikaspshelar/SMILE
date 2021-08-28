/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.paymentgateway;

import com.smilecoms.commons.util.Oauth20Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class AccessBank {

    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String authenticationServerUrl;
    private final String password;
    private final String username;
    private final String grantType;
    private final String resourceServerUrl;
    private static final Logger log = LoggerFactory.getLogger(AccessBank.class);

    public AccessBank(String queryStatusURL, String authServerURL, String username, String password, String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = null;
        this.authenticationServerUrl = authServerURL;
        this.password = password;
        this.username = username;
        this.grantType = "client_credentials";
        this.resourceServerUrl = queryStatusURL;
    }
    
    public String getTransactionDetails(String orderId) throws Exception {
        String queryStatusURL = this.resourceServerUrl+ "?tx_ref=" + orderId;
        String ret ="";        
        String auth = "Bearer " + this.clientSecret;
        HttpURLConnection req = null;        
        try {
            URL url = new URL(queryStatusURL);
            req =(HttpURLConnection)url.openConnection();
            req.setRequestProperty("Authorization", auth);
            req.setRequestProperty("Content-Type", "application/json");
            req.setRequestMethod("GET");
            req.setUseCaches(false);
            req.setDoInput(true);
            req.setDoOutput(true);
            req.connect();
            
            BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }
            br.close();
            ret=sb.toString();
          
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
                ex.printStackTrace();
        } finally {
           if (req != null) {
              try {
                  req.disconnect();
              } catch (Exception ex) {
                 ex.printStackTrace();
              }
           }
        }      
        req.disconnect();
        return ret;
    }

    public String queryPaymentByTransactionRef(String ref) throws Exception {
        Map<String, String> oauthParams = new HashMap<>();
        oauthParams.put("order_id", ref);
        String ret = Oauth20Utils.getProtectedResource(clientId, clientSecret, scope, authenticationServerUrl, password, username, grantType, resourceServerUrl, oauthParams);
        return ret;
    }

}
