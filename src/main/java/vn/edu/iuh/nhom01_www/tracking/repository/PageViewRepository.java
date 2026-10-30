package vn.edu.iuh.nhom01_www.tracking.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;

import java.util.List;
import java.util.UUID;

@Repository
public interface PageViewRepository extends JpaRepository<PageView, UUID> {

    List<PageView> findBySessionIdOrderByEnteredAtAsc(UUID sessionId);

    @Query("SELECT p.pageUrl, COUNT(p), AVG(p.activeDurationSeconds), AVG(p.idleDurationSeconds) " +
           "FROM PageView p GROUP BY p.pageUrl ORDER BY COUNT(p) DESC")
    List<Object[]> getPageDurationStats(Pageable pageable);
}
