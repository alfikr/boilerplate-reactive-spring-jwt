package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Execution log written by {@code AspectConfiguration}. The collection grows with traffic;
 * add a TTL index on {@code auditEventDate} (or a capped collection) matching your retention policy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("audit_events")
public class AuditEventModel {
    @Id
    private String id;
    private String principal;
    @Builder.Default
    private Instant auditEventDate = Instant.now();
    private String logType;
    private long executionTime;
    private boolean error;
    private long executionStart;
    private String errorMessage;
    private long executionEnd;
    private String className;
    private String methodName;
    private String message;
    private String title;
}
