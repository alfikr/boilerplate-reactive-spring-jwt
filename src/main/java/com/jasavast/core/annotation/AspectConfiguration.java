package com.jasavast.core.annotation;

import com.jasavast.core.security.SecurityUtils;
import com.jasavast.domain.AuditEventModel;
import com.jasavast.repository.AuditAspectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Execution log for every API operation annotated with {@link GetExecution},
 * {@link PostExecution}, {@link PutExecution} or {@link DeleteExecution}.
 * <p>
 * Timing starts at <em>subscription</em> and ends at completion/error of the returned
 * {@link Mono}; measuring around {@code proceed()} would only measure assembly.
 * <p>
 * The log write is fire-and-forget: a slow or failing MongoDB write must not add
 * latency to, or fail, the business request. Trade-off: entries can be lost if the
 * process dies before the write completes. Request payloads are deliberately not
 * stored (they may contain credentials or PII).
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AspectConfiguration {

    private static final String ANONYMOUS = "anonymous";

    private final AuditAspectRepository auditAspectRepository;

    @Around("@annotation(com.jasavast.core.annotation.GetExecution) || " +
            "@annotation(com.jasavast.core.annotation.PostExecution) || " +
            "@annotation(com.jasavast.core.annotation.PutExecution) || " +
            "@annotation(com.jasavast.core.annotation.DeleteExecution)")
    public Object proses(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object result = joinPoint.proceed();
        if (!(result instanceof Mono<?> mono)) {
            // ApiDispatcher rejects non-Mono operations at startup; nothing to time here.
            return result;
        }
        return SecurityUtils.getCurrentUserLogin()
                .defaultIfEmpty(ANONYMOUS)
                .flatMap(principal -> {
                    long start = System.currentTimeMillis();
                    return mono
                            .doOnSuccess(v -> save(principal, className, methodName, start, null))
                            .doOnError(e -> save(principal, className, methodName, start, e));
                });
    }

    private void save(String principal, String className, String methodName, long start, Throwable error) {
        long end = System.currentTimeMillis();
        AuditEventModel event = AuditEventModel.builder()
                .principal(principal)
                .auditEventDate(Instant.ofEpochMilli(end))
                .logType("log")
                .title("executed method")
                .className(className)
                .methodName(methodName)
                .executionStart(start)
                .executionEnd(end)
                .executionTime(end - start)
                .error(error != null)
                .errorMessage(error != null ? error.getMessage() : null)
                .message(error != null ? error.getClass().getName() : "ok")
                .build();
        auditAspectRepository.save(event)
                .subscribe(null, e -> log.warn("Failed to write execution log for {}.{}: {}", className, methodName, e.toString()));
    }
}
