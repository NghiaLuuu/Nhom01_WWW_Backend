package vn.edu.iuh.nhom01_www.common.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.nhom01_www.common.audit.entity.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityName(String entityName, Pageable pageable);

    @Query("SELECT DISTINCT a.entityName FROM AuditLog a WHERE a.entityName IS NOT NULL ORDER BY a.entityName")
    List<String> findDistinctEntityNames();
}
