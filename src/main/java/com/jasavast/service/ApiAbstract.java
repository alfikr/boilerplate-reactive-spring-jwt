package com.jasavast.service;

import com.jasavast.core.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Base class of beans exposed through {@code /api/{module}/{subModule}/{action}}. A subclass
 * registered as bean {@code api<Module><SubModule>} exposes every method annotated with
 * {@code @GetExecution}, {@code @PostExecution}, {@code @PutExecution} or {@code @DeleteExecution}.
 * <p>
 * Required signature: {@code public Mono<?> action(ApiRequest request)}. Beans are singletons
 * shared by concurrent requests, so request data must flow through the parameter, never
 * through instance fields.
 * <p>
 * {@link #template()} / {@link #db()} give direct R2DBC access for operations that are better
 * expressed as explicit SQL than as repository methods.
 */
public abstract class ApiAbstract {

    protected static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private R2dbcEntityTemplate template;

    @Autowired
    void setTemplate(R2dbcEntityTemplate template) {
        this.template = template;
    }

    protected R2dbcEntityTemplate template() {
        return template;
    }

    protected DatabaseClient db() {
        return template.getDatabaseClient();
    }

    protected <T> ApiResponse<T> success(T data) {
        return ApiResponse.ok(data);
    }

    protected ObjectNode object() {
        return JSON.objectNode();
    }
}
