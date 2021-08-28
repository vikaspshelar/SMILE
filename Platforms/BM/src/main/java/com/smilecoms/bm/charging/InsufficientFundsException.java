/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.bm.charging;

import com.smilecoms.commons.util.JPAUtils;

/**
 *
 * @author paul
 */
public class InsufficientFundsException extends Exception {


    public InsufficientFundsException() {
        JPAUtils.setRollbackOnly();
    }
}
