/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.soapcache;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author paul
 */
public class ResourceKeyConfiguration {
    private List<ResourceKeyPartConfiguration> partList = new ArrayList<ResourceKeyPartConfiguration>();;
    private int document;
    public static final int DOCUMENT_REQUEST = 1;
    public static final int DOCUMENT_RESPONSE = 2;

    public int getDocumentToExtractPartFrom() {
        return document;
    }

    public void setApplyToRequest() {
        document = DOCUMENT_REQUEST;
    }
    public void setApplyToResponse() {
        document = DOCUMENT_RESPONSE;
    }

    public List<ResourceKeyPartConfiguration> getPartsList() {
        return partList;
    }

}
