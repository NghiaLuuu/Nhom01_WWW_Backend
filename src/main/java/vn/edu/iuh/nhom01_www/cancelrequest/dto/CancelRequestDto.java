package vn.edu.iuh.nhom01_www.cancelrequest.dto;

import lombok.Data;

@Data
public class CancelRequestDto {
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long bookingId;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 2000)
    private String reason;
}
