/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sra.helpers;

/**
 *
 * @author rajeshkumar
 */
public class UserLockedException extends RuntimeException {
   
    public UserLockedException(String message) {
        super(message);
    }
}
