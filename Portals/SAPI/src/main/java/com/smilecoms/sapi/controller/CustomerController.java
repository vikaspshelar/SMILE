/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.controller;

import static com.smilecoms.sapi.config.JwtTokenUtil.JWT_TOKEN_VALIDITY;
import com.smilecoms.sapi.dao.CustomerDAO;
import com.smilecoms.sapi.model.ApiResponse;
import com.smilecoms.sapi.model.AuthenticationResponse;
import com.smilecoms.sapi.model.CustomerList;
import com.smilecoms.sapi.model.CustomerRequest;
import com.smilecoms.sapi.model.JwtRequest;
import com.smilecoms.sapi.service.JwtUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author bhaskarhg
 */
@RestController
//@CrossOrigin
public class CustomerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUserDetailsService.class);

    @Autowired
    private CustomerDAO customerDao;

    @RequestMapping(value = "/searchCustomer", method = RequestMethod.POST)
    public ResponseEntity<?> searchCustomer(@RequestBody CustomerRequest customerRequest) throws Exception {
        LOGGER.debug("In CustomerController and in searchCutomer "+ customerRequest.getId()+"****"+customerRequest.getUsername());
        CustomerList customerList = customerDao.searchCustomer(customerRequest);
        if (customerList != null) {
            return ResponseEntity.ok(customerList);
        }

        return new ResponseEntity(new ApiResponse(false, "UserName and password is not valid"), HttpStatus.BAD_REQUEST);
    }
}
