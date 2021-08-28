package com.smilecoms.rra.service;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.smilecoms.rra.dao.UCCDao;
import com.smilecoms.rra.model.User;

/**
 * 
 * @author rajeshkumar
 *
 */
@Service
public class JwtUserDetailsService implements UserDetailsService {

	@Autowired
	private UCCDao userDao;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userDao.findByUserName(username);

		if (user == null) {
			throw new UsernameNotFoundException("Invalid user " + username);
		}

		return new org.springframework.security.core.userdetails.User(user.getUserName(), user.getPassword(), true,
				true, true, true, getAuthorities("ROLE_USER"));
	}

	private Collection<? extends GrantedAuthority> getAuthorities(String role) {
		return Arrays.asList(new SimpleGrantedAuthority(role));
	}

	public boolean existsByUsername(String username) {
		if(userDao.findByUserName(username) != null) {
			return true;
		}
		return false;
	}

	public void save(User user) {
		userDao.saveUser(user);
		
	}

}
