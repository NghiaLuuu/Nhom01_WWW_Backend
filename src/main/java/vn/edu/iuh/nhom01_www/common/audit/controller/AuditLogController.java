package vn.edu.iuh.nhom01_www.common.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.audit.entity.AuditLog;
import vn.edu.iuh.nhom01_www.common.audit.service.AuditLogService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String userEmail) {
        Page<AuditLog> result = auditLogService.getAuditLogs(page, size, entityName, userEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", result));
    }

    @GetMapping("/entity-names")
    public ResponseEntity<ApiResponse<List<String>>> getEntityNames() {
        List<String> entityNames = auditLogService.getDistinctEntityNames();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", entityNames));
    }

    @GetMapping("/user-emails")
    public ResponseEntity<ApiResponse<List<String>>> searchUserEmails(@RequestParam String keyword) {
        List<String> emails = auditLogService.searchUserEmails(keyword);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", emails));
    }
}
