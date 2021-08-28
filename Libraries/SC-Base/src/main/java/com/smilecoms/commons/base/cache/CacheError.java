/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.smilecoms.commons.base.cache;

/**
 *
 * @author PCB
 */
public class CacheError extends RuntimeException {
    public CacheError(String err) {
        super(err);
    }
}
