package com.jasavast.repository;

import com.jasavast.domain.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findOneByLogin(String login);

    /** Emails are stored lower-cased, so an exact (index-friendly) match is used. */
    Mono<User> findOneByEmail(String email);

    Mono<User> findOneByActivationKeyHash(String activationKeyHash);

    Mono<User> findOneByResetKeyHash(String resetKeyHash);
}
