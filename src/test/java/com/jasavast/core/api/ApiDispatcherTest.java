package com.jasavast.core.api;

import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.error.MethodNotAllowedException;
import com.jasavast.core.error.MethodNotFoundException;
import com.jasavast.service.ApiAbstract;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiDispatcherTest {

    public static class GoodApi extends ApiAbstract {
        @GetExecution
        public Mono<String> hello(ApiRequest req) {
            return Mono.just("hello " + req.data().get("name").asString());
        }

        @PostExecution(id = "create")
        public Mono<String> doCreate(ApiRequest req) {
            return Mono.just("created");
        }

        @GetExecution
        public Mono<String> boom(ApiRequest req) {
            throw new IllegalStateException("sync failure");
        }

        public Mono<String> notExposed(ApiRequest req) {
            return Mono.just("secret");
        }
    }

    public static class BadSignatureApi extends ApiAbstract {
        @GetExecution
        public String wrong() {
            return "x";
        }
    }

    private static ApiDispatcher dispatcherWith(String beanName, Class<?> beanClass) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(beanName, beanClass);
        context.registerBean("notAnApi", String.class, () -> "plain bean");
        context.refresh();
        return new ApiDispatcher(context);
    }

    private static ApiDispatcher good() {
        return dispatcherWith("apiCobaAja", GoodApi.class);
    }

    private static ApiRequest req() {
        return new ApiRequest(JsonNodeFactory.instance.objectNode().put("name", "bob"), new LinkedMultiValueMap<>(), null);
    }

    private static Mono<Object> call(ApiDispatcher d, HttpMethod verb, String module, String sub, String action) {
        return d.dispatch(verb, module, sub, action, req()).cast(Object.class);
    }

    @Test
    void registersAnnotatedOperationsUsingIdWhenPresent() {
        assertThat(good().operations()).containsExactly(
                "GET apicobaaja.boom", "GET apicobaaja.hello", "POST apicobaaja.create");
    }

    @Test
    void dispatchesByPathCaseInsensitivelyAndVerb() {
        StepVerifier.create(call(good(), HttpMethod.GET, "coba", "aja", "hello")).expectNext("hello bob").verifyComplete();
        StepVerifier.create(call(good(), HttpMethod.GET, "Coba", "AJA", "hello")).expectNext("hello bob").verifyComplete();
        StepVerifier.create(call(good(), HttpMethod.POST, "coba", "aja", "create")).expectNext("created").verifyComplete();
    }

    @Test
    void wrongVerbIsMethodNotAllowed() {
        StepVerifier.create(call(good(), HttpMethod.POST, "coba", "aja", "hello"))
                .expectError(MethodNotAllowedException.class).verify();
    }

    @Test
    void unknownOrUnannotatedOperationsAreNotFound() {
        ApiDispatcher d = good();
        String[][] cases = {{"coba", "aja", "notExposed"}, {"coba", "aja", "Hello"}, {"coba", "aja", "doCreate"},
                {"coba", "lain", "hello"}, {"not", "anapi", "toString"}};
        for (String[] c : cases) {
            StepVerifier.create(call(d, HttpMethod.GET, c[0], c[1], c[2])).expectError(MethodNotFoundException.class).verify();
        }
    }

    @Test
    void synchronousHandlerExceptionBecomesErrorSignal() {
        StepVerifier.create(call(good(), HttpMethod.GET, "coba", "aja", "boom")).expectErrorMessage("sync failure").verify();
    }

    @Test
    void invalidSignatureOrBeanNameFailsAtStartup() {
        assertThatThrownBy(() -> dispatcherWith("apiBad", BadSignatureApi.class))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("must have signature");
        assertThatThrownBy(() -> dispatcherWith("cobaAja", GoodApi.class))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("must be named");
    }
}
