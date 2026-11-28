package vn.edu.iuh.nhom01_www.report.revenue;

import org.springframework.http.HttpStatus;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public record RevenueReportFilter(LocalDate fromDate, LocalDate toDate, String groupBy,
                                  String scope, Long routeId, String paymentMethod) {
    public RevenueReportFilter {
        groupBy = normalize(groupBy, "MONTH");
        scope = normalize(scope, "REAL");
        paymentMethod = paymentMethod == null || paymentMethod.isBlank()
                ? null : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!"DAY".equals(groupBy) && !"MONTH".equals(groupBy)) badRequest("Chỉ hỗ trợ nhóm theo DAY hoặc MONTH.");
        if (!"REAL".equals(scope) && !"DEMO".equals(scope)) badRequest("Phạm vi phải là REAL hoặc DEMO.");
        if (routeId != null && routeId <= 0) badRequest("Mã tuyến phải lớn hơn 0.");
        if (paymentMethod != null && paymentMethod.length() > 50) badRequest("Phương thức thanh toán quá dài.");
        for (LocalDate date : new LocalDate[]{fromDate, toDate}) {
            if (date != null && (date.getYear() < 1900 || date.getYear() > 9998)) {
                badRequest("Ngày báo cáo phải nằm trong khoảng năm 1900 đến 9998.");
            }
        }
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) badRequest("Ngày bắt đầu không được sau ngày kết thúc.");
            long buckets = "DAY".equals(groupBy) ? ChronoUnit.DAYS.between(fromDate, toDate)
                    : ChronoUnit.MONTHS.between(fromDate.withDayOfMonth(1), toDate.withDayOfMonth(1));
            if (buckets >= ("DAY".equals(groupBy) ? 366 : 1200)) {
                badRequest("Khoảng báo cáo quá dài: tối đa 366 ngày hoặc 1200 tháng. Hãy chọn nhóm theo tháng hoặc thu hẹp khoảng ngày.");
            }
        }
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    static void badRequest(String message) {
        throw new CustomException(message, HttpStatus.BAD_REQUEST);
    }
}
