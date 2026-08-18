package vn.edu.iuh.nhom01_www.common.audit.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.common.audit.entity.AuditLog;
import vn.edu.iuh.nhom01_www.common.audit.repository.AuditLogRepository;
import vn.edu.iuh.nhom01_www.common.audit.util.RequestInfoUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class HibernateAuditListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final ApplicationContext applicationContext;

    private AuditLogRepository getAuditLogRepository() {
        return applicationContext.getBean(AuditLogRepository.class);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (isAuditLogEntity(event.getEntity())) return;
        saveAuditLog("CREATE", event.getEntity().getClass().getSimpleName(),
                getId(event.getId()), null, event.getState(), event.getPersister());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (isAuditLogEntity(event.getEntity())) return;
        saveAuditLog("UPDATE", event.getEntity().getClass().getSimpleName(),
                getId(event.getId()), event.getOldState(), event.getState(), event.getPersister());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (isAuditLogEntity(event.getEntity())) return;
        saveAuditLog("DELETE", event.getEntity().getClass().getSimpleName(),
                getId(event.getId()), event.getDeletedState(), null, event.getPersister());
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private boolean isAuditLogEntity(Object entity) {
        return entity instanceof AuditLog;
    }

    private String getId(Object id) {
        return id != null ? id.toString() : null;
    }

    private void saveAuditLog(String action, String entityName, String entityId,
                              Object[] oldState, Object[] newState, EntityPersister persister) {
        try {
            Map<String, Object> oldValues = stateToMap(oldState, persister);
            Map<String, Object> newValues = stateToMap(newState, persister);

            // CRITICAL: Capture request context NOW on the current HTTP thread,
            // because CompletableFuture.runAsync() runs on a different thread
            // where RequestContextHolder is empty.
            String userEmail = RequestInfoUtil.getCurrentUserEmail();
            String ipAddress = RequestInfoUtil.getClientIpAddress();
            String userAgent = RequestInfoUtil.getUserAgent();
            String endpoint = RequestInfoUtil.getEndpoint();

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .userEmail(userEmail)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .endpoint(endpoint)
                    .build();

            // Save asynchronously to avoid interfering with the current Hibernate flush cycle.
            // All request-scoped data has already been captured above on the HTTP thread.
            CompletableFuture.runAsync(() -> {
                try {
                    getAuditLogRepository().save(auditLog);
                } catch (Exception ex) {
                    log.error("Error persisting audit log for {} on {}: {}", action, entityName, ex.getMessage());
                }
            });

        } catch (Exception e) {
            // Never let audit logging crash the main business transaction
            log.error("Failed to build audit log for {} on {}", action, entityName, e);
        }
    }

    private Map<String, Object> stateToMap(Object[] state, EntityPersister persister) {
        if (state == null) return null;
        String[] propertyNames = persister.getPropertyNames();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < propertyNames.length; i++) {
            Object value = state[i];
            String propName = propertyNames[i];

            // Mask sensitive fields
            if ("password".equalsIgnoreCase(propName)) {
                map.put(propName, "********");
                continue;
            }

            if (value == null) {
                map.put(propName, null);
            } else if (isBasicType(value)) {
                map.put(propName, value);
            } else if (value instanceof java.util.Collection) {
                // Skip collections (e.g., OneToMany lists) - they cause circular ref / lazy init errors
                map.put(propName, "[Collection]");
            } else {
                // For ManyToOne / complex refs, just log the class name to avoid deep serialization
                map.put(propName, value.getClass().getSimpleName() + "@ref");
            }
        }
        return map;
    }

    private boolean isBasicType(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value.getClass().isEnum()
                || value instanceof java.time.temporal.TemporalAccessor
                || value instanceof java.util.Date
                || value instanceof java.util.UUID
                || value instanceof java.math.BigDecimal
                || value instanceof java.math.BigInteger;
    }
}
