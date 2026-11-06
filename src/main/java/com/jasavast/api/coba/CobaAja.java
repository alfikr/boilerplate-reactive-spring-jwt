package com.jasavast.api.coba;

import com.jasavast.core.annotation.DeleteExecution;
import com.jasavast.core.annotation.GetExecution;
import com.jasavast.core.annotation.PostExecution;
import com.jasavast.core.annotation.PutExecution;
import com.jasavast.core.api.ApiResponse;
import com.jasavast.resource.vm.ReqVM;
import com.jasavast.service.ApiAbstract;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.node.ObjectNode;

/**
 * Example API bean. Call it with
 * <pre>
 * GET    /api/request?method=apiCobaAja.getMethod&amp;foo=bar
 * POST   /api/request  {"method":"apiCobaAja.postMethod","data":{...}}
 * PUT    /api/request  {"method":"apiCobaAja.putMethod","data":{...}}
 * DELETE /api/request?method=apiCobaAja.deleteMethod&amp;id=1
 * </pre>
 */
@Component("apiCobaAja")
public class CobaAja extends ApiAbstract {

    @GetExecution
    public Mono<ApiResponse<ObjectNode>> getMethod(ReqVM request) {
        return Mono.just(success(echo("Get method success", request)));
    }

    @PostExecution
    public Mono<ApiResponse<ObjectNode>> postMethod(ReqVM request) {
        return Mono.just(success(echo("Post method success", request)));
    }

    @PutExecution
    public Mono<ApiResponse<ObjectNode>> putMethod(ReqVM request) {
        return Mono.just(success(echo("Put method success", request)));
    }

    /** Example of per-operation authorization on top of "authenticated". */
    @DeleteExecution
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ApiResponse<ObjectNode>> deleteMethod(ReqVM request) {
        return Mono.just(success(echo("Delete method success", request)));
    }

    private ObjectNode echo(String message, ReqVM request) {
        ObjectNode node = object().put("message", message);
        node.set("data", request.getData());
        return node;
    }
}
