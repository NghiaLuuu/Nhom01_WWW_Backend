package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static vn.edu.iuh.nhom01_www.report.revenue.RevenueReportResponse.*;

@ExtendWith(MockitoExtension.class)
class RevenueReportServiceTest {
    @Mock RevenueReportRepository repository;
    @InjectMocks RevenueReportService service;
    private static final LocalDate START = LocalDate.of(2026, 11, 1);

    private RevenueReportFilter filter() { return new RevenueReportFilter(START, START.plusDays(2), "DAY", "REAL", null, null); }

    @Test
    void missingDaysAreFilledWithoutChangingDatabaseAmounts() {
        var filter = filter();
        var totals = new Totals(new BigDecimal("300000"), 2, 1, 1, 0, 4, 4, 2, 3, 4, 1);
        when(repository.totals(filter)).thenReturn(totals);
        when(repository.trend(filter)).thenReturn(List.of(new TrendPoint(START, new BigDecimal("100000"), 1),
                new TrendPoint(START.plusDays(2), new BigDecimal("200000"), 1)));

        var report = service.summary(filter);

        assertEquals(3, report.trend().size());
        assertEquals(BigDecimal.ZERO, report.trend().get(1).amount());
        assertEquals(0, report.trend().get(1).paymentCount());
        assertEquals(new BigDecimal("300000"), report.trend().stream().map(TrendPoint::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertSame(totals, report.totals());
        assertEquals("PAYMENT_CREATED_AT", report.timeBasis());
        assertEquals("Asia/Ho_Chi_Minh", report.timezone());
        verify(repository).routes(filter);
        verify(repository).methods(filter);
        verify(repository).bookingStatuses(filter);
    }

    @Test
    void monthGroupsIncludeEmptyMonthsAtBothBoundaries() {
        var filter = new RevenueReportFilter(START, LocalDate.of(2027, 1, 31), "MONTH", "DEMO", null, null);
        when(repository.trend(filter)).thenReturn(List.of(new TrendPoint(LocalDate.of(2026, 12, 1), BigDecimal.TEN, 1)));
        var report = service.summary(filter);
        assertEquals(List.of(START, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1)), report.trend().stream().map(TrendPoint::periodStart).toList());
        assertEquals("DEMO", report.scope());
    }

    @Test
    void emptyBoundedReportIncludesZeroBucketsButUnboundedReportStaysEmpty() {
        var bounded = service.summary(filter());
        assertEquals(3, bounded.trend().size());
        assertTrue(bounded.trend().stream().allMatch(point -> point.amount().signum() == 0));
        assertTrue(service.summary(new RevenueReportFilter(null, null, null, null, null, null)).trend().isEmpty());
    }

    @Test
    void invalidDateRangesAndGroupingAreRejectedBeforeQueries() {
        assertThrows(CustomException.class, () -> new RevenueReportFilter(START.plusDays(1), START, "DAY", "REAL", null, null));
        assertThrows(CustomException.class, () -> new RevenueReportFilter(START, START.plusDays(366), "DAY", "REAL", null, null));
        assertThrows(CustomException.class, () -> new RevenueReportFilter(START, START, "INJECT", "REAL", null, null));
        assertThrows(CustomException.class, () -> new RevenueReportFilter(START, START, "DAY", "ALL", null, null));
        assertThrows(CustomException.class, () -> new RevenueReportFilter(START, START, "DAY", "REAL", -1L, null));
        verifyNoInteractions(repository);
    }

    @Test
    void filterNormalizesScopeAndPaymentMethod() {
        var filter = new RevenueReportFilter(null, null, " month ", " demo ", 10L, " cash ");
        assertEquals("MONTH", filter.groupBy());
        assertEquals("DEMO", filter.scope());
        assertEquals("CASH", filter.paymentMethod());
    }

    @Test
    void leapYearAllowsExactly366DailyBuckets() {
        var filter = new RevenueReportFilter(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), "DAY", "REAL", null, null);
        assertEquals(366, service.summary(filter).trend().size());
    }

    @Test
    void paginationAndSortAreValidatedAndPassedThrough() {
        var filter = filter();
        var expected = new RevenueTransactionPage(List.of(), 0, 1, 15, 0);
        when(repository.transactions(filter, 1, 15, "customer", "SUCCESS", "AMOUNT_DESC")).thenReturn(expected);
        assertSame(expected, service.transactions(filter, 1, 15, "customer", " success ", "amount_desc"));
    }

    @Test
    void bookingCancellationFilterDoesNotRequirePaymentCancellation() {
        var filter = filter();
        var expected = new RevenueTransactionPage(List.of(), 1, 0, 15, 1);
        when(repository.transactions(filter, 0, 15, null, null, "NEWEST", "CANCELLED")).thenReturn(expected);
        assertSame(expected, service.transactions(filter, 0, 15, null, null, null, " cancelled "));
        verify(repository).transactions(filter, 0, 15, null, null, "NEWEST", "CANCELLED");
    }

    @Test
    void invalidBookingStatusIsRejectedBeforeReadingDatabase() {
        assertThrows(CustomException.class, () -> service.transactions(filter(), 0, 15, null, null, null, "SUCCESS"));
        verifyNoInteractions(repository);
    }

    @Test
    void invalidPaginationStatusAndSearchNeverReachRepository() {
        assertThrows(CustomException.class, () -> service.transactions(filter(), -1, 15, null, null, null));
        assertThrows(CustomException.class, () -> service.transactions(filter(), 0, 101, null, null, null));
        assertThrows(CustomException.class, () -> service.transactions(filter(), 0, 15, "x".repeat(101), null, null));
        assertThrows(CustomException.class, () -> service.transactions(filter(), 0, 15, null, "PAID", null));
        var exception = assertThrows(CustomException.class, () -> service.transactions(filter(), 0, 15, null, null, "amount; DROP TABLE payments"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    void unboundedDailyQueryIsStillProtectedFromExcessiveChartSize() {
        var filter = new RevenueReportFilter(null, null, "DAY", "REAL", null, null);
        when(repository.trend(filter)).thenReturn(List.of(new TrendPoint(START, BigDecimal.ONE, 1), new TrendPoint(START.plusYears(5), BigDecimal.ONE, 1)));
        assertThrows(CustomException.class, () -> service.summary(filter));
    }
}
