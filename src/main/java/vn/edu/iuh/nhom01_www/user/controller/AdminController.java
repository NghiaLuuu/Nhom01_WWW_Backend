package vn.edu.iuh.nhom01_www.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<Staff>>> getAllStaffs() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", userService.getAllStaffs()));
    }

    @PostMapping("/staffs")
    public ResponseEntity<ApiResponse<Staff>> createStaff(@RequestBody Object request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff created", null));
    }

    @PutMapping("/staffs/{id}")
    public ResponseEntity<ApiResponse<Staff>> updateStaff(@PathVariable UUID id, @RequestBody Object request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff updated", null));
    }

    @PatchMapping("/staffs/{id}/status")
    public ResponseEntity<ApiResponse<Staff>> updateStaffStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusBody) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Staff status updated", null));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", userService.getAllCustomers()));
    }

    @PatchMapping("/customers/{id}/status")
    public ResponseEntity<ApiResponse<Customer>> updateCustomerStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusBody) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer status updated", null));
    }

    @GetMapping("/statistics/revenue")
    public ResponseEntity<ApiResponse<Object>> getRevenueStatistics() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", userService.getRevenueStatistics()));
    }
}
