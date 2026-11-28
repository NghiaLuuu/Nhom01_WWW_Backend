package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import vn.edu.iuh.nhom01_www.report.revenue.dto.RevenueReportDtos.*;
import vn.edu.iuh.nhom01_www.report.revenue.repository.RevenueReportRepository;
import vn.edu.iuh.nhom01_www.report.revenue.service.RevenueReportService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/** Explicit opt-in, isolated database only. Never boots the application or its seeders. */
@EnabledIfSystemProperty(named = "revenue.it.enabled", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevenueReportPostgresIntegrationTest {
    private JdbcTemplate jdbc;
    private RevenueReportService service;

    @BeforeAll
    void initializeIsolatedDatabase() {
        String url = System.getProperty("revenue.it.jdbc-url", "");
        if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/busbooking_revenue_it")) {
            throw new IllegalStateException("Revenue integration test requires a dedicated loopback busbooking_revenue_it database");
        }
        jdbc = new JdbcTemplate(new DriverManagerDataSource(url,
                System.getProperty("revenue.it.username"), System.getProperty("revenue.it.password")));
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).isEqualTo("busbooking_revenue_it");
        jdbc.execute("CREATE TABLE IF NOT EXISTS routes (id bigint PRIMARY KEY, departure_location text NOT NULL, arrival_location text NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS trips (id bigint PRIMARY KEY, route_id bigint NOT NULL REFERENCES routes(id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS bookings (id bigint PRIMARY KEY, trip_id bigint NOT NULL REFERENCES trips(id), status text, total_price numeric(38,2), created_at timestamp)");
        // Deliberately allow multiple payments per booking in this fixture to verify DISTINCT counts.
        jdbc.execute("CREATE TABLE IF NOT EXISTS payments (id bigint PRIMARY KEY, booking_id bigint NOT NULL REFERENCES bookings(id), amount numeric(38,2), status text, payment_method text, transaction_id text, created_at timestamp)");
        service = new RevenueReportService(new RevenueReportRepository(new NamedParameterJdbcTemplate(jdbc)));
    }

    @BeforeEach
    void fixture() {
        jdbc.execute("TRUNCATE payments, bookings, trips, routes");
        jdbc.execute("INSERT INTO routes VALUES (1,'TP.HCM','Đà Lạt'),(2,'Hà Nội','Hải Phòng'),(3,'Đà Nẵng','Huế')");
        jdbc.execute("INSERT INTO trips VALUES (10,1),(20,2)");
        jdbc.execute("""
                INSERT INTO bookings VALUES
                (101,10,'PAID',9999999,'2020-01-01'),(102,10,'CANCELLED',100000,'2020-01-01'),
                (103,10,'PENDING_PAYMENT',99999999,'2020-01-01'),(104,20,'PAID',500000,'2020-01-01'),
                (105,10,'PAID',380000,'2020-01-01'),(106,10,'PAID',200000,'2020-01-01')
                """);
        jdbc.execute("""
                INSERT INTO payments VALUES
                (1,101,250000.25,'SUCCESS','VNPAY','P1','2026-11-01 00:00:00'),
                (2,102,100000.50,'SUCCESS','CASH','P2','2026-11-02 12:00:00'),
                (3,103,99999999,'FAILED','VNPAY','P3','2026-11-02 12:00:00'),
                (4,104,500000.25,'SUCCESS','MOMO','P4','2026-11-03 23:59:59.999999'),
                (5,105,80000,' success ',' cash ','RESCHEDULE_5','2026-11-03 08:00:00'),
                (6,101,123456,'SUCCESS','VNPAY','P6','2026-10-31 23:59:59.999999'),
                (7,104,999999,'SUCCESS','VNPAY','P7','2026-11-04 00:00:00'),
                (8,103,5000000,'PENDING','CASH','P8','2026-11-02 12:00:00'),
                (9,106,150000,'SUCCESS','CASH','P9','2026-11-03 09:00:00'),
                (10,106,50000,'SUCCESS','VNPAY','P10','2026-11-03 10:00:00')
                """);
    }

    @Test
    void sumsOnlySuccessfulPaymentsAtPreciseDateBoundariesAndCountsBookingsOnce() {
        Report report = report("2026-11-01", "2026-11-03", "DAY", null, null);
        assertThat(report.summary().amount()).isEqualByComparingTo("1130001.00");
        assertThat(report.summary().transactionCount()).isEqualTo(6);
        assertThat(report.summary().bookingCount()).isEqualTo(5);
        assertThat(report.summary().cancelledBookingAmount()).isEqualByComparingTo("100000.50");
        assertThat(report.summary().cancelledBookingCount()).isEqualTo(1);
        assertThat(report.summary().cancelledTransactionCount()).isEqualTo(1);
        assertThat(report.timeSeries()).hasSize(3);
        assertThat(report.timeSeries().get(2).amount()).isEqualByComparingTo("780000.25");
        assertThat(report.timeSeries().get(2).transactionCount()).isEqualTo(4);
        assertThat(report.timeSeries().get(2).bookingCount()).isEqualTo(3);
    }

    @Test
    void groupsByRouteAndPaymentMethodWithoutJoiningTicketRows() {
        Report report = report("2026-11-01", "2026-11-03", "MONTH", null, null);
        assertThat(report.byRoute()).hasSize(2);
        assertThat(report.byRoute().get(0).routeId()).isEqualTo(1L);
        assertThat(report.byRoute().get(0).amount()).isEqualByComparingTo("630000.75");
        assertThat(report.byPaymentMethod()).hasSize(3);
        assertThat(report.byPaymentMethod().stream().filter(row -> row.paymentMethod().equals("CASH")).findFirst().orElseThrow().amount())
                .isEqualByComparingTo("330000.50");
        assertThat(report.timeSeries()).singleElement().satisfies(row -> assertThat(row.period()).isEqualTo("2026-11"));
    }

    @Test
    void countsOnlyTheActualAdditionalPaymentForRescheduling() {
        Report report = report("2026-11-03", "2026-11-03", "DAY", 1L, " cash ");
        assertThat(report.summary().amount()).isEqualByComparingTo("230000");
        assertThat(report.summary().transactionCount()).isEqualTo(2);
        assertThat(report.summary().bookingCount()).isEqualTo(2);
    }

    @Test
    void returnsZeroForNoTransactionsAndFillsMissingPeriods() {
        Report report = report("2026-11-01", "2026-11-03", "DAY", 3L, null);
        assertThat(report.summary().amount()).isEqualByComparingTo("0");
        assertThat(report.summary().transactionCount()).isZero();
        assertThat(report.timeSeries()).hasSize(3).allSatisfy(row -> assertThat(row.amount()).isEqualByComparingTo("0"));
        assertThat(report.byRoute()).isEmpty();
    }

    @Test
    void bindsMethodFiltersInsteadOfConcatenatingSql() {
        Report report = report("2026-11-01", "2026-11-03", "DAY", null, "CASH' OR 1=1 --");
        assertThat(report.summary().amount()).isEqualByComparingTo("0");
    }

    @Test
    void retainsArbitraryPrecisionForVeryLargeRevenueValues() {
        jdbc.update("UPDATE payments SET amount = 900719925474099312345.75 WHERE id = 1");
        assertThat(report("2026-11-01", "2026-11-03", "YEAR", null, null).summary().amount())
                .isEqualByComparingTo("900719925474100192346.50");
    }

    @Test
    void returnsHistoricalRoutesAndNormalizedSuccessfulPaymentMethods() {
        Options options = service.options();
        assertThat(options.routes()).hasSize(3);
        assertThat(options.paymentMethods()).containsExactly("CASH", "MOMO", "VNPAY");
    }

    private Report report(String from, String to, String grouping, Long route, String method) {
        return service.report(LocalDate.parse(from), LocalDate.parse(to), grouping, route, method);
    }
}
