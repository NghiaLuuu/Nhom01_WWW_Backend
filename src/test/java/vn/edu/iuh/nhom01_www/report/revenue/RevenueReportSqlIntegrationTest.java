package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in SQL execution on PostgreSQL. The connection is READ ONLY and every reporting query
 * receives VALUES CTEs that shadow the business tables. No tables or real business rows are changed.
 * Run with -Drevenue.sql.it.enabled=true (optional -Drevenue.sql.it.url=jdbc:postgresql://...).
 */
@EnabledIfSystemProperty(named = "revenue.sql.it.enabled", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevenueReportSqlIntegrationTest {
    Connection connection;
    RevenueReportRepository repository;
    RevenueReportService service;

    private static final String FIXTURES = """
        WITH payments(id, booking_id, amount, payment_method, status, transaction_id, created_at) AS (
          VALUES
          (1::bigint,100::bigint,250000::numeric,'VNPAY'::text,'SUCCESS'::text,'REAL-1'::text,TIMESTAMP '2026-11-01 00:00:00'),
          (2,101,150000,'CASH','SUCCESS','REAL-2',TIMESTAMP '2026-11-30 23:59:59.999999'),
          (3,102,900000,'VNPAY','FAILED','REAL-3',TIMESTAMP '2026-11-15 10:00:00'),
          (4,103,600000,'DEMO_SIMULATION','SIMULATED_PAID','DEMO_TXN_4',TIMESTAMP '2026-11-10 10:00:00'),
          (5,104,100000,'VNPAY','PENDING','REAL-5',TIMESTAMP '2026-11-12 10:00:00'),
          (6,105,200000,'VNPAY','REFUNDED','REAL-6',TIMESTAMP '2026-11-13 10:00:00'),
          (7,106,300000,'VNPAY','SUCCESS','DEMO-legacy',TIMESTAMP '2026-11-15 10:00:00'),
          (8,107,100000,'VNPAY','SUCCESS','REAL-8',TIMESTAMP '2026-12-01 00:00:00'),
          (9,100,50000,'CASH','SUCCESS','REAL-9',TIMESTAMP '2026-11-02 10:00:00'),
          (10,108,80000,'VNPAY','SUCCESS','REAL-10',TIMESTAMP '2026-11-14 10:00:00'),
          (11,100,10000,'VNPAY','SUCCESS','REAL-11',TIMESTAMP '2026-10-31 23:59:59'),
          (12,108,70000,'VNPAY','SUCCESS','REAL-12',NULL::timestamp)
        ), bookings(id, customer_id, trip_id, status) AS (
          VALUES (100::bigint,1::bigint,10::bigint,'PAID'::text),(101,2,10,'CANCELLED'),
          (102,3,11,'PAID'),(103,4,11,'PAID'),(104,5,10,'PENDING_PAYMENT'),
          (105,6,11,'CANCELLED'),(106,7,11,'PAID'),(107,8,10,'PAID'),(108,9,11,'PAID')
        ), tickets(id, booking_id) AS (
          VALUES (1::bigint,100::bigint),(2,100),(3,101),(4,102),(5,103),(6,103),(7,103),
          (8,104),(9,105),(10,106),(11,106),(12,108),(13,107)
        ), trips(id, route_id, departure_time) AS (
          VALUES (10::bigint,1::bigint,TIMESTAMP '2026-12-15 10:00:00'),(11,2,TIMESTAMP '2026-12-16 10:00:00')
        ), routes(id, departure_location, arrival_location) AS (
          VALUES (1::bigint,'TP.HCM'::text,'Đà Lạt'::text),(2,'Hà Nội','Sapa')
        ), customers(id, full_name) AS (
          VALUES (1::bigint,'O''Brien'::text),(2,'Khách 2'),(3,'Khách 3'),(4,'Khách 4'),
          (5,'Khách 5'),(6,'Khách 6'),(7,'Khách 7'),(8,'Khách 8'),(9,'Khách 9')
        )
        """;

    @BeforeAll
    void connectReadOnly() throws Exception {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));
        Properties configuration = yaml.getObject();
        assertNotNull(configuration);
        Properties credentials = new Properties();
        credentials.setProperty("user", configuration.getProperty("spring.datasource.username"));
        credentials.setProperty("password", configuration.getProperty("spring.datasource.password"));
        credentials.setProperty("connectTimeout", "3");
        credentials.setProperty("socketTimeout", "15");
        String url = System.getProperty("revenue.sql.it.url", configuration.getProperty("spring.datasource.url"));
        connection = DriverManager.getConnection(url, credentials);
        connection.setReadOnly(true);
        connection.setAutoCommit(false);
        Connection fixtureConnection = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("prepareStatement".equals(method.getName()) && args != null && args[0] instanceof String sql) {
                        if (!sql.stripLeading().toUpperCase(java.util.Locale.ROOT).startsWith("SELECT ")) {
                            throw new SQLException("Only SELECT statements are permitted in this test");
                        }
                        args[0] = FIXTURES + sql;
                    }
                    try { return method.invoke(connection, args); }
                    catch (InvocationTargetException exception) { throw exception.getCause(); }
                });
        var jdbc = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(fixtureConnection, true));
        assertEquals("on", jdbc.getJdbcTemplate().queryForObject("SELECT current_setting('transaction_read_only')", String.class));
        repository = new RevenueReportRepository(jdbc);
        service = new RevenueReportService(repository);
    }

    @AfterAll
    void close() throws Exception {
        if (connection != null) { connection.rollback(); connection.close(); }
    }

    private RevenueReportFilter filter(String scope) {
        return new RevenueReportFilter(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "DAY", scope, null, null);
    }

    @Test
    void realRevenueKeepsCancelledPaidBookingsAndExcludesDemoFailedPendingRefundedAndOutsideDates() {
        var report = service.summary(filter("REAL"));
        var totals = report.totals();
        assertEquals(0, new BigDecimal("530000").compareTo(totals.successfulAmount()));
        assertEquals(4, totals.successfulPayments());
        assertEquals(7, totals.paymentCount());
        assertEquals(6, totals.bookingCount());
        assertEquals(3, totals.successfulBookingCount());
        assertEquals(4, totals.ticketCount(), "Two payments for one booking must not double its two tickets");
        assertEquals(1, totals.failedPayments());
        assertEquals(1, totals.pendingPayments());
        assertEquals(1, totals.refundedPayments());
        assertEquals(30, report.trend().size());
        assertEquals(0, totals.successfulAmount().compareTo(report.trend().stream().map(RevenueReportResponse.TrendPoint::amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        assertEquals(0, totals.successfulAmount().compareTo(report.routes().stream().map(RevenueReportResponse.RouteRevenue::amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        assertEquals(0, totals.successfulAmount().compareTo(report.methods().stream().map(RevenueReportResponse.MethodRevenue::amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
        assertEquals(2, report.bookingStatuses().stream().filter(row -> "CANCELLED".equals(row.status())).findFirst().orElseThrow().count());
    }

    @Test
    void demoRevenueIncludesExplicitSimulationAndLegacyDemoTransactionIds() {
        var totals = repository.totals(filter("DEMO"));
        assertEquals(0, new BigDecimal("900000").compareTo(totals.successfulAmount()));
        assertEquals(2, totals.successfulPayments());
        assertEquals(5, totals.ticketCount());
    }

    @Test
    void routeMethodSearchSortAndDatabasePaginationWorkWithRealSql() {
        var cashRoute = new RevenueReportFilter(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "DAY", "REAL", 1L, "CASH");
        assertEquals(0, new BigDecimal("200000").compareTo(repository.totals(cashRoute).successfulAmount()));
        var result = service.transactions(filter("REAL"), 1, 1, "O'Brien", "SUCCESS", "AMOUNT_DESC");
        assertEquals(2, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(9L, result.content().get(0).paymentId());
        assertEquals(2, result.content().get(0).ticketCount());
        assertEquals("O'Brien", result.content().get(0).customerName());
    }

    @Test
    void cancelledBookingWithSuccessfulPaymentIsFoundByBookingFilterNotPaymentFilter() {
        var filter = filter("REAL");
        var before = repository.totals(filter).successfulAmount();
        assertEquals(0, service.transactions(filter, 0, 15, null, "CANCELLED", "NEWEST").totalElements());

        var cancelledOrders = service.transactions(filter, 0, 15, null, null, "NEWEST", "CANCELLED");
        assertEquals(2, cancelledOrders.totalElements());
        assertTrue(cancelledOrders.content().stream().allMatch(row -> "CANCELLED".equals(row.bookingStatus())));
        assertTrue(cancelledOrders.content().stream().anyMatch(row -> "SUCCESS".equals(row.paymentStatus())));

        var cancelledButPaid = service.transactions(filter, 0, 15, null, "SUCCESS", "NEWEST", "CANCELLED");
        assertEquals(1, cancelledButPaid.totalElements());
        assertEquals(101L, cancelledButPaid.content().get(0).bookingId());
        assertEquals("SUCCESS", cancelledButPaid.content().get(0).paymentStatus());
        assertEquals(0, before.compareTo(repository.totals(filter).successfulAmount()), "Filtering must not change payment states or revenue");
    }

    @Test
    void emptyRangeAndMonthlyGroupingReturnConsistentZerosAndBuckets() {
        var empty = new RevenueReportFilter(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2), "DAY", "REAL", null, null);
        assertEquals(BigDecimal.ZERO, repository.totals(empty).successfulAmount());
        assertEquals(2, service.summary(empty).trend().size());
        assertEquals(0, service.transactions(empty, 0, 15, null, null, null).totalElements());
        var monthly = new RevenueReportFilter(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), "MONTH", "REAL", null, null);
        assertEquals(3, service.summary(monthly).trend().size());
    }
}
