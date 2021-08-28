/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

/**
 *
 * @author paul
 */
public class SRASystemException extends SRAException {

    public SRASystemException(String message) {
        super(message, SRAException.SYSTEM_ERROR);
    }
    
    
}
