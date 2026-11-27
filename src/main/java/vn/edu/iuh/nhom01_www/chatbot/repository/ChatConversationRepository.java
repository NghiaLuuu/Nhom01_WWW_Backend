package vn.edu.iuh.nhom01_www.chatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.nhom01_www.chatbot.model.ChatConversation;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
}
