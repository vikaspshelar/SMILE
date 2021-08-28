/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Contact;
import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Contact.UnknownParam;
import gov.nist.javax.sip.header.ims.PathList;
import java.io.Serializable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaybeepee
 */
public class ImpuContact implements Serializable{
    private static final Logger log = LoggerFactory.getLogger(ImpuContact.class);
    private String uri;
    public enum SmsFormat {binary3gpp, sip};
    private SmsFormat smsType;
    private PathList pathList;

    public ImpuContact(String uri, SmsFormat smsType) {
        this.uri = uri;
        this.smsType = smsType;
    }

    public ImpuContact(String uri) {
        this.uri = uri;
        this.smsType = SmsFormat.sip;
    }
    
    public static SmsFormat getSmsFormat(Contact ct) {
        
        List<UnknownParam> unknownParams = ct.getUnknownParam();
        
        for (UnknownParam p : unknownParams) {
            if (p.getName().contains("smsip")) {
                return SmsFormat.binary3gpp;
            }
        }
        
        return SmsFormat.sip;
    }

    public String getUri() {
        return this.uri;
    }

    public SmsFormat getSmsType() {
        return smsType;
    }

    public void setSmsType(SmsFormat smsType) {
        this.smsType = smsType;
    }

    public PathList getPathList() {
        return pathList;
    }

    public void setPathList(PathList pathList) {
        this.pathList = pathList;
    }
    
}
