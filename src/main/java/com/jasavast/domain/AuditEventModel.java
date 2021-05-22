package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("jvs-audit")
public class AuditEventModel {
    @Id
    @Column("event_id")
    private String id;

    @NotNull
    private String principal;

    @Column("event_date")
    private Instant auditEventDate;

    @Column("event_type")
    private String auditEventType;
    @Transient
    private Map<String, String> data = new HashMap<>();
}
