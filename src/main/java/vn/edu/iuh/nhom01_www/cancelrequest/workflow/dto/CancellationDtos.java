package vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CancellationDtos {
    private CancellationDtos() {
    }

    public record SubmitRequest(@NotNull @Positive Long bookingId, @NotBlank @Size(max = 1000) String reason) {
    }

    public record ApproveRequest(@NotNull @PositiveOrZero Long version, @NotNull @AssertTrue Boolean confirm,
                                 @Size(max = 1000) String note) {
    }

    public record RejectRequest(@NotNull @PositiveOrZero Long version,
                                @NotBlank @Size(max = 255, message = "Lý do từ chối tối đa 255 ký tự") String reason) {
    }

    public record RequestRow(Long id, Long bookingId, String bookingCode, String customerName,
                             String customerEmail, String customerPhone, String route, LocalDateTime departureTime,
                             String status, String bookingStatus, String requestReason, String rejectionReason,
                             LocalDateTime submittedAt, LocalDateTime resolvedAt, String processedBy,
                             String source, Long version) {
    }

    public record TicketInfo(Long id, String code, String seat, String status, LocalDateTime usedAt) {
    }

    public record BookingInfo(Long id, String code, String customerName, String customerEmail, String customerPhone,
                              String route, LocalDateTime departureTime, String status,
                              @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalPrice,
                              @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal paidAmount,
                              String paymentStatus, List<TicketInfo> tickets, Long existingRequestId,
                              boolean eligible, String ineligibleReason) {
    }

    public record RequestDetails(RequestRow request, BookingInfo booking, String decisionNote,
                                 boolean canApprove, String financialNotice) {
    }

    public record PageResult<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
    }
}
