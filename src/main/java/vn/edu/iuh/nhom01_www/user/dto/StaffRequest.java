package vn.edu.iuh.nhom01_www.user.dto;

import lombok.Data;

@Data
public class StaffRequest {
    @jakarta.validation.constraints.Email
    @jakarta.validation.constraints.Size(max = 254)
    private String email;
    @jakarta.validation.constraints.Size(min = 1, max = 255)
    private String fullName;
    @jakarta.validation.constraints.Size(min = 8, max = 72)
    @lombok.ToString.Exclude
    private String password;
    @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_-]{2,40}")
    private String staffCode;
    @jakarta.validation.constraints.Size(max = 255)
    private String position;
    @jakarta.validation.constraints.Size(max = 255)
    private String workStation;
    private Long roleId;
    @jakarta.validation.constraints.Size(max = 50)
    private java.util.List<@jakarta.validation.constraints.NotBlank String> permissions;
    @jakarta.validation.constraints.Pattern(regexp = "\\+?[0-9]{8,15}")
    private String phoneNumber;
    @jakarta.validation.constraints.Past
    private java.time.LocalDate dateOfBirth;
}
