package com.fittrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fittrack")
public record FitTrackProperties(
		Jwt jwt,
		Seed seed,
		Oauth2 oauth2,
		Cors cors
) {
	public record Jwt(String secret, long expirationMinutes) {
	}

	public record Seed(
			String defaultUsername,
			String defaultPassword,
			String defaultDisplayName,
			String defaultEmail
	) {
	}

	public record Oauth2(Google google, String successRedirect) {
		public record Google(boolean enabled) {
		}
	}

	public record Cors(String allowedOrigins) {
	}
}
