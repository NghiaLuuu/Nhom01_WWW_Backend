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
public class TrackActionRequest {
    private UUID sessionId;
    private UUID pageViewId;
    private String actionType; // SEARCH, FILTER_PRICE, VIEW_DETAIL, CLICK_BOT, BOOK_CLICK, SELECT_SEAT
    private String payload;
}

