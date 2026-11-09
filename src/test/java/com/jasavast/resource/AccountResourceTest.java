package com.jasavast.resource;

import com.jasavast.core.security.AuthorityConstants;
import com.jasavast.core.security.filter.TokenProvider;
import com.jasavast.domain.User;
import com.jasavast.repository.AuditAspectRepository;
import com.jasavast.repository.UserCredentials;
import com.jasavast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
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
    @MockitoBean
    private R2dbcEntityTemplate template;
    @MockitoBean
    private TransactionalOperator transactionalOperator;

    @BeforeEach
    void setUp() {
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findCredentialsByLogin(anyString())).thenReturn(Mono.empty());
        when(userRepository.findOneByEmail(anyString())).thenReturn(Mono.empty());
        when(userRepository.deleteInactiveByLoginOrEmail(anyString(), anyString())).thenReturn(Mono.just(0L));
        when(userRepository.addAuthority(anyLong(), anyString())).thenReturn(Mono.just(1L));
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return Mono.just(u);
        });
    }

    private void givenUser(boolean aktif) {
        when(userRepository.findCredentialsByLogin("alice")).thenReturn(Mono.just(new UserCredentials(
                "alice", passwordEncoder.encode("secret-password"), aktif, new String[]{AuthorityConstants.USER})));
    }

    private WebTestClient.ResponseSpec login(String login, String password) {
        return client.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "password", password)).exchange();
    }

    @Test
    void loginIssuesUsableToken() {
        givenUser(true);
        when(userRepository.findOneByLogin("alice")).thenReturn(Mono.just(
                User.builder().id(1L).login("alice").email("alice@example.com").aktif(true).password("hash").build()));
        when(userRepository.findAuthorityNames(1L)).thenReturn(Flux.just(AuthorityConstants.USER));

        String jwt = login("Alice", "secret-password")
                .expectStatus().isOk()
                .expectHeader().value("Authorization", h -> assertThat(h).startsWith("Bearer "))
                .expectBody(Map.class).returnResult().getResponseBody().get("id_token").toString();

        assertThat(tokenProvider.getAuthentication(jwt)).get()
                .satisfies(a -> assertThat(a.getName()).isEqualTo("alice"))
                .satisfies(a -> assertThat(a.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER"));

        client.get().uri("/api/account").header("Authorization", "Bearer " + jwt).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("alice")
                .jsonPath("$.authorities[0]").isEqualTo("ROLE_USER")
                .jsonPath("$.password").doesNotExist();
    }

    @Test
    void loginFailuresAre401WithoutUserEnumeration() {
        givenUser(true);
        login("alice", "wrong-password").expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("Bad credentials");
        login("nobody", "whatever-123").expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("Bad credentials");
    }

    @Test
    void inactiveUserCannotLogin() {
        givenUser(false);
        login("alice", "secret-password").expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.detail").isEqualTo("User is not activated");
    }

    @Test
    void accountRequiresAuthentication() {
        client.get().uri("/api/account").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void registerCreatesInactiveUserWithHashedPasswordInOneTransaction() {
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
        verify(userRepository).deleteInactiveByLoginOrEmail("bob", "bob@example.com");
        verify(userRepository).addAuthority(42L, AuthorityConstants.USER);
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void uniqueViolationsAre409() {
        doReturn(Mono.error(new DuplicateKeyException("duplicate key value violates unique constraint \"ux_users_login\"")))
                .when(userRepository).save(any());
        client.post().uri("/api/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", "alice", "email", "new@example.com", "password", "long-enough-pw"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().exists("X-Jasavast-error")
                .expectBody().jsonPath("$.message").isEqualTo("error.userexists");

        doReturn(Mono.error(new DuplicateKeyException("duplicate key value violates unique constraint \"ux_users_email\"")))
                .when(userRepository).save(any());
        client.post().uri("/api/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", "other", "email", "alice@example.com", "password", "long-enough-pw"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("error.emailexists");
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

        client.post().uri("/api/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of()).exchange().expectStatus().isBadRequest();
    }
}
