/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author paul
 */
public class InsufficientFundsException extends Exception {

    private static final Logger log = LoggerFactory.getLogger(InsufficientFundsException.class);

    InsufficientFundsException(String from, String to, String incomingTrunk, String outgoingTrunk, String leg) {
        log.info("Insufficient funds exception raised From [{}] To [{}] From Trunk [{}] To Trunk [{}] Leg [{}]", new Object[]{from, to, incomingTrunk, outgoingTrunk, leg});
    }
    
}
