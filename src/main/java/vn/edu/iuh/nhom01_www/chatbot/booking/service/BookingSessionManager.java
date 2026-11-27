package vn.edu.iuh.nhom01_www.chatbot.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.BookingSessionState;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.BookingStep;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingSessionManager {

    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    private static final String SESSION_PREFIX = "ai_booking_session:";
    private final Map<String, BookingSessionState> localSessionMap = new ConcurrentHashMap<>();

    public BookingSessionState getOrCreateSession(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            sessionKey = "anonymous_session";
        }

        Optional<BookingSessionState> existing = getSession(sessionKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        BookingSessionState fresh = BookingSessionState.builder()
                .sessionKey(sessionKey)
                .step(BookingStep.COLLECTING_INFO)
                .passengerCount(1)
                .updatedAt(LocalDateTime.now())
                .build();
        saveSession(fresh);
        return fresh;
    }

    public Optional<BookingSessionState> getSession(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return Optional.empty();
        }

        BookingSessionState local = localSessionMap.get(sessionKey);
        if (local != null) {
            return Optional.of(local);
        }

        if (redisTemplate.isPresent()) {
            try {
                Object obj = redisTemplate.get().opsForValue().get(SESSION_PREFIX + sessionKey);
                if (obj instanceof BookingSessionState state) {
                    localSessionMap.put(sessionKey, state);
                    return Optional.of(state);
                }
            } catch (Exception e) {
                log.warn("[BookingSessionManager] Redis fetch error for key {}: {}", sessionKey, e.getMessage());
            }
        }
        return Optional.empty();
    }

    public void saveSession(BookingSessionState state) {
        if (state == null || state.getSessionKey() == null) return;
        state.setUpdatedAt(LocalDateTime.now());
        localSessionMap.put(state.getSessionKey(), state);

        redisTemplate.ifPresent(template -> {
            try {
                template.opsForValue().set(SESSION_PREFIX + state.getSessionKey(), state, 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("[BookingSessionManager] Redis save error for key {}: {}", state.getSessionKey(), e.getMessage());
            }
        });
    }

    public void clearSession(String sessionKey) {
        if (sessionKey == null) return;
        localSessionMap.remove(sessionKey);
        redisTemplate.ifPresent(template -> {
            try {
                template.delete(SESSION_PREFIX + sessionKey);
            } catch (Exception ignored) {}
        });
    }

    public boolean hasActiveBookingFlow(String sessionKey) {
        Optional<BookingSessionState> opt = getSession(sessionKey);
        if (opt.isEmpty()) return false;
        BookingStep step = opt.get().getStep();
        if (step == BookingStep.COLLECTING_INFO) {
            return opt.get().getSelectedTripId() != null;
        }
        return step == BookingStep.DEPARTURE_SELECTION
                || step == BookingStep.TRIP_SELECTION
                || step == BookingStep.SEAT_SELECTION
                || step == BookingStep.BOOKING_REVIEW
                || step == BookingStep.PENDING_PAYMENT;
    }
}
