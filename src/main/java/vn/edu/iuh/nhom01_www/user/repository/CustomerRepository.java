package vn.edu.iuh.nhom01_www.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, java.util.UUID> {
    Customer findByEmail(String email);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Customer c SET c.fullName = COALESCE(:fullName, c.fullName), c.phoneNumber = COALESCE(:phoneNumber, c.phoneNumber), c.dateOfBirth = COALESCE(:dateOfBirth, c.dateOfBirth), c.address = COALESCE(:address, c.address) WHERE c.email = :email")
    void updateProfileByEmail(String email, String fullName, String phoneNumber, java.time.LocalDate dateOfBirth, String address);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Customer c SET c.password = :password WHERE c.email = :email")
    void updatePasswordByEmail(String email, String password);
}
