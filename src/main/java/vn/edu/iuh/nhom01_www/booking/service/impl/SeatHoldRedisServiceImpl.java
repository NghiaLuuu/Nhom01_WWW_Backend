package vn.edu.iuh.nhom01_www.booking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.booking.dto.BookingHoldDto;
import vn.edu.iuh.nhom01_www.booking.dto.SeatHoldDto;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.seat.model.Seat;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldRedisServiceImpl implements SeatHoldRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String SEAT_HOLD_PREFIX = "seat_hold:trip:";
    private static final String BOOKING_HOLD_PREFIX = "booking_hold:";

    private String getSeatHoldKey(Long tripId, Long seatId) {
        return SEAT_HOLD_PREFIX + tripId + ":seat:" + seatId;
    }

    private String getBookingHoldKey(Long bookingId) {
        return BOOKING_HOLD_PREFIX + bookingId;
    }

    @Override
    public List<String> tryHoldSeats(Long tripId, List<Seat> seats, Long bookingId, UUID customerId, long durationInSeconds) {
        List<String> failedSeatNames = new ArrayList<>();
        List<String> successfullyHeldKeys = new ArrayList<>();

        for (Seat seat : seats) {
            String seatKey = getSeatHoldKey(tripId, seat.getId());
            SeatHoldDto seatHoldDto = SeatHoldDto.builder()
                    .tripId(tripId)
                    .seatId(seat.getId())
                    .seatName(seat.getSeatName())
                    .bookingId(bookingId)
                    .customerId(customerId)
                    .holdExpiresAt(LocalDateTime.now().plusSeconds(durationInSeconds))
                    .build();

            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(seatKey, seatHoldDto, durationInSeconds, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(acquired)) {
                successfullyHeldKeys.add(seatKey);
            } else {
                failedSeatNames.add(seat.getSeatName());
            }
        }

        // Nếu có bất kỳ ghế nào đã bị giữ trước đó, rollback toàn bộ các ghế vừa giữ được trong lượt này
        if (!failedSeatNames.isEmpty()) {
            if (!successfullyHeldKeys.isEmpty()) {
                redisTemplate.delete(successfullyHeldKeys);
            }
            return failedSeatNames;
        }

        // Lưu thông tin giữ chỗ theo bookingId để giải phóng hoặc tra cứu nhanh
        String bookingKey = getBookingHoldKey(bookingId);
        BookingHoldDto bookingHoldDto = BookingHoldDto.builder()
                .bookingId(bookingId)
                .tripId(tripId)
                .customerId(customerId)
                .seatIds(seats.stream().map(Seat::getId).collect(Collectors.toList()))
                .holdExpiresAt(LocalDateTime.now().plusSeconds(durationInSeconds))
                .build();
        redisTemplate.opsForValue().set(bookingKey, bookingHoldDto, durationInSeconds, TimeUnit.SECONDS);

        return Collections.emptyList();
    }

    @Override
    public List<Long> getHeldSeatIdsByTripId(Long tripId) {
        String pattern = SEAT_HOLD_PREFIX + tripId + ":seat:*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> seatIds = new ArrayList<>();
        for (String key : keys) {
            try {
                String[] parts = key.split(":seat:");
                if (parts.length == 2) {
                    seatIds.add(Long.parseLong(parts[1]));
                }
            } catch (Exception e) {
                log.warn("Lỗi khi parse seatId từ key Redis: {}", key);
            }
        }
        return seatIds;
    }

    @Override
    public boolean isBookingHoldActive(Long bookingId) {
        String bookingKey = getBookingHoldKey(bookingId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(bookingKey));
    }

    @Override
    public Optional<BookingHoldDto> getBookingHold(Long bookingId) {
        String bookingKey = getBookingHoldKey(bookingId);
        try {
            Object obj = redisTemplate.opsForValue().get(bookingKey);
            if (obj == null) {
                return Optional.empty();
            }
            if (obj instanceof BookingHoldDto) {
                return Optional.of((BookingHoldDto) obj);
            }
            return Optional.ofNullable(objectMapper.convertValue(obj, BookingHoldDto.class));
        } catch (Exception e) {
            log.error("Lỗi khi đọc booking_hold từ Redis cho booking #{}: {}", bookingId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<LocalDateTime> getHoldExpiresAt(Long bookingId) {
        return getBookingHold(bookingId).map(BookingHoldDto::getHoldExpiresAt);
    }

    @Override
    public long getRemainingHoldSeconds(Long bookingId) {
        String bookingKey = getBookingHoldKey(bookingId);
        Long expire = redisTemplate.getExpire(bookingKey, TimeUnit.SECONDS);
        return expire != null && expire > 0 ? expire : 0;
    }

    @Override
    public void releaseHoldByBookingId(Long bookingId) {
        String bookingKey = getBookingHoldKey(bookingId);
        try {
            Object obj = redisTemplate.opsForValue().get(bookingKey);
            if (obj != null) {
                BookingHoldDto dto;
                if (obj instanceof BookingHoldDto) {
                    dto = (BookingHoldDto) obj;
                } else {
                    dto = objectMapper.convertValue(obj, BookingHoldDto.class);
                }

                if (dto != null && dto.getSeatIds() != null && dto.getTripId() != null) {
                    List<String> seatKeys = dto.getSeatIds().stream()
                            .map(seatId -> getSeatHoldKey(dto.getTripId(), seatId))
                            .collect(Collectors.toList());
                    redisTemplate.delete(seatKeys);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi giải phóng ghế trên Redis cho booking #{}: {}", bookingId, e.getMessage());
        } finally {
            redisTemplate.delete(bookingKey);
        }
    }

    @Override
    public void releaseHoldBySeats(Long tripId, List<Long> seatIds) {
        if (tripId != null && seatIds != null && !seatIds.isEmpty()) {
            List<String> keys = seatIds.stream()
                    .map(seatId -> getSeatHoldKey(tripId, seatId))
                    .collect(Collectors.toList());
            redisTemplate.delete(keys);
        }
    }
}
