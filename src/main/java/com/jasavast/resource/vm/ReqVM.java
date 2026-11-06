package com.jasavast.resource.vm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * Envelope of every call to {@code /api/request}.
 *
 * @see com.jasavast.core.api.ApiDispatcher
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReqVM {
    /** {@code "<beanName>.<operation>"}, e.g. {@code apiCobaAja.getMethod}. */
    @NotBlank
    private String method;
    /** Operation payload. For GET/DELETE it is built from the query parameters. */
    @NotNull
    private JsonNode data;
}
