package vn.edu.iuh.nhom01_www.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.chatbot.service.ChatbotService;
import vn.edu.iuh.nhom01_www.common.audit.annotation.Auditable;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    @Auditable(action = "CHATBOT_ASK", entityName = "Chatbot")
    public ResponseEntity<ApiResponse<Object>> ask(@RequestBody ChatbotRequest request) {
        String currentUserEmail = "ANONYMOUS";
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            currentUserEmail = auth.getName();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", chatbotService.ask(request, currentUserEmail)));
    }

    @PostMapping("/ingest")
    @Auditable(action = "CHATBOT_INGEST_VECTORS", entityName = "Chatbot")
    public ResponseEntity<ApiResponse<Object>> ingest() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã hoàn thành nạp vector thành công", chatbotService.ingestAll()));
    }
}
