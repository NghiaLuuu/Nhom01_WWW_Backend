package vn.edu.iuh.nhom01_www.user.adminmanagement.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.user.adminmanagement.dto.UserManagementDtos.*;
import vn.edu.iuh.nhom01_www.user.adminmanagement.service.AdminUserManagementService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/user-management")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserManagementController {
    private final AdminUserManagementService service;

    public AdminUserManagementController(AdminUserManagementService service) {
        this.service = service;
    }

    @GetMapping("/options")
    public ApiResponse<Options> options() {
        return ApiResponse.success(service.options());
    }

    @GetMapping("/staff")
    public ApiResponse<PageResult<UserAccount>> staff(
            @RequestParam(defaultValue = "") @Size(max = 200) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Positive Long roleId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.listStaff(keyword, status, roleId, page, size));
    }

    @GetMapping("/staff/{id}")
    public ApiResponse<UserAccount> staff(@PathVariable UUID id) {
        return ApiResponse.success(service.getStaff(id));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<UserAccount>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createStaff(request), "Đã tạo nhân viên."));
    }

    @PutMapping("/staff/{id}")
    public ApiResponse<UserAccount> updateStaff(@PathVariable UUID id, @Valid @RequestBody UpdateStaffRequest request) {
        return ApiResponse.success(service.updateStaff(id, request), "Đã cập nhật nhân viên.");
    }

    @PatchMapping("/staff/{id}/status")
    public ApiResponse<UserAccount> staffStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.success(service.changeStaffStatus(id, request), "Đã cập nhật trạng thái nhân viên.");
    }

    @PutMapping("/staff/{id}/password")
    public ApiResponse<Void> staffPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        service.resetStaffPassword(id, request);
        return ApiResponse.success(null, "Đã đặt lại mật khẩu và thu hồi phiên đăng nhập cũ.");
    }

    @GetMapping("/customers")
    public ApiResponse<PageResult<UserAccount>> customers(
            @RequestParam(defaultValue = "") @Size(max = 200) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.listCustomers(keyword, status, page, size));
    }

    @GetMapping("/customers/{id}")
    public ApiResponse<UserAccount> customer(@PathVariable UUID id) {
        return ApiResponse.success(service.getCustomer(id));
    }

    @PatchMapping("/customers/{id}/status")
    public ApiResponse<UserAccount> customerStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return ApiResponse.success(service.changeCustomerStatus(id, request), "Đã cập nhật trạng thái khách hàng.");
    }

    @PutMapping("/customers/{id}/password")
    public ApiResponse<Void> customerPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        service.resetCustomerPassword(id, request);
        return ApiResponse.success(null, "Đã đặt lại mật khẩu và thu hồi phiên đăng nhập cũ.");
    }
}
