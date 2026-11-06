package com.jasavast.service;

import com.jasavast.core.api.ApiResponse;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base class of beans exposed through {@code /api/request}. A subclass registered as a Spring
 * bean exposes every method annotated with {@code @GetExecution}, {@code @PostExecution},
 * {@code @PutExecution} or {@code @DeleteExecution} as operation {@code "<beanName>.<method>"}.
 * <p>
 * Required signature: {@code public Mono<?> name(ReqVM request)}. Beans are singletons and
 * shared by concurrent requests, so request data must flow through the parameter, never
 * through instance fields.
 */
public abstract class ApiAbstract {

    protected static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    protected <T> ApiResponse<T> success(T data) {
        return ApiResponse.ok(data);
    }

    protected ObjectNode object() {
        return JSON.objectNode();
    }
}
