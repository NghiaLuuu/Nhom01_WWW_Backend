package vn.edu.iuh.nhom01_www.user.adminmanagement.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Additional queries without changing the existing repository interfaces or database schema. */
@Repository
public class UserManagementQueries {
    private final EntityManager entityManager;

    public UserManagementQueries(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<Staff> findStaff(String keyword, String status, Long roleId, Pageable pageable) {
        return search(Staff.class, keyword, status, roleId, pageable);
    }

    public Page<Customer> findCustomers(String keyword, String status, Pageable pageable) {
        return search(Customer.class, keyword, status, null, pageable);
    }

    private <T> Page<T> search(Class<T> type, String keyword, String status, Long roleId, Pageable pageable) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(u.fullName) LIKE :keyword ESCAPE '!'"
                    + " OR LOWER(u.email) LIKE :keyword ESCAPE '!'"
                    + " OR LOWER(COALESCE(u.phoneNumber, '')) LIKE :keyword ESCAPE '!'");
            if (type == Staff.class) {
                where.append(" OR LOWER(u.staffCode) LIKE :keyword ESCAPE '!'");
            }
            where.append(")");
            String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            parameters.put("keyword", "%" + escaped + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND u.status = :status");
            parameters.put("status", status);
        }
        if (roleId != null) {
            where.append(" AND u.role.id = :roleId");
            parameters.put("roleId", roleId);
        }
        String from = " FROM " + type.getSimpleName() + " u";
        TypedQuery<T> rows = entityManager.createQuery(
                "SELECT u" + from + where + " ORDER BY u.createdAt DESC, u.id ASC", type);
        TypedQuery<Long> count = entityManager.createQuery("SELECT COUNT(u)" + from + where, Long.class);
        parameters.forEach((name, value) -> {
            rows.setParameter(name, value);
            count.setParameter(name, value);
        });
        rows.setFirstResult(Math.toIntExact(pageable.getOffset()));
        rows.setMaxResults(pageable.getPageSize());
        return new PageImpl<>(rows.getResultList(), pageable, count.getSingleResult());
    }

    public boolean emailInUse(String email, UUID excludedStaffId) {
        return exists("Staff", "email", email, excludedStaffId)
                || exists("Customer", "email", email, null);
    }

    public boolean staffCodeInUse(String code, UUID excludedStaffId) {
        return exists("Staff", "staffCode", code, excludedStaffId);
    }

    public boolean staffPhoneInUse(String phone, UUID excludedStaffId) {
        return phone != null && exists("Staff", "phoneNumber", phone, excludedStaffId);
    }

    private boolean exists(String entity, String field, String value, UUID excludedId) {
        // Entity/field identifiers come only from the fixed calls above, never from a request.
        String jpql = "SELECT COUNT(u) FROM " + entity + " u WHERE LOWER(u." + field + ") = :value"
                + (excludedId == null ? "" : " AND u.id <> :excludedId");
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("value", value.toLowerCase(Locale.ROOT));
        if (excludedId != null) {
            query.setParameter("excludedId", excludedId);
        }
        return query.getSingleResult() > 0;
    }

    public List<Staff> lockActiveAdministrators() {
        return entityManager.createQuery("SELECT s FROM Staff s WHERE s.role.name = 'ROLE_ADMIN'"
                        + " AND s.status = 'ACTIVE' ORDER BY s.id", Staff.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }
}
