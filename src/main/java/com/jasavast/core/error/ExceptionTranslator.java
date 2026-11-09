package com.jasavast.core.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * Translates exceptions into RFC 9457 problem details using Spring's built-in
 * {@link ProblemDetail} support. {@link BadRequestAlertException} and every other
 * {@code ErrorResponseException} are handled by the base class.
 */
@RestControllerAdvice
@Slf4j
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    private static final String MESSAGE_KEY = "message";
    private static final String FIELD_ERRORS_KEY = "fieldErrors";

    @Override
    protected Mono<ResponseEntity<Object>> handleWebExchangeBindException(WebExchangeBindException ex, HttpHeaders headers,
                                                                          HttpStatusCode status, ServerWebExchange exchange) {
        List<FieldErrorVM> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldErrorVM(f.getObjectName().replaceFirst("(DTO|VM)$", ""), f.getField(), f.getCode()))
                .toList();
        ProblemDetail problem = ex.getBody();
        problem.setType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE);
        problem.setTitle("Data binding and validation failure");
        problem.setProperty(MESSAGE_KEY, ErrorConstants.ERR_VALIDATION);
        problem.setProperty(FIELD_ERRORS_KEY, fieldErrors);
        return handleExceptionInternal(ex, problem, headers, status, exchange);
    }

    /** Login failures. One generic message for unknown user / bad password, to avoid user enumeration. */
    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<Object>> handleAuthentication(AuthenticationException ex, ServerWebExchange exchange) {
        String detail = ex instanceof DisabledException ? "User is not activated" : "Bad credentials";
        return problem(ex, HttpStatus.UNAUTHORIZED, ErrorConstants.UNAUTHORIZED_TYPE, detail, "error.http.401", exchange);
    }

    /** {@code @PreAuthorize} failures inside API operations. */
    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<Object>> handleAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        return problem(ex, HttpStatus.FORBIDDEN, ErrorConstants.FORBIDDEN_TYPE, "Access denied", "error.http.403", exchange);
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public Mono<ResponseEntity<Object>> handleConcurrencyFailure(ConcurrencyFailureException ex, ServerWebExchange exchange) {
        log.warn("Concurrency failure: {}", ex.getMessage());
        return problem(ex, HttpStatus.CONFLICT, ErrorConstants.DEFAULT_TYPE, "Concurrent modification",
                ErrorConstants.ERR_CONCURRENCY_FAILURE, exchange);
    }

    private Mono<ResponseEntity<Object>> problem(Exception ex, HttpStatus status, URI type, String detail, String message,
                                                 ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setProperty(MESSAGE_KEY, message);
        return handleExceptionInternal(ex, problem, new HttpHeaders(), status, exchange);
    }
}
