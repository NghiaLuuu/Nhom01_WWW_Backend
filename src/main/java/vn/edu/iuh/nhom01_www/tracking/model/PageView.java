package vn.edu.iuh.nhom01_www.tracking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "page_views", indexes = {
        @Index(name = "idx_page_views_session_id", columnList = "session_id"),
        @Index(name = "idx_page_views_page_url", columnList = "pageUrl")
})
public class PageView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private UserSession session;

    @Column(nullable = false, length = 1000)
    private String pageUrl;

    @Column(length = 255)
    private String pageTitle;

    @Column(length = 1000)
    private String previousPageUrl;

    private LocalDateTime enteredAt;

    private LocalDateTime leftAt;

    @Builder.Default
    private Integer activeDurationSeconds = 0;

    @Builder.Default
    private Integer idleDurationSeconds = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "pageView", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAction> actions;
}

