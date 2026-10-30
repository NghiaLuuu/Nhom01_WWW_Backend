package vn.edu.iuh.nhom01_www.cancelrequest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestDto;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.ResolveCancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.common.dto.PageResponse;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;

@RestController
@RequestMapping("/api/cancel-requests")
@RequiredArgsConstructor
@Transactional
public class CancelRequestController {
    private final CancelRequestService service;
    private final CurrentUserService currentUser;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CancelRequestResponse> submit(@Valid @RequestBody CancelRequestDto request) {
        return new ApiResponse<>(true, "Cancel request submitted", CancelRequestResponse.from(
                service.submitCancelRequest(request.getBookingId(), request.getReason())));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'TICKET_MANAGE', 'MANAGE_TICKET')")
    public ApiResponse<PageResponse<CancelRequestResponse>> search(@RequestParam(required = false) String status,
            @RequestParam(required = false) Long bookingId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return new ApiResponse<>(true, "Success", service.search(status, bookingId, page, size));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<PageResponse<CancelRequestResponse>> mine(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return new ApiResponse<>(true, "Success", service.mine(status, page, size));
    }

    @GetMapping("/mine/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CancelRequestResponse> myDetail(@PathVariable Long id) {
        return new ApiResponse<>(true, "Success", service.myDetail(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'TICKET_MANAGE', 'MANAGE_TICKET')")
    public ApiResponse<CancelRequestResponse> detail(@PathVariable Long id) {
        return new ApiResponse<>(true, "Success", service.detail(id));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'TICKET_MANAGE', 'MANAGE_TICKET')")
    public ApiResponse<CancelRequestResponse> resolve(@PathVariable Long id, @Valid @RequestBody ResolveCancelRequest request) {
        return new ApiResponse<>(true, "Request resolved", CancelRequestResponse.from(service.resolveCancelRequest(
                id, currentUser.getCurrentStaff().getId(), request.isApproved(), request.rejectReason())));
    }
}
