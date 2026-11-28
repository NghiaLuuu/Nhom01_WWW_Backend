package vn.edu.iuh.nhom01_www.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueReportService {
    private final PaymentRepository payments;

    public enum GroupBy { DAY, MONTH }
    public record RevenuePoint(String period, BigDecimal revenue, long paymentCount) {}
    public record MethodTotal(String paymentMethod, BigDecimal revenue, long paymentCount) {}
    public record Report(LocalDate from, LocalDate to, GroupBy groupBy, String currency,
                         BigDecimal grossRevenue, long successfulPayments, BigDecimal cancelledBookingReceipts,
                         List<RevenuePoint> series, List<MethodTotal> byPaymentMethod) {}

    public Report report(LocalDate from, LocalDate to, GroupBy groupBy) {
        if (from == null || to == null || groupBy == null || to.isBefore(from)
                || ChronoUnit.DAYS.between(from, to) > 3660 || to.equals(LocalDate.MAX)) {
            throw new CustomException("Invalid date range (maximum 3660 days)", HttpStatus.BAD_REQUEST);
        }
        var rows = payments.aggregateRevenue(from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                groupBy == GroupBy.DAY ? "YYYY-MM-DD" : "YYYY-MM");
        Map<String, RevenuePoint> series = new TreeMap<>();
        Map<String, MethodTotal> methods = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal cancelled = BigDecimal.ZERO;
        long count = 0;
        for (var row : rows) {
            total = total.add(row.getRevenue());
            count += row.getPaymentCount();
            cancelled = cancelled.add(row.getCancelledBookingReceipts());
            series.merge(row.getPeriod(), new RevenuePoint(row.getPeriod(), row.getRevenue(), row.getPaymentCount()),
                    (a, b) -> new RevenuePoint(a.period(), a.revenue().add(b.revenue()), a.paymentCount() + b.paymentCount()));
            methods.merge(row.getPaymentMethod(), new MethodTotal(row.getPaymentMethod(), row.getRevenue(), row.getPaymentCount()),
                    (a, b) -> new MethodTotal(a.paymentMethod(), a.revenue().add(b.revenue()), a.paymentCount() + b.paymentCount()));
        }
        return new Report(from, to, groupBy, "VND", total, count, cancelled,
                List.copyOf(series.values()), List.copyOf(methods.values()));
    }
}
