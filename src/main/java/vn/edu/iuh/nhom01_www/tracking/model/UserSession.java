package vn.edu.iuh.nhom01_www.tracking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_user_sessions_anonymous_id", columnList = "anonymousId"),
        @Index(name = "idx_user_sessions_customer_id", columnList = "customerId"),
        @Index(name = "idx_user_sessions_ip_address", columnList = "ipAddress")
})
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID customerId;

    @Column(nullable = false)
    private String anonymousId;

    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    @Column(length = 100)
    private String trafficSource; // direct, facebook, google, zalo, etc.

    @Column(length = 255)
    private String utmCampaign;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Builder.Default
    private Integer totalDurationSeconds = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PageView> pageViews;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAction> actions;
}

