package vn.edu.iuh.nhom01_www.report.revenue;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static vn.edu.iuh.nhom01_www.report.revenue.RevenueReportResponse.*;

/** Read-only reporting queries over the existing schema. No booking/payment entity is changed. */
@Repository
@RequiredArgsConstructor
public class RevenueReportRepository {
    private final NamedParameterJdbcTemplate jdbc;

    private static final String FROM = " FROM payments p JOIN bookings b ON b.id = p.booking_id "
            + "LEFT JOIN trips tr ON tr.id = b.trip_id LEFT JOIN routes r ON r.id = tr.route_id "
            + "LEFT JOIN customers c ON c.id = b.customer_id ";
    private static final String SUCCESS = "UPPER(p.status) IN ('SUCCESS', 'SIMULATED_PAID')";
    private static final String DEMO = "(UPPER(COALESCE(p.payment_method, '')) LIKE 'DEMO%' "
            + "OR UPPER(COALESCE(p.status, '')) LIKE 'SIMULATED%' "
            + "OR UPPER(COALESCE(p.transaction_id, '')) LIKE 'DEMO%')";

    public Totals totals(RevenueReportFilter filter) {
        SqlFilter sql = filter(filter);
        String query = "SELECT COALESCE(SUM(CASE WHEN " + SUCCESS + " THEN p.amount ELSE 0 END), 0) AS successful_amount, "
                + "COUNT(CASE WHEN " + SUCCESS + " THEN 1 END) AS successful_payments, "
                + "COUNT(CASE WHEN UPPER(p.status) = 'FAILED' THEN 1 END) AS failed_payments, "
                + "COUNT(CASE WHEN UPPER(p.status) = 'PENDING' THEN 1 END) AS pending_payments, "
                + "COUNT(CASE WHEN UPPER(p.status) = 'REFUNDED' THEN 1 END) AS refunded_payments, "
                + "COUNT(*) AS payment_count, COUNT(DISTINCT b.id) AS booking_count, "
                + "COUNT(DISTINCT CASE WHEN " + SUCCESS + " THEN b.id END) AS successful_booking_count, "
                + "COUNT(DISTINCT b.customer_id) AS customer_count, COUNT(DISTINCT b.trip_id) AS trip_count"
                + FROM + sql.where();
        Totals result = jdbc.queryForObject(query, sql.parameters(), (rs, row) -> new Totals(
                money(rs, "successful_amount"), rs.getLong("successful_payments"), rs.getLong("failed_payments"),
                rs.getLong("pending_payments"), rs.getLong("refunded_payments"), rs.getLong("payment_count"),
                rs.getLong("booking_count"), rs.getLong("successful_booking_count"), 0,
                rs.getLong("customer_count"), rs.getLong("trip_count")));
        // EXISTS counts each ticket once even if legacy data has multiple payments for a booking.
        Long ticketCount = jdbc.queryForObject("SELECT COUNT(*) FROM tickets t WHERE EXISTS (SELECT 1"
                + FROM + sql.where() + " AND b.id = t.booking_id AND " + SUCCESS + ")", sql.parameters(), Long.class);
        return new Totals(result.successfulAmount(), result.successfulPayments(), result.failedPayments(),
                result.pendingPayments(), result.refundedPayments(), result.paymentCount(), result.bookingCount(),
                result.successfulBookingCount(), ticketCount == null ? 0 : ticketCount,
                result.customerCount(), result.tripCount());
    }

    public List<TrendPoint> trend(RevenueReportFilter filter) {
        SqlFilter sql = filter(filter);
        // groupBy was validated and is mapped to fixed SQL, never interpolated from request input.
        String bucket = "DAY".equals(filter.groupBy()) ? "date_trunc('day', p.created_at)::date"
                : "date_trunc('month', p.created_at)::date";
        return jdbc.query("SELECT " + bucket + " AS period_start, SUM(p.amount) AS amount, COUNT(*) AS payment_count"
                + FROM + sql.where() + " AND " + SUCCESS + " GROUP BY 1 ORDER BY 1", sql.parameters(),
                (rs, row) -> new TrendPoint(rs.getDate("period_start").toLocalDate(), money(rs, "amount"), rs.getLong("payment_count")));
    }

    public List<RouteRevenue> routes(RevenueReportFilter filter) {
        SqlFilter sql = filter(filter);
        return jdbc.query("SELECT r.id AS route_id, r.departure_location, r.arrival_location, "
                + "SUM(p.amount) AS amount, COUNT(*) AS payment_count" + FROM + sql.where() + " AND " + SUCCESS
                + " GROUP BY r.id, r.departure_location, r.arrival_location ORDER BY amount DESC, r.id",
                sql.parameters(), (rs, row) -> new RouteRevenue(rs.getObject("route_id", Long.class),
                        rs.getString("departure_location"), rs.getString("arrival_location"),
                        money(rs, "amount"), rs.getLong("payment_count")));
    }

    public List<MethodRevenue> methods(RevenueReportFilter filter) {
        SqlFilter sql = filter(filter);
        return jdbc.query("SELECT COALESCE(p.payment_method, 'UNKNOWN') AS payment_method, "
                + "SUM(p.amount) AS amount, COUNT(*) AS payment_count" + FROM + sql.where() + " AND " + SUCCESS
                + " GROUP BY p.payment_method ORDER BY amount DESC, p.payment_method", sql.parameters(),
                (rs, row) -> new MethodRevenue(rs.getString("payment_method"), money(rs, "amount"), rs.getLong("payment_count")));
    }

    public List<BookingStatusCount> bookingStatuses(RevenueReportFilter filter) {
        SqlFilter sql = filter(filter);
        return jdbc.query("SELECT b.status, COUNT(DISTINCT b.id) AS booking_count" + FROM + sql.where()
                + " GROUP BY b.status ORDER BY b.status", sql.parameters(),
                (rs, row) -> new BookingStatusCount(rs.getString("status"), rs.getLong("booking_count")));
    }

    public RevenueTransactionPage transactions(RevenueReportFilter filter, int page, int size,
                                                String search, String status, String sort) {
        return transactions(filter, page, size, search, status, sort, null);
    }

    public RevenueTransactionPage transactions(RevenueReportFilter filter, int page, int size,
                                                String search, String status, String sort, String bookingStatus) {
        SqlFilter base = filter(filter);
        StringBuilder where = new StringBuilder(base.where());
        if (search != null && !search.isBlank()) {
            where.append(" AND (LOWER(COALESCE(p.transaction_id, '')) LIKE :search "
                    + "OR CAST(b.id AS VARCHAR) LIKE :search OR LOWER(COALESCE(c.full_name, '')) LIKE :search)");
            base.parameters().addValue("search", "%" + search.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND UPPER(p.status) = :paymentStatus");
            base.parameters().addValue("paymentStatus", status);
        }
        if (bookingStatus != null && !bookingStatus.isBlank()) {
            where.append(" AND UPPER(b.status) = :bookingStatus");
            base.parameters().addValue("bookingStatus", bookingStatus);
        }
        String order = switch (sort) {
            case "OLDEST" -> "p.created_at ASC, p.id ASC";
            case "AMOUNT_DESC" -> "p.amount DESC, p.id DESC";
            case "AMOUNT_ASC" -> "p.amount ASC, p.id ASC";
            default -> "p.created_at DESC, p.id DESC";
        };
        Long count = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where, base.parameters(), Long.class);
        base.parameters().addValue("limit", size).addValue("offset", (long) page * size);
        String select = "SELECT p.id AS payment_id, b.id AS booking_id, p.transaction_id, c.full_name AS customer_name, "
                + "r.departure_location, r.arrival_location, tr.departure_time, "
                + "(SELECT COUNT(*) FROM tickets t WHERE t.booking_id = b.id) AS ticket_count, "
                + "p.amount, p.payment_method, p.status AS payment_status, b.status AS booking_status, p.created_at";
        var rows = jdbc.query(select + FROM + where + " ORDER BY " + order + " LIMIT :limit OFFSET :offset",
                base.parameters(), (rs, row) -> new RevenueTransactionPage.Transaction(
                        rs.getLong("payment_id"), rs.getLong("booking_id"), rs.getString("transaction_id"),
                        rs.getString("customer_name"), rs.getString("departure_location"), rs.getString("arrival_location"),
                        dateTime(rs, "departure_time"), rs.getLong("ticket_count"), money(rs, "amount"),
                        rs.getString("payment_method"), rs.getString("payment_status"), rs.getString("booking_status"),
                        dateTime(rs, "created_at")));
        long total = count == null ? 0 : count;
        return new RevenueTransactionPage(rows, total, page, size, (total + size - 1) / size);
    }

    private SqlFilter filter(RevenueReportFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE p.created_at IS NOT NULL AND ");
        where.append("DEMO".equals(filter.scope()) ? DEMO : "NOT " + DEMO);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (filter.fromDate() != null) {
            where.append(" AND p.created_at >= :fromDate");
            parameters.addValue("fromDate", filter.fromDate().atStartOfDay());
        }
        if (filter.toDate() != null) {
            where.append(" AND p.created_at < :toDateExclusive");
            parameters.addValue("toDateExclusive", filter.toDate().plusDays(1).atStartOfDay());
        }
        if (filter.routeId() != null) {
            where.append(" AND tr.route_id = :routeId");
            parameters.addValue("routeId", filter.routeId());
        }
        if (filter.paymentMethod() != null) {
            where.append(" AND UPPER(p.payment_method) = :paymentMethod");
            parameters.addValue("paymentMethod", filter.paymentMethod());
        }
        return new SqlFilter(where.toString(), parameters);
    }

    private static BigDecimal money(ResultSet rs, String column) throws SQLException {
        BigDecimal amount = rs.getBigDecimal(column);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record SqlFilter(String where, MapSqlParameterSource parameters) { }
}
