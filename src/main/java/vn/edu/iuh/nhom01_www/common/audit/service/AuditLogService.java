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
    public Page<AuditLog> getAuditLogs(int page, int size, String entityName, String userEmail) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        boolean hasEntity = entityName != null && !entityName.isBlank();
        boolean hasEmail = userEmail != null && !userEmail.isBlank();

        if (hasEntity && hasEmail) {
            return auditLogRepository.findByEntityNameAndUserEmailContainingIgnoreCase(entityName, userEmail, pageable);
        } else if (hasEntity) {
            return auditLogRepository.findByEntityName(entityName, pageable);
        } else if (hasEmail) {
            return auditLogRepository.findByUserEmailContainingIgnoreCase(userEmail, pageable);
        }
        
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctEntityNames() {
        return auditLogRepository.findDistinctEntityNames();
    }

    @Transactional(readOnly = true)
    public List<String> searchUserEmails(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return auditLogRepository.findDistinctUserEmailsByKeyword(keyword.trim());
    }
}
