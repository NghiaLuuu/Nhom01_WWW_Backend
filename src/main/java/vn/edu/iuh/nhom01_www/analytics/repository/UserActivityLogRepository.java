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

    @Query("""
            select count(a.id)
            from UserActivityLog a
            where a.createdAt >= :from and a.createdAt < :to
              and (a.userEmail is null or a.userEmail not in (select s.email from Staff s))
              and (a.pageUrl is null or a.pageUrl not like '/admin%')
            """)
    long countEvents(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select count(distinct coalesce(a.visitorId, a.sessionId))
            from UserActivityLog a
            where a.createdAt >= :from and a.createdAt < :to
              and (a.userEmail is null or a.userEmail not in (select s.email from Staff s))
              and (a.pageUrl is null or a.pageUrl not like '/admin%')
            """)
    long countDistinctVisitors(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select new map(
                function('date_trunc', 'day', a.createdAt) as day,
                count(a.id) as events,
                count(distinct a.visitorId) as visitors)
            from UserActivityLog a
            where a.createdAt >= :from and a.createdAt < :to
              and (a.userEmail is null or a.userEmail not in (select s.email from Staff s))
              and (a.pageUrl is null or a.pageUrl not like '/admin%')
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
              and (a.userEmail is null or a.userEmail not in (select s.email from Staff s))
              and (a.pageUrl is null or a.pageUrl not like '/admin%')
            group by a.pageUrl
            order by avg(a.dwellTimeSeconds) desc
            """)
    List<Map<String, Object>> findDwellTimeByPage(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    @Query("""
            select new map(a.actionType as actionType, count(a.id) as eventCount)
            from UserActivityLog a
            where a.createdAt >= :from and a.createdAt < :to
              and (a.userEmail is null or a.userEmail not in (select s.email from Staff s))
              and (a.pageUrl is null or a.pageUrl not like '/admin%')
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
              and (user_email is null or user_email not in (select email from staffs))
              and (page_url is null or page_url not like '/admin%')
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
              and (user_email is null or user_email not in (select email from staffs))
              and (page_url is null or page_url not like '/admin%')
            group by action_type
            order by case action_type
                when 'SEARCH_TRIPS' then 1 when 'VIEW_SEATS' then 2
                when 'SELECT_SEAT' then 3 when 'FILL_INFO' then 4
                when 'BOOK_SUCCESS' then 5 when 'ABANDON_BOOKING' then 6 else 7 end
            """, nativeQuery = true)
    List<Map<String, Object>> findFunnelConversion(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    @Query(value = """
            select
                event_data ->> 'fromTab' as from_tab,
                event_data ->> 'toTab' as to_tab,
                count(*) as transition_count
            from user_activity_logs
            where action_type = 'TAB_SWITCH'
              and created_at >= :from and created_at < :to
              and event_data ->> 'fromTab' is not null
              and event_data ->> 'toTab' is not null
              and (user_email is null or user_email not in (select email from staffs))
              and (page_url is null or page_url not like '/admin%')
            group by event_data ->> 'fromTab', event_data ->> 'toTab'
            order by count(*) desc
            limit :limit
            """, nativeQuery = true)
    List<Map<String, Object>> findTabFlows(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("limit") int limit);

    @Query(value = """
            select
                coalesce(user_email, visitor_id) as user_key,
                user_email,
                visitor_id,
                cast(user_id as varchar) as user_id_str,
                max(ip_address) as ip_address,
                max(device_info) as device_info,
                count(*) as total_actions,
                count(distinct session_id) as total_sessions,
                max(created_at) as last_active
            from user_activity_logs
            where created_at >= :from and created_at < :to
              and (user_email is null or user_email not in (select email from staffs))
              and (page_url is null or page_url not like '/admin%')
            group by coalesce(user_email, visitor_id), user_email, visitor_id, user_id
            order by max(created_at) desc
            limit :limit
            """, nativeQuery = true)
    List<Map<String, Object>> findActiveUsers(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             @Param("limit") int limit);

    @Query(value = """
            select 
                cast(id as varchar) as id,
                action_type,
                page_url,
                page_title,
                referrer,
                dwell_time_seconds,
                user_email,
                cast(user_id as varchar) as user_id_str,
                visitor_id,
                session_id,
                ip_address,
                device_info,
                event_data,
                created_at
            from user_activity_logs
            where (:query is null or :query = '' 
                   or lower(user_email) like lower(concat('%', :query, '%')) 
                   or visitor_id like concat('%', :query, '%')
                   or ip_address like concat('%', :query, '%')
                   or cast(user_id as varchar) like concat('%', :query, '%'))
              and (user_email is null or user_email not in (select email from staffs))
              and (page_url is null or page_url not like '/admin%')
            order by created_at desc
            limit :limit
            """, nativeQuery = true)
    List<Map<String, Object>> findUserJourney(@Param("query") String query,
                                             @Param("limit") int limit);
}
