package com.jasavast.service.dto;

import com.jasavast.domain.User;

import java.util.Set;

/** Read model of the current account (never exposes password or one-time key hashes). */
public record UserDTO(Long id, String login, String firstName, String lastName, String email,
                      boolean aktif, Set<String> authorities) {

    public static UserDTO from(User user, Set<String> authorities) {
        return new UserDTO(user.getId(), user.getLogin(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.isAktif(), Set.copyOf(authorities));
    }
}
