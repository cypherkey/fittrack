package com.fittrack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fittrack.config.FitTrackProperties;
import com.fittrack.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

class GoogleOAuthSuccessHandlerTest {

	@Test
	void redirectsToSpaWithJwtFragment() throws Exception {
		GoogleUserService googleUserService = mock(GoogleUserService.class);
		JwtService jwtService = mock(JwtService.class);
		FitTrackProperties properties = new FitTrackProperties(
				new FitTrackProperties.Jwt("fittrack-test-secret-change-me-must-be-at-least-256-bits!!", 60),
				new FitTrackProperties.Seed("admin", "admin", "Admin", "admin@localhost"),
				new FitTrackProperties.Oauth2(new FitTrackProperties.Oauth2.Google(false), "http://localhost:4200/auth/callback"),
				new FitTrackProperties.Cors("http://localhost:4200")
		);

		User user = new User();
		user.setId("user-1");
		user.setUsername("google-user");
		user.setDisplayName("Google User");
		when(googleUserService.upsertFromOidc(any(OidcUser.class))).thenReturn(user);
		when(jwtService.createToken(any(AppUserDetails.class))).thenReturn("jwt-token-value");

		OidcIdToken idToken = new OidcIdToken(
				"token-value",
				java.time.Instant.now(),
				java.time.Instant.now().plusSeconds(3600),
				Map.of("sub", "google-sub-1", "name", "Google User", "email", "g@example.com")
		);
		OidcUser oidcUser = new DefaultOidcUser(java.util.List.of(new OidcUserAuthority(idToken)), idToken);
		Authentication authentication = mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(oidcUser);

		HttpServletResponse response = mock(HttpServletResponse.class);
		GoogleOAuthSuccessHandler handler = new GoogleOAuthSuccessHandler(googleUserService, jwtService, properties);
		handler.onAuthenticationSuccess(mock(HttpServletRequest.class), response, authentication);

		ArgumentCaptor<String> redirect = ArgumentCaptor.forClass(String.class);
		verify(response).sendRedirect(redirect.capture());
		assertThat(redirect.getValue()).isEqualTo("http://localhost:4200/auth/callback#token=jwt-token-value");
	}
}
