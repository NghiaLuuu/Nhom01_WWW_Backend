package vn.edu.iuh.nhom01_www.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @JsonAlias("email")
    private String username;

    @NotBlank
    private String password;
}
