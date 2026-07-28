package com.fittrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	public static final String BEARER_JWT = "bearer-jwt";

	@Bean
	OpenAPI fitTrackOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("FitTrack API")
						.description("""
								Fitness tracking REST API: local/Google auth (JWT), exercises, templates, and workouts.

								**Auth:** `POST /api/v1/auth/login` for a JWT, then Authorize with `Bearer <token>`.
								Google SSO (when configured): `/oauth2/authorization/google` -> SPA `#token=<jwt>`.
								""")
						.version("v1")
						.contact(new Contact().name("FitTrack"))
						.license(new License().name("Proprietary")))
				.servers(List.of(new Server().url("/").description("Current host")))
				.components(new Components()
						.addSecuritySchemes(BEARER_JWT, new SecurityScheme()
								.name(BEARER_JWT)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("JWT from POST /api/v1/auth/login or Google OAuth handoff")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
	}
}