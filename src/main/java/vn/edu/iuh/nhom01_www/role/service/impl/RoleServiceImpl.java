package vn.edu.iuh.nhom01_www.role.service.impl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.dto.RoleRequest;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.role.service.RoleService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    @Override
    public List<Role> getAllRoles() { return roleRepository.findAll(); }
    @Override
    public Role createRole(RoleRequest request) {
        if(roleRepository.findByName(request.getName()).isPresent()){
            throw new CustomException("Role already exists", HttpStatus.BAD_REQUEST);
        }
        Role role = Role.builder().name(request.getName()).description(request.getDescription()).build();
        return roleRepository.save(role);
    }
    @Override
    public Role findByName(String name) {
        return roleRepository.findByName(name).orElseThrow(() -> new CustomException("Role not found", HttpStatus.NOT_FOUND));
    }
}
