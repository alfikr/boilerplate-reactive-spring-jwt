package com.jasavast.service;

import com.jasavast.core.error.InvalidKeyException;
import com.jasavast.core.util.RandomUtil;
import com.jasavast.domain.User;
import com.jasavast.repository.UserRepository;
import com.jasavast.resource.vm.ForgotPasswordVM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-02T00:00:00Z");

    private final UserRepository repository = mock(UserRepository.class);
    private final MailService mailService = mock(MailService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AccountService service =
            new AccountService(encoder, repository, mailService, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(encoder.encode(anyString())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));
        when(mailService.sendPasswordResetMail(any(), anyString())).thenReturn(Mono.empty());
        when(repository.findOneByEmail(anyString())).thenReturn(Mono.empty());
        when(repository.findOneByLogin(anyString())).thenReturn(Mono.empty());
    }

    @Test
    void activationConsumesKey() {
        User user = User.builder().login("bob").activationKeyHash(RandomUtil.sha256("k")).build();
        when(repository.findOneByActivationKeyHash(RandomUtil.sha256("k"))).thenReturn(Mono.just(user));

        StepVerifier.create(service.activate("k"))
                .assertNext(u -> {
                    assertThat(u.isAktif()).isTrue();
                    assertThat(u.getActivationKeyHash()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void unknownActivationKeyFails() {
        when(repository.findOneByActivationKeyHash(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(service.activate("nope")).expectError(InvalidKeyException.class).verify();
    }

    @Test
    void resetRequestStoresOnlyHashAndMailsPlainKey() {
        User user = User.builder().login("bob").email("bob@example.com").aktif(true).build();
        when(repository.findOneByEmail("bob@example.com")).thenReturn(Mono.just(user));
        ForgotPasswordVM vm = new ForgotPasswordVM();
        vm.setEmail(" Bob@Example.com ");

        StepVerifier.create(service.requestPasswordReset(vm)).verifyComplete();

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetMail(eq(user), key.capture());
        assertThat(user.getResetKeyHash()).isEqualTo(RandomUtil.sha256(key.getValue())).isNotEqualTo(key.getValue());
        assertThat(user.getResetDate()).isEqualTo(NOW);
    }

    @Test
    void resetRequestForInactiveOrUnknownUserIsSilent() {
        when(repository.findOneByLogin("carol")).thenReturn(Mono.just(User.builder().login("carol").aktif(false).build()));
        ForgotPasswordVM vm = new ForgotPasswordVM();
        vm.setLogin("carol");

        StepVerifier.create(service.requestPasswordReset(vm)).verifyComplete();
        verify(repository, never()).save(any());
    }

    @Test
    void completeResetWithFreshKey() {
        User user = User.builder().login("bob").resetKeyHash(RandomUtil.sha256("k"))
                .resetDate(NOW.minusSeconds(3600)).password("old").build();
        when(repository.findOneByResetKeyHash(RandomUtil.sha256("k"))).thenReturn(Mono.just(user));

        StepVerifier.create(service.completePasswordReset("k", "new-password")).verifyComplete();
        assertThat(user.getPassword()).isEqualTo("hashed:new-password");
        assertThat(user.getResetKeyHash()).isNull();
        assertThat(user.getResetDate()).isNull();
    }

    @Test
    void completeResetWithExpiredKeyFails() {
        User user = User.builder().login("bob").resetKeyHash(RandomUtil.sha256("k"))
                .resetDate(NOW.minus(AccountService.RESET_KEY_VALIDITY).minusSeconds(1)).password("old").build();
        when(repository.findOneByResetKeyHash(RandomUtil.sha256("k"))).thenReturn(Mono.just(user));

        StepVerifier.create(service.completePasswordReset("k", "new-password")).expectError(InvalidKeyException.class).verify();
        assertThat(user.getPassword()).isEqualTo("old");
    }
}
