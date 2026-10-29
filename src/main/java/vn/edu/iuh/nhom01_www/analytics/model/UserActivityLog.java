package vn.edu.iuh.nhom01_www.analytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_activity_logs", indexes = {
        @Index(name = "idx_activity_session", columnList = "session_id"),
        @Index(name = "idx_activity_visitor", columnList = "visitor_id"),
        @Index(name = "idx_activity_created_at", columnList = "created_at"),
        @Index(name = "idx_activity_action", columnList = "action_type"),
        @Index(name = "idx_activity_page_url", columnList = "page_url")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "visitor_id", length = 100)
    private String visitorId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", length = 320)
    private String userEmail;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "page_url", length = 2048)
    private String pageUrl;

    @Column(name = "page_title", length = 255)
    private String pageTitle;

    @Column(length = 2048)
    private String referrer;

    @Column(name = "dwell_time_seconds")
    private Long dwellTimeSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private Map<String, Object> eventData;

    @Column(name = "device_info", length = 1000)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
