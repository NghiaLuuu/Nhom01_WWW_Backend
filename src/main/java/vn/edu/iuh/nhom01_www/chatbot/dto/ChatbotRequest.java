package vn.edu.iuh.nhom01_www.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class ChatbotRequest {
    private String question;
    private String conversationId;
    private String sessionId;
    private List<ChatMessage> history;

    // Structured interactive action payloads
    private String action; // e.g. "SELECT_DEPARTURE", "SELECT_TRIP", "MANUAL_SEAT_MAP", "SELECT_SEATS", "AUTO_ADJACENT_SEATS", "CONFIRM_BOOKING", "SIMULATE_PAYMENT"
    private Long tripId;
    private List<Long> seatIds;
    private List<String> seatNames;
    private String departureLocation;
    private Integer passengerCount;
    private Integer stateVersion;
    private String idempotencyKey;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
