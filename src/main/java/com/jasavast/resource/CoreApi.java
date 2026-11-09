package com.jasavast.resource;

import com.jasavast.core.api.ApiDispatcher;
import com.jasavast.core.api.ApiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Generic entry point: {@code /api/{module}/{subModule}/{action}} is dispatched to bean
 * {@code api<module><subModule>}, method {@code action}. The HTTP verb must match the
 * method's {@code @Get/@Post/@Put/@DeleteExecution} annotation.
 * <ul>
 *   <li>POST / PUT: the JSON body is {@code data} (empty object when absent).</li>
 *   <li>GET / DELETE: query parameters become {@code data} (first value of each, as strings).</li>
 * </ul>
 * Example: {@code GET /api/coba/aja/getMethod?foo=bar} → {@code CobaAja#getMethod}.
 */
@RestController
@RequestMapping("/api/{module}/{subModule}/{action}")
@RequiredArgsConstructor
@Slf4j
public class CoreApi {

    private final ApiDispatcher dispatcher;

    @GetMapping
    public Mono<?> get(@PathVariable String module, @PathVariable String subModule, @PathVariable String action,
                       ServerWebExchange exchange) {
        return dispatch(HttpMethod.GET, module, subModule, action, queryAsData(exchange), exchange);
    }

    @DeleteMapping
    public Mono<?> delete(@PathVariable String module, @PathVariable String subModule, @PathVariable String action,
                          ServerWebExchange exchange) {
        return dispatch(HttpMethod.DELETE, module, subModule, action, queryAsData(exchange), exchange);
    }

    @PostMapping
    public Mono<?> post(@PathVariable String module, @PathVariable String subModule, @PathVariable String action,
                        @RequestBody(required = false) JsonNode body, ServerWebExchange exchange) {
        return dispatch(HttpMethod.POST, module, subModule, action, orEmpty(body), exchange);
    }

    @PutMapping
    public Mono<?> put(@PathVariable String module, @PathVariable String subModule, @PathVariable String action,
                       @RequestBody(required = false) JsonNode body, ServerWebExchange exchange) {
        return dispatch(HttpMethod.PUT, module, subModule, action, orEmpty(body), exchange);
    }

    private Mono<?> dispatch(HttpMethod verb, String module, String subModule, String action, JsonNode data,
                             ServerWebExchange exchange) {
        log.debug("{} /api/{}/{}/{}", verb, module, subModule, action);
        ApiRequest request = new ApiRequest(data, exchange.getRequest().getQueryParams(), exchange);
        return dispatcher.dispatch(verb, module, subModule, action, request);
    }

    private static JsonNode orEmpty(JsonNode body) {
        return body == null || body.isNull() || body.isMissingNode() ? JsonNodeFactory.instance.objectNode() : body;
    }

    private static ObjectNode queryAsData(ServerWebExchange exchange) {
        MultiValueMap<String, String> params = exchange.getRequest().getQueryParams();
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        params.forEach((name, values) -> {
            if (!values.isEmpty()) {
                data.put(name, values.getFirst());
            }
        });
        return data;
    }
}
