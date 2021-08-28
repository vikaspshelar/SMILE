/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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
public class SAPIHealthCheck {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<?> healthCheck(){
        Map<String,String> healthMap = new HashMap<>();
        healthMap.put("status", "success");
        return ResponseEntity.ok(healthMap);
    }
}
