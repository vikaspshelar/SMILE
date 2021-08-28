/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.tt;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class TTBasicHttpAuthenticationHandlerImpl implements AuthenticationHandler {

    private final String username;
    private final String password;
    private static final String BASIC = "Basic";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Logger log = LoggerFactory.getLogger(TTBasicHttpAuthenticationHandlerImpl.class);

    public TTBasicHttpAuthenticationHandlerImpl(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void configure(com.atlassian.httpclient.api.Request rqst) {
        rqst.setHeader(AUTHORIZATION_HEADER, getBasicAuthorizationHeader());
    }

    private String getBasicAuthorizationHeader() {
        return BASIC + " " + encodeCredentials();
    }

    private String encodeCredentials() {
        String cred = username + ":" + password;
        String encodedValue;
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes());
        encodedValue = new String(encodedBytes);
        log.debug("encodedBytes " + new String(encodedBytes));

        byte[] decodedBytes = Base64.decodeBase64(encodedBytes);
        log.debug("decodedBytes " + new String(decodedBytes));

        return encodedValue;
    }

}
