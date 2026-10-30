package vn.edu.iuh.nhom01_www.tracking.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findTopByAnonymousIdOrderByCreatedAtDesc(String anonymousId);

    Optional<UserSession> findTopByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<UserSession> findByAnonymousIdOrderByCreatedAtDesc(String anonymousId);

    List<UserSession> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<UserSession> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    @Query("SELECT DISTINCT s.anonymousId FROM UserSession s WHERE s.customerId = :customerId AND s.anonymousId IS NOT NULL")
    List<String> findDistinctAnonymousIdsByCustomerId(@Param("customerId") UUID customerId);

    List<UserSession> findByAnonymousIdInOrderByCreatedAtDesc(List<String> anonymousIds);

    @Query("SELECT s FROM UserSession s ORDER BY s.createdAt DESC")
    List<UserSession> findRecentSessions(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT s.anonymousId) FROM UserSession s")
    long countUniqueVisitors();

    @Query("SELECT AVG(s.totalDurationSeconds) FROM UserSession s WHERE s.totalDurationSeconds > 0")
    Double getAverageSessionDuration();

    @Query("SELECT s.trafficSource, COUNT(s) FROM UserSession s GROUP BY s.trafficSource")
    List<Object[]> countByTrafficSource();

    List<UserSession> findByCreatedAtAfter(LocalDateTime since);
}
