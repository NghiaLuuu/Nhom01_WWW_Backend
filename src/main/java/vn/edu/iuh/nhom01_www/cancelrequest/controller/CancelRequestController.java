package vn.edu.iuh.nhom01_www.cancelrequest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestDto;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cancel-requests")
@RequiredArgsConstructor
public class CancelRequestController {

    private final CancelRequestService cancelRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<CancelRequest>> submitCancelRequest(@Valid @RequestBody CancelRequestDto request) {
        CancelRequest created = cancelRequestService.submitCancelRequest(request.getBookingId(), request.getReason());
        return ResponseEntity.ok(new ApiResponse<>(true, "Cancel request submitted", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CancelRequest>>> getPendingRequests() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", Collections.emptyList()));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<CancelRequest>> resolveCancelRequest(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UUID staffId = UUID.randomUUID(); // Mocking authenticated staff ID
        boolean isApproved = (boolean) body.getOrDefault("isApproved", false);
        String rejectReason = (String) body.getOrDefault("rejectReason", "");
        
        CancelRequest request = cancelRequestService.resolveCancelRequest(id, staffId, isApproved, rejectReason);
        return ResponseEntity.ok(new ApiResponse<>(true, "Request resolved", request));
    }
}
