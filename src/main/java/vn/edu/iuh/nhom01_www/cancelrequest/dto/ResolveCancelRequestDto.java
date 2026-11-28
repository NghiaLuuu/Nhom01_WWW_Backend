package vn.edu.iuh.nhom01_www.cancelrequest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResolveCancelRequestDto {
    @NotNull
    private Boolean isApproved;

    @Size(max = 1000)
    private String rejectReason;
}
