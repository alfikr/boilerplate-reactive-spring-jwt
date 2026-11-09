package com.jasavast.repository;

import com.jasavast.domain.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * State transitions (activate, reset) are single conditional UPDATEs: the WHERE clause is
 * the check, so a key cannot be used twice by concurrent requests and no read-modify-write
 * race exists. Callers treat "0 rows updated" as an invalid key.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    Mono<User> findOneByLogin(String login);

    Mono<User> findOneByEmail(String email);

    /** Login + authorities in one round trip (uses ux_users_login and the user_authority PK). */
    @Query("""
            SELECT u.login, u.password, u.aktif,
                   COALESCE(array_agg(ua.authority_name) FILTER (WHERE ua.authority_name IS NOT NULL), '{}') AS authorities
            FROM users u
            LEFT JOIN user_authority ua ON ua.user_id = u.id
            WHERE u.login = :login
            GROUP BY u.id""")
    Mono<UserCredentials> findCredentialsByLogin(String login);

    @Query("SELECT authority_name FROM user_authority WHERE user_id = :userId")
    Flux<String> findAuthorityNames(Long userId);

    /** Frees a login/email held by an abandoned (never activated) registration. Cascades to user_authority. */
    @Modifying
    @Query("DELETE FROM users WHERE (login = :login OR email = :email) AND aktif = false")
    Mono<Long> deleteInactiveByLoginOrEmail(String login, String email);

    @Modifying
    @Query("INSERT INTO user_authority (user_id, authority_name) VALUES (:userId, :authority)")
    Mono<Long> addAuthority(Long userId, String authority);

    @Modifying
    @Query("""
            UPDATE users SET aktif = true, activation_key_hash = NULL, mod_on = :now, mod_by = 'system'
            WHERE activation_key_hash = :keyHash AND aktif = false""")
    Mono<Long> activate(String keyHash, Instant now);

    @Modifying
    @Query("UPDATE users SET reset_key_hash = :keyHash, reset_date = :now, mod_on = :now, mod_by = 'system' WHERE id = :id")
    Mono<Long> setResetKey(Long id, String keyHash, Instant now);

    @Modifying
    @Query("""
            UPDATE users SET password = :passwordHash, reset_key_hash = NULL, reset_date = NULL, mod_on = :now, mod_by = 'system'
            WHERE reset_key_hash = :keyHash AND reset_date > :oldestValid AND aktif = true""")
    Mono<Long> completeReset(String keyHash, String passwordHash, Instant oldestValid, Instant now);
}
