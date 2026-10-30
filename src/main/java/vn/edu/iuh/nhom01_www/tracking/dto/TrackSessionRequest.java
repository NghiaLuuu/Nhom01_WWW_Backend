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
public class TrackSessionRequest {
    private String anonymousId;
    private UUID customerId;
    private String trafficSource; // direct, facebook, google, zalo
    private String utmCampaign;
    private String userAgent;
}
