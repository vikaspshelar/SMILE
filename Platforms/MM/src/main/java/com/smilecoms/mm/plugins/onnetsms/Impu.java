/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.mm.plugins.onnetsms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author jaybeepee
 */
public class Impu implements Serializable {
    private HashMap<String, ImpuContact> contacts;
    private String aor;

    public Impu() {
        contacts = new HashMap<String, ImpuContact>();
    }

    public Impu(String aor) {
        this();
        this.aor = aor;
    }
    
    public int getNumContacts() {
        return contacts.size();
    }
    
    public void addContact(String uri, ImpuContact.SmsFormat smsType) {
        ImpuContact ct = new ImpuContact(uri, smsType);
        contacts.put(uri, ct);
    }
    
    ImpuContact getContact(String uri) {
        return contacts.get(uri);
    }
    
    public void removeContact(String uri) {
        contacts.remove(uri);
    }
    
    public void removeContact(ImpuContact contact) {
        contacts.remove(contact.getUri());
    }

    public String getAor() {
        return aor;
    }

    public HashMap<String, ImpuContact> getContacts() {
        return this.contacts;
    }

    void addContact(ImpuContact newContact) {
        contacts.put(newContact.getUri(), newContact);
    }

}
