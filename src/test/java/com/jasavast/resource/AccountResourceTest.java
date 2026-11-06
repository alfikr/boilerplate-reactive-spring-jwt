package com.jasavast.resource;

import com.jasavast.core.security.AuthorityConstants;
import com.jasavast.core.security.filter.TokenProvider;
import com.jasavast.domain.Authority;
import com.jasavast.domain.User;
import com.jasavast.repository.AuditAspectRepository;
import com.jasavast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AccountResource.class)
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class AccountResourceTest {

    @Autowired
    private WebTestClient client;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenProvider tokenProvider;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private AuditAspectRepository auditRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.findOneByLogin(anyString())).thenReturn(Mono.empty());
        when(userRepository.findOneByEmail(anyString())).thenReturn(Mono.empty());
        when(userRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    private User user(boolean aktif) {
        return User.builder().id("1").login("alice").email("alice@example.com")
                .password(passwordEncoder.encode("secret-password")).aktif(aktif)
                .authorities(Set.of(new Authority(AuthorityConstants.USER))).build();
    }

    private WebTestClient.ResponseSpec login(String login, String password) {
        return client.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "password", password)).exchange();
    }

    @Test
    void loginIssuesUsableToken() {
        when(userRepository.findOneByLogin("alice")).thenReturn(Mono.just(user(true)));

        String jwt = login("Alice", "secret-password")
                .expectStatus().isOk()
                .expectHeader().value("Authorization", h -> assertThat(h).startsWith("Bearer "))
                .expectBody(Map.class).returnResult().getResponseBody().get("id_token").toString();

        assertThat(tokenProvider.getAuthentication(jwt)).get()
                .satisfies(a -> assertThat(a.getName()).isEqualTo("alice"))
                .satisfies(a -> assertThat(a.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER"));

        client.get().uri("/api/account").header("Authorization", "Bearer " + jwt).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.login").isEqualTo("alice")
                .jsonPath("$.password").doesNotExist();
    }

    @Test
    void loginFailuresAre401WithoutUserEnumeration() {
        when(userRepository.findOneByLogin("alice")).thenReturn(Mono.just(user(true)));
        login("alice", "wrong-password").expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("Bad credentials");
        login("nobody", "whatever-123").expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("Bad credentials");
    }

    @Test
    void inactiveUserCannotLogin() {
        when(userRepository.findOneByLogin("alice")).thenReturn(Mono.just(user(false)));
        login("alice", "secret-password").expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("User is not activated");
    }

    @Test
    void accountRequiresAuthentication() {
        client.get().uri("/api/account").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void registerCreatesInactiveUserWithHashedPassword() {
        client.post().uri("/api/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", "Bob", "email", "Bob@Example.com", "password", "long-enough-pw",
                        "firstName", "Bob", "aktif", true, "authorities", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isCreated();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getLogin()).isEqualTo("bob");
        assertThat(saved.getEmail()).isEqualTo("bob@example.com");
        assertThat(saved.getFirstName()).isEqualTo("Bob");
        assertThat(saved.isAktif()).isFalse();
        assertThat(saved.getActivationKeyHash()).hasSize(64);
        assertThat(passwordEncoder.matches("long-enough-pw", saved.getPassword())).isTrue();
        assertThat(saved.getAuthorities()).extracting(Authority::getName).containsExactly(AuthorityConstants.USER);
    }

    @Test
    void registerRejectsTakenLoginWith409() {
        when(userRepository.findOneByLogin("alice")).thenReturn(Mono.just(user(true)));
        client.post().uri("/api/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", "alice", "email", "new@example.com", "password", "long-enough-pw"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().exists("X-Jasavast-error")
                .expectBody().jsonPath("$.message").isEqualTo("error.userexists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerValidatesInput() {
        client.post().uri("/api/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", "a", "email", "not-an-email", "password", "short"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors[?(@.field == 'login')]").exists()
                .jsonPath("$.fieldErrors[?(@.field == 'email')]").exists()
                .jsonPath("$.fieldErrors[?(@.field == 'password')]").exists();
    }

    @Test
    void forgotPasswordAnswersTheSameForUnknownAccounts() {
        client.post().uri("/api/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "ghost@example.com"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.success").isEqualTo(true);
        verify(userRepository, never()).save(any());

        client.post().uri("/api/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of()).exchange().expectStatus().isBadRequest();
    }
}
