package vn.edu.iuh.nhom01_www.user.adminmanagement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import vn.edu.iuh.nhom01_www.user.adminmanagement.repository.UserManagementQueries;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserManagementQueriesTest {
    @Test
    @SuppressWarnings("unchecked")
    void searchesWithBoundEscapedParametersAndDatabasePagination() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<Staff> rows = mock(TypedQuery.class);
        TypedQuery<Long> count = mock(TypedQuery.class);
        when(manager.createQuery(anyString(), eq(Staff.class))).thenReturn(rows);
        when(manager.createQuery(anyString(), eq(Long.class))).thenReturn(count);
        when(rows.getResultList()).thenReturn(List.of());
        when(count.getSingleResult()).thenReturn(0L);
        new UserManagementQueries(manager).findStaff(" 50%_! ", "ACTIVE", 2L, PageRequest.of(2, 20));
        verify(rows).setParameter("keyword", "%50!%!_!!%");
        verify(rows).setParameter("status", "ACTIVE");
        verify(rows).setParameter("roleId", 2L);
        verify(rows).setFirstResult(40);
        verify(rows).setMaxResults(20);
        verify(manager).createQuery(argThat(query -> query.contains("u.staffCode") && !query.contains("50%")), eq(Staff.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void locksAdministratorsInAConsistentOrderForLastAdminChecks() {
        EntityManager manager = mock(EntityManager.class);
        TypedQuery<Staff> query = mock(TypedQuery.class);
        when(manager.createQuery(anyString(), eq(Staff.class))).thenReturn(query);
        when(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        assertThat(new UserManagementQueries(manager).lockActiveAdministrators()).isEmpty();
        verify(manager).createQuery(contains("ORDER BY s.id"), eq(Staff.class));
        verify(query).setLockMode(LockModeType.PESSIMISTIC_WRITE);
    }
}
