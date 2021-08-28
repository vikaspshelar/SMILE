/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.foreigner.verify.restclient;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.im.tz.nida.restclient.NidaHelper;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bhaskarhg
 */
public class VerifyForeignerHelper {
    private static final Logger log = LoggerFactory.getLogger(NidaHelper.class);
    
    public static Properties props = null;
   
    public static void initialise() {
        props = new Properties();
        try {            
            log.warn("Here are the verify foreigner properties as contained in env.verify.foreigner.config\n");
            log.warn(BaseUtils.getProperty("env.verify.foreigner.config"));
            props.load(BaseUtils.getPropertyAsStream("env.verify.foreigner.config"));
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.verify.foreigner.config: ", ex);
        }
    }
}
