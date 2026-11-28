package vn.edu.iuh.nhom01_www.cancelrequest.workflow.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;

@RestControllerAdvice(assignableTypes = {StaffCancellationController.class, CustomerCancellationController.class, ProfileCancellationController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CancellationExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CancellationExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> business(CustomException error) { return response(error.getStatus(), error.getMessage()); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied() { return response(HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý yêu cầu hủy vé."); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException error) {
        return response(HttpStatus.BAD_REQUEST, error.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage()).findFirst().orElse("Dữ liệu không hợp lệ."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> malformed() { return response(HttpStatus.BAD_REQUEST, "Dữ liệu không đúng định dạng; mã booking/yêu cầu phải là số nguyên."); }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> conflict() { return response(HttpStatus.CONFLICT, "Yêu cầu bị trùng hoặc vừa được xử lý. Vui lòng tải lại danh sách."); }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> unavailable(DataAccessException error) {
        LOG.error("Cancellation workflow database error", error);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Không truy cập được dữ liệu yêu cầu hủy. Vui lòng thử lại.");
    }

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
}
