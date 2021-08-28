/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.nida.restclient;



import com.smilecoms.commons.base.BaseUtils;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author mukosi
 */
public class NidaHelper {
    
    private static final Logger log = LoggerFactory.getLogger(NidaHelper.class);
    
    public static Properties props = null;
   
    public static void initialise() {
        props = new Properties();
        try {            
            log.warn("Here are the NIDA properties as contained in env.nida.config\n");
            log.warn(BaseUtils.getProperty("env.nida.config"));
            props.load(BaseUtils.getPropertyAsStream("env.nida.config"));
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.nida.config: ", ex);
        }
    }
}
