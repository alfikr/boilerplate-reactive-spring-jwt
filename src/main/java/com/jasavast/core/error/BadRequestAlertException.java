package com.jasavast.core.error;

import com.jasavast.core.util.HeaderUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * Base class of the application's business errors. Rendered as RFC 9457 problem detail
 * ({@code application/problem+json}) with {@code message} and {@code params} properties for
 * client-side translation, plus {@code X-<app>-error}/{@code X-<app>-params} alert headers.
 */
public class BadRequestAlertException extends ErrorResponseException {

    private final String entityName;
    private final String errorKey;

    public BadRequestAlertException(String defaultMessage, String entityName, String errorKey) {
        this(ErrorConstants.DEFAULT_TYPE, defaultMessage, entityName, errorKey);
    }

    public BadRequestAlertException(URI type, String defaultMessage, String entityName, String errorKey) {
        this(HttpStatus.BAD_REQUEST, type, defaultMessage, entityName, errorKey);
    }

    protected BadRequestAlertException(HttpStatus status, URI type, String defaultMessage, String entityName, String errorKey) {
        super(status, problem(status, type, defaultMessage, entityName, errorKey), null);
        this.entityName = entityName;
        this.errorKey = errorKey;
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String detail, String entityName, String errorKey) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("message", "error." + errorKey);
        problem.setProperty("params", entityName);
        return problem;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }

    @Override
    public String getMessage() {
        return getBody().getDetail();
    }

    @Override
    public HttpHeaders getHeaders() {
        return HeaderUtil.createFailureAlert(ErrorConstants.APPLICATION_NAME, false, entityName, errorKey, getBody().getDetail());
    }
}
