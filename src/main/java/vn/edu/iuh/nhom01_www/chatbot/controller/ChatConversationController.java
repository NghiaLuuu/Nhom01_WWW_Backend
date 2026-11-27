package vn.edu.iuh.nhom01_www.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.nhom01_www.chatbot.service.ChatConversationStore;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot/conversations")
@RequiredArgsConstructor
public class ChatConversationController {
    private final ChatConversationStore store;

    private String owner(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())
                ? "guest" : "user:" + authentication.getName();
    }

    @GetMapping("/{token}")
    public ResponseEntity<List<Map<String, Object>>> load(@PathVariable String token, Authentication authentication) throws IOException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(store.load(owner(authentication), token));
    }

    @PutMapping("/{token}")
    public ResponseEntity<Void> save(@PathVariable String token, @RequestBody List<Map<String, Object>> messages, Authentication authentication) throws IOException {
        store.save(owner(authentication), token, messages);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
