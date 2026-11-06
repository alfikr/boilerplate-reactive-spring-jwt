package com.jasavast.resource;

import com.jasavast.core.api.ApiDispatcher;
import com.jasavast.core.error.MethodNotFoundException;
import com.jasavast.resource.vm.ReqVM;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Single entry point of the RPC-style API: {@code /api/request} with
 * {@code method = "<beanName>.<operation>"}. The HTTP verb must match the operation's
 * {@code @Get/@Post/@Put/@DeleteExecution} annotation.
 * <ul>
 *   <li>GET / DELETE: {@code ?method=...&k=v}; other query parameters become {@code data}
 *       (first value of each parameter, as strings).</li>
 *   <li>POST / PUT: JSON body {@code {"method": "...", "data": {...}}}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/request")
@RequiredArgsConstructor
@Slf4j
public class CoreApi {

    private static final String METHOD_PARAM = "method";

    private final ApiDispatcher dispatcher;

    @GetMapping
    public Mono<?> getMethod(ServerHttpRequest request) {
        return fromQuery(request).flatMap(req -> dispatch(HttpMethod.GET, req));
    }

    @PostMapping
    public Mono<?> postMethod(@Valid @RequestBody ReqVM req) {
        return dispatch(HttpMethod.POST, req);
    }

    @PutMapping
    public Mono<?> putMethod(@Valid @RequestBody ReqVM req) {
        return dispatch(HttpMethod.PUT, req);
    }

    @DeleteMapping
    public Mono<?> deleteMethod(ServerHttpRequest request) {
        return fromQuery(request).flatMap(req -> dispatch(HttpMethod.DELETE, req));
    }

    private Mono<?> dispatch(HttpMethod verb, ReqVM req) {
        log.debug("{} /api/request method={}", verb, req.getMethod());
        return dispatcher.dispatch(verb, req);
    }

    private static Mono<ReqVM> fromQuery(ServerHttpRequest request) {
        MultiValueMap<String, String> params = request.getQueryParams();
        String method = params.getFirst(METHOD_PARAM);
        if (method == null || method.isBlank()) {
            return Mono.error(new MethodNotFoundException());
        }
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        params.forEach((name, values) -> {
            if (!METHOD_PARAM.equals(name) && !values.isEmpty()) {
                data.put(name, values.getFirst());
            }
        });
        return Mono.just(new ReqVM(method, data));
    }
}
