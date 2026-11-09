package com.jasavast.service;

import com.jasavast.config.ApplicationProperties;
import com.jasavast.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends account e-mails. JavaMail is blocking, so sending runs on boundedElastic.
 * Failures are logged and swallowed: a mail outage must not fail registration or
 * password reset (the user can retry the flow). Configure SMTP timeouts
 * ({@code spring.mail.properties.mail.smtp.*timeout}) so a hung server cannot pin threads.
 * <p>
 * If {@code spring.mail.host} is not set, Boot creates no {@link JavaMailSender}; mails are
 * skipped and the link is logged at DEBUG (dev convenience; never enable DEBUG for this
 * logger in production since links are credentials).
 */
@Service
@Slf4j
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ApplicationProperties.Mail mail;

    public MailService(ObjectProvider<JavaMailSender> mailSender, ApplicationProperties properties) {
        this.mailSender = mailSender;
        this.mail = properties.mail();
    }

    public Mono<Void> sendActivationEmail(User user, String key) {
        String link = mail.baseUrl() + "/api/activate?key=" + encode(key);
        return send(user, "Account activation",
                "Hello " + user.getLogin() + ",\n\nActivate your account: " + link + "\n", link);
    }

    public Mono<Void> sendPasswordResetMail(User user, String key) {
        String link = mail.baseUrl() + "/reset-password?key=" + encode(key);
        return send(user, "Password reset",
                "Hello " + user.getLogin() + ",\n\nReset your password (valid 24h): " + link
                        + "\n\nIf you did not request this, ignore this e-mail.\n", link);
    }

    private Mono<Void> send(User user, String subject, String text, String link) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Mail not configured (spring.mail.host); '{}' for user {} not sent", subject, user.getLogin());
            log.debug("'{}' link for user {}: {}", subject, user.getLogin(), link);
            return Mono.empty();
        }
        return Mono.<Void>fromRunnable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mail.from());
                    message.setTo(user.getEmail());
                    message.setSubject(subject);
                    message.setText(text);
                    sender.send(message);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.debug("Sent '{}' to user {}", subject, user.getLogin()))
                .onErrorResume(e -> {
                    log.warn("Failed to send '{}' to user {}: {}", subject, user.getLogin(), e.toString());
                    return Mono.empty();
                });
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
