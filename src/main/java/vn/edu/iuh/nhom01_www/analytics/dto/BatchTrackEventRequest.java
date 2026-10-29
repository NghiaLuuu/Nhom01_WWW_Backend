package vn.edu.iuh.nhom01_www.analytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchTrackEventRequest(
        @NotEmpty
        @Size(max = 100)
        List<@Valid TrackEventDto> events
) {
}
