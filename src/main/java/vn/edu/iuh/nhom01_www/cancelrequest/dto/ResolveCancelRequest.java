package vn.edu.iuh.nhom01_www.cancelrequest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveCancelRequest(@NotNull Boolean isApproved, @Size(max = 2000) String rejectReason) {}
