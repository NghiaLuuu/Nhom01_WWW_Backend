package vn.edu.iuh.nhom01_www.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    interface RevenueRow {
        String getPeriod();
        String getPaymentMethod();
        java.math.BigDecimal getRevenue();
        Long getPaymentCount();
        java.math.BigDecimal getCancelledBookingReceipts();
    }

    // Group by SELECT positions: repeating :format in GROUP BY produces a second
    // JDBC parameter, which PostgreSQL does not consider the same expression.
    @org.springframework.data.jpa.repository.Query(value = """
            select to_char(p.created_at, :format) as period, p.payment_method as "paymentMethod",
                   sum(p.amount) as revenue, count(*) as "paymentCount",
                   sum(case when b.status = 'CANCELLED' then p.amount else 0 end) as "cancelledBookingReceipts"
            from payments p join bookings b on b.id = p.booking_id
            where p.status = 'SUCCESS' and p.created_at >= :start and p.created_at < :end
            group by 1, 2
            order by 1, 2
            """, nativeQuery = true)
    java.util.List<RevenueRow> aggregateRevenue(
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
            @org.springframework.data.repository.query.Param("format") String format);
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByTransactionId(String transactionId);
}
