package vn.edu.iuh.nhom01_www.report.revenue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static vn.edu.iuh.nhom01_www.report.revenue.RevenueReportResponse.TrendPoint;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class RevenueReportService {
    private final RevenueReportRepository repository;

    public RevenueReportResponse summary(RevenueReportFilter filter) {
        var totals = repository.totals(filter);
        var trend = fillMissingPeriods(filter, repository.trend(filter));
        return new RevenueReportResponse(filter.fromDate(), filter.toDate(), filter.groupBy(), filter.scope(),
                "PAYMENT_CREATED_AT", "Asia/Ho_Chi_Minh", totals, trend, repository.routes(filter),
                repository.methods(filter), repository.bookingStatuses(filter));
    }

    public RevenueTransactionPage transactions(RevenueReportFilter filter, int page, int size,
                                               String search, String status, String sort) {
        return transactions(filter, page, size, search, status, sort, null);
    }

    public RevenueTransactionPage transactions(RevenueReportFilter filter, int page, int size,
                                               String search, String status, String sort, String bookingStatus) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 100) {
            RevenueReportFilter.badRequest("Trang phải từ 0 đến 1000000, kích thước trang từ 1 đến 100.");
        }
        if (search != null && search.length() > 100) RevenueReportFilter.badRequest("Từ khóa tìm kiếm tối đa 100 ký tự.");
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (normalizedStatus != null && !Set.of("SUCCESS", "SIMULATED_PAID", "PENDING", "FAILED", "CANCELLED", "REFUNDED").contains(normalizedStatus)) {
            RevenueReportFilter.badRequest("Trạng thái thanh toán không hợp lệ.");
        }
        String normalizedSort = sort == null ? "NEWEST" : sort.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("NEWEST", "OLDEST", "AMOUNT_DESC", "AMOUNT_ASC").contains(normalizedSort)) {
            RevenueReportFilter.badRequest("Thứ tự sắp xếp không hợp lệ.");
        }
        String normalizedBookingStatus = bookingStatus == null || bookingStatus.isBlank()
                ? null : bookingStatus.trim().toUpperCase(Locale.ROOT);
        if (normalizedBookingStatus != null && !Set.of("PAID", "COMPLETED", "PENDING_PAYMENT", "CANCELLED",
                "PARTIALLY_CANCELLED", "CANCEL_REQUESTED", "EXPIRED").contains(normalizedBookingStatus)) {
            RevenueReportFilter.badRequest("Trạng thái đơn vé không hợp lệ.");
        }
        return normalizedBookingStatus == null
                ? repository.transactions(filter, page, size, search, normalizedStatus, normalizedSort)
                : repository.transactions(filter, page, size, search, normalizedStatus, normalizedSort, normalizedBookingStatus);
    }

    private List<TrendPoint> fillMissingPeriods(RevenueReportFilter filter, List<TrendPoint> points) {
        boolean daily = "DAY".equals(filter.groupBy());
        LocalDate start = filter.fromDate() != null ? filter.fromDate()
                : points.stream().map(TrendPoint::periodStart).min(LocalDate::compareTo).orElse(null);
        LocalDate end = filter.toDate() != null ? filter.toDate()
                : points.stream().map(TrendPoint::periodStart).max(LocalDate::compareTo).orElse(null);
        if (start == null || end == null) return points;
        if (!daily) { start = start.withDayOfMonth(1); end = end.withDayOfMonth(1); }
        long buckets = (daily ? ChronoUnit.DAYS : ChronoUnit.MONTHS).between(start, end);
        if (buckets >= (daily ? 366 : 1200)) {
            RevenueReportFilter.badRequest("Biểu đồ có quá nhiều kỳ. Hãy chọn nhóm theo tháng hoặc thu hẹp khoảng ngày.");
        }
        Map<LocalDate, TrendPoint> indexed = points.stream().collect(Collectors.toMap(TrendPoint::periodStart, Function.identity()));
        List<TrendPoint> filled = new ArrayList<>();
        for (LocalDate current = start; !current.isAfter(end); current = daily ? current.plusDays(1) : current.plusMonths(1)) {
            filled.add(indexed.getOrDefault(current, new TrendPoint(current, BigDecimal.ZERO, 0)));
        }
        return filled;
    }
}
