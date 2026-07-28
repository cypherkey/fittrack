package com.fittrack.security;

import com.fittrack.config.FitTrackProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final FitTrackProperties properties;

	public JwtService(FitTrackProperties properties) {
		this.properties = properties;
		SecretKey key = new SecretKeySpec(
				properties.jwt().secret().getBytes(StandardCharsets.UTF_8),
				"HmacSHA256"
		);
		this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	public String createToken(AppUserDetails userDetails) {
		Instant now = Instant.now();
		Instant expires = now.plusSeconds(properties.jwt().expirationMinutes() * 60);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.id(UUID.randomUUID().toString())
				.issuer("fittrack")
				.issuedAt(now)
				.expiresAt(expires)
				.subject(userDetails.getUserId())
				.claim("username", userDetails.getUsername())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
