/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.controller;

import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author bhaskarhg
 */
@RestController
@CrossOrigin
@RequestMapping("/")
public class SAPIFilter {
    private static final Logger log = LoggerFactory.getLogger(SAPIFilter.class);
    

    @RequestMapping(value = "/isup", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN)
    public String isSAPIUp() {
        return "<Done>true</Done>";	
    }
}
