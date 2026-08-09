package vn.edu.iuh.nhom01_www.role.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class RoleRequest {
    @NotBlank
    private String name;
    private String description;
}
