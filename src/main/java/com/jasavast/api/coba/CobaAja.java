package com.jasavast.api.coba;

import com.jasavast.core.annotation.DeleteExecution;
import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.annotation.PutExecution;
import com.jasavast.core.api.ApiRequest;
import com.jasavast.core.api.ApiResponse;
import com.jasavast.service.ApiAbstract;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;

/**
 * Example API bean ({@code apiCobaAja} → module {@code coba}, sub-module {@code aja}):
 * <pre>
 * GET    /api/coba/aja/getMethod?foo=bar
 * POST   /api/coba/aja/postMethod     {...}
 * PUT    /api/coba/aja/putMethod      {...}
 * DELETE /api/coba/aja/deleteMethod?id=1      (ROLE_ADMIN)
 * GET    /api/coba/aja/dbTime                 (explicit SQL via R2DBC)
 * </pre>
 */
@Component("apiCobaAja")
public class CobaAja extends ApiAbstract {

    @GetExecution
    public Mono<ApiResponse<ObjectNode>> getMethod(ApiRequest request) {
        return Mono.just(success(echo("Get method success", request)));
    }

    @PostExecution
    public Mono<ApiResponse<ObjectNode>> postMethod(ApiRequest request) {
        return Mono.just(success(echo("Post method success", request)));
    }

    @PutExecution
    public Mono<ApiResponse<ObjectNode>> putMethod(ApiRequest request) {
        return Mono.just(success(echo("Put method success", request)));
    }

    /** Example of per-operation authorization on top of "authenticated". */
    @DeleteExecution
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ApiResponse<ObjectNode>> deleteMethod(ApiRequest request) {
        return Mono.just(success(echo("Delete method success", request)));
    }

    /** Example of explicit SQL through the R2DBC DatabaseClient provided by ApiAbstract. */
    @GetExecution
    public Mono<ApiResponse<ObjectNode>> dbTime(ApiRequest request) {
        return db().sql("SELECT now() AS db_time")
                .map(row -> row.get("db_time", OffsetDateTime.class))
                .one()
                .map(time -> success(object().put("dbTime", time.toString())));
    }

    private ObjectNode echo(String message, ApiRequest request) {
        ObjectNode node = object().put("message", message);
        node.set("data", request.data());
        return node;
    }
}
