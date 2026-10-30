package vn.edu.iuh.nhom01_www.cancelrequest.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.user.model.Staff;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cancel_requests")
public class CancelRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Staff staff;

    @Column(nullable = false)
    private String status;

    @Column(length = 2000)
    private String rejectReason;

    @Column(length = 2000)
    private String reason;

    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void approve(Staff resolvingStaff) {
        this.status = "APPROVED";
        this.staff = resolvingStaff;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reject(Staff resolvingStaff, String reason) {
        this.status = "REJECTED";
        this.staff = resolvingStaff;
        this.rejectReason = reason;
        this.resolvedAt = LocalDateTime.now();
    }
}
