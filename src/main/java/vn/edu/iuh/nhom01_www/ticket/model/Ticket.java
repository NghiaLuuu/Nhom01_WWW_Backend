package vn.edu.iuh.nhom01_www.ticket.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.seat.model.Seat;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal price;

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void use() {
        this.status = "USED";
    }

    public void release() {
        this.status = "RELEASED";
    }

    public void issue() {
        this.status = "ISSUED";
    }
}
