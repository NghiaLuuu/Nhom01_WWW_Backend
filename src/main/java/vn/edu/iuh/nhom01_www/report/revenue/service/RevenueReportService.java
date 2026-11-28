package vn.edu.iuh.nhom01_www.report.revenue.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.report.revenue.dto.RevenueReportDtos.*;
import vn.edu.iuh.nhom01_www.report.revenue.repository.RevenueReportRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RevenueReportService {
    public static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> NOTES = List.of(
            "Tổng tiền là SUM(Payment.amount) của các giao dịch SUCCESS, không phải SUM(Booking.totalPrice).",
            "Khoản thu của booking hiện đã hủy vẫn nằm trong tổng tiền và được tách riêng; không phải số tiền đã hoàn.",
            "Chưa có dữ liệu giao dịch hoàn tiền; báo cáo không tính doanh thu ròng sau hoàn tiền.",
            "Thời gian lọc là Payment.createdAt (LocalDateTime), theo quy ước giờ Việt Nam của dữ liệu hiện có.");

    private final RevenueReportRepository repository;
    private final Clock clock;

    @Autowired
    public RevenueReportService(RevenueReportRepository repository) {
        this(repository, Clock.system(REPORT_ZONE));
    }

    public RevenueReportService(RevenueReportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock.withZone(REPORT_ZONE);
    }

    /** All aggregates share a PostgreSQL snapshot, even if another payment commits during this request. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Report report(LocalDate from, LocalDate to, String groupBy, Long routeId, String method) {
        Filter filter = validatedFilter(from, to, groupBy, routeId, method);
        Summary summary = repository.summary(filter);
        List<TimeBucket> series = fillEmptyPeriods(filter, repository.timeSeries(filter));
        return new Report(filter, "VND", REPORT_ZONE.getId(), "PAYMENT_CREATED_AT",
                OffsetDateTime.now(clock).toString(), false, summary, series,
                repository.byPaymentMethod(filter), repository.byRoute(filter), NOTES);
    }

    @Transactional(readOnly = true)
    public Options options() {
        return repository.options();
    }

    private Filter validatedFilter(LocalDate from, LocalDate to, String groupBy, Long routeId, String method) {
        if ((from == null) != (to == null)) {
            throw badRequest("Cần cung cấp cả from và to, hoặc bỏ cả hai để xem tháng hiện tại.");
        }
        if (from == null) {
            from = LocalDate.now(clock).withDayOfMonth(1);
            to = from.plusMonths(1).minusDays(1);
        }
        if (from.getYear() < 1900 || to.getYear() > 9998 || from.isAfter(to)) {
            throw badRequest("Khoảng ngày không hợp lệ: from không được sau to; năm phải từ 1900 đến 9998.");
        }
        GroupBy grouping;
        try {
            grouping = groupBy == null || groupBy.isBlank() ? GroupBy.DAY
                    : GroupBy.valueOf(groupBy.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw badRequest("groupBy chỉ nhận DAY, MONTH hoặc YEAR.");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        int maximum = grouping == GroupBy.DAY ? 366 : 3660;
        if (days > maximum) {
            throw badRequest("Khoảng báo cáo tối đa " + maximum + " ngày với cách nhóm đã chọn.");
        }
        if (routeId != null && (routeId <= 0 || !repository.routeExists(routeId))) {
            throw badRequest("Tuyến đường không tồn tại hoặc mã tuyến không hợp lệ.");
        }
        String normalizedMethod = method == null || method.isBlank() ? null : method.trim().toUpperCase(Locale.ROOT);
        if (normalizedMethod != null && normalizedMethod.length() > 255) {
            throw badRequest("Tên phương thức thanh toán không được vượt quá 255 ký tự.");
        }
        return new Filter(from, to, grouping, routeId, normalizedMethod);
    }

    private List<TimeBucket> fillEmptyPeriods(Filter filter, List<TimeBucket> rows) {
        Map<LocalDate, TimeBucket> indexed = new HashMap<>();
        rows.forEach(row -> indexed.put(row.periodStart(), row));
        List<TimeBucket> result = new ArrayList<>();
        for (LocalDate day = filter.groupBy().firstDay(filter.from()); !day.isAfter(filter.to()); day = filter.groupBy().next(day)) {
            result.add(indexed.getOrDefault(day, new TimeBucket(filter.groupBy().label(day), day, BigDecimal.ZERO, 0, 0)));
        }
        return List.copyOf(result);
    }

    private CustomException badRequest(String message) {
        return new CustomException(message, HttpStatus.BAD_REQUEST);
    }
}
