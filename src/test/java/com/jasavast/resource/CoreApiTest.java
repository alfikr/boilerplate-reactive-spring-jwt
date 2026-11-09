package com.jasavast.resource;

import com.jasavast.core.security.filter.TokenProvider;
import com.jasavast.domain.AuditEventModel;
import com.jasavast.repository.AuditAspectRepository;
import com.jasavast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CoreApi.class)
@Import(WebSliceTestConfig.class)
@ActiveProfiles("test")
class CoreApiTest {

    @Autowired
    private WebTestClient client;
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
        when(auditRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    private String token(String... roles) {
        return "Bearer " + tokenProvider.createToken(UsernamePasswordAuthenticationToken.authenticated("alice", null,
                List.of(roles).stream().map(SimpleGrantedAuthority::new).toList()), false);
    }

    @Test
    void requiresAuthentication() {
        client.get().uri("/api/coba/aja/getMethod").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/coba/aja/getMethod")
                .header("Authorization", "Bearer garbage").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getPassesQueryParametersAsData() {
        client.get().uri("/api/coba/aja/getMethod?foo=bar")
                .header("Authorization", token("ROLE_USER"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.message").isEqualTo("Get method success")
                .jsonPath("$.data.data.foo").isEqualTo("bar")
                .jsonPath("$.errors").isEmpty();
    }

    @Test
    void postAndPutUseJsonBody() {
        for (String verb : List.of("post", "put")) {
            String uri = "/api/coba/aja/" + verb + "Method";
            WebTestClient.RequestBodySpec spec = verb.equals("post") ? client.post().uri(uri) : client.put().uri(uri);
            spec.header("Authorization", token("ROLE_USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("n", 1))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().jsonPath("$.data.data.n").isEqualTo(1);
        }
        // body is optional
        client.post().uri("/api/coba/aja/postMethod").header("Authorization", token("ROLE_USER"))
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.data.data").isEmpty();
    }

    @Test
    void wrongVerbIs405AndUnknownOperationIs404AsProblemDetail() {
        client.post().uri("/api/coba/aja/getMethod").header("Authorization", token("ROLE_USER"))
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.message").isEqualTo("error.methodnotallowed");

        for (String path : List.of("/api/coba/aja/nope", "/api/user/repository/findAll", "/api/coba/aja/GETMETHOD")) {
            client.get().uri(path).header("Authorization", token("ROLE_USER"))
                    .exchange().expectStatus().isNotFound();
        }
    }

    @Test
    void malformedJsonIs400() {
        client.post().uri("/api/coba/aja/postMethod").header("Authorization", token("ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{not json")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void deleteDispatchesToDeleteExecutionAndHonoursPreAuthorize() {
        client.delete().uri("/api/coba/aja/deleteMethod?id=7")
                .header("Authorization", token("ROLE_USER"))
                .exchange().expectStatus().isForbidden();

        client.delete().uri("/api/coba/aja/deleteMethod?id=7")
                .header("Authorization", token("ROLE_ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.message").isEqualTo("Delete method success")
                .jsonPath("$.data.data.id").isEqualTo("7");
    }

    @Test
    void executionIsAuditedWithPrincipal() {
        client.get().uri("/api/coba/aja/getMethod")
                .header("Authorization", token("ROLE_USER"))
                .exchange().expectStatus().isOk();

        ArgumentCaptor<AuditEventModel> captor = ArgumentCaptor.forClass(AuditEventModel.class);
        verify(auditRepository, timeout(2000)).save(captor.capture());
        AuditEventModel event = captor.getValue();
        assertThat(event.getPrincipal()).isEqualTo("alice");
        assertThat(event.getMethodName()).isEqualTo("getMethod");
        assertThat(event.isError()).isFalse();
        assertThat(event.getExecutionTime()).isGreaterThanOrEqualTo(0);
    }

    /** Regression: request data used to be stored in fields of the singleton bean. */
    @Test
    void concurrentRequestsDoNotShareData() {
        String auth = token("ROLE_USER");
        List<Integer> ids = IntStream.range(0, 50).boxed().toList();
        reactor.core.publisher.Flux.fromIterable(ids)
                .flatMap(i -> client.post().uri("/api/coba/aja/postMethod").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("i", i))
                        .exchange()
                        .expectBody(Map.class).returnResult().getResponseBody() instanceof Map<?, ?> body
                        ? Mono.just(Map.entry(i, body)) : Mono.empty(), 16)
                .doOnNext(e -> assertThat(((Map<?, ?>) ((Map<?, ?>) e.getValue().get("data")).get("data")).get("i"))
                        .isEqualTo(e.getKey()))
                .blockLast();
    }
}
