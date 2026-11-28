package vn.edu.iuh.nhom01_www.user.adminmanagement.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;

/** Scoped to the new controller; the existing exception handler is left untouched. */
@RestControllerAdvice(assignableTypes = AdminUserManagementController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserManagementExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> business(CustomException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden() {
        return error(HttpStatus.FORBIDDEN, "Chỉ Admin được sử dụng chức năng quản lý này.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).findFirst().orElse("Dữ liệu không hợp lệ.");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodValidationException.class})
    public ResponseEntity<ApiResponse<Void>> parameters() {
        return error(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ. Trang phải từ 0, số dòng từ 1 đến 100.");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> malformedRequest() {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không đúng định dạng. Vui lòng kiểm tra mã tài khoản và các trường nhập.");
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> conflict() {
        return error(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vừa thay đổi. Vui lòng tải lại và kiểm tra email, mã nhân viên, số điện thoại.");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
}
