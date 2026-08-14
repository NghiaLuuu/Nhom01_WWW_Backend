package vn.edu.iuh.nhom01_www.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.user.dto.UpdatePasswordRequest;
import vn.edu.iuh.nhom01_www.user.service.UserService;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", userService.getProfile(email)));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Object>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return ResponseEntity.ok(new ApiResponse<>(true, "Password updated successfully", userService.updatePassword(email, request)));
    }
}
