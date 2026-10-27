package vn.edu.iuh.nhom01_www.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginApiResponse {
    private boolean success;
    private String message;
    private String token;
    private LoginUserResponse user;
    private JwtResponse data;
}
