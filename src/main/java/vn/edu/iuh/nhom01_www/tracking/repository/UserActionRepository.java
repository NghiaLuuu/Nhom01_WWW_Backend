package vn.edu.iuh.nhom01_www.tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, UUID> {

    List<UserAction> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    @Query("SELECT a FROM UserAction a WHERE a.session.id IN :sessionIds ORDER BY a.createdAt ASC")
    List<UserAction> findBySessionIds(List<UUID> sessionIds);

    List<UserAction> findByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT a.actionType, COUNT(a) FROM UserAction a GROUP BY a.actionType")
    List<Object[]> countByActionType();
}

