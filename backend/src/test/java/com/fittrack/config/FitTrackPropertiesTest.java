package com.fittrack.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FitTrackPropertiesTest {

	@Test
	void oauth2LoginRedirectUriUsesFrontendUrlSchemeAndHost() {
		FitTrackProperties properties = new FitTrackProperties(
				new FitTrackProperties.Jwt("secret-must-be-long-enough-for-hs256-signing-key!!", 60),
				new FitTrackProperties.Seed("admin", "admin", "Admin", "admin@localhost"),
				new FitTrackProperties.Oauth2(new FitTrackProperties.Oauth2.Google(true)),
				"https://fittrack.example.com/"
		);

		assertThat(properties.spaAuthCallbackUrl())
				.isEqualTo("https://fittrack.example.com/auth/callback");
		assertThat(properties.oauth2LoginRedirectUriTemplate())
				.isEqualTo("https://fittrack.example.com/login/oauth2/code/{registrationId}");
	}
}
