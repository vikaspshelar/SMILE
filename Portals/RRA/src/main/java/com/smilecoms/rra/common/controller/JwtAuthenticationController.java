package com.smilecoms.rra.common.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.smilecoms.rra.config.JwtTokenUtil;
import com.smilecoms.rra.model.ApiResponse;
import com.smilecoms.rra.model.JwtRequest;
import com.smilecoms.rra.model.JwtResponse;
import com.smilecoms.rra.model.SignUpRequest;
import com.smilecoms.rra.model.User;
import com.smilecoms.rra.service.JwtUserDetailsService;

/**
 * 
 * @author rajeshkumar
 *
 */
@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class JwtAuthenticationController {
	private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationController.class);
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUserDetailsService userDetailsService;

	/**
	 * 
	 * @param authenticationRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/token", method = RequestMethod.POST)
	public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
		LOGGER.info("called create token");
		authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

		final UserDetails userDetails = userDetailsService
				.loadUserByUsername(authenticationRequest.getUsername());

		final String token = jwtTokenUtil.generateToken(userDetails);

		return ResponseEntity.ok(new JwtResponse(token));

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

	/**
	 * 
	 * @param signUpRequest
	 * @return
	 */
	@RequestMapping(value = "/signup", method = RequestMethod.POST)
	public ResponseEntity<?> registerUser(@RequestBody SignUpRequest signUpRequest) {
		
		if (userDetailsService.existsByUsername(signUpRequest.getUsername())) {
			return new ResponseEntity(new ApiResponse(false, "Username is already taken!"), HttpStatus.BAD_REQUEST);
		}
		// Creating user's account
		User user = new User(signUpRequest.getUsername(), signUpRequest.getPassword());

		user.setPassword(passwordEncoder.encode(user.getPassword()));

		userDetailsService.save(user);

		return new ResponseEntity(new ApiResponse(true, "User create successfuly"), HttpStatus.CREATED);
	}
}