package vn.edu.iuh.nhom01_www.chatbot.cancellation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationIntent {
    private String rawQuery;
    private Long targetBookingId;
    private String targetBookingCode;
    private List<Long> targetTicketIds;
    private List<String> targetSeatNames;
    private String destinationKeyword;
    private String departureKeyword;
    private LocalDate dateKeyword;
    private String timeKeyword;
    @Builder.Default
    private int ticketCount = 0;
    @Builder.Default
    private boolean cancelAll = false;
    private String confirmationToken;
    @Builder.Default
    private boolean isConfirmation = false;
    @Builder.Default
    private boolean isKeepTickets = false;
    @Builder.Default
    private boolean isPromptInjection = false;
    private String promptInjectionReason;
    @Builder.Default
    private boolean isFindMyBookings = false;
}
