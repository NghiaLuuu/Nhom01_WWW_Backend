package vn.edu.iuh.nhom01_www.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoShowSummaryDto {

    private Long tripId;
    private int totalNoShowMarked;
    private List<String> noShowTicketCodes;
    private String message;
}
