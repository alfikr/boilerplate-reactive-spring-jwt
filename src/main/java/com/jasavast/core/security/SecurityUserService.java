package com.jasavast.core.security;

import com.jasavast.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Loads users for {@code UserDetailsRepositoryReactiveAuthenticationManager} with one query.
 * <ul>
 *   <li>Unknown user → empty Mono → {@code BadCredentialsException} (same as wrong password).</li>
 *   <li>Not activated → {@code disabled} → {@code DisabledException}, checked after the password.</li>
 * </ul>
 */
@Component("userDetailsService")
@RequiredArgsConstructor
public class SecurityUserService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String login) {
        return userRepository.findCredentialsByLogin(login.toLowerCase(Locale.ROOT))
                .map(user -> User.withUsername(user.login())
                        .password(user.password())
                        .disabled(!user.aktif())
                        .authorities(user.authorities())
                        .build());
    }
}
