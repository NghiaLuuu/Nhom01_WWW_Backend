package vn.edu.iuh.nhom01_www.cancelrequest.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto.CancellationDtos.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.service.CancellationWorkflowService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/admin/cancel-requests")
@PreAuthorize("hasRole('ADMIN') or (hasRole('STAFF') and (hasAuthority('MANAGE_TICKET') or hasAuthority('TICKET_MANAGE')))")
public class StaffCancellationController {
    private final CancellationWorkflowService service;

    public StaffCancellationController(CancellationWorkflowService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResult<RequestRow>> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(service.staffList(keyword, status == null ? "PENDING" : status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<RequestDetails> details(@PathVariable Long id) { return ApiResponse.success(service.staffDetails(id)); }

    @GetMapping("/bookings/{bookingId}/eligibility")
    public ApiResponse<BookingInfo> eligibility(@PathVariable Long bookingId) { return ApiResponse.success(service.lookup(bookingId)); }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<RequestDetails> receive(@Valid @RequestBody SubmitRequest input) {
        return ApiResponse.success(service.submitByStaff(input), "Đã tiếp nhận yêu cầu hủy; booking và vé chưa bị thay đổi.");
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<RequestDetails> approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest input) {
        return ApiResponse.success(service.approve(id, input), "Đã duyệt hủy booking và vé. Chưa thực hiện hoàn tiền.");
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<RequestDetails> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest input) {
        return ApiResponse.success(service.reject(id, input), "Đã từ chối yêu cầu; booking và vé giữ nguyên.");
    }
}
