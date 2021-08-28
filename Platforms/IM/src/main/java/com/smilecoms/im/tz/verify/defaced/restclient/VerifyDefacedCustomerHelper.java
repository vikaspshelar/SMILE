/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.im.tz.verify.defaced.restclient;

import com.smilecoms.commons.base.BaseUtils;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bhaskarhg
 */
public class VerifyDefacedCustomerHelper {

    private static final Logger log = LoggerFactory.getLogger(VerifyDefacedCustomerHelper.class);

    public static Properties props = null;

    public static void initialise() {
        props = new Properties();
        try {
            log.warn("Here are the verify defaced properties as contained in env.defaced.customer.api.config\n");
            log.warn(BaseUtils.getProperty("env.defaced.customer.api.config"));
            props.load(BaseUtils.getPropertyAsStream("env.defaced.customer.api.config"));
        } catch (Exception ex) {
            log.debug("Failed to load properties from env.defaced.customer.api.config: ", ex);
        }
    }
}
