package com.jasavast.service;

import com.jasavast.core.error.EmailAlreadyUsedException;
import com.jasavast.core.error.InvalidKeyException;
import com.jasavast.core.error.LoginNotAlreadyRegisteredException;
import com.jasavast.core.error.UsernameAlreadyUsedException;
import com.jasavast.core.security.AuthorityConstants;
import com.jasavast.core.security.SecurityUtils;
import com.jasavast.core.util.RandomUtil;
import com.jasavast.domain.User;
import com.jasavast.repository.UserRepository;
import com.jasavast.resource.vm.ForgotPasswordVM;
import com.jasavast.resource.vm.UserVM;
import com.jasavast.service.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;

/**
 * Transaction boundaries are explicit ({@link TransactionalOperator}) and contain database
 * work only: BCrypt runs before the transaction and e-mail is sent after commit, so no
 * connection is held during CPU work or SMTP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    static final Duration RESET_KEY_VALIDITY = Duration.ofDays(1);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TransactionalOperator transactionalOperator;
    private final MailService mailService;
    private final Clock clock;

    /**
     * Creates an inactive account and e-mails an activation key.
     * <p>
     * In one transaction: delete abandoned (inactive) registrations holding the same login or
     * email, insert the user, insert its role. Active owners are protected by the unique
     * constraints; a violation rolls everything back and is reported as 409.
     */
    public Mono<User> register(UserVM vm) {
        String login = vm.getLogin().toLowerCase(Locale.ROOT);
        String email = vm.getEmail().toLowerCase(Locale.ROOT);
        String activationKey = RandomUtil.generateKey();

        Mono<User> insert = Mono.fromCallable(() -> passwordEncoder.encode(vm.getPassword()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(hash -> userRepository.deleteInactiveByLoginOrEmail(login, email)
                        .then(userRepository.save(User.builder()
                                .login(login)
                                .email(email)
                                .firstName(vm.getFirstName())
                                .lastName(vm.getLastName())
                                .password(hash)
                                .aktif(false)
                                .activationKeyHash(RandomUtil.sha256(activationKey))
                                .build()))
                        .flatMap(user -> userRepository.addAuthority(user.getId(), AuthorityConstants.USER).thenReturn(user))
                        .as(transactionalOperator::transactional));

        return insert
                .onErrorMap(DuplicateKeyException.class, AccountService::toConflict)
                .doOnNext(user -> log.info("Created user {}", user.getLogin()))
                .flatMap(user -> mailService.sendActivationEmail(user, activationKey).thenReturn(user));
    }

    public Mono<Void> activate(String key) {
        return userRepository.activate(RandomUtil.sha256(key), clock.instant())
                .flatMap(rows -> rows == 1 ? Mono.<Void>empty() : Mono.error(new InvalidKeyException("Activation")));
    }

    /**
     * Completes empty whether or not the account exists, so the endpoint cannot be used
     * to discover registered emails/logins. Only active accounts receive a reset key.
     */
    public Mono<Void> requestPasswordReset(ForgotPasswordVM vm) {
        Mono<User> user = StringUtils.isNotBlank(vm.getEmail())
                ? userRepository.findOneByEmail(vm.getEmail().trim().toLowerCase(Locale.ROOT))
                : userRepository.findOneByLogin(vm.getLogin().trim().toLowerCase(Locale.ROOT));
        return user
                .filter(User::isAktif)
                .flatMap(found -> {
                    String resetKey = RandomUtil.generateKey();
                    return userRepository.setResetKey(found.getId(), RandomUtil.sha256(resetKey), clock.instant())
                            .then(mailService.sendPasswordResetMail(found, resetKey));
                })
                .then();
    }

    /**
     * The new password is hashed before the lookup, so valid and invalid keys cost the same
     * time; the conditional UPDATE validates key, expiry and status atomically.
     */
    public Mono<Void> completePasswordReset(String key, String newPassword) {
        Instant now = clock.instant();
        return Mono.fromCallable(() -> passwordEncoder.encode(newPassword))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(hash -> userRepository.completeReset(RandomUtil.sha256(key), hash, now.minus(RESET_KEY_VALIDITY), now))
                .flatMap(rows -> rows == 1 ? Mono.<Void>empty() : Mono.error(new InvalidKeyException("Reset")));
    }

    public Mono<UserDTO> getCurrentAccount() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .flatMap(user -> userRepository.findAuthorityNames(user.getId())
                        .collect(HashSet<String>::new, HashSet::add)
                        .map(authorities -> UserDTO.from(user, authorities)))
                .switchIfEmpty(Mono.error(LoginNotAlreadyRegisteredException::new));
    }

    private static Throwable toConflict(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        return message.contains("ux_users_email") ? new EmailAlreadyUsedException() : new UsernameAlreadyUsedException();
    }
}
