package vn.edu.iuh.nhom01_www.cancelrequest.workflow.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto.CancellationDtos.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.service.CancellationWorkflowService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/cancellation-workflow")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerCancellationController {
    private final CancellationWorkflowService service;

    public CustomerCancellationController(CancellationWorkflowService service) { this.service = service; }

    @PostMapping("/requests")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<RequestDetails> submit(@Valid @RequestBody SubmitRequest input) {
        return ApiResponse.success(service.submitByCustomer(input), "Đã gửi yêu cầu hủy, chờ nhân viên xử lý.");
    }

    @GetMapping("/my-requests")
    public ApiResponse<PageResult<RequestRow>> history(@RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(service.myRequests(status, page, size));
    }

    @GetMapping("/my-requests/{id}")
    public ApiResponse<RequestDetails> details(@PathVariable Long id) { return ApiResponse.success(service.myDetails(id)); }
}
