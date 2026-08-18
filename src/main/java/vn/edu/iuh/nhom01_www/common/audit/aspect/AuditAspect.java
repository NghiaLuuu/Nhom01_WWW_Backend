package vn.edu.iuh.nhom01_www.common.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.common.audit.annotation.Auditable;
import vn.edu.iuh.nhom01_www.common.audit.entity.AuditLog;
import vn.edu.iuh.nhom01_www.common.audit.repository.AuditLogRepository;
import vn.edu.iuh.nhom01_www.common.audit.util.RequestInfoUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        // Capture request context on the HTTP thread BEFORE execution
        String userEmail = RequestInfoUtil.getCurrentUserEmail();
        String ipAddress = RequestInfoUtil.getClientIpAddress();
        String userAgent = RequestInfoUtil.getUserAgent();
        String endpoint = RequestInfoUtil.getEndpoint();

        // Capture method arguments as newValues
        Map<String, Object> inputValues = new HashMap<>();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = getParamNames(joinPoint);
        for (int i = 0; i < args.length; i++) {
            String key = (i < paramNames.length) ? paramNames[i] : "arg" + i;
            try {
                // Convert to a simple string representation to avoid serialization issues
                if (args[i] != null) {
                    String val = objectMapper.writeValueAsString(args[i]);
                    // Mask password fields in the JSON string
                    val = val.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"********\"");
                    val = val.replaceAll("\"oldPassword\"\\s*:\\s*\"[^\"]*\"", "\"oldPassword\":\"********\"");
                    val = val.replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]*\"", "\"newPassword\":\"********\"");
                    inputValues.put(key, val);
                }
            } catch (Exception e) {
                inputValues.put(key, args[i] != null ? args[i].toString() : null);
            }
        }

        // Execute the original method
        Object result = joinPoint.proceed();

        // Save audit log asynchronously
        AuditLog auditLog = AuditLog.builder()
                .action(auditable.action())
                .entityName(auditable.entityName().isEmpty() ? joinPoint.getTarget().getClass().getSimpleName() : auditable.entityName())
                .newValues(inputValues)
                .userEmail(userEmail)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .endpoint(endpoint)
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                auditLogRepository.save(auditLog);
            } catch (Exception ex) {
                log.error("Error persisting AOP audit log for {}: {}", auditable.action(), ex.getMessage());
            }
        });

        return result;
    }

    private String[] getParamNames(ProceedingJoinPoint joinPoint) {
        try {
            org.aspectj.lang.reflect.MethodSignature signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
            return signature.getParameterNames();
        } catch (Exception e) {
            return new String[0];
        }
    }
}
