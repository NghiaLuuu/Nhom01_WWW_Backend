package vn.edu.iuh.nhom01_www.cancelrequest.workflow.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto.CancellationDtos.RequestDetails;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.service.CancellationWorkflowService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

/** Missing endpoint already called by the existing customer profile; no old frontend file is changed. */
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('CUSTOMER')")
public class ProfileCancellationController {
    private final CancellationWorkflowService service;

    public ProfileCancellationController(CancellationWorkflowService service) { this.service = service; }

    @PatchMapping("/{bookingId}/request-cancel")
    public ApiResponse<RequestDetails> submit(@PathVariable Long bookingId) {
        return ApiResponse.success(service.submitFromProfile(bookingId), "Đã gửi yêu cầu hủy; booking và vé giữ nguyên trong khi chờ duyệt.");
    }
}
