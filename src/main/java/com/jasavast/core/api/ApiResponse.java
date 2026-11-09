package com.jasavast.core.api;

import java.util.List;

/**
 * Success envelope returned by API operations. Errors are not wrapped here; they are
 * returned as problem details by {@code ExceptionTranslator}.
 */
public record ApiResponse<T>(boolean success, T data, List<Object> errors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, List.of());
    }
}
