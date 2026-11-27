package vn.edu.iuh.nhom01_www.cancelrequest.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_financial_records")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CancellationFinancialRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    @Column(nullable = false, unique = true, length = 100)
    private String confirmationToken;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;
    @Column(nullable = false)
    private int feePercent;
    @Column(nullable = false)
    private LocalDateTime recordedAt;
}
