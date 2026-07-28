package com.fittrack.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

class GoogleOAuthEnabledConditionTest {

	private final GoogleOAuthEnabledCondition condition = new GoogleOAuthEnabledCondition();

	@Test
	void disabledWhenCredentialsMissing() {
		assertThat(matches(new MockEnvironment())).isFalse();
	}

	@Test
	void enabledWhenClientIdAndSecretPresent() {
		MockEnvironment env = new MockEnvironment()
				.withProperty("spring.security.oauth2.client.registration.google.client-id", "cid")
				.withProperty("spring.security.oauth2.client.registration.google.client-secret", "csecret");
		assertThat(matches(env)).isTrue();
	}

	private boolean matches(MockEnvironment env) {
		ConditionContext context = mock(ConditionContext.class);
		when(context.getEnvironment()).thenReturn(env);
		return condition.matches(context, mock(AnnotatedTypeMetadata.class));
	}
}
