package com.jasavast.repository;

import com.jasavast.domain.AuditEventModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersitentAuditEventRepository extends ReactiveMongoRepository<AuditEventModel,String> {
}
