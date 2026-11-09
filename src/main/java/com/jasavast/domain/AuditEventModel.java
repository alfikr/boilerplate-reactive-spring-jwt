package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/** Execution log written by {@code AspectConfiguration}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("jvs_audit")
public class AuditEventModel {
    @Id
    @Column("event_id")
    private Long id;
    private String principal;
    @Column("event_date")
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
