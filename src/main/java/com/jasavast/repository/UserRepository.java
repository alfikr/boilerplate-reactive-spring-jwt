package com.jasavast.repository;

import com.jasavast.domain.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User,String> {
    public Mono<User> findOneByLogin(String login);
    public Mono<User> findFirstByEmailIgnoreCase(String email);
}
