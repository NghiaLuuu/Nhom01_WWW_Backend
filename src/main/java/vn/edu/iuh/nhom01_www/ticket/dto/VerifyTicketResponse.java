package vn.edu.iuh.nhom01_www.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyTicketResponse {
    private boolean valid;
    private String reason;
    private TicketDetails ticket;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketDetails {
        private String code;
        private String passengerName;
        private String seat;
        private String route;
        private LocalDateTime departureTime;
    }
}
