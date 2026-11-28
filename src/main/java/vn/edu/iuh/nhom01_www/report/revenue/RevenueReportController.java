package vn.edu.iuh.nhom01_www.report.revenue;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/revenue-reports")
@RequiredArgsConstructor
public class RevenueReportController {
    private final RevenueReportService service;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_STATISTICS')")
    public ApiResponse<RevenueReportResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "MONTH") String groupBy,
            @RequestParam(defaultValue = "REAL") String scope,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String paymentMethod) {
        return ApiResponse.success(service.summary(new RevenueReportFilter(fromDate, toDate, groupBy, scope, routeId, paymentMethod)));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('VIEW_STATISTICS')")
    public ApiResponse<RevenueTransactionPage> transactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "REAL") String scope,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(required = false) String bookingStatus) {
        var filter = new RevenueReportFilter(fromDate, toDate, "MONTH", scope, routeId, paymentMethod);
        return ApiResponse.success(bookingStatus == null || bookingStatus.isBlank()
                ? service.transactions(filter, page, size, search, status, sort)
                : service.transactions(filter, page, size, search, status, sort, bookingStatus));
    }

    // Scope error handling to this new controller; the legacy global handler remains unchanged.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền VIEW_STATISTICS để xem báo cáo."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> invalidParameter() {
        return ResponseEntity.badRequest().body(ApiResponse.error("Tham số không hợp lệ. Ngày phải theo định dạng yyyy-MM-dd; trang và mã tuyến phải là số."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> dataUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Không thể truy xuất dữ liệu báo cáo lúc này. Vui lòng thử lại sau."));
    }
}
