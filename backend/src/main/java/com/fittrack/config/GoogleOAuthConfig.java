package com.fittrack.config;

import com.fittrack.security.GoogleOAuthSuccessHandler;
import com.fittrack.security.GoogleUserService;
import com.fittrack.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Google OAuth login (optional). Enabled only when client-id and client-secret are non-empty.
 * Registers {@link ClientRegistrationRepository} explicitly because
 * {@code OAuth2ClientAutoConfiguration} is excluded on {@code FitTrackApplication}.
 */
@Configuration
@Conditional(GoogleOAuthEnabledCondition.class)
public class GoogleOAuthConfig {

	private static final Logger log = LoggerFactory.getLogger(GoogleOAuthConfig.class);

	@Bean
	ClientRegistrationRepository googleClientRegistrationRepository(
			@Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
			@Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret,
			FitTrackProperties properties
	) {
		String redirectUri = properties.oauth2LoginRedirectUriTemplate();
		log.info("Google OAuth enabled; redirect URI template={}", redirectUri);
		ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
				.clientId(clientId.trim())
				.clientSecret(clientSecret.trim())
				.scope("openid", "profile", "email")
				.redirectUri(redirectUri)
				.build();
		return new InMemoryClientRegistrationRepository(google);
	}

	@Bean
	GoogleOAuthSuccessHandler googleOAuthSuccessHandler(
			GoogleUserService googleUserService,
			JwtService jwtService,
			FitTrackProperties properties
	) {
		return new GoogleOAuthSuccessHandler(googleUserService, jwtService, properties);
	}

	@Bean
	@Order(1)
	SecurityFilterChain googleOAuthSecurityFilterChain(
			HttpSecurity http,
			GoogleOAuthSuccessHandler successHandler
	) throws Exception {
		http
				.securityMatcher("/oauth2/**", "/login/oauth2/**")
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.oauth2Login(oauth2 -> oauth2.successHandler(successHandler));
		return http.build();
	}
}