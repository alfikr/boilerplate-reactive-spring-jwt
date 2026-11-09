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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * login and email are stored lower-cased; the unique indexes are the real guard against
 * concurrent duplicate registrations (the existence check in AccountService is only a
 * fast path for a friendly error).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String login;

    @JsonIgnore
    @ToString.Exclude
    private String password;

    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;

    private boolean aktif;

    /** SHA-256 of the activation key (see RandomUtil#sha256). */
    @JsonIgnore
    @ToString.Exclude
    @Indexed(sparse = true)
    private String activationKeyHash;

    /** SHA-256 of the password reset key. */
    @JsonIgnore
    @ToString.Exclude
    @Indexed(sparse = true)
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

    @Builder.Default
    private Set<Authority> authorities = new HashSet<>();
}
