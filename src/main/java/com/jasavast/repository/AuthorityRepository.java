package com.jasavast.repository;

import com.jasavast.domain.Authority;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends ReactiveMongoRepository<Authority,String> {
}
