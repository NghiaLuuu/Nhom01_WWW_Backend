package vn.edu.iuh.nhom01_www.tracking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_recommendations", indexes = {
        @Index(name = "idx_user_recs_anonymous_id", columnList = "anonymousId"),
        @Index(name = "idx_user_recs_customer_id", columnList = "customerId"),
        @Index(name = "idx_user_recs_ip_address", columnList = "ipAddress")
})
public class UserRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID customerId;

    private String anonymousId;

    private String ipAddress;

    @Column(nullable = false)
    private Long recommendedTripId;

    private Double matchScore; // 0.0 -> 1.0

    @Column(length = 500)
    private String reason; // "Hội đồng điệu: 85% khách hàng có cùng lịch sử đã đặt vé chuyến này"

    @Column(length = 100)
    private String personaCluster; // DALAT_LIMOUSINE, NHATRANG_FAMILY, BUDGET_SAVER

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
