package vn.edu.iuh.nhom01_www.cancelrequest.service;

import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse;

import java.util.List;

public interface CancelRequestService {
    CancelRequestResponse submitCancelRequest(Long bookingId, String reason);
    List<CancelRequestResponse> getRequests(String status);
    CancelRequestResponse resolveCancelRequest(Long cancelRequestId, boolean isApproved, String rejectReason);
}
