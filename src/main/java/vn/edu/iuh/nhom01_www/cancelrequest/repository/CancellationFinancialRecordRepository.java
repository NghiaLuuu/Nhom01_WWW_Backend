package vn.edu.iuh.nhom01_www.cancelrequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancellationFinancialRecord;
import java.math.BigDecimal;
import java.util.List;

public interface CancellationFinancialRecordRepository extends JpaRepository<CancellationFinancialRecord, Long> {
    @Query("SELECT COALESCE(SUM(r.feeAmount), 0) FROM CancellationFinancialRecord r")
    BigDecimal sumFees();
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM CancellationFinancialRecord r")
    BigDecimal sumRefunds();
    @Query("SELECT r.booking.id, SUM(r.feeAmount), SUM(r.refundAmount), MIN(r.feePercent), MAX(r.feePercent) FROM CancellationFinancialRecord r GROUP BY r.booking.id")
    List<Object[]> summarizeByBooking();
}
