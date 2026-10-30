package vn.edu.iuh.nhom01_www.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.user.model.Staff;

public interface StaffRepository extends JpaRepository<Staff, java.util.UUID> {
    Staff findByEmail(String email);
    Staff findByEmailIgnoreCase(String email);
    Staff findByStaffCodeIgnoreCase(String staffCode);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Staff s SET s.fullName = COALESCE(:fullName, s.fullName), s.phoneNumber = COALESCE(:phoneNumber, s.phoneNumber), s.dateOfBirth = COALESCE(:dateOfBirth, s.dateOfBirth) WHERE s.email = :email")
    void updateProfileByEmail(String email, String fullName, String phoneNumber, java.time.LocalDate dateOfBirth);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Staff s SET s.password = :password WHERE s.email = :email")
    void updatePasswordByEmail(String email, String password);
}
