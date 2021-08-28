/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.telcoregulator.helpers;

import com.smilecoms.commons.base.BaseUtils;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bhaskarhg
 */
public class ApiClientHelper {

    private static final Logger log = LoggerFactory.getLogger(ApiClientHelper.class);

    public static Properties props = null;

    public static void initialise() {
        props = new Properties();
        try {
            log.warn("Here properties as contained in env.hcs.client.config\n");
            log.warn(BaseUtils.getProperty("env.hcs.client.config"));
            props.load(BaseUtils.getPropertyAsStream("env.hcs.client.config"));
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.hcs.client.config: ", ex);
        }
    }
}
