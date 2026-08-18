package vn.edu.iuh.nhom01_www.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.user.model.Staff;

public interface StaffRepository extends JpaRepository<Staff, java.util.UUID> {
    Staff findByEmail(String email);
}
