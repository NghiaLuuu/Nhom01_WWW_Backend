package vn.edu.iuh.nhom01_www.user.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    @jakarta.validation.constraints.NotBlank
    private String fullName;
    @jakarta.validation.constraints.Email
    private String email;
    private String password;
}
