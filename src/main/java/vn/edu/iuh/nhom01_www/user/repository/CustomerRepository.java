package vn.edu.iuh.nhom01_www.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, java.util.UUID> {
    Customer findByEmail(String email);
}
