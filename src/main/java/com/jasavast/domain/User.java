package com.jasavast.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table
public class User {
    @Id
    private String id;
    private String login;
    private String password;
    private String email;
    private boolean aktif;
    private Set<Authority> authorities= new HashSet<>();
}
