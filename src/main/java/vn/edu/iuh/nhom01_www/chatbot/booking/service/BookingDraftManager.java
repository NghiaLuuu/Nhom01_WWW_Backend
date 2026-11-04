package vn.edu.iuh.nhom01_www.chatbot.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.BookingConfirmationResultDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.BookingDraftDto;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingDraftManager {

    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    private static final String DRAFT_PREFIX = "ai_booking_draft:";
    private static final String IDEMPOTENCY_PREFIX = "ai_booking_idempotency:";

    // In-memory fallback
    private final Map<String, BookingDraftDto> localDraftMap = new ConcurrentHashMap<>();
    private final Map<String, BookingConfirmationResultDto> localIdempotencyMap = new ConcurrentHashMap<>();

    public void saveDraft(BookingDraftDto draft) {
        String draftKey = buildDraftKey(draft.getCustomerId(), draft.getIdempotencyKey());
        localDraftMap.put(draftKey, draft);
        if (draft.getCustomerId() != null) {
            // Also store latest draft by customerId for convenient multi-turn reference
            localDraftMap.put("latest_customer_draft:" + draft.getCustomerId(), draft);
        }

        redisTemplate.ifPresent(template -> {
            try {
                template.opsForValue().set(DRAFT_PREFIX + draftKey, draft, 15, TimeUnit.MINUTES);
                if (draft.getCustomerId() != null) {
                    template.opsForValue().set(DRAFT_PREFIX + "latest:" + draft.getCustomerId(), draft, 15, TimeUnit.MINUTES);
                }
            } catch (Exception e) {
                log.warn("Redis draft cache failure, relying on in-memory store: {}", e.getMessage());
            }
        });
    }

    public Optional<BookingDraftDto> getDraft(UUID customerId, String idempotencyKey) {
        String draftKey = buildDraftKey(customerId, idempotencyKey);
        BookingDraftDto local = localDraftMap.get(draftKey);
        if (local != null) {
            return Optional.of(local);
        }

        if (redisTemplate.isPresent()) {
            try {
                Object obj = redisTemplate.get().opsForValue().get(DRAFT_PREFIX + draftKey);
                if (obj instanceof BookingDraftDto dto) {
                    return Optional.of(dto);
                }
            } catch (Exception e) {
                log.warn("Redis draft fetch error: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    public Optional<BookingDraftDto> getLatestDraftForCustomer(UUID customerId) {
        if (customerId == null) return Optional.empty();
        BookingDraftDto local = localDraftMap.get("latest_customer_draft:" + customerId);
        if (local != null) return Optional.of(local);

        if (redisTemplate.isPresent()) {
            try {
                Object obj = redisTemplate.get().opsForValue().get(DRAFT_PREFIX + "latest:" + customerId);
                if (obj instanceof BookingDraftDto dto) {
                    return Optional.of(dto);
                }
            } catch (Exception e) {
                log.warn("Redis latest draft fetch error: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    public void removeDraft(UUID customerId, String idempotencyKey) {
        String draftKey = buildDraftKey(customerId, idempotencyKey);
        localDraftMap.remove(draftKey);
        if (customerId != null) {
            localDraftMap.remove("latest_customer_draft:" + customerId);
        }
        redisTemplate.ifPresent(template -> {
            try {
                template.delete(DRAFT_PREFIX + draftKey);
                if (customerId != null) {
                    template.delete(DRAFT_PREFIX + "latest:" + customerId);
                }
            } catch (Exception ignored) {}
        });
    }

    public void registerIdempotentResult(String idempotencyKey, BookingConfirmationResultDto result) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        localIdempotencyMap.put(idempotencyKey, result);

        redisTemplate.ifPresent(template -> {
            try {
                template.opsForValue().set(IDEMPOTENCY_PREFIX + idempotencyKey, result, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis idempotency cache failure: {}", e.getMessage());
            }
        });
    }

    public Optional<BookingConfirmationResultDto> getIdempotentResult(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        BookingConfirmationResultDto local = localIdempotencyMap.get(idempotencyKey);
        if (local != null) return Optional.of(local);

        if (redisTemplate.isPresent()) {
            try {
                Object obj = redisTemplate.get().opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
                if (obj instanceof BookingConfirmationResultDto dto) {
                    return Optional.of(dto);
                }
            } catch (Exception e) {
                log.warn("Redis idempotency fetch error: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    private String buildDraftKey(UUID customerId, String idempotencyKey) {
        return (customerId != null ? customerId.toString() : "anonymous") + ":" + (idempotencyKey != null ? idempotencyKey : "default");
    }
}
