package com.jasavast.it;

import com.jasavast.core.util.RandomUtil;
import com.jasavast.domain.User;
import com.jasavast.service.MailService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Full application against a real PostgreSQL: Flyway migration, R2DBC SQL, constraints,
 * transactions, auditing. Database source, in order:
 * <ol>
 *   <li>{@code IT_DB_HOST} (+ {@code IT_DB_PORT}, {@code IT_DB_NAME}, {@code IT_DB_USERNAME}, {@code IT_DB_PASSWORD});</li>
 *   <li>a Testcontainers {@code postgres:16-alpine} when Docker is available;</li>
 *   <li>otherwise the test is skipped.</li>
 * </ol>
 * Tests use unique logins, so they do not require an empty database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class PostgresIntegrationTest {

    private static final String EXTERNAL_HOST = System.getenv("IT_DB_HOST");
    private static final PostgreSQLContainer POSTGRES =
            EXTERNAL_HOST == null && DockerClientFactory.instance().isDockerAvailable()
                    ? new PostgreSQLContainer("postgres:16-alpine") : null;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            POSTGRES.start();
            registry.add("DB_HOST", POSTGRES::getHost);
            registry.add("DB_PORT", () -> POSTGRES.getMappedPort(5432));
            registry.add("DB_NAME", POSTGRES::getDatabaseName);
            registry.add("DB_USERNAME", POSTGRES::getUsername);
            registry.add("DB_PASSWORD", POSTGRES::getPassword);
        } else if (EXTERNAL_HOST != null) {
            registry.add("DB_HOST", () -> EXTERNAL_HOST);
            registry.add("DB_PORT", () -> env("IT_DB_PORT", "5432"));
            registry.add("DB_NAME", () -> env("IT_DB_NAME", "postgres"));
            registry.add("DB_USERNAME", () -> env("IT_DB_USERNAME", "postgres"));
            registry.add("DB_PASSWORD", () -> env("IT_DB_PASSWORD", "postgres"));
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    @BeforeAll
    static void requireDatabase() {
        assumeThat(POSTGRES != null || EXTERNAL_HOST != null)
                .as("No Docker and no IT_DB_HOST: skipping PostgreSQL integration tests").isTrue();
    }

    @Autowired
    private WebTestClient client;
    @Autowired
    private DatabaseClient db;
    @MockitoBean
    private MailService mailService;

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private WebTestClient.ResponseSpec register(String login, String email, String password) {
        return client.post().uri("/api/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "email", email, "password", password)).exchange();
    }

    private String lastActivationKey() {
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendActivationEmail(any(User.class), key.capture());
        return key.getValue();
    }

    private String login(String login, String password) {
        return client.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "password", password)).exchange()
                .expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody().get("id_token").toString();
    }

    private long count(String sql, String param) {
        return db.sql(sql).bind(0, param).map(row -> row.get(0, Long.class)).one().block();
    }

    @Test
    void registerActivateLoginAndCallApi() {
        when(mailService.sendActivationEmail(any(), anyString())).thenReturn(Mono.empty());
        String login = unique("it");

        register(login.toUpperCase(), login + "@Example.com", "rahasia-123").expectStatus().isCreated();
        client.post().uri("/api/login").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "password", "rahasia-123")).exchange()
                .expectStatus().isUnauthorized();

        String key = lastActivationKey();
        client.get().uri("/api/activate?key={k}", key).exchange().expectStatus().isOk();
        client.get().uri("/api/activate?key={k}", key).exchange().expectStatus().isBadRequest();

        String jwt = login(login, "rahasia-123");
        client.get().uri("/api/account").header("Authorization", "Bearer " + jwt).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo(login)
                .jsonPath("$.email").isEqualTo(login + "@example.com")
                .jsonPath("$.authorities[0]").isEqualTo("ROLE_USER");

        client.get().uri("/api/coba/aja/dbTime").header("Authorization", "Bearer " + jwt).exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.data.dbTime").exists();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(
                count("SELECT count(*) FROM jvs_audit WHERE principal = $1 AND method_name = 'dbTime'", login)).isEqualTo(1));
        assertThat(count("SELECT count(*) FROM users WHERE login = $1 AND create_by = 'system' AND ins_on IS NOT NULL", login))
                .isEqualTo(1);

        register(login, unique("x") + "@example.com", "rahasia-123").expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("error.userexists");
        register(unique("y"), login + "@EXAMPLE.com", "rahasia-123").expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("error.emailexists");
    }

    @Test
    void inactiveRegistrationIsReplacedAndRolesCascade() {
        when(mailService.sendActivationEmail(any(), anyString())).thenReturn(Mono.empty());
        String login = unique("re");
        register(login, login + "@example.com", "rahasia-123").expectStatus().isCreated();
        register(login, login + "@example.com", "rahasia-456").expectStatus().isCreated();

        assertThat(count("SELECT count(*) FROM users WHERE login = $1", login)).isEqualTo(1);
        assertThat(count("""
                SELECT count(*) FROM user_authority ua JOIN users u ON u.id = ua.user_id WHERE u.login = $1""", login))
                .isEqualTo(1);
        assertThat(db.sql("SELECT count(*) FROM user_authority WHERE user_id NOT IN (SELECT id FROM users)")
                .map(row -> row.get(0, Long.class)).one().block()).isZero();
    }

    @Test
    void passwordResetHonoursExpiryAndSingleUse() {
        when(mailService.sendActivationEmail(any(), anyString())).thenReturn(Mono.empty());
        when(mailService.sendPasswordResetMail(any(), anyString())).thenReturn(Mono.empty());
        String login = unique("pw");
        register(login, login + "@example.com", "rahasia-123").expectStatus().isCreated();
        client.get().uri("/api/activate?key={k}", lastActivationKey()).exchange().expectStatus().isOk();

        client.post().uri("/api/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login)).exchange().expectStatus().isOk();
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendPasswordResetMail(any(User.class), key.capture());

        // Expired key: push reset_date back beyond the 24h validity.
        db.sql("UPDATE users SET reset_date = now() - interval '25 hours' WHERE reset_key_hash = $1")
                .bind(0, RandomUtil.sha256(key.getValue())).fetch().rowsUpdated().block();
        resetPassword(key.getValue()).expectStatus().isBadRequest();

        db.sql("UPDATE users SET reset_date = now() WHERE reset_key_hash = $1")
                .bind(0, RandomUtil.sha256(key.getValue())).fetch().rowsUpdated().block();
        resetPassword(key.getValue()).expectStatus().isOk();
        resetPassword(key.getValue()).expectStatus().isBadRequest();

        login(login, "baru-baru-123");
    }

    private WebTestClient.ResponseSpec resetPassword(String key) {
        return client.post().uri("/api/reset-password").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("key", key, "newPassword", "baru-baru-123")).exchange();
    }

    @Test
    void concurrentRegistrationsNeverCorruptOrError() {
        when(mailService.sendActivationEmail(any(), anyString())).thenReturn(Mono.empty());
        String login = unique("race");

        List<Integer> statuses = Flux.range(0, 8)
                .flatMap(i -> Mono.fromCallable(() -> register(login, login + i + "@example.com", "rahasia-123")
                        .returnResult(Void.class).getStatus().value()), 8)
                .collectList().block();

        assertThat(statuses).allMatch(s -> s == 201 || s == 409).contains(201);
        assertThat(count("SELECT count(*) FROM users WHERE login = $1", login)).isEqualTo(1);
    }
}
