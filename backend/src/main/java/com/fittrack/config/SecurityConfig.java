package com.fittrack.config;

import com.fittrack.security.AppUserDetailsService;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(
			AppUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder
	) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	JwtDecoder jwtDecoder(FitTrackProperties properties) {
		SecretKey key = new SecretKeySpec(
				properties.jwt().secret().getBytes(StandardCharsets.UTF_8),
				"HmacSHA256"
		);
		return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
			AppUserDetailsService userDetailsService
	) {
		return jwt -> {
			var userDetails = userDetailsService.loadByUserId(jwt.getSubject());
			return new JwtAuthenticationToken(jwt, userDetails.getAuthorities(), userDetails.getUsername());
		};
	}

	@Bean
	@Order(2)
	SecurityFilterChain apiSecurityFilterChain(
			HttpSecurity http,
			Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
	) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
				);
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(FitTrackProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origins = Arrays.stream(properties.cors().allowedOrigins().split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		config.setAllowedOrigins(origins);
		config.setAllowedHeaders(List.of(CorsConfiguration.ALL));
		config.setAllowedMethods(List.of(CorsConfiguration.ALL));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
