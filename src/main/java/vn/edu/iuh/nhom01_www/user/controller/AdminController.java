package vn.edu.iuh.nhom01_www.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.dto.PageResponse;
import vn.edu.iuh.nhom01_www.user.dto.ManagementViews.*;
import vn.edu.iuh.nhom01_www.user.dto.StaffRequest;
import vn.edu.iuh.nhom01_www.user.dto.StatusRequest;
import vn.edu.iuh.nhom01_www.user.service.AdminManagementService;
import vn.edu.iuh.nhom01_www.user.service.UserService;
import vn.edu.iuh.nhom01_www.payment.service.RevenueReportService;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Transactional
public class AdminController {
    private final UserService userService;
    private final AdminManagementService management;
    private final RevenueReportService revenue;
    private final vn.edu.iuh.nhom01_www.booking.service.BookingService bookingService;

    @GetMapping("/staffs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<PageResponse<StaffView>> staffs(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(management.staffs(keyword, status, page, size));
    }

    @GetMapping("/staffs/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<StaffView> staff(@PathVariable UUID id) { return ok(management.staff(id)); }

    @PostMapping("/staffs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<StaffView> createStaff(@Valid @RequestBody StaffRequest request) {
        return ok(StaffView.from(userService.createStaff(request)));
    }

    @PutMapping("/staffs/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<StaffView> updateStaff(@PathVariable UUID id, @Valid @RequestBody StaffRequest request) {
        return ok(StaffView.from(userService.updateStaff(id, request)));
    }

    @PatchMapping("/staffs/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<StaffView> staffStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return ok(StaffView.from(userService.updateStaffStatus(id, request.status().name())));
    }

    @PatchMapping("/staffs/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<StaffView> resetStaffPassword(@PathVariable UUID id,
            @Valid @RequestBody vn.edu.iuh.nhom01_www.user.dto.AdminResetPasswordRequest request) {
        return ok(StaffView.from(userService.resetStaffPassword(id, request.newPassword())));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'CUSTOMER_MANAGE')")
    public ApiResponse<PageResponse<CustomerView>> customers(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(management.customers(keyword, status, page, size));
    }

    @GetMapping("/customers/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'CUSTOMER_MANAGE')")
    public ApiResponse<CustomerView> customer(@PathVariable UUID id) { return ok(management.customer(id)); }

    @GetMapping("/customers/{id}/bookings")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'CUSTOMER_MANAGE')")
    public ApiResponse<PageResponse<BookingView>> customerBookings(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ok(management.customerBookings(id, page, size));
    }

    @PatchMapping("/customers/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'CUSTOMER_MANAGE')")
    public ApiResponse<CustomerView> customerStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return ok(CustomerView.from(userService.updateCustomerStatus(id, request.status().name())));
    }

    @PatchMapping("/customers/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<CustomerView> resetCustomerPassword(@PathVariable UUID id,
            @Valid @RequestBody vn.edu.iuh.nhom01_www.user.dto.AdminResetPasswordRequest request) {
        return ok(CustomerView.from(userService.resetCustomerPassword(id, request.newPassword())));
    }

    @GetMapping("/statistics/revenue")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'VIEW_STATISTICS')")
    public ApiResponse<RevenueReportService.Report> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") RevenueReportService.GroupBy groupBy) {
        LocalDate end = to == null ? LocalDate.now() : to;
        return ok(revenue.report(from == null ? end.withDayOfMonth(1) : from, end, groupBy));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'MANAGE_TICKET')")
    public ApiResponse<java.util.List<vn.edu.iuh.nhom01_www.booking.model.Booking>> bookings() {
        return ok(bookingService.getAllBookings());
    }

    private static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, "Success", data); }
}
