package vn.edu.iuh.nhom01_www.role.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.role.service.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new CustomException("Role not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public Role createRole(Role role) {
        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new CustomException("Role already exists", HttpStatus.BAD_REQUEST);
        }
        return roleRepository.save(role);
    }

    @Override
    public Role updateRole(Long id, Role roleDetails) {
        Role role = getRoleById(id);
        role.setDescription(roleDetails.getDescription());
        return roleRepository.save(role);
    }

    @Override
    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        roleRepository.delete(role);
    }

    @Override
    public Role assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        Role role = getRoleById(roleId);
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.setPermissions(new HashSet<>(permissions));
        return roleRepository.save(role);
    }

    @Override
    public Set<Permission> getPermissionsByRole(Long roleId) {
        Role role = getRoleById(roleId);
        return role.getPermissions();
    }
}
