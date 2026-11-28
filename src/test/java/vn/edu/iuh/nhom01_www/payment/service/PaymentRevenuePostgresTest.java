package vn.edu.iuh.nhom01_www.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in PostgreSQL regression test. Only SELECTs, using a read-only connection. */
@EnabledIfEnvironmentVariable(named = "VEXE_REPORT_TEST_URL", matches = ".+")
class PaymentRevenuePostgresTest {
    @Test void dayAggregationMatchesReceiptsAndPreservesProjectionAliases() throws Exception {
        verifyReport("YYYY-MM-DD");
    }

    @Test void monthAggregationMatchesReceiptsAndPreservesProjectionAliases() throws Exception {
        verifyReport("YYYY-MM");
    }

    private void verifyReport(String format) throws Exception {
        String sql = PaymentRepository.class.getMethod("aggregateRevenue",
                LocalDateTime.class, LocalDateTime.class, String.class).getAnnotation(Query.class).value();
        var end = LocalDate.now().plusDays(1).atStartOfDay();
        var start = end.minusDays(3660);
        var parameters = Map.of("start", start, "end", end, "format", format);
        try (var connection = DriverManager.getConnection(System.getenv("VEXE_REPORT_TEST_URL"),
                System.getenv("VEXE_REPORT_TEST_USER"), System.getenv("VEXE_REPORT_TEST_PASSWORD"))) {
            connection.setReadOnly(true);
            connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                var jdbc = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));
                var rows = jdbc.queryForList(sql, parameters);
                var projections = new SpelAwareProxyProjectionFactory();
                BigDecimal actualRevenue = BigDecimal.ZERO;
                BigDecimal actualCancelled = BigDecimal.ZERO;
                long actualCount = 0;
                for (var values : rows) {
                    assertTrue(values.containsKey("paymentMethod"));
                    assertTrue(values.containsKey("paymentCount"));
                    assertTrue(values.containsKey("cancelledBookingReceipts"));
                    var row = projections.createProjection(PaymentRepository.RevenueRow.class, values);
                    assertNotNull(row.getPeriod());
                    assertNotNull(row.getPaymentMethod());
                    assertEquals(format.equals("YYYY-MM-DD") ? 10 : 7, row.getPeriod().length());
                    actualRevenue = actualRevenue.add(row.getRevenue());
                    actualCancelled = actualCancelled.add(row.getCancelledBookingReceipts());
                    actualCount += row.getPaymentCount();
                }
                var expected = jdbc.queryForMap("""
                        select coalesce(sum(p.amount), 0) as revenue, count(*) as count,
                               coalesce(sum(case when b.status = 'CANCELLED' then p.amount else 0 end), 0) as cancelled
                        from payments p join bookings b on b.id = p.booking_id
                        where p.status = 'SUCCESS' and p.created_at >= :start and p.created_at < :end
                        """, parameters);
                assertEquals(0, actualRevenue.compareTo((BigDecimal) expected.get("revenue")));
                assertEquals(0, actualCancelled.compareTo((BigDecimal) expected.get("cancelled")));
                assertEquals(((Number) expected.get("count")).longValue(), actualCount);
            } finally {
                connection.rollback();
            }
        }
    }
}
