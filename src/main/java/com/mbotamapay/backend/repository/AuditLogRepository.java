package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);

    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);
}
