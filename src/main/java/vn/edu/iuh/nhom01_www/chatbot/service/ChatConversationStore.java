package vn.edu.iuh.nhom01_www.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.iuh.nhom01_www.chatbot.model.ChatConversation;
import vn.edu.iuh.nhom01_www.chatbot.repository.ChatConversationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class ChatConversationStore {
    private final StringRedisTemplate redis;
    private final ChatConversationRepository conversations;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Duration GUEST_RETENTION = Duration.ofHours(24);

    public ChatConversationStore(StringRedisTemplate redis, ChatConversationRepository conversations) {
        this.redis = redis;
        this.conversations = conversations;
    }

    private void validateToken(String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã hội thoại không hợp lệ");
        }
    }

    private String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String redisKey(String owner, String token) {
        return "chat_history:v1:" + digest(owner + ":" + token);
    }

    @Transactional
    public List<Map<String, Object>> load(String owner, String token) throws java.io.IOException {
        validateToken(token);
        String json;
        if ("guest".equals(owner)) {
            String key = redisKey(owner, token);
            json = redis.opsForValue().get(key);
            if (json != null) {
                Long remaining = redis.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
                if (remaining != null && (remaining == -1 || remaining > GUEST_RETENTION.toSeconds())) {
                    redis.expire(key, GUEST_RETENTION);
                }
            }
        } else {
            String ownerKey = digest(owner);
            var saved = conversations.findById(ownerKey);
            if (saved.isPresent()) {
                json = saved.get().getMessagesJson();
            } else {
                // Import the previous account-scoped Redis history once when available.
                try { json = redis.opsForValue().get(redisKey(owner, token)); }
                catch (org.springframework.dao.DataAccessException e) { json = null; }
                if (json != null) {
                    ChatConversation conversation = new ChatConversation();
                    conversation.setOwnerKey(ownerKey);
                    conversation.setMessagesJson(json);
                    conversation.setUpdatedAt(LocalDateTime.now());
                    conversations.save(conversation);
                }
            }
        }
        return json == null ? List.of() : mapper.readValue(json, new TypeReference<>() {});
    }

    @Transactional
    public void save(String owner, String token, List<Map<String, Object>> messages) throws java.io.IOException {
        validateToken(token);
        if (messages.size() > 200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hội thoại vượt quá 200 tin nhắn");
        String json = mapper.writeValueAsString(messages);
        if (json.getBytes(StandardCharsets.UTF_8).length > 1_000_000) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Hội thoại quá lớn");
        if ("guest".equals(owner)) {
            redis.opsForValue().set(redisKey(owner, token), json, GUEST_RETENTION);
        } else {
            String ownerKey = digest(owner);
            ChatConversation conversation = conversations.findById(ownerKey).orElseGet(() -> {
                ChatConversation created = new ChatConversation();
                created.setOwnerKey(ownerKey);
                return created;
            });
            conversation.setMessagesJson(json);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversations.save(conversation);
        }
    }
}
