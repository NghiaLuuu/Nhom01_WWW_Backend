package vn.edu.iuh.nhom01_www.cancelrequest.service;

import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;

import java.util.UUID;

public interface CancelRequestService {
    CancelRequest submitCancelRequest(Long bookingId, String reason);
    CancelRequest resolveCancelRequest(Long cancelRequestId, UUID staffId, boolean isApproved, String rejectReason);
}
