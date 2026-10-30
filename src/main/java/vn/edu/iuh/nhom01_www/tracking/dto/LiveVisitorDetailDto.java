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
public class LiveVisitorDetailDto {
    private LiveVisitorDto visitor;
    private boolean isOnline;
    private List<SessionPageViewDto> pageViews;
    private List<SessionActionDto> actions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionPageViewDto {
        private String id;
        private String pageUrl;
        private String pageTitle;
        private String enteredAt;
        private int activeSeconds;
        private int idleSeconds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionActionDto {
        private String id;
        private String actionType;
        private String payload;
        private String createdAt;
    }
}

