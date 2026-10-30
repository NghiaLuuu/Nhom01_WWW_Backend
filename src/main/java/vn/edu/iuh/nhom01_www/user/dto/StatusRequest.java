package vn.edu.iuh.nhom01_www.user.dto;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull AccountStatus status) {
    public enum AccountStatus { ACTIVE, LOCKED }
}
