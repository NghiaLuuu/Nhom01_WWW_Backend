package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.report.revenue.dto.RevenueReportDtos.*;
import vn.edu.iuh.nhom01_www.report.revenue.repository.RevenueReportRepository;
import vn.edu.iuh.nhom01_www.report.revenue.service.RevenueReportService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueReportServiceTest {
    @Mock private RevenueReportRepository repository;
    private RevenueReportService service;

    @BeforeEach
    void setUp() {
        // October in UTC, already November in Vietnam.
        service = new RevenueReportService(repository, Clock.fixed(Instant.parse("2026-10-31T18:30:00Z"), ZoneOffset.UTC));
    }

    @Test
    void defaultsToTheCurrentVietnameseCalendarMonth() {
        emptySummary();
        Report report = service.report(null, null, "DAY", null, null);
        assertThat(report.filter().from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(report.filter().to()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(report.timeSeries()).hasSize(30);
        assertThat(report.timezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(report.generatedAt()).contains("2026-11-01T01:30", "+07:00");
    }

    @Test
    void usesInclusiveStartAndExclusiveNextDayEnd() {
        emptySummary();
        service.report(date("2026-11-01"), date("2026-11-03"), "DAY", null, null);
        ArgumentCaptor<Filter> captor = ArgumentCaptor.forClass(Filter.class);
        verify(repository).summary(captor.capture());
        assertThat(captor.getValue().startInclusive()).isEqualTo(date("2026-11-01").atStartOfDay());
        assertThat(captor.getValue().endExclusive()).isEqualTo(date("2026-11-04").atStartOfDay());
    }

    @Test
    void fillsMissingDaysWithoutChangingExactAggregates() {
        Summary summary = new Summary(new BigDecimal("250000.25"), 1, 1, BigDecimal.ZERO, 0, 0);
        when(repository.summary(any())).thenReturn(summary);
        when(repository.timeSeries(any())).thenReturn(List.of(new TimeBucket("2026-11-02", date("2026-11-02"), summary.amount(), 1, 1)));
        Report report = service.report(date("2026-11-01"), date("2026-11-03"), "DAY", null, null);
        assertThat(report.summary()).isSameAs(summary);
        assertThat(report.timeSeries()).extracting(TimeBucket::period).containsExactly("2026-11-01", "2026-11-02", "2026-11-03");
        assertThat(report.timeSeries().get(0).amount()).isEqualByComparingTo("0");
        assertThat(report.timeSeries().get(1).amount()).isEqualByComparingTo("250000.25");
        assertThat(report.refundDataAvailable()).isFalse();
        assertThat(report.notes()).anyMatch(note -> note.contains("không tính doanh thu ròng"));
    }

    @Test
    void fillsMonthlyBucketsAcrossYearBoundaries() {
        emptySummary();
        Report report = service.report(date("2025-12-20"), date("2026-02-02"), "month", null, null);
        assertThat(report.timeSeries()).extracting(TimeBucket::period).containsExactly("2025-12", "2026-01", "2026-02");
    }

    @Test
    void fillsYearlyBuckets() {
        emptySummary();
        Report report = service.report(date("2024-12-31"), date("2026-01-01"), "YEAR", null, null);
        assertThat(report.timeSeries()).extracting(TimeBucket::period).containsExactly("2024", "2025", "2026");
    }

    @Test
    void includesLeapDay() {
        emptySummary();
        Report report = service.report(date("2024-02-28"), date("2024-03-01"), "DAY", null, null);
        assertThat(report.timeSeries()).extracting(TimeBucket::period).containsExactly("2024-02-28", "2024-02-29", "2024-03-01");
    }

    @Test
    void normalizesFiltersAndChecksRouteExistence() {
        emptySummary();
        when(repository.routeExists(7L)).thenReturn(true);
        Report report = service.report(date("2026-11-01"), date("2026-11-03"), " day ", 7L, " cash ");
        assertThat(report.filter().paymentMethod()).isEqualTo("CASH");
        assertThat(report.filter().routeId()).isEqualTo(7L);
    }

    @Test
    void rejectsPartiallySpecifiedDateRange() {
        assertThatThrownBy(() -> service.report(date("2026-11-01"), null, "DAY", null, null))
                .isInstanceOf(CustomException.class).hasMessageContaining("cả from và to");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsReversedDates() {
        assertThatThrownBy(() -> service.report(date("2026-11-03"), date("2026-11-01"), "DAY", null, null))
                .hasMessageContaining("Khoảng ngày không hợp lệ");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnknownGroupingAndUnboundedRanges() {
        assertThatThrownBy(() -> service.report(date("2026-01-01"), date("2026-11-01"), "WEEK", null, null)).hasMessageContaining("groupBy");
        assertThatThrownBy(() -> service.report(date("2024-01-01"), date("2026-11-01"), "DAY", null, null)).hasMessageContaining("366 ngày");
        assertThatThrownBy(() -> service.report(date("2000-01-01"), date("2026-11-01"), "YEAR", null, null)).hasMessageContaining("3660 ngày");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidAndMissingRoutes() {
        assertThatThrownBy(() -> service.report(date("2026-11-01"), date("2026-11-03"), "DAY", -1L, null)).hasMessageContaining("Tuyến đường");
        assertThatThrownBy(() -> service.report(date("2026-11-01"), date("2026-11-03"), "DAY", 999L, null)).hasMessageContaining("Tuyến đường");
        verify(repository, never()).summary(any());
    }

    @Test
    void rejectsOversizedPaymentMethod() {
        assertThatThrownBy(() -> service.report(date("2026-11-01"), date("2026-11-03"), "DAY", null, "A".repeat(256)))
                .hasMessageContaining("255 ký tự");
        verifyNoInteractions(repository);
    }

    @Test
    void keepsAllReportQueriesReadOnlyInOneRepeatableReadSnapshot() throws Exception {
        Transactional annotation = RevenueReportService.class
                .getMethod("report", LocalDate.class, LocalDate.class, String.class, Long.class, String.class)
                .getAnnotation(Transactional.class);
        assertThat(annotation.readOnly()).isTrue();
        assertThat(annotation.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    private void emptySummary() {
        when(repository.summary(any())).thenReturn(new Summary(BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, 0, 0));
    }

    private LocalDate date(String value) {
        return LocalDate.parse(value);
    }
}
