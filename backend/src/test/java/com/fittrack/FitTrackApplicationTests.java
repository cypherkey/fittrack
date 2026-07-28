package com.fittrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FitTrackApplicationTests {

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("FITTRACK_DB_PATH", () -> "file:./target/test-fittrack.db");
		registry.add("spring.datasource.url", () -> "jdbc:sqlite:./target/test-fittrack.db?foreign_keys=true");
		registry.add("fittrack.jwt.secret", () -> "fittrack-test-secret-change-me-must-be-at-least-256-bits!!");
	}

	@Test
	void contextLoads() {
	}
}
