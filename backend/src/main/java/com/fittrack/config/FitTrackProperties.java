package com.fittrack.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "fittrack")
public record FitTrackProperties(
		Jwt jwt,
		Seed seed,
		Oauth2 oauth2,
		String frontendUrl
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

	public record Oauth2(Google google) {
		public record Google(boolean enabled) {
		}
	}

	/** OAuth JWT handoff URL: {@code {FRONTEND_URL}/auth/callback}. */
	public String spaAuthCallbackUrl() {
		return stripTrailingSlash(primaryFrontendOrigin()) + "/auth/callback";
	}

	/** CORS allowed origins from {@code FRONTEND_URL} (comma-separated supported). */
	public List<String> corsAllowedOrigins() {
		return Arrays.stream((frontendUrl == null ? "" : frontendUrl).split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.map(FitTrackProperties::stripTrailingSlash)
				.toList();
	}

	private String primaryFrontendOrigin() {
		List<String> origins = corsAllowedOrigins();
		if (origins.isEmpty()) {
			throw new IllegalStateException("fittrack.frontend-url / FRONTEND_URL must be set");
		}
		return origins.getFirst();
	}

	private static String stripTrailingSlash(String url) {
		if (url == null || url.isEmpty()) {
			return url;
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}