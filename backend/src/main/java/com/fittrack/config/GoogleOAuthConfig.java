package com.fittrack.config;

import com.fittrack.security.GoogleOAuthSuccessHandler;
import com.fittrack.security.GoogleUserService;
import com.fittrack.security.JwtService;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Conditional(GoogleOAuthEnabledCondition.class)
@ImportAutoConfiguration(OAuth2ClientAutoConfiguration.class)
public class GoogleOAuthConfig {

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
