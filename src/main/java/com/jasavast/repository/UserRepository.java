package com.jasavast.repository;

import com.jasavast.domain.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User,String> {
    public Mono<User> findOneByLogin(String login);
}
