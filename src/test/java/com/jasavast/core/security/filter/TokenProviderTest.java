package com.jasavast.core.security.filter;

import com.jasavast.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenProviderTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static TokenProvider provider(Clock clock) {
        return new TokenProvider(new ApplicationProperties.Security(SECRET, 60, 3600), clock);
    }

    private static Authentication auth(String... roles) {
        return UsernamePasswordAuthenticationToken.authenticated("alice", null,
                List.of(roles).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void roundTripKeepsSubjectAndAuthorities() {
        TokenProvider provider = provider(Clock.fixed(NOW, ZoneOffset.UTC));
        String token = provider.createToken(auth("ROLE_USER", "ROLE_ADMIN"), false);

        Authentication parsed = provider.getAuthentication(token).orElseThrow();

        assertThat(parsed.getName()).isEqualTo("alice");
        assertThat(parsed.getAuthorities()).map(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void tokenWithoutAuthoritiesIsAccepted() {
        TokenProvider provider = provider(Clock.fixed(NOW, ZoneOffset.UTC));
        String token = provider.createToken(auth(), false);

        assertThat(provider.getAuthentication(token)).get()
                .satisfies(a -> assertThat(a.getAuthorities()).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = provider(Clock.fixed(NOW, ZoneOffset.UTC)).createToken(auth("ROLE_USER"), false);
        TokenProvider later = provider(Clock.fixed(NOW.plusSeconds(61), ZoneOffset.UTC));

        assertThat(later.getAuthentication(token)).isEmpty();
    }

    @Test
    void rememberMeExtendsValidity() {
        String token = provider(Clock.fixed(NOW, ZoneOffset.UTC)).createToken(auth("ROLE_USER"), true);
        TokenProvider later = provider(Clock.fixed(NOW.plusSeconds(61), ZoneOffset.UTC));

        assertThat(later.getAuthentication(token)).isPresent();
    }

    @Test
    void tamperedOrForeignTokenIsRejected() {
        TokenProvider provider = provider(Clock.fixed(NOW, ZoneOffset.UTC));
        String token = provider.createToken(auth("ROLE_USER"), false);
        String foreignSecret = Base64.getEncoder().encodeToString(new byte[64]).replace('A', 'B');
        TokenProvider other = new TokenProvider(new ApplicationProperties.Security(foreignSecret, 60, 60),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(provider.getAuthentication(token.substring(0, token.length() - 2) + "xx")).isEmpty();
        assertThat(provider.getAuthentication(other.createToken(auth("ROLE_ADMIN"), false))).isEmpty();
        assertThat(provider.getAuthentication("not-a-jwt")).isEmpty();
    }

    @Test
    void weakOrNonBase64SecretFailsFast() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[32]);
        assertThatThrownBy(() -> new TokenProvider(new ApplicationProperties.Security(shortSecret, 60, 60), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("at least 64 bytes");
        assertThatThrownBy(() -> new TokenProvider(new ApplicationProperties.Security("%%%not base64%%%", 60, 60), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
