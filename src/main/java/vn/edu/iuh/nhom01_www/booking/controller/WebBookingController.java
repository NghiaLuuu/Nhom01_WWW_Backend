package vn.edu.iuh.nhom01_www.booking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.nhom01_www.booking.dto.WebBookingView;
import vn.edu.iuh.nhom01_www.booking.service.WebBookingQueryService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import java.util.List;

@RestController
@RequestMapping("/api/web/bookings")
@RequiredArgsConstructor
public class WebBookingController {
    private final WebBookingQueryService bookings;
    private final CurrentUserService currentUser;

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<WebBookingView>> history() {
        return new ApiResponse<>(true, "Success", bookings.history(currentUser.getCurrentCustomer().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'MANAGE_TICKET', 'TICKET_MANAGE')")
    public ApiResponse<List<WebBookingView>> management() {
        return new ApiResponse<>(true, "Success", bookings.management());
    }
}
