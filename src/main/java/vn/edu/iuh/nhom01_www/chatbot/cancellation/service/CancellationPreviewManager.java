package vn.edu.iuh.nhom01_www.chatbot.cancellation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.dto.CancellationPreviewDto;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.dto.CancellationResultDto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationPreviewManager {

    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    // Concurrent in-memory fallback for local/unit test executions
    private final Map<String, CancellationPreviewDto> localPreviewMap = new ConcurrentHashMap<>();
    private final Map<String, CancellationResultDto> localExecutedResults = new ConcurrentHashMap<>();

    private static final String PREVIEW_PREFIX = "cancel_preview:";
    private static final String IDEMPOTENT_PREFIX = "cancel_idempotent:";
    private static final long DEFAULT_TTL_MINUTES = 5;

    public void savePreview(CancellationPreviewDto preview) {
        if (preview == null || preview.getConfirmationToken() == null) return;

        localPreviewMap.put(preview.getConfirmationToken(), preview);

        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().set(
                        PREVIEW_PREFIX + preview.getConfirmationToken(),
                        preview,
                        DEFAULT_TTL_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (Exception e) {
                log.warn("Notice: Redis not reachable for cancel preview, falling back to in-memory: {}", e.getMessage());
            }
        }
    }

    public Optional<CancellationPreviewDto> getPreview(String confirmationToken, UUID customerId) {
        if (confirmationToken == null || confirmationToken.isBlank()) return Optional.empty();

        CancellationPreviewDto preview = null;
        if (redisTemplate.isPresent()) {
            try {
                Object obj = redisTemplate.get().opsForValue().get(PREVIEW_PREFIX + confirmationToken);
                if (obj instanceof CancellationPreviewDto p) {
                    preview = p;
                }
            } catch (Exception ignored) {}
        }

        if (preview == null) {
            preview = localPreviewMap.get(confirmationToken);
        }

        if (preview == null) return Optional.empty();

        // Check customer ownership
        if (customerId != null && preview.getCustomerId() != null && !preview.getCustomerId().equals(customerId)) {
            log.warn("[Security] Unauthorized preview access attempt! Expected customerId={}, actual={}", preview.getCustomerId(), customerId);
            return Optional.empty();
        }

        // Check expiration
        if (preview.getExpiresAt() != null && LocalDateTime.now().isAfter(preview.getExpiresAt())) {
            log.warn("[Cancellation] Preview confirmationToken={} has expired at {}", confirmationToken, preview.getExpiresAt());
            removePreview(confirmationToken);
            return Optional.empty();
        }

        return Optional.of(preview);
    }

    public void removePreview(String confirmationToken) {
        if (confirmationToken == null) return;
        localPreviewMap.remove(confirmationToken);
        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().delete(PREVIEW_PREFIX + confirmationToken);
            } catch (Exception ignored) {}
        }
    }

    public void registerExecutedResult(String confirmationToken, CancellationResultDto result) {
        if (confirmationToken == null || result == null) return;
        localExecutedResults.put(confirmationToken, result);
        removePreview(confirmationToken);

        if (redisTemplate.isPresent()) {
            try {
                redisTemplate.get().opsForValue().set(
                        IDEMPOTENT_PREFIX + confirmationToken,
                        result,
                        1,
                        TimeUnit.HOURS
                );
            } catch (Exception ignored) {}
        }
    }

    public Optional<CancellationResultDto> getExecutedResult(String confirmationToken) {
        if (confirmationToken == null) return Optional.empty();

        if (redisTemplate.isPresent()) {
            try {
                Object obj = redisTemplate.get().opsForValue().get(IDEMPOTENT_PREFIX + confirmationToken);
                if (obj instanceof CancellationResultDto res) {
                    return Optional.of(res);
                }
            } catch (Exception ignored) {}
        }

        return Optional.ofNullable(localExecutedResults.get(confirmationToken));
    }

    public Optional<CancellationPreviewDto> getLatestPreviewForCustomer(UUID customerId) {
        if (customerId == null) return Optional.empty();
        return localPreviewMap.values().stream()
                .filter(p -> customerId.equals(p.getCustomerId()))
                .filter(p -> p.getExpiresAt() == null || LocalDateTime.now().isBefore(p.getExpiresAt()))
                .max((a, b) -> a.getExpiresAt().compareTo(b.getExpiresAt()));
    }
}
