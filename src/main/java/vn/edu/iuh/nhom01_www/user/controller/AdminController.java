package vn.edu.iuh.nhom01_www.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.user.dto.StaffRequest;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/staffs")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<List<Staff>>> getAllStaffs() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", userService.getAllStaffs()));
    }

    @PostMapping("/staffs")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<Staff>> createStaff(@RequestBody StaffRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff created", userService.createStaff(request)));
    }

    @PutMapping("/staffs/{id}")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<Staff>> updateStaff(@PathVariable UUID id, @RequestBody StaffRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff updated", userService.updateStaff(id, request)));
    }

    @PatchMapping("/staffs/{id}/status")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<Staff>> updateStaffStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusBody) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff status updated", userService.updateStaffStatus(id, statusBody.get("status"))));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", userService.getAllCustomers()));
    }

    @PatchMapping("/customers/{id}/status")
    @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
    public ResponseEntity<ApiResponse<Customer>> updateCustomerStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusBody) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer status updated", userService.updateCustomerStatus(id, statusBody.get("status"))));
    }

    @GetMapping("/statistics/revenue")
    @PreAuthorize("hasAuthority('VIEW_STATISTICS')")
    public ResponseEntity<ApiResponse<Object>> getRevenueStatistics() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", userService.getRevenueStatistics()));
    }
}
