/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.offnetsms.smsc;

/**
 *
 * @author paul
 */
public class TemporaryDeliveryFailure extends Exception {

    public TemporaryDeliveryFailure() {
        super();
    }

    public TemporaryDeliveryFailure(String message) {
        super(message);
    }

}
