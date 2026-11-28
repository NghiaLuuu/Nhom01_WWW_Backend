package vn.edu.iuh.nhom01_www.report.revenue.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;

@RestControllerAdvice(assignableTypes = RevenueReportController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RevenueReportExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RevenueReportExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> business(CustomException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> invalidParameters() {
        return error(HttpStatus.BAD_REQUEST, "Ngày phải theo định dạng yyyy-MM-dd; routeId phải là số nguyên dương.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden() {
        return error(HttpStatus.FORBIDDEN, "Cần quyền Admin hoặc VIEW_STATISTICS để xem báo cáo.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> dataUnavailable(DataAccessException exception) {
        LOG.error("Revenue report query failed", exception);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Không truy xuất được dữ liệu báo cáo. Vui lòng thử lại sau.");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
}
