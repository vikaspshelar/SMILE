/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.sapi.controller;

/**
 *
 * @author bhaskarhg
 */
import com.smilecoms.commons.util.Codec;
import com.smilecoms.sapi.config.JwtTokenUtil;
import static com.smilecoms.sapi.config.JwtTokenUtil.JWT_TOKEN_VALIDITY;
import com.smilecoms.sapi.dao.CustomerDAO;
import com.smilecoms.sapi.dao.TokenException;
import com.smilecoms.sapi.model.ApiResponse;
import com.smilecoms.sapi.model.AuthenticationResponse;
import com.smilecoms.sapi.model.Customer;
import com.smilecoms.sapi.model.Customers;
import com.smilecoms.sapi.model.JwtRequest;
import com.smilecoms.sapi.model.JwtResponse;
import com.smilecoms.sapi.service.JwtUserDetailsService;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
//@RequestMapping("/api/auth")
public class JwtAuthenticationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUserDetailsService.class);

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private JwtUserDetailsService userDetailsService;
    @Autowired
    private CustomerDAO customerDao;

    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
        Customers customer = new Customers(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        customerDao.setCustomer(customer);
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        Customers auth = customerDao.authenticateCustomer(customer);
        LOGGER.info("In controller" + auth);
        if (auth.getFirstName() != null && !auth.getFirstName().isEmpty()) {
            final String token = jwtTokenUtil.generateToken(userDetails);
            return ResponseEntity.ok(new AuthenticationResponse(auth.getUserId(), auth.getFirstName(), auth.getLastName(), token, System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000));
        }
        return new ResponseEntity(new ApiResponse(false, "UserName and password is not valid"), HttpStatus.BAD_REQUEST);
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}
