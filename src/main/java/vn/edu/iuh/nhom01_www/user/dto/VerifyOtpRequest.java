package vn.edu.iuh.nhom01_www.user.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    @jakarta.validation.constraints.NotBlank
    private String email;
    @jakarta.validation.constraints.NotBlank
    private String otp;
}
