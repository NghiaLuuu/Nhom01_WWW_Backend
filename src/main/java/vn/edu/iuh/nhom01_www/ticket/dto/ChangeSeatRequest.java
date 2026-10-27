package vn.edu.iuh.nhom01_www.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeSeatRequest {
    @NotNull(message = "ID ghế mới không được để trống!")
    private Long newSeatId;
}
