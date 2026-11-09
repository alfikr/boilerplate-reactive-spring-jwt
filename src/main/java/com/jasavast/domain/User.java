package com.jasavast.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Row of {@code users}. R2DBC has no relationship mapping: authorities live in
 * {@code user_authority} and are read with explicit queries (see UserRepository).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User {
    @Id
    private Long id;

    private String login;

    @JsonIgnore
    @ToString.Exclude
    private String password;

    private String firstName;
    private String lastName;
    private String email;
    private boolean aktif;

    /** SHA-256 of the activation key (see RandomUtil#sha256). */
    @JsonIgnore
    @ToString.Exclude
    private String activationKeyHash;

    /** SHA-256 of the password reset key. */
    @JsonIgnore
    @ToString.Exclude
    private String resetKeyHash;

    private Instant resetDate;

    @CreatedDate
    private Instant insOn;
    @CreatedBy
    private String createBy;
    @LastModifiedDate
    private Instant modOn;
    @LastModifiedBy
    private String modBy;
}
