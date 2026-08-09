package vn.edu.iuh.nhom01_www.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.chatbot.model.TableMetadata;
import vn.edu.iuh.nhom01_www.chatbot.repository.TableMetadataRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatModel chatModel;
    private final TableMetadataRepository tableMetadataRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Object ask(ChatbotRequest request) {
        String userQuestion = request.getQuestion();

        // 1. Fetch Metadata
        List<TableMetadata> allMetadata = tableMetadataRepository.findAll();
        if (allMetadata.isEmpty()) {
            return Map.of("answer", "H? th?ng chua du?c c?u hình Metadata d? tr? l?i câu h?i.");
        }

        String metadataContext = allMetadata.stream()
                .map(m -> "- B?ng '" + m.getTableName() + "': " + m.getShortDescription())
                .collect(Collectors.joining("
"));

        // Stage 1: Routing
        String routingPrompt = String.format(
                "Ngu?i dùng h?i: '%s'.
" +
                "Du?i dây là các b?ng d? li?u trong h? th?ng:
%s
" +
                "D?a vào câu h?i, hãy cho bi?t c?n truy v?n d? li?u t? nh?ng b?ng nào? " +
                "Hãy tr? l?i CH? B?NG TÊN CÁC B?NG cách nhau b?ng d?u ph?y, không gi?i thích gì thêm.",
                userQuestion, metadataContext);
        
        String chosenTablesStr = chatModel.call(routingPrompt).trim();
        log.info("Stage 1 - Chosen Tables: {}", chosenTablesStr);

        // Prepare schema for SQL Generation
        String schemaContext = allMetadata.stream()
                .filter(m -> chosenTablesStr.contains(m.getTableName()))
                .map(m -> m.getSchemaDefinition())
                .collect(Collectors.joining("

"));

        if (schemaContext.isEmpty()) {
            return Map.of("answer", "Xin l?i, tôi không tìm th?y d? li?u phù h?p d? tr? l?i câu h?i c?a b?n.");
        }

        // Stage 2: SQL Generation
        String sqlPrompt = String.format(
                "D?a vào schema CSDL PostgreSQL sau:
%s

" +
                "Hãy vi?t M?T câu l?nh SQL SELECT duy nh?t d? tr? l?i câu h?i c?a ngu?i dùng: '%s'.
" +
                "LUU Ý QUAN TR?NG: Ch? tr? v? câu l?nh SQL thô, không dùng code block, không gi?i thích.",
                schemaContext, userQuestion);
        
        String rawSql = chatModel.call(sqlPrompt).trim();
        rawSql = rawSql.replaceAll("^`sql\\s*", "").replaceAll("^`\\s*", "").replaceAll("`$", "").trim();
        log.info("Stage 2 - Generated SQL: {}", rawSql);

        // Stage 3: Execution & Security Validation
        if (!rawSql.toUpperCase().startsWith("SELECT")) {
            throw new CustomException("L?nh SQL sinh ra không h?p l? (Không ph?i l?nh SELECT).", HttpStatus.BAD_REQUEST);
        }
        String upperSql = rawSql.toUpperCase();
        if (upperSql.contains("INSERT ") || upperSql.contains("UPDATE ") || upperSql.contains("DELETE ") 
                || upperSql.contains("DROP ") || upperSql.contains("ALTER ")) {
            throw new CustomException("Phát hi?n l?nh SQL nguy hi?m!", HttpStatus.FORBIDDEN);
        }

        String dbResultJson = "[]";
        try {
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(rawSql);
            dbResultJson = objectMapper.writeValueAsString(resultList);
            log.info("Stage 3 - Execution Result: {}", dbResultJson);
        } catch (Exception e) {
            log.error("L?i khi ch?y SQL: {}", e.getMessage());
            return Map.of("answer", "R?t ti?c, tôi không th? tra c?u thông tin lúc này. L?i truy v?n Database.");
        }

        // Stage 4: NLG (Natural Language Generation)
        String nlgPrompt = String.format(
                "B?n là m?t nhân viên cham sóc khách hàng thân thi?n c?a hãng xe khách.
" +
                "Khách hàng h?i: '%s'
" +
                "D? li?u tra c?u du?c t? h? th?ng (d?nh d?ng JSON): %s

" +
                "D?a vào d? li?u trên, hãy tóm t?t và tr? l?i khách hàng b?ng ti?ng Vi?t m?t cách t? nhiên, l?ch s? và d? hi?u nh?t. N?u JSON r?ng ([]), hãy báo v?i khách là không tìm th?y thông tin.",
                userQuestion, dbResultJson);

        String finalAnswer = chatModel.call(nlgPrompt);
        log.info("Stage 4 - Final Answer: {}", finalAnswer);

        return Map.of("answer", finalAnswer);
    }
}
