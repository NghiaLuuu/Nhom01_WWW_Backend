package vn.edu.iuh.nhom01_www.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.iuh.nhom01_www.payment.model.Payment;


public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
