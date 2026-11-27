package vn.edu.iuh.nhom01_www.report.revenue.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Reporting-only contracts: no payment, booking, customer or credential entity is exposed. */
public final class RevenueReportDtos {
    private RevenueReportDtos() {
    }

    public enum GroupBy {
        DAY("day"), MONTH("month"), YEAR("year");

        private final String databaseGrain;

        GroupBy(String databaseGrain) {
            this.databaseGrain = databaseGrain;
        }

        public String databaseGrain() {
            return databaseGrain;
        }

        public LocalDate firstDay(LocalDate date) {
            return switch (this) {
                case DAY -> date;
                case MONTH -> date.withDayOfMonth(1);
                case YEAR -> date.withDayOfYear(1);
            };
        }

        public LocalDate next(LocalDate date) {
            return switch (this) {
                case DAY -> date.plusDays(1);
                case MONTH -> date.plusMonths(1);
                case YEAR -> date.plusYears(1);
            };
        }

        public String label(LocalDate date) {
            return switch (this) {
                case DAY -> date.toString();
                case MONTH -> String.format(Locale.ROOT, "%04d-%02d", date.getYear(), date.getMonthValue());
                case YEAR -> String.format(Locale.ROOT, "%04d", date.getYear());
            };
        }
    }

    public record Filter(LocalDate from, LocalDate to, GroupBy groupBy, Long routeId, String paymentMethod) {
        public LocalDateTime startInclusive() {
            return from.atStartOfDay();
        }

        public LocalDateTime endExclusive() {
            return to.plusDays(1).atStartOfDay();
        }
    }

    // Amounts are JSON decimal strings so JavaScript cannot silently round large money values.
    public record Summary(
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            long transactionCount, long bookingCount,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cancelledBookingAmount,
            long cancelledTransactionCount, long cancelledBookingCount) {
    }

    public record TimeBucket(String period, LocalDate periodStart,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            long transactionCount, long bookingCount) {
    }

    public record MethodBreakdown(String paymentMethod,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            long transactionCount, long bookingCount) {
    }

    public record RouteBreakdown(Long routeId, String departureLocation, String arrivalLocation,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            long transactionCount, long bookingCount) {
    }

    public record RouteOption(Long id, String departureLocation, String arrivalLocation) {
    }

    public record Options(List<RouteOption> routes, List<String> paymentMethods) {
    }

    public record Report(Filter filter, String currency, String timezone, String timeBasis, String generatedAt,
            boolean refundDataAvailable, Summary summary, List<TimeBucket> timeSeries,
            List<MethodBreakdown> byPaymentMethod, List<RouteBreakdown> byRoute, List<String> notes) {
    }
}
