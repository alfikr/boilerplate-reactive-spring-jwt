package com.jasavast.core.error;

import com.jasavast.core.util.HeaderUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.zalando.problem.*;
import org.zalando.problem.spring.webflux.advice.ProblemHandling;
import org.zalando.problem.spring.webflux.advice.security.SecurityAdviceTrait;
import org.zalando.problem.violations.ConstraintViolationProblem;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ControllerAdvice
public class ExceptionTranslator implements ProblemHandling, SecurityAdviceTrait {
    private static final String FIELD_ERRORS_KEY = "fieldErrors";
    private static final String MESSAGE_KEY = "message";
    private static final String PATH_KEY = "path";
    private static final String VIOLATIONS_KEY = "violations";
    private final String applicationName="Jasavast";

    @Override
    public Mono<ResponseEntity<Problem>> process(ResponseEntity<Problem> entity, ServerWebExchange request) {
        if (entity==null){
            return Mono.empty();
        }
        Problem problem= entity.getBody();
        if (!(problem instanceof ConstraintViolationProblem || problem instanceof DefaultProblem)) {
            return Mono.just(entity);
        }
        ProblemBuilder builder = Problem.builder()
                .withType(Problem.DEFAULT_TYPE.equals(problem.getType()) ? ErrorConstants.DEFAULT_TYPE : problem.getType())
                .withStatus(problem.getStatus())
                .withTitle(problem.getTitle())
                .with(PATH_KEY, request.getRequest().getPath().value());

        if (problem instanceof ConstraintViolationProblem) {
            builder
                    .with(VIOLATIONS_KEY, ((ConstraintViolationProblem) problem).getViolations())
                    .with(MESSAGE_KEY, ErrorConstants.ERR_VALIDATION);
        } else {
            builder
                    .withCause(((DefaultProblem) problem).getCause())
                    .withDetail(problem.getDetail())
                    .withInstance(problem.getInstance());
            problem.getParameters().forEach(builder::with);
            if (!problem.getParameters().containsKey(MESSAGE_KEY) && problem.getStatus() != null) {
                builder.with(MESSAGE_KEY, "error.http." + problem.getStatus().getStatusCode());
            }
        }
        return Mono.just(new ResponseEntity<>(builder.build(), entity.getHeaders(), entity.getStatusCode()));
    }

    @Override
    public Mono<ResponseEntity<Problem>> handleBindingResult(WebExchangeBindException ex, ServerWebExchange request) {
        BindingResult result = ex.getBindingResult();
        List<FieldErrorVM> fieldErrors = result.getFieldErrors().stream()
                .map(f -> new FieldErrorVM(f.getObjectName().replaceFirst("DTO$", ""), f.getField(), f.getCode()))
                .collect(Collectors.toList());

        Problem problem = Problem.builder()
                .withType(ErrorConstants.CONSTRAINT_VIOLATION_TYPE)
                .withTitle("Data binding and validation failure")
                .withStatus(Status.BAD_REQUEST)
                .with(MESSAGE_KEY, ErrorConstants.ERR_VALIDATION)
                .with(FIELD_ERRORS_KEY, fieldErrors)
                .build();
        return create(ex, problem, request);
    }

    @Override
    public ProblemBuilder prepare(Throwable throwable, StatusType status, URI type) {
        return Problem.builder()
                .withType(type)
                .withStatus(status)
                .withTitle(status.getReasonPhrase())
                .withDetail(throwable.getMessage())
                .withCause(Optional.ofNullable(throwable.getCause())
                        .filter(cause->isCausalChainsEnabled())
                        .map(this::toProblem)
                        .orElse(null));
    }

    private boolean containsPackageName(String message) {

        // This list is for sure not complete
        return StringUtils.containsAny(message, "org.", "java.", "net.", "javax.", "com.", "io.", "de.", "com.jasavast.ppob");
    }

//    register custom exception

    @ExceptionHandler
    public Mono<ResponseEntity<Problem>> handleUsernameAlreadyUsedException(UsernameAlreadyUsedException ex, ServerWebExchange request) {
        UsernameAlreadyUsedException problem = new UsernameAlreadyUsedException();
        return create(problem, request, HeaderUtil.createFailureAlert(applicationName,  false, problem.getEntityName(), problem.getErrorKey(), problem.getMessage()));
    }


    @ExceptionHandler
    public Mono<ResponseEntity<Problem>> handleBadRequestAlertException(BadRequestAlertException ex, ServerWebExchange request) {
        return create(ex, request, HeaderUtil.createFailureAlert(applicationName, false, ex.getEntityName(), ex.getErrorKey(), ex.getMessage()));
    }

    @ExceptionHandler
    public Mono<ResponseEntity<Problem>> handleConcurrencyFailure(ConcurrencyFailureException ex, ServerWebExchange request) {
        Problem problem = Problem.builder()
                .withStatus(Status.CONFLICT)
                .with(MESSAGE_KEY, ErrorConstants.ERR_CONCURRENCY_FAILURE)
                .build();
        return create(ex, problem, request);
    }

}
