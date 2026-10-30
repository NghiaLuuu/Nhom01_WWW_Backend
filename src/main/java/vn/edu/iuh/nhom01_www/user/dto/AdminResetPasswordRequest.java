package vn.edu.iuh.nhom01_www.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(@NotBlank @Size(min = 8, max = 72) String newPassword) {}
