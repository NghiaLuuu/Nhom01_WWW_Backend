package vn.edu.iuh.nhom01_www.report.revenue.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.report.revenue.dto.RevenueReportDtos.*;

import java.sql.Timestamp;
import java.util.List;

/** Read-only PostgreSQL aggregates; a payment is joined only to many-to-one parents, never to tickets. */
@Repository
public class RevenueReportRepository {
    private static final String METHOD = "COALESCE(NULLIF(UPPER(BTRIM(p.payment_method)), ''), 'UNKNOWN')";
    private static final String FROM = """
             FROM payments p
             JOIN bookings b ON b.id = p.booking_id
             JOIN trips t ON t.id = b.trip_id
             JOIN routes r ON r.id = t.route_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public RevenueReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Summary summary(Filter filter) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(p.amount), 0) AS amount,
                       COUNT(*) AS transaction_count,
                       COUNT(DISTINCT b.id) AS booking_count,
                       COALESCE(SUM(p.amount) FILTER (WHERE UPPER(BTRIM(b.status)) = 'CANCELLED'), 0) AS cancelled_amount,
                       COUNT(*) FILTER (WHERE UPPER(BTRIM(b.status)) = 'CANCELLED') AS cancelled_transaction_count,
                       COUNT(DISTINCT b.id) FILTER (WHERE UPPER(BTRIM(b.status)) = 'CANCELLED') AS cancelled_booking_count
                """ + FROM + where(filter), parameters(filter), (rs, row) -> new Summary(
                rs.getBigDecimal("amount"), rs.getLong("transaction_count"), rs.getLong("booking_count"),
                rs.getBigDecimal("cancelled_amount"), rs.getLong("cancelled_transaction_count"), rs.getLong("cancelled_booking_count")));
    }

    public List<TimeBucket> timeSeries(Filter filter) {
        return jdbc.query("""
                SELECT CAST(date_trunc(:grain, p.created_at) AS date) AS period_start,
                       SUM(p.amount) AS amount, COUNT(*) AS transaction_count, COUNT(DISTINCT b.id) AS booking_count
                """ + FROM + where(filter) + " GROUP BY 1 ORDER BY 1", parameters(filter), (rs, row) -> {
            var start = rs.getDate("period_start").toLocalDate();
            return new TimeBucket(filter.groupBy().label(start), start, rs.getBigDecimal("amount"),
                    rs.getLong("transaction_count"), rs.getLong("booking_count"));
        });
    }

    public List<MethodBreakdown> byPaymentMethod(Filter filter) {
        return jdbc.query("SELECT " + METHOD + " AS method, SUM(p.amount) AS amount,"
                + " COUNT(*) AS transaction_count, COUNT(DISTINCT b.id) AS booking_count"
                + FROM + where(filter) + " GROUP BY 1 ORDER BY amount DESC, method ASC",
                parameters(filter), (rs, row) -> new MethodBreakdown(rs.getString("method"), rs.getBigDecimal("amount"),
                        rs.getLong("transaction_count"), rs.getLong("booking_count")));
    }

    public List<RouteBreakdown> byRoute(Filter filter) {
        return jdbc.query("""
                SELECT r.id AS route_id, r.departure_location, r.arrival_location,
                       SUM(p.amount) AS amount, COUNT(*) AS transaction_count, COUNT(DISTINCT b.id) AS booking_count
                """ + FROM + where(filter)
                + " GROUP BY r.id, r.departure_location, r.arrival_location ORDER BY amount DESC, r.id ASC",
                parameters(filter), (rs, row) -> new RouteBreakdown(rs.getLong("route_id"),
                        rs.getString("departure_location"), rs.getString("arrival_location"), rs.getBigDecimal("amount"),
                        rs.getLong("transaction_count"), rs.getLong("booking_count")));
    }

    public Options options() {
        List<RouteOption> routes = jdbc.query("""
                SELECT id, departure_location, arrival_location FROM routes
                ORDER BY departure_location, arrival_location, id
                """, (rs, row) -> new RouteOption(rs.getLong("id"), rs.getString("departure_location"), rs.getString("arrival_location")));
        List<String> methods = jdbc.query("SELECT DISTINCT " + METHOD + " AS method FROM payments p"
                + " WHERE UPPER(BTRIM(p.status)) = 'SUCCESS' ORDER BY method", (rs, row) -> rs.getString("method"));
        return new Options(routes, methods);
    }

    public boolean routeExists(long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM routes WHERE id = :id", new MapSqlParameterSource("id", id), Long.class);
        return count != null && count > 0;
    }

    private String where(Filter filter) {
        return " WHERE UPPER(BTRIM(p.status)) = 'SUCCESS'"
                + " AND p.created_at >= :start AND p.created_at < :end"
                + (filter.routeId() == null ? "" : " AND r.id = :routeId")
                + (filter.paymentMethod() == null ? "" : " AND " + METHOD + " = :method");
    }

    private MapSqlParameterSource parameters(Filter filter) {
        return new MapSqlParameterSource()
                .addValue("start", Timestamp.valueOf(filter.startInclusive()))
                .addValue("end", Timestamp.valueOf(filter.endExclusive()))
                .addValue("grain", filter.groupBy().databaseGrain())
                .addValue("routeId", filter.routeId()).addValue("method", filter.paymentMethod());
    }
}
