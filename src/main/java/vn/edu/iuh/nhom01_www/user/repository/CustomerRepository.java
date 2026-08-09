package vn.edu.iuh.nhom01_www.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.user.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, java.util.UUID> {
    Customer findByEmail(String email);
}
