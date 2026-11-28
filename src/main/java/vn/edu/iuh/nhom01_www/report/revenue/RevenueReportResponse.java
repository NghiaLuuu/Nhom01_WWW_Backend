package vn.edu.iuh.nhom01_www.report.revenue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueReportResponse(LocalDate fromDate, LocalDate toDate, String groupBy,
                                    String scope, String timeBasis, String timezone,
                                    Totals totals, List<TrendPoint> trend,
                                    List<RouteRevenue> routes, List<MethodRevenue> methods,
                                    List<BookingStatusCount> bookingStatuses) {
    public record Totals(BigDecimal successfulAmount, long successfulPayments, long failedPayments,
                         long pendingPayments, long refundedPayments, long paymentCount,
                         long bookingCount, long successfulBookingCount, long ticketCount,
                         long customerCount, long tripCount) { }
    public record TrendPoint(LocalDate periodStart, BigDecimal amount, long paymentCount) { }
    public record RouteRevenue(Long routeId, String departureLocation, String arrivalLocation,
                               BigDecimal amount, long paymentCount) { }
    public record MethodRevenue(String paymentMethod, BigDecimal amount, long paymentCount) { }
    public record BookingStatusCount(String status, long count) { }
}
