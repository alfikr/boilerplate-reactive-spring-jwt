package com.jasavast.repository;

import com.jasavast.domain.Authority;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends R2dbcRepository<Authority,String> {
}
