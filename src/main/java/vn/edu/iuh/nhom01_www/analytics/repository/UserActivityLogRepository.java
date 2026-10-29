package vn.edu.iuh.nhom01_www.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.nhom01_www.analytics.model.UserActivityLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, UUID> {

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    @Query("""
            select count(distinct coalesce(a.visitorId, a.sessionId))
            from UserActivityLog a
            where a.createdAt >= :from and a.createdAt < :to
            """)
    long countDistinctVisitors(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select new map(
                function('date_trunc', 'day', a.createdAt) as day,
                count(a.id) as events,
                count(distinct a.visitorId) as visitors)
            from UserActivityLog a
            where a.createdAt >= :from and a.createdAt < :to
            group by function('date_trunc', 'day', a.createdAt)
            order by function('date_trunc', 'day', a.createdAt)
            """)
    List<Map<String, Object>> countDailyOverview(@Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    @Query("""
            select new map(
                a.pageUrl as pageUrl,
                avg(a.dwellTimeSeconds) as averageDwellTimeSeconds,
                count(a.id) as eventCount)
            from UserActivityLog a
            where a.actionType = 'DWELL_TIME'
              and a.dwellTimeSeconds is not null
              and a.createdAt >= :from and a.createdAt < :to
            group by a.pageUrl
            order by avg(a.dwellTimeSeconds) desc
            """)
    List<Map<String, Object>> findDwellTimeByPage(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    @Query("""
            select new map(a.actionType as actionType, count(a.id) as eventCount)
            from UserActivityLog a
            where a.createdAt >= :from and a.createdAt < :to
            group by a.actionType
            order by count(a.id) desc
            """)
    List<Map<String, Object>> countActions(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query(value = """
            select event_data ->> 'searchKeyword' as keyword, count(*) as search_count
            from user_activity_logs
            where action_type in ('SEARCH_TRIP', 'SEARCH_TRIPS')
              and created_at >= :from and created_at < :to
              and event_data ->> 'searchKeyword' is not null
            group by event_data ->> 'searchKeyword'
            order by count(*) desc
            limit :limit
            """, nativeQuery = true)
    List<Map<String, Object>> findSearchTrends(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("limit") int limit);

    @Query(value = """
            select action_type as step, count(distinct coalesce(session_id, visitor_id)) as sessions
            from user_activity_logs
            where action_type in ('SEARCH_TRIPS', 'VIEW_SEATS', 'SELECT_SEAT',
                                  'FILL_INFO', 'BOOK_SUCCESS', 'ABANDON_BOOKING')
              and created_at >= :from and created_at < :to
            group by action_type
            order by case action_type
                when 'SEARCH_TRIPS' then 1 when 'VIEW_SEATS' then 2
                when 'SELECT_SEAT' then 3 when 'FILL_INFO' then 4
                when 'BOOK_SUCCESS' then 5 when 'ABANDON_BOOKING' then 6 else 7 end
            """, nativeQuery = true)
    List<Map<String, Object>> findFunnelConversion(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
