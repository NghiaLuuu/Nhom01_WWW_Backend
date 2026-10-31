package vn.edu.iuh.nhom01_www.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveVisitorsSummaryResponse {
    private int totalOnline;
    private int totalIdle;
    private int totalOffline;
    private int registeredOnline;
    private int guestOnline;
    private String checkedAt;
    private List<LiveVisitorDto> visitors;
}

