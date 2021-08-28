/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.platform;

import com.smilecoms.commons.util.JPAUtils;
import org.slf4j.*;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author paul
 */
public class Platform {

    private static EntityManagerFactory emf = null;
    private static final Logger log = LoggerFactory.getLogger(Platform.class.getName());

    public static void init() {
        log.debug("Smile Platform class is initialising and getting resources");
        if (emf != null) {
            return;
        }
        try {
            emf = JPAUtils.getEMF("SPPU_RL");
        } catch (Throwable e) {
            log.warn("Error creating platform EMF. Will ignore and see what happens", e);
        }
    }

    public static void close() {
        log.debug("Smile Platform class is closing down and freeing resources");
        JPAUtils.closeEMF(emf);
        emf = null;
    }

    public static EntityManagerFactory getSPEMF() {
        if (emf == null) {
            try {
                log.warn("In getSPEMF and the EMF is null. Going to try and create it again");
                emf = JPAUtils.getEMF("SPPU_RL");
            } catch (Throwable e) {
                log.warn("Error creating platform EMF. Will ignore and see what happens", e);
            }
            log.warn("Smile Commons Platform classes cannot be used until Platform.init() has been called! Call Platform.init in a constructor after deployment");
            log.warn("Also make sure that Platform.close() is called during undeployment and not before then. Here is a stack trace:");
            log.warn("Stacktrace: ", new Exception());
        }
        return emf;
    }
}
