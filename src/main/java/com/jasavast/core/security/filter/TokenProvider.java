package com.jasavast.core.security.filter;

import com.jasavast.config.ApplicationProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Issues and verifies HS512-signed JWTs. Tokens are stateless: there is no revocation,
 * so keep {@code token-validity-seconds} short enough for your threat model.
 */
@Component
@Slf4j
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final int MIN_KEY_BYTES = 64; // HS512 requires a >= 512-bit key

    private final SecretKey key;
    private final JwtParser parser;
    private final Duration tokenValidity;
    private final Duration tokenValidityRememberMe;
    private final Clock clock;

    @Autowired
    public TokenProvider(ApplicationProperties properties) {
        this(properties.security(), Clock.systemUTC());
    }

    TokenProvider(ApplicationProperties.Security security, Clock clock) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(security.secret());
        } catch (DecodingException e) {
            throw new IllegalStateException("application.security.secret must be Base64 encoded", e);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("application.security.secret must decode to at least " + MIN_KEY_BYTES
                    + " bytes (got " + keyBytes.length + "). Generate one with: openssl rand -base64 64");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
        this.tokenValidity = Duration.ofSeconds(security.tokenValiditySeconds());
        this.tokenValidityRememberMe = Duration.ofSeconds(security.tokenValiditySecondsRememberMe());
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        Instant now = clock.instant();
        Instant validity = now.plus(rememberMe ? tokenValidityRememberMe : tokenValidity);
        return Jwts.builder()
                .subject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(validity))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Verifies signature and expiry and builds the Authentication in a single parse.
     *
     * @return empty when the token is invalid, expired or tampered with
     */
    public Optional<Authentication> getAuthentication(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            List<SimpleGrantedAuthority> authorities = Arrays.stream(
                            String.valueOf(claims.getOrDefault(AUTHORITIES_KEY, "")).split(","))
                    .map(String::trim)
                    .filter(a -> !a.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            User principal = new User(subject, "", authorities);
            return Optional.of(new UsernamePasswordAuthenticationToken(principal, token, authorities));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean validateToken(String authToken) {
        return getAuthentication(authToken).isPresent();
    }
}
