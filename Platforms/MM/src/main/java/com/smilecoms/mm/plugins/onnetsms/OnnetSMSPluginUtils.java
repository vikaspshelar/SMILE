/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwContact;
import com.smilecoms.mm.plugins.onnetsms.model.IpsmgwContact.SmsFormatType;
import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Contact;
import com.smilecoms.mm.plugins.onnetsms.reginfo.ietf.params.xml.ns.reginfo.Contact.UnknownParam;
import java.util.List;

/**
 *
 * @author jaybeepee
 */
public class OnnetSMSPluginUtils {
    
    public static SmsFormatType getSmsFormat(Contact ct) {
        
        List<UnknownParam> unknownParams = ct.getUnknownParam();
        
        for (UnknownParam p : unknownParams) {
            if (p.getName().contains("smsip")) {
                return IpsmgwContact.SmsFormatType.binary3gpp;
            }
        }
        
        return SmsFormatType.sip;
    }

    static String getUriStringNoParams(String to) {
        int pos = to.indexOf(";");
        if (pos != -1) {
            return to.substring(0, pos);
        } else {
            return to;
        }
    }
}
