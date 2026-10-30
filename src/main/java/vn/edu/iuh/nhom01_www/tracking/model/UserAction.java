package vn.edu.iuh.nhom01_www.tracking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_actions", indexes = {
        @Index(name = "idx_user_actions_session_id", columnList = "session_id"),
        @Index(name = "idx_user_actions_action_type", columnList = "actionType")
})
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private UserSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_view_id")
    @JsonIgnore
    private PageView pageView;

    @Column(nullable = false, length = 100)
    private String actionType; // SEARCH, FILTER_PRICE, VIEW_DETAIL, CLICK_BOT, BOOK_CLICK, SELECT_SEAT

    @Column(columnDefinition = "TEXT")
    private String payload;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

