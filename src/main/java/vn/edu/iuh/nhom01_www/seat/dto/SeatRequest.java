package vn.edu.iuh.nhom01_www.seat.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class SeatRequest {
    @NotNull
    private Integer totalSeats;
}
