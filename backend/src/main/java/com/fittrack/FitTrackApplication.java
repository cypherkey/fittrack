package com.fittrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = OAuth2ClientAutoConfiguration.class)
@ConfigurationPropertiesScan
public class FitTrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(FitTrackApplication.class, args);
	}
}
