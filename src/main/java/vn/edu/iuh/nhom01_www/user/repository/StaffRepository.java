package vn.edu.iuh.nhom01_www.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.user.model.Staff;

@Repository
public interface StaffRepository extends JpaRepository<Staff, java.util.UUID> {
    Staff findByEmail(String email);
}
