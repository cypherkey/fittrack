package com.fittrack.web;

import com.fittrack.domain.User;
import com.fittrack.security.AppUserDetails;
import com.fittrack.security.AppUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserResolver {

	private final AppUserDetailsService userDetailsService;

	public CurrentUserResolver(AppUserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	public User requireUser(Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		AppUserDetails details = userDetailsService.loadByUserId(jwt.getSubject());
		return details.getUser();
	}
}
