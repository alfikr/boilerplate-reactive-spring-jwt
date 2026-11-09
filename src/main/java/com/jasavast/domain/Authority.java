package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Reference data, seeded by the Flyway migration (read-only for the application). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("authority")
public class Authority {
    @Id
    private String name;
}
