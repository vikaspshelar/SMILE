/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.mm.plugins.onnetsms;

import com.smilecoms.commons.util.NamedLock;
import com.smilecoms.commons.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to lock a presentity irrespective of its prefix e.g. tel: sip: etc as
 * MySQL cannot provide for this kind of fuzziness without getting deadlocks
 *
 * @author PCB
 */
public class PresentityLock {

    private static final Logger log = LoggerFactory.getLogger(PresentityLock.class);
    private final String normalised;
    NamedLock lock;

    public PresentityLock(String presentity) {
        normalised = normalisePresentity(presentity);
        log.debug("Locking normalised presentity [{}]", normalised);
        lock = new NamedLock(normalised);
        log.debug("Locked normalised presentity [{}]", normalised);
    }

    public void unlock() {
        log.debug("Unlocking normalised presentity [{}]", normalised);
        lock.unlock();
    }

    private String normalisePresentity(String presentity) {
        // sip:+2347020150510@ng.smilecoms.com
        // tel:+2347020150510
        // Become 2347020150510
        return Utils.getFirstNumericPartOfStringAsString(presentity);
    }
}
