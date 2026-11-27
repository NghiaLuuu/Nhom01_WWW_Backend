package vn.edu.iuh.nhom01_www.user;

import org.junit.jupiter.api.Test;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancellationFinancialRecordRepository;
import vn.edu.iuh.nhom01_www.user.service.impl.UserServiceImpl;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RevenueStatisticsTest {
    @Test
    void addsCancellationFeesOnceAndSubtractsCancelledPartialTickets() {
        BookingRepository bookings = mock(BookingRepository.class);
        CancellationFinancialRecordRepository records = mock(CancellationFinancialRecordRepository.class);
        when(bookings.sumPaidAndPartialBookingTotals()).thenReturn(new BigDecimal("1000000"));
        when(bookings.sumCancelledTicketsInPartialBookings()).thenReturn(new BigDecimal("200000"));
        when(records.sumFees()).thenReturn(new BigDecimal("50000"));
        when(records.sumRefunds()).thenReturn(new BigDecimal("150000"));
        when(records.summarizeByBooking()).thenReturn(List.<Object[]>of(new Object[]{42L, new BigDecimal("50000"), new BigDecimal("150000"), 25, 25}));
        UserServiceImpl service = new UserServiceImpl(null, null, null, null, bookings, null, records);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) service.getRevenueStatistics();
        assertThat((BigDecimal) stats.get("ticketRevenue")).isEqualByComparingTo("800000");
        assertThat((BigDecimal) stats.get("totalRevenue")).isEqualByComparingTo("850000");
        assertThat((BigDecimal) stats.get("simulatedRefunds")).isEqualByComparingTo("150000");
        assertThat(((Map<?, ?>) stats.get("cancellationByBooking")).containsKey("42")).isTrue();
    }
}
