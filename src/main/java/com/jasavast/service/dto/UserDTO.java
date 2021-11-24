package com.jasavast.service.dto;

import com.jasavast.domain.Authority;
import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class UserDTO {
    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private boolean aktif;
    private Set<Authority> authoritySet= new HashSet<>();
}
