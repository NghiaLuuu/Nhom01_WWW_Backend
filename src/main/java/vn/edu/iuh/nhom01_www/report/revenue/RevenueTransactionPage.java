package vn.edu.iuh.nhom01_www.report.revenue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RevenueTransactionPage(List<Transaction> content, long totalElements,
                                      int page, int size, long totalPages) {
    public record Transaction(Long paymentId, Long bookingId, String transactionId,
                              String customerName, String departureLocation, String arrivalLocation,
                              LocalDateTime departureTime, long ticketCount, BigDecimal amount,
                              String paymentMethod, String paymentStatus, String bookingStatus,
                              LocalDateTime createdAt) { }
}
