package com.jasavast.core.api;

import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.error.MethodNotAllowedException;
import com.jasavast.core.error.MethodNotFoundException;
import com.jasavast.resource.vm.ReqVM;
import com.jasavast.service.ApiAbstract;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiDispatcherTest {

    public static class GoodApi extends ApiAbstract {
        @GetExecution
        public Mono<String> hello(ReqVM req) {
            return Mono.just("hello " + req.getData().get("name").asString());
        }

        @PostExecution(id = "create")
        public Mono<String> doCreate(ReqVM req) {
            return Mono.just("created");
        }

        @GetExecution
        public Mono<String> boom(ReqVM req) {
            throw new IllegalStateException("sync failure");
        }

        public Mono<String> notExposed(ReqVM req) {
            return Mono.just("secret");
        }
    }

    public static class BadSignatureApi extends ApiAbstract {
        @GetExecution
        public String wrong() {
            return "x";
        }
    }

    private static ApiDispatcher dispatcherWith(Class<?> beanClass) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("api", beanClass);
        context.registerBean("notAnApi", String.class, () -> "plain bean");
        context.refresh();
        return new ApiDispatcher(context);
    }

    private static ReqVM req(String method) {
        return new ReqVM(method, JsonNodeFactory.instance.objectNode().put("name", "bob"));
    }

    @Test
    void registersAnnotatedOperationsUsingIdWhenPresent() {
        assertThat(dispatcherWith(GoodApi.class).operations()).containsExactlyInAnyOrder("api.hello", "api.create", "api.boom");
    }

    @Test
    void dispatchesByNameAndVerb() {
        ApiDispatcher dispatcher = dispatcherWith(GoodApi.class);
        StepVerifier.create(dispatcher.dispatch(HttpMethod.GET, req("api.hello")).cast(Object.class)).expectNext("hello bob").verifyComplete();
        StepVerifier.create(dispatcher.dispatch(HttpMethod.POST, req("api.create")).cast(Object.class)).expectNext("created").verifyComplete();
    }

    @Test
    void wrongVerbIsMethodNotAllowed() {
        StepVerifier.create(dispatcherWith(GoodApi.class).dispatch(HttpMethod.POST, req("api.hello")))
                .expectError(MethodNotAllowedException.class).verify();
    }

    @Test
    void unknownUnannotatedOrNonApiBeansAreNotFound() {
        ApiDispatcher dispatcher = dispatcherWith(GoodApi.class);
        for (String m : new String[]{"api.notExposed", "api.missing", "notAnApi.toString", "nope", "a.b.c"}) {
            StepVerifier.create(dispatcher.dispatch(HttpMethod.GET, req(m)))
                    .expectError(MethodNotFoundException.class).verify();
        }
    }

    @Test
    void synchronousHandlerExceptionBecomesErrorSignal() {
        StepVerifier.create(dispatcherWith(GoodApi.class).dispatch(HttpMethod.GET, req("api.boom")))
                .expectErrorMessage("sync failure").verify();
    }

    @Test
    void invalidSignatureFailsAtStartup() {
        assertThatThrownBy(() -> dispatcherWith(BadSignatureApi.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must have signature");
    }
}
