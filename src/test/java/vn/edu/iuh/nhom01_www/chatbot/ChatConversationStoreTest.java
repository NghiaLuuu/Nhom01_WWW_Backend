package vn.edu.iuh.nhom01_www.chatbot;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.iuh.nhom01_www.chatbot.service.ChatConversationStore;
import vn.edu.iuh.nhom01_www.chatbot.model.ChatConversation;
import vn.edu.iuh.nhom01_www.chatbot.repository.ChatConversationRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatConversationStoreTest {
    @Test
    @SuppressWarnings("unchecked")
    void importsExistingAccountRedisHistoryIntoDatabase() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("[{\"sender\":\"user\",\"text\":\"xin chào\"}]");
        ChatConversationRepository repository = mock(ChatConversationRepository.class);
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        ChatConversationStore store = new ChatConversationStore(redis, repository);

        assertThat(store.load("user:alice", "a".repeat(64)))
                .containsExactly(Map.of("sender", "user", "text", "xin chào"));
        var captured = org.mockito.ArgumentCaptor.forClass(ChatConversation.class);
        verify(repository).save(captured.capture());
        assertThat(captured.getValue().getMessagesJson()).contains("xin chào");
    }

    @Test
    @SuppressWarnings("unchecked")
    void guestExpiresAfter24HoursAndAccountsPersistIndependentlyOfToken() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AtomicReference<String> storedKey = new AtomicReference<>();
        AtomicReference<String> storedValue = new AtomicReference<>();
        doAnswer(invocation -> { storedKey.set(invocation.getArgument(0)); storedValue.set(invocation.getArgument(1)); return null; })
                .when(values).set(anyString(), anyString(), eq(Duration.ofHours(24)));
        when(values.get(anyString())).thenAnswer(invocation -> java.util.Objects.equals(storedKey.get(), invocation.getArgument(0)) ? storedValue.get() : null);
        ChatConversationRepository repository = mock(ChatConversationRepository.class);
        Map<String, ChatConversation> database = new HashMap<>();
        when(repository.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(database.get(invocation.getArgument(0))));
        when(repository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation conversation = invocation.getArgument(0);
            database.put(conversation.getOwnerKey(), conversation);
            return conversation;
        });

        ChatConversationStore store = new ChatConversationStore(redis, repository);
        String token = "a".repeat(64);
        List<Map<String, Object>> messages = List.of(Map.of("sender", "user", "text", "Xin chào"),
                Map.of("sender", "bot", "text", "Dạ", "trips", List.of(Map.of("tripId", 123))));
        store.save("user:alice", token, messages);
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
        assertThat(store.load("user:alice", "b".repeat(64))).isEqualTo(messages);
        assertThat(store.load("user:bob", token)).isEmpty();
        assertThat(store.load("guest", token)).isEmpty();
        store.save("guest", token, messages);
        assertThat(store.load("guest", token)).isEqualTo(messages);
        verify(values).set(anyString(), anyString(), eq(Duration.ofHours(24)));
        assertThatThrownBy(() -> store.load("guest", "weak-session")) .isInstanceOf(ResponseStatusException.class);
    }
}
