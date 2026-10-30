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
public class TrackPageViewRequest {
    private UUID sessionId;
    private String pageUrl;
    private String pageTitle;
    private String previousPageUrl;
}
