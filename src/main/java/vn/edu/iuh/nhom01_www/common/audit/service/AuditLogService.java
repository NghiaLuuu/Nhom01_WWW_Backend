package vn.edu.iuh.nhom01_www.common.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.common.audit.entity.AuditLog;
import vn.edu.iuh.nhom01_www.common.audit.repository.AuditLogRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(int page, int size, String entityName) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (entityName != null && !entityName.isBlank()) {
            return auditLogRepository.findByEntityName(entityName, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctEntityNames() {
        return auditLogRepository.findDistinctEntityNames();
    }
}
