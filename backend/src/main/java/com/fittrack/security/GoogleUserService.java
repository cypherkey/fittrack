package com.fittrack.security;

import com.fittrack.domain.User;
import com.fittrack.repository.UserRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GoogleUserService {

	private final UserRepository userRepository;

	public GoogleUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public User upsertFromOidc(OidcUser oidcUser) {
		String subject = oidcUser.getSubject();
		return userRepository.findByGoogleSubject(subject)
				.map(existing -> updateProfile(existing, oidcUser))
				.orElseGet(() -> createUser(oidcUser));
	}

	private User updateProfile(User user, OidcUser oidcUser) {
		if (StringUtils.hasText(oidcUser.getFullName())) {
			user.setDisplayName(oidcUser.getFullName());
		}
		if (StringUtils.hasText(oidcUser.getEmail())) {
			user.setEmail(oidcUser.getEmail());
		}
		if (StringUtils.hasText(oidcUser.getPicture())) {
			user.setAvatarUrl(oidcUser.getPicture());
		}
		return userRepository.save(user);
	}

	private User createUser(OidcUser oidcUser) {
		User user = new User();
		user.setGoogleSubject(oidcUser.getSubject());
		user.setEmail(oidcUser.getEmail());
		String displayName = StringUtils.hasText(oidcUser.getFullName())
				? oidcUser.getFullName()
				: (StringUtils.hasText(oidcUser.getEmail()) ? oidcUser.getEmail() : "Google User");
		user.setDisplayName(displayName);
		user.setAvatarUrl(oidcUser.getPicture());
		return userRepository.save(user);
	}
}
