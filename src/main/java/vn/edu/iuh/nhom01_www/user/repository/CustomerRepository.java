package vn.edu.iuh.nhom01_www.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Customer> {
    Customer findByEmailIgnoreCase(String email);
    Customer findByEmail(String email);
    Optional<Customer> findOptionalByEmail(String email);
    Optional<Customer> findByPhoneNumber(String phoneNumber);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Customer c SET c.fullName = COALESCE(:fullName, c.fullName), c.phoneNumber = COALESCE(:phoneNumber, c.phoneNumber), c.dateOfBirth = COALESCE(:dateOfBirth, c.dateOfBirth), c.address = COALESCE(:address, c.address) WHERE c.email = :email")
    void updateProfileByEmail(String email, String fullName, String phoneNumber, java.time.LocalDate dateOfBirth, String address);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Customer c SET c.password = :password WHERE c.email = :email")
    void updatePasswordByEmail(String email, String password);
}
