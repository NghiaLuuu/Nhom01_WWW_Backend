package vn.edu.iuh.nhom01_www.role.service;

import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;

import java.util.List;
import java.util.Set;

public interface RoleService {
    List<Role> getAllRoles();
    Role getRoleById(Long id);
    Role createRole(Role role);
    Role updateRole(Long id, Role role);
    void deleteRole(Long id);
    
    Role assignPermissionsToRole(Long roleId, List<Long> permissionIds);
    Set<Permission> getPermissionsByRole(Long roleId);
}
