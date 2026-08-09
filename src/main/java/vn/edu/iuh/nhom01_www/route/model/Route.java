package vn.edu.iuh.nhom01_www.route.model;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.iuh.nhom01_www.trip.model.Trip;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String departureLocation;

    @Column(nullable = false)
    private String arrivalLocation;

    private BigDecimal distanceKm;

    private Integer durationMinutes;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Builder.Default
    private Boolean status = true;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "route")
    private List<Trip> trips;
}
