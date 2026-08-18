package vn.edu.iuh.nhom01_www.role.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.iuh.nhom01_www.role.model.Permission;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
}
