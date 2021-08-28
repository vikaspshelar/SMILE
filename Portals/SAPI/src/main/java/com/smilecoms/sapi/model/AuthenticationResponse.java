/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.model;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author bhaskarhg
 */
public class AuthenticationResponse implements Serializable {

    private static final long serialVersionUID = -8091879091924046844L;
    private int userId;
    private String firstName;
    private String lastName;
    private final String jwttoken;
    private long expires;
    
    public AuthenticationResponse(int userId, String firstName, String lastName,String jwttoken, long expires) {
        this.jwttoken = jwttoken;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.expires = expires;
    }
    
   
    public String getToken() {
        return this.jwttoken;
    }

    public int getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }

    public long getExpires() {
        return expires;
    }
}