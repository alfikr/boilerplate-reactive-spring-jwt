package com.jasavast.service.dto;

import com.jasavast.domain.Authority;
import com.jasavast.domain.User;

import java.util.Set;
import java.util.stream.Collectors;

/** Read model of the current account (never exposes password or one-time key hashes). */
public record UserDTO(String id, String login, String firstName, String lastName, String email,
                      boolean aktif, Set<String> authorities) {

    public static UserDTO from(User user) {
        return new UserDTO(user.getId(), user.getLogin(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.isAktif(), user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toUnmodifiableSet()));
    }
}
