package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class AuditEventModel {
    @Id
    @Field("event_id")
    private String id;

    @NotNull
    private String principal;

    @Field("event_date")
    private Instant auditEventDate;

    @Field("event_type")
    private String auditEventType;
    @Transient
    private Map<String, String> data = new HashMap<>();
}
