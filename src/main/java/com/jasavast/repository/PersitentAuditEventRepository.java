package com.jasavast.repository;

import com.jasavast.domain.AuditEventModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersitentAuditEventRepository extends R2dbcRepository<AuditEventModel,String> {
}
