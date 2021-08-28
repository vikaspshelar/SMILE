/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.ug.nira;


/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// package org.apache.cxf.ws.security.wss4j;

import java.io.ByteArrayOutputStream;
import static java.lang.Math.random;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import org.apache.commons.codec.binary.Base64;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class UsernameTokenInterceptor extends AbstractSoapInterceptor {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(NiraHelper.class);
    private String base64PasswordDigest;
    private String base64Nonce;
    private String created;
    
    /**
     * @param p
     */
    public UsernameTokenInterceptor() {
        super(Phase.PRE_PROTOCOL);
        // addAfter(PolicyBasedWSS4JOutInterceptor.class.getName());
    }
    


    public void handleMessage(SoapMessage message) throws Fault {

        boolean isReq = MessageUtils.isRequestor(message);
        boolean isOut = MessageUtils.isOutbound(message);
        if (isReq != isOut) {
            //outbound on server side and inbound on client side doesn't need
            //any username token stuff, assert policies and return
            //assertUsernameTokens(message, null);
            return;
        }
        if (isReq) {
            addUsernameToken(message);
        } 
    }

    private void addUsernameToken(SoapMessage message) throws Fault {
        
        try {
            generateUsernameTokenFieldValues();
        } catch (Exception ex) {
            throw new Fault(ex);
        }
        
        Map hmap = new HashMap(); 			
        hmap.put("fac","http://facade.server.pilatus.thirdparty.tidis.muehlbauer.de/");
        hmap.put("wsse", WSConstants.WSSE_NS);
        message.put("soap.env.ns.map", hmap);
        // message.put("disable.outputstream.optimization", true);
                
        // Create a new SOAP Header the
        Document doc = DOMUtils.createDocument();
        Element userNameTokenElement = doc.createElementNS(WSConstants.WSSE_NS, "wsse:UsernameToken");
        Element userNameElement = doc.createElementNS(WSConstants.WSSE_NS, "wsse:Username");
        userNameElement.setTextContent(NiraHelper.props.getProperty("NiraUsername"));
        Element passwordElement = doc.createElementNS(WSConstants.WSSE_NS, "wsse:Password");
        passwordElement.setAttribute("Type", "PasswordDigest");
        passwordElement.setTextContent(base64PasswordDigest);
        Element nonceElement = doc.createElementNS(WSConstants.WSSE_NS, "wsse:Nonce");
        nonceElement.setTextContent(base64Nonce);
        Element createdElement = doc.createElementNS(WSConstants.WSSE_NS, "wsse:Created");
        createdElement.setTextContent(created);
                
        userNameTokenElement.appendChild(userNameElement);
        userNameTokenElement.appendChild(passwordElement);
        userNameTokenElement.appendChild(nonceElement);
        userNameTokenElement.appendChild(createdElement);
        
        SoapHeader sh = new SoapHeader(new QName(WSConstants.WSSE_NS, "UsernameToken"), userNameTokenElement);
        message.getHeaders().add(sh);
             
    }
    
    private void generateUsernameTokenFieldValues() throws Exception {
        
        SecureRandom random = new SecureRandom();
        byte nonceBytes[] = new byte[16];
        random.nextBytes(nonceBytes);
        
        // Password_Digest = Base64(SHA-1(Nonce + Created + SHA1(Password)))
        MessageDigest sha1PasswordOnlyDigest = MessageDigest.getInstance("SHA-1");
        sha1PasswordOnlyDigest.update(NiraHelper.props.getProperty("NiraPassword").getBytes());
        byte [] sha1PasswordBytes = sha1PasswordOnlyDigest.digest();
                
        SimpleDateFormat sdfForCreated  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); //With last colon
        SimpleDateFormat sdfForPassword = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); //Without last colon
        
        Date oCreated  = new Date();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(nonceBytes);
        outputStream.write(sdfForPassword.format(oCreated).getBytes());
        outputStream.write(sha1PasswordBytes);
        
        MessageDigest sha1PasswordMessageDigest = MessageDigest.getInstance("SHA-1");
        sha1PasswordMessageDigest.update(outputStream.toByteArray());
        
        byte [] sha1PasswordDigest = sha1PasswordMessageDigest.digest();
        
        // Password_Digest = Base64(SHA-1(Nonce + Created + SHA1(Password)))
        
        base64PasswordDigest = Base64.encodeBase64String(sha1PasswordDigest);
        base64Nonce =  Base64.encodeBase64String(nonceBytes);
        created = sdfForCreated.format(oCreated);
        
    }
}
