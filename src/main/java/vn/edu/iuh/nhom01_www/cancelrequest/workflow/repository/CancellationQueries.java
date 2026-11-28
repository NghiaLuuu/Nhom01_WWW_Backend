package vn.edu.iuh.nhom01_www.cancelrequest.workflow.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CancellationQueries {
    // Keep Vietnamese case matching stable even on PostgreSQL databases initialized with the C locale.
    private static final String VIETNAMESE_UPPER = "ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ";
    private final EntityManager entityManager;

    public CancellationQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Booking> booking(Long id, UUID ownerId, boolean lock) {
        var query = entityManager.createQuery("SELECT b FROM Booking b WHERE b.id = :id"
                + (ownerId == null ? "" : " AND b.customer.id = :owner"), Booking.class).setParameter("id", id);
        if (ownerId != null) query.setParameter("owner", ownerId);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultList().stream().findFirst();
    }

    public Optional<CancelRequest> request(Long id, UUID ownerId) {
        var query = entityManager.createQuery("SELECT c FROM CancelRequest c WHERE c.id = :id"
                + (ownerId == null ? "" : " AND c.booking.customer.id = :owner"), CancelRequest.class)
                .setParameter("id", id);
        if (ownerId != null) query.setParameter("owner", ownerId);
        return query.getResultList().stream().findFirst();
    }

    public Optional<Long> bookingIdForRequest(Long requestId) {
        return entityManager.createQuery("SELECT c.booking.id FROM CancelRequest c WHERE c.id = :id", Long.class)
                .setParameter("id", requestId).getResultList().stream().findFirst();
    }

    public Optional<Long> requestIdForBooking(Long bookingId) {
        return entityManager.createQuery("SELECT c.id FROM CancelRequest c WHERE c.booking.id = :id ORDER BY c.id", Long.class)
                .setParameter("id", bookingId).setMaxResults(1).getResultList().stream().findFirst();
    }

    public CancelRequest lockRequest(Long id) {
        return entityManager.find(CancelRequest.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<Ticket> tickets(Long bookingId, boolean lock) {
        var query = entityManager.createQuery("SELECT t FROM Ticket t WHERE t.booking.id = :id ORDER BY t.id", Ticket.class)
                .setParameter("id", bookingId);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultList();
    }

    public Page<CancelRequest> list(String keyword, String status, UUID ownerId, Pageable page) {
        String from = " FROM CancelRequest c JOIN c.booking b JOIN b.customer customer JOIN b.trip trip JOIN trip.route route LEFT JOIN c.staff staff";
        String fetch = " FROM CancelRequest c JOIN FETCH c.booking b JOIN FETCH b.customer customer"
                + " JOIN FETCH b.trip trip JOIN FETCH trip.route route LEFT JOIN FETCH c.staff staff";
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (status != null) {
            where.append(" AND UPPER(c.status) = :status"); parameters.put("status", status);
        }
        if (ownerId != null) {
            where.append(" AND customer.id = :owner"); parameters.put("owner", ownerId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (" + searchText("customer.fullName") + " LIKE :keyword ESCAPE '!'"
                    + " OR " + searchText("customer.email") + " LIKE :keyword ESCAPE '!'"
                    + " OR COALESCE(customer.phoneNumber, '') LIKE :keyword ESCAPE '!'"
                    + " OR CAST(b.id AS string) LIKE :keyword ESCAPE '!' OR CAST(c.id AS string) LIKE :keyword ESCAPE '!'"
                    + " OR " + searchText("route.departureLocation") + " LIKE :keyword ESCAPE '!' OR "
                    + searchText("route.arrivalLocation") + " LIKE :keyword ESCAPE '!')");
            parameters.put("upperVietnamese", VIETNAMESE_UPPER);
            parameters.put("lowerVietnamese", VIETNAMESE_UPPER.toLowerCase(Locale.ROOT));
            parameters.put("keyword", "%" + keyword.trim().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
        }
        var rows = entityManager.createQuery("SELECT c" + fetch + where + " ORDER BY c.createdAt DESC, c.id DESC", CancelRequest.class);
        var count = entityManager.createQuery("SELECT COUNT(c)" + from + where, Long.class);
        parameters.forEach((key, value) -> { rows.setParameter(key, value); count.setParameter(key, value); });
        return new PageImpl<>(rows.setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize())
                .getResultList(), page, count.getSingleResult());
    }

    private String searchText(String field) {
        return "LOWER(TRANSLATE(" + field + ", :upperVietnamese, :lowerVietnamese))";
    }
}
