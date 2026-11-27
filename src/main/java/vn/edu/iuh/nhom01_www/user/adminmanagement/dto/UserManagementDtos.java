package vn.edu.iuh.nhom01_www.user.adminmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** API contracts for the new module; existing entities are never serialized directly. */
public final class UserManagementDtos {
    private UserManagementDtos() {
    }

    public record StaffDetails(
            @NotBlank @Size(max = 255) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @Pattern(regexp = "^$|^[+0-9][0-9 .()-]{7,24}$", message = "Số điện thoại không hợp lệ") String phoneNumber,
            @Past LocalDate dateOfBirth,
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_-]+") String staffCode,
            @PastOrPresent LocalDate hireDate,
            @Size(max = 255) String position,
            @Size(max = 255) String workStation,
            @NotNull @Positive Long roleId,
            @NotNull @Size(max = 100) List<@NotBlank @Size(max = 100) String> permissions) {
    }

    public record CreateStaffRequest(
            @NotNull @jakarta.validation.Valid StaffDetails details,
            @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự") String password) {
        @Override
        public String toString() {
            return "CreateStaffRequest[password=REDACTED]";
        }
    }

    public record UpdateStaffRequest(
            @NotNull @jakarta.validation.Valid StaffDetails details,
            @NotNull @PositiveOrZero Long version) {
    }

    public record ChangeStatusRequest(
            @NotBlank @Pattern(regexp = "ACTIVE|LOCKED") String status,
            @NotNull @PositiveOrZero Long version) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự") String password,
            @NotNull @PositiveOrZero Long version) {
        @Override
        public String toString() {
            return "ResetPasswordRequest[password=REDACTED, version=" + version + "]";
        }
    }

    public record RoleOption(Long id, String name, String description, List<String> permissions) {
    }

    public record PermissionOption(Long id, String code, String description) {
    }

    public record Options(List<RoleOption> roles, List<PermissionOption> permissions) {
    }

    public record UserAccount(
            UUID id, String fullName, String email, String phoneNumber, LocalDate dateOfBirth,
            String status, LocalDateTime createdAt, LocalDateTime updatedAt, Long version,
            String staffCode, LocalDate hireDate, String position, String workStation, String address,
            RoleOption role, List<String> permissions, List<String> effectivePermissions) {
    }

    public record PageResult<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
    }
}
