package vn.edu.iuh.nhom01_www.chatbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.chatbot.service.ChatbotService;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<Object>> askChatbot(@Valid @RequestBody ChatbotRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", chatbotService.ask(request)));
    }
}
