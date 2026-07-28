package com.fittrack.seed;

import com.fittrack.config.FitTrackProperties;
import com.fittrack.domain.User;
import com.fittrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultUserSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DefaultUserSeeder.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final FitTrackProperties properties;

	public DefaultUserSeeder(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			FitTrackProperties properties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String username = properties.seed().defaultUsername();
		if (userRepository.existsByUsername(username)) {
			return;
		}
		User user = new User();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(properties.seed().defaultPassword()));
		user.setDisplayName(properties.seed().defaultDisplayName());
		user.setEmail(properties.seed().defaultEmail());
		user.setAdmin(true);
		userRepository.save(user);
		log.info("Seeded default local user '{}'", username);
	}
}
