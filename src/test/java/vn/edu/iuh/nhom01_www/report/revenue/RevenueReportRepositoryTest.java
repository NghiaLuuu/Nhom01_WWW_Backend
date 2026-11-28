package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static vn.edu.iuh.nhom01_www.report.revenue.RevenueReportResponse.*;

class RevenueReportRepositoryTest {
    NamedParameterJdbcTemplate jdbc;
    RevenueReportRepository repository;
    RevenueReportFilter filter;

    @BeforeEach
    void setUp() {
        jdbc = mock(NamedParameterJdbcTemplate.class);
        repository = new RevenueReportRepository(jdbc);
        filter = new RevenueReportFilter(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "DAY", "REAL", 3L, "VNPAY");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sumsPaymentsNotBookingPricesAndDoesNotMultiplyByTicketCount() throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getBigDecimal("successful_amount")).thenReturn(new BigDecimal("450000"));
        when(row.getLong("successful_payments")).thenReturn(2L);
        when(row.getLong("booking_count")).thenReturn(2L);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<Totals>) invocation.getArgument(2)).mapRow(row, 0));
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(5L);

        var totals = repository.totals(filter);

        assertEquals(new BigDecimal("450000"), totals.successfulAmount());
        assertEquals(5, totals.ticketCount());
        var sql = ArgumentCaptor.forClass(String.class);
        var params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForObject(sql.capture(), params.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().contains("THEN p.amount ELSE 0"));
        assertFalse(sql.getValue().contains("b.total_price"));
        assertFalse(sql.getValue().contains("JOIN tickets"));
        assertFalse(sql.getValue().contains("b.status = 'PAID'"), "Cancelled bookings must not automatically erase successful payments");
        assertTrue(sql.getValue().contains("NOT (UPPER(COALESCE(p.payment_method"));
        assertEquals(filter.fromDate().atStartOfDay(), params.getValue().getValue("fromDate"));
        assertEquals(LocalDate.of(2026, 12, 1).atStartOfDay(), params.getValue().getValue("toDateExclusive"));
        assertEquals(3L, params.getValue().getValue("routeId"));
        assertEquals("VNPAY", params.getValue().getValue("paymentMethod"));
        verify(jdbc).queryForObject(contains("WHERE EXISTS (SELECT 1"), any(SqlParameterSource.class), eq(Long.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void demoCohortIncludesLegacyDemoTransactionIdsAndSimulationStatuses() {
        var demo = new RevenueReportFilter(null, null, "MONTH", "DEMO", null, null);
        repository.methods(demo);
        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(SqlParameterSource.class), any(RowMapper.class));
        assertTrue(sql.getValue().contains("WHERE p.created_at IS NOT NULL AND (UPPER"));
        assertTrue(sql.getValue().contains("LIKE 'DEMO%'"));
        assertTrue(sql.getValue().contains("LIKE 'SIMULATED%'"));
        assertFalse(sql.getValue().contains("AND NOT ("));
    }

    @Test
    @SuppressWarnings("unchecked")
    void dailyAndMonthlyQueriesUseFixedBucketsAndOnlySuccessfulPayments() {
        repository.trend(filter);
        verify(jdbc).query(contains("date_trunc('day', p.created_at)::date"), any(SqlParameterSource.class), any(RowMapper.class));
        repository.trend(new RevenueReportFilter(null, null, "MONTH", "REAL", null, null));
        verify(jdbc).query(contains("date_trunc('month', p.created_at)::date"), any(SqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void paginationUsesBoundParametersAndReturnsDatabaseTotalNotCurrentPageLength() {
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(31L);
        var result = repository.transactions(filter, 2, 15, "O'Brien", "SUCCESS", "AMOUNT_DESC");

        assertEquals(31, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(2, result.page());
        var sql = ArgumentCaptor.forClass(String.class);
        var params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().endsWith("ORDER BY p.amount DESC, p.id DESC LIMIT :limit OFFSET :offset"));
        assertFalse(sql.getValue().contains("O'Brien"));
        assertEquals("%o'brien%", params.getValue().getValue("search"));
        assertEquals(30L, params.getValue().getValue("offset"));
        assertEquals(15, params.getValue().getValue("limit"));
        assertEquals("SUCCESS", params.getValue().getValue("paymentStatus"));
        assertTrue(sql.getValue().contains("SELECT COUNT(*) FROM tickets t WHERE t.booking_id = b.id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void bookingAndPaymentStatusesUseDifferentColumnsAndTheSameCountAndPageFilters() {
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(1L);
        repository.transactions(filter, 0, 15, null, "SUCCESS", "NEWEST", "CANCELLED");
        var sql = ArgumentCaptor.forClass(String.class);
        var params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().contains("UPPER(p.status) = :paymentStatus"));
        assertTrue(sql.getValue().contains("UPPER(b.status) = :bookingStatus"));
        assertEquals("SUCCESS", params.getValue().getValue("paymentStatus"));
        assertEquals("CANCELLED", params.getValue().getValue("bookingStatus"));
        verify(jdbc).queryForObject(contains("UPPER(b.status) = :bookingStatus"), any(SqlParameterSource.class), eq(Long.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void transactionMappingDoesNotSerializeEntitiesOrCredentials() throws Exception {
        ResultSet row = mock(ResultSet.class);
        when(row.getLong("payment_id")).thenReturn(1L);
        when(row.getLong("booking_id")).thenReturn(100L);
        when(row.getBigDecimal("amount")).thenReturn(new BigDecimal("250000"));
        when(row.getString("booking_status")).thenReturn("CANCELLED");
        when(row.getString("payment_status")).thenReturn("SUCCESS");
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(1L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<RevenueTransactionPage.Transaction>) invocation.getArgument(2)).mapRow(row, 0)));
        var result = repository.transactions(filter, 0, 15, null, null, "NEWEST");
        assertEquals("CANCELLED", result.content().get(0).bookingStatus());
        assertEquals("SUCCESS", result.content().get(0).paymentStatus());
        assertEquals(new BigDecimal("250000"), result.content().get(0).amount());
    }
}
