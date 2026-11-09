package com.jasavast.core.security;

import com.jasavast.domain.Authority;
import com.jasavast.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Loads users for {@code UserDetailsRepositoryReactiveAuthenticationManager}.
 * <ul>
 *   <li>Unknown user → empty Mono → the manager raises {@code BadCredentialsException}
 *       (same response as a wrong password: no user enumeration).</li>
 *   <li>Not activated → {@code disabled} → the manager raises {@code DisabledException},
 *       checked after the password so it is only revealed to the account owner.</li>
 * </ul>
 */
@Component("userDetailsService")
@RequiredArgsConstructor
public class SecurityUserService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String login) {
        return userRepository.findOneByLogin(login.toLowerCase(Locale.ROOT))
                .map(user -> User.withUsername(user.getLogin())
                        .password(user.getPassword())
                        .disabled(!user.isAktif())
                        .authorities(user.getAuthorities().stream().map(Authority::getName).toArray(String[]::new))
                        .build());
    }
}
