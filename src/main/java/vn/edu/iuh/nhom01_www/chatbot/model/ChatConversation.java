package vn.edu.iuh.nhom01_www.chatbot.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_conversations")
@Getter @Setter @NoArgsConstructor
public class ChatConversation {
    @Id
    @Column(name = "owner_key", length = 64, nullable = false)
    private String ownerKey;

    @Column(name = "messages_json", columnDefinition = "TEXT", nullable = false)
    private String messagesJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
