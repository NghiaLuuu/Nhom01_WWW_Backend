package vn.edu.iuh.nhom01_www.role.service;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.dto.RoleRequest;
import java.util.List;
public interface RoleService {
    List<Role> getAllRoles();
    Role createRole(RoleRequest request);
    Role findByName(String name);
}
