package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document
public class AuditEventModel {
    @Id
    private String id;

    @NotNull
    private String principal;

    @Builder.Default
    private LocalDateTime auditEventDate=LocalDateTime.now();
    private String auditEventType;
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
    private Map<String,String> data = new HashMap<>();

}
