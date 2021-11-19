package com.jasavast.repository;

import com.jasavast.domain.AuditEventModel;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditAspectRepository extends PagingAndSortingRepository<AuditEventModel,String> {
}
