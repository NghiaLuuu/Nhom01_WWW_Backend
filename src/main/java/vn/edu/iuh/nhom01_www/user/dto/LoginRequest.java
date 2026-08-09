package vn.edu.iuh.nhom01_www.user.dto;

import lombok.Data;

@Data
public class LoginRequest {
    @jakarta.validation.constraints.NotBlank
    private String email;
    @jakarta.validation.constraints.NotBlank
    private String password;
}
