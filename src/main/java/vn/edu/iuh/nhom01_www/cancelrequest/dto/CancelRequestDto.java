package vn.edu.iuh.nhom01_www.cancelrequest.dto;

import lombok.Data;

@Data
public class CancelRequestDto {
    @jakarta.validation.constraints.NotNull
    private Long bookingId;
    private String reason;
}
