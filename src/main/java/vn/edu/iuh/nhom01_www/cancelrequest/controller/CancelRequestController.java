package vn.edu.iuh.nhom01_www.cancelrequest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestDto;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.ResolveCancelRequestDto;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/cancel-requests")
@RequiredArgsConstructor
public class CancelRequestController {

    private final CancelRequestService cancelRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<CancelRequestResponse>> submitCancelRequest(@Valid @RequestBody CancelRequestDto request) {
        var created = cancelRequestService.submitCancelRequest(request.getBookingId(), request.getReason());
        return ResponseEntity.ok(new ApiResponse<>(true, "Yêu cầu hủy vé đã được ghi nhận và đang chờ nhân viên xử lý.", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CancelRequestResponse>>> getPendingRequests(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", cancelRequestService.getRequests(status)));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<CancelRequestResponse>> resolveCancelRequest(
            @PathVariable Long id, @Valid @RequestBody ResolveCancelRequestDto body) {
        var request = cancelRequestService.resolveCancelRequest(id, body.getIsApproved(), body.getRejectReason());
        return ResponseEntity.ok(new ApiResponse<>(true,
                body.getIsApproved() ? "Đã duyệt hủy vé theo yêu cầu của khách hàng." : "Đã từ chối yêu cầu hủy vé.", request));
    }
}
