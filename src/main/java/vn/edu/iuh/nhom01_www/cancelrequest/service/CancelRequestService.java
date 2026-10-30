package vn.edu.iuh.nhom01_www.cancelrequest.service;

import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse;
import vn.edu.iuh.nhom01_www.common.dto.PageResponse;

import java.util.UUID;

public interface CancelRequestService {
    PageResponse<CancelRequestResponse> search(String status, Long bookingId, int page, int size);
    CancelRequestResponse detail(Long id);
    PageResponse<CancelRequestResponse> mine(String status, int page, int size);
    CancelRequestResponse myDetail(Long id);
    CancelRequest submitCancelRequest(Long bookingId, String reason);
    CancelRequest resolveCancelRequest(Long cancelRequestId, UUID staffId, boolean isApproved, String rejectReason);
}
