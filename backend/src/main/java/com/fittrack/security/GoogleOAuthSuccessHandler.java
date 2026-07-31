package com.fittrack.security;

import com.fittrack.config.FitTrackProperties;
import com.fittrack.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

	private final GoogleUserService googleUserService;
	private final JwtService jwtService;
	private final FitTrackProperties properties;

	public GoogleOAuthSuccessHandler(
			GoogleUserService googleUserService,
			JwtService jwtService,
			FitTrackProperties properties
	) {
		this.googleUserService = googleUserService;
		this.jwtService = jwtService;
		this.properties = properties;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException {
		OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
		User user = googleUserService.upsertFromOidc(oidcUser);
		String token = jwtService.createToken(new AppUserDetails(user));
		String base = properties.spaAuthCallbackUrl();
		response.sendRedirect(base + "#token=" + token);
	}
}