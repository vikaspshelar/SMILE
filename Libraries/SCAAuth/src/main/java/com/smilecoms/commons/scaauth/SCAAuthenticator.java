/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.scaauth;


/**
 * Interface implemented by the SCAAuthentication implementation in Smile
 * Commons.
 *
 * @author PCB
 */
public interface SCAAuthenticator {

    public String[] authenticate(String user, String password, String application) throws SCALoginException;

}
