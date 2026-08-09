package vn.edu.iuh.nhom01_www.seat.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String seatName;

    @Builder.Default
    private Integer floor = 1;
}
