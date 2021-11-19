package com.jasavast.repository;

import com.jasavast.domain.User;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Repository
public interface UserRepository extends PagingAndSortingRepository<User,String> {
    public Optional<User> findOneByLogin(String login);
}
