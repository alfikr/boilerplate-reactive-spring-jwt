package com.jasavast.service;

import com.jasavast.core.error.EmailAlreadyUsedException;
import com.jasavast.core.error.InvalidKeyException;
import com.jasavast.core.error.LoginNotAlreadyRegisteredException;
import com.jasavast.core.error.UsernameAlreadyUsedException;
import com.jasavast.core.security.AuthorityConstants;
import com.jasavast.core.security.SecurityUtils;
import com.jasavast.core.util.RandomUtil;
import com.jasavast.domain.Authority;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    static final Duration RESET_KEY_VALIDITY = Duration.ofDays(1);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final Clock clock;

    /**
     * Creates an inactive account and e-mails an activation key.
     * <p>
     * An existing <em>inactive</em> account holding the same login or email is replaced
     * (abandoned registrations do not block the identifier forever). The pre-checks only
     * produce a friendly error; under concurrent registration the unique indexes decide.
     */
    public Mono<User> register(UserVM vm) {
        String login = vm.getLogin().toLowerCase(Locale.ROOT);
        String email = vm.getEmail().toLowerCase(Locale.ROOT);

        Mono<Void> releaseLogin = userRepository.findOneByLogin(login)
                .flatMap(existing -> existing.isAktif()
                        ? Mono.<Void>error(new UsernameAlreadyUsedException())
                        : userRepository.delete(existing));
        Mono<Void> releaseEmail = userRepository.findOneByEmail(email)
                .flatMap(existing -> existing.isAktif()
                        ? Mono.<Void>error(new EmailAlreadyUsedException())
                        : userRepository.delete(existing));
        // BCrypt is CPU-bound (~100ms); keep it off the event loop.
        Mono<String> passwordHash = Mono.fromCallable(() -> passwordEncoder.encode(vm.getPassword()))
                .subscribeOn(Schedulers.boundedElastic());

        String activationKey = RandomUtil.generateKey();
        return releaseLogin
                .then(releaseEmail)
                .then(passwordHash)
                .map(hash -> {
                    Set<Authority> authorities = new HashSet<>();
                    authorities.add(new Authority(AuthorityConstants.USER));
                    return User.builder()
                            .login(login)
                            .email(email)
                            .firstName(vm.getFirstName())
                            .lastName(vm.getLastName())
                            .password(hash)
                            .aktif(false)
                            .activationKeyHash(RandomUtil.sha256(activationKey))
                            .authorities(authorities)
                            .build();
                })
                .flatMap(userRepository::save)
                .onErrorMap(DuplicateKeyException.class, AccountService::toConflict)
                .doOnNext(user -> log.info("Created user {}", user.getLogin()))
                .flatMap(user -> mailService.sendActivationEmail(user, activationKey).thenReturn(user));
    }

    public Mono<User> activate(String key) {
        return userRepository.findOneByActivationKeyHash(RandomUtil.sha256(key))
                .switchIfEmpty(Mono.error(() -> new InvalidKeyException("Activation")))
                .flatMap(user -> {
                    user.setAktif(true);
                    user.setActivationKeyHash(null);
                    return userRepository.save(user);
                })
                .doOnNext(user -> log.info("Activated user {}", user.getLogin()));
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
                    found.setResetKeyHash(RandomUtil.sha256(resetKey));
                    found.setResetDate(clock.instant());
                    return userRepository.save(found)
                            .flatMap(saved -> mailService.sendPasswordResetMail(saved, resetKey));
                })
                .then();
    }

    public Mono<Void> completePasswordReset(String key, String newPassword) {
        Instant oldestValid = clock.instant().minus(RESET_KEY_VALIDITY);
        return userRepository.findOneByResetKeyHash(RandomUtil.sha256(key))
                .filter(user -> user.getResetDate() != null && user.getResetDate().isAfter(oldestValid))
                .switchIfEmpty(Mono.error(() -> new InvalidKeyException("Reset")))
                .flatMap(user -> Mono.fromCallable(() -> passwordEncoder.encode(newPassword))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(hash -> {
                            user.setPassword(hash);
                            user.setResetKeyHash(null);
                            user.setResetDate(null);
                            return userRepository.save(user);
                        }))
                .doOnNext(user -> log.info("Password reset for user {}", user.getLogin()))
                .then();
    }

    public Mono<UserDTO> getCurrentAccount() {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .map(UserDTO::from)
                .switchIfEmpty(Mono.error(LoginNotAlreadyRegisteredException::new));
    }

    private static Throwable toConflict(DuplicateKeyException e) {
        String message = String.valueOf(e.getMessage());
        return message.contains("email") ? new EmailAlreadyUsedException() : new UsernameAlreadyUsedException();
    }
}
