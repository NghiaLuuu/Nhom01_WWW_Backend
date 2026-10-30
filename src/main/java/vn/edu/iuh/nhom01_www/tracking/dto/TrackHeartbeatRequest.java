package vn.edu.iuh.nhom01_www.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackHeartbeatRequest {
    private UUID sessionId;
    private UUID pageViewId;
    private Integer additionalActiveSeconds;
    private Integer additionalIdleSeconds;
}
