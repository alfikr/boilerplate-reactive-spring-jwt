package com.jasavast.repository;

/** Projection for authentication: one row per user with authorities aggregated in SQL. */
public record UserCredentials(String login, String password, boolean aktif, String[] authorities) {
}
