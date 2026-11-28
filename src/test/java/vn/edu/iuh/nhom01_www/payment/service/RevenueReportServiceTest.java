package vn.edu.iuh.nhom01_www.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueReportServiceTest {
    @Mock PaymentRepository payments;
    @InjectMocks RevenueReportService service;
    private final LocalDate from = LocalDate.of(2026, 10, 1);
    private final LocalDate to = LocalDate.of(2026, 10, 31);

    @Test void aggregatesGrossReceiptsAndSeparatesCancelledBookingsWithoutInventingRefunds() {
        var rows = List.of(row("2026-10-01", "CASH", "100", 1L, "0"),
                row("2026-10-01", "VNPAY", "200", 2L, "50"), row("2026-10-02", "CASH", "300", 1L, "0"));
        when(payments.aggregateRevenue(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), "YYYY-MM-DD"))
                .thenReturn(rows);
        var report = service.report(from, to, RevenueReportService.GroupBy.DAY);
        assertEquals(new BigDecimal("600"), report.grossRevenue());
        assertEquals(4L, report.successfulPayments());
        assertEquals(new BigDecimal("50"), report.cancelledBookingReceipts());
        assertEquals(new BigDecimal("300"), report.series().get(0).revenue());
        assertEquals(new BigDecimal("400"), report.byPaymentMethod().get(0).revenue());
        assertEquals("VND", report.currency());
    }

    @Test void emptyRangeReturnsZero() {
        when(payments.aggregateRevenue(any(), any(), anyString())).thenReturn(List.of());
        assertEquals(BigDecimal.ZERO, service.report(from, to, RevenueReportService.GroupBy.DAY).grossRevenue());
    }

    @Test void monthGroupingAndInclusiveEndDate() {
        when(payments.aggregateRevenue(any(), any(), anyString())).thenReturn(List.of());
        service.report(from, to, RevenueReportService.GroupBy.MONTH);
        verify(payments).aggregateRevenue(from.atStartOfDay(), LocalDate.of(2026, 11, 1).atStartOfDay(), "YYYY-MM");
    }

    @Test void rejectsReversedAndUnboundedRanges() {
        assertThrows(CustomException.class, () -> service.report(to, from, RevenueReportService.GroupBy.DAY));
        assertThrows(CustomException.class, () -> service.report(from, from.plusYears(11), RevenueReportService.GroupBy.DAY));
        verifyNoInteractions(payments);
    }

    private PaymentRepository.RevenueRow row(String period, String method, String amount, long count, String cancelled) {
        var row = mock(PaymentRepository.RevenueRow.class);
        when(row.getPeriod()).thenReturn(period);
        when(row.getPaymentMethod()).thenReturn(method);
        when(row.getRevenue()).thenReturn(new BigDecimal(amount));
        when(row.getPaymentCount()).thenReturn(count);
        when(row.getCancelledBookingReceipts()).thenReturn(new BigDecimal(cancelled));
        return row;
    }
}
