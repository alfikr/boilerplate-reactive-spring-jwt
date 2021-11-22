package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
@Builder
public class User {
    @Id
    private String id;
    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private boolean aktif;
    private LocalDateTime insOn;
    private String createBy;
    private LocalDateTime modOn;
    private String modBy;
    private Set<Authority> authorities= new HashSet<>();
}
