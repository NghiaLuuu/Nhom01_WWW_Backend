package vn.edu.iuh.nhom01_www.role.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionRepository permissionRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", roleService.getAllRoles()));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", permissionRepository.findAll()));
    }
}
