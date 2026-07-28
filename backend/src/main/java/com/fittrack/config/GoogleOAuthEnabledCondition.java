package com.fittrack.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Google OAuth is enabled when client-id and client-secret are both non-empty.
 * {@code fittrack.oauth2.google.enabled=true} alone is not enough without credentials
 * (avoids crashing startup); credentials alone are enough to enable.
 */
public class GoogleOAuthEnabledCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		var env = context.getEnvironment();
		String clientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id", "");
		String clientSecret = env.getProperty("spring.security.oauth2.client.registration.google.client-secret", "");
		return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
	}
}
