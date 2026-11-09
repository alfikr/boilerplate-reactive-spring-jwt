package com.jasavast.core.api;

import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import tools.jackson.databind.JsonNode;

/**
 * Per-request input of an API operation. Passed as a method argument because API beans are
 * singletons shared by concurrent requests.
 *
 * @param data        JSON body for POST/PUT; for GET/DELETE an object built from the query parameters
 * @param queryParams raw query parameters (all verbs)
 * @param exchange    access to request/response headers, status, attributes
 */
public record ApiRequest(JsonNode data, MultiValueMap<String, String> queryParams, ServerWebExchange exchange) {
}
