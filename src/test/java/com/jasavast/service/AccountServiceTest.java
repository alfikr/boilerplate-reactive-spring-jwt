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
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private final TransactionalOperator tx = mock(TransactionalOperator.class);
    private final AccountService service =
            new AccountService(encoder, repository, tx, mailService, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(encoder.encode(anyString())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));
        when(mailService.sendPasswordResetMail(any(), anyString())).thenReturn(Mono.empty());
        when(repository.findOneByEmail(anyString())).thenReturn(Mono.empty());
        when(repository.findOneByLogin(anyString())).thenReturn(Mono.empty());
        when(repository.setResetKey(anyLong(), anyString(), any())).thenReturn(Mono.just(1L));
    }

    @Test
    void activationUsesHashedKeyAndSucceedsOnOneRow() {
        when(repository.activate(RandomUtil.sha256("k"), NOW)).thenReturn(Mono.just(1L));
        StepVerifier.create(service.activate("k")).verifyComplete();
    }

    @Test
    void unknownOrUsedActivationKeyFails() {
        when(repository.activate(anyString(), any())).thenReturn(Mono.just(0L));
        StepVerifier.create(service.activate("nope")).expectError(InvalidKeyException.class).verify();
    }

    @Test
    void resetRequestStoresOnlyHashAndMailsPlainKey() {
        User user = User.builder().id(7L).login("bob").email("bob@example.com").aktif(true).build();
        when(repository.findOneByEmail("bob@example.com")).thenReturn(Mono.just(user));
        ForgotPasswordVM vm = new ForgotPasswordVM();
        vm.setEmail(" Bob@Example.com ");

        StepVerifier.create(service.requestPasswordReset(vm)).verifyComplete();

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetMail(eq(user), key.capture());
        verify(repository).setResetKey(7L, RandomUtil.sha256(key.getValue()), NOW);
    }

    @Test
    void resetRequestForInactiveOrUnknownUserIsSilent() {
        when(repository.findOneByLogin("carol")).thenReturn(Mono.just(User.builder().id(8L).login("carol").aktif(false).build()));
        ForgotPasswordVM vm = new ForgotPasswordVM();
        vm.setLogin("carol");

        StepVerifier.create(service.requestPasswordReset(vm)).verifyComplete();
        verify(repository, never()).setResetKey(anyLong(), anyString(), any());
    }

    @Test
    void completeResetPassesHashExpiryAndEncodedPassword() {
        when(repository.completeReset(anyString(), anyString(), any(), any())).thenReturn(Mono.just(1L));

        StepVerifier.create(service.completePasswordReset("k", "new-password")).verifyComplete();
        verify(repository).completeReset(RandomUtil.sha256("k"), "hashed:new-password",
                NOW.minus(AccountService.RESET_KEY_VALIDITY), NOW);
    }

    @Test
    void completeResetWithInvalidOrExpiredKeyFails() {
        when(repository.completeReset(anyString(), anyString(), any(), any())).thenReturn(Mono.just(0L));
        StepVerifier.create(service.completePasswordReset("k", "new-password")).expectError(InvalidKeyException.class).verify();
        assertThat(RandomUtil.sha256("k")).hasSize(64);
    }
}
