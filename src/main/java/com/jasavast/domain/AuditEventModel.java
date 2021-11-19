package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("jvs-audit")
@Builder
public class AuditEventModel {
    @Id
    @Column("event_id")
    private String id;

    @NotNull
    private String principal;

    @Column("event_date")
    @Builder.Default
    private LocalDateTime auditEventDate=LocalDateTime.now();

    @Column("event_type")
    private String auditEventType;
    @Column("log_type")
    private String logType;
    @Column("execution_time")
    private long executionTime;
    @Column
    private boolean error;
    @Column
    private long executionStart;
    private String errorMessage;
    @Column
    private long executionEnd;
    private String className;
    private String methodName;
    private String message;
    private String title;
    @Transient
    private Map<String, String> data = new HashMap<>();
}
