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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Object ask(ChatbotRequest request) {
        String userQuestion = request.getQuestion();

        // 1. Fetch Metadata
        List<TableMetadata> allMetadata = tableMetadataRepository.findAll();
        if (allMetadata.isEmpty()) {
            return Map.of("answer", "H? th?ng chua du?c c?u h�nh Metadata d? tr? l?i c�u h?i.");
        }

        String metadataContext = allMetadata.stream()
                .map(m -> "- B?ng '" + m.getTableName() + "': " + m.getShortDescription())
                .collect(Collectors.joining("\n" +
""));

        // Stage 1: Routing
        String routingPrompt = String.format(
                "Ngu?i d�ng h?i: '%s'.\n" +
"" +
                "Du?i d�y l� c�c b?ng d? li?u trong h? th?ng:\n" +
"%s\n" +
"" +
                "D?a v�o c�u h?i, h�y cho bi?t c?n truy v?n d? li?u t? nh?ng b?ng n�o? " +
                "H�y tr? l?i CH? B?NG T�N C�C B?NG c�ch nhau b?ng d?u ph?y, kh�ng gi?i th�ch g� th�m.",
                userQuestion, metadataContext);
        
        String chosenTablesStr = chatModel.call(routingPrompt).trim();
        log.info("Stage 1 - Chosen Tables: {}", chosenTablesStr);

        // Prepare schema for SQL Generation
        String schemaContext = allMetadata.stream()
                .filter(m -> chosenTablesStr.contains(m.getTableName()))
                .map(m -> m.getSchemaDefinition())
                .collect(Collectors.joining("\n" +
"\n" +
""));

        if (schemaContext.isEmpty()) {
            return Map.of("answer", "Xin l?i, t�i kh�ng t�m th?y d? li?u ph� h?p d? tr? l?i c�u h?i c?a b?n.");
        }

        // Stage 2: SQL Generation
        String sqlPrompt = String.format(
                "D?a v�o schema CSDL PostgreSQL sau:\n" +
"%s\n" +
"\n" +
"" +
                "H�y vi?t M?T c�u l?nh SQL SELECT duy nh?t d? tr? l?i c�u h?i c?a ngu?i d�ng: '%s'.\n" +
"" +
                "LUU � QUAN TR?NG: Ch? tr? v? c�u l?nh SQL th�, kh�ng d�ng code block, kh�ng gi?i th�ch.",
                schemaContext, userQuestion);
        
        String rawSql = chatModel.call(sqlPrompt).trim();
        rawSql = rawSql.replaceAll("^`sql\\s*", "").replaceAll("^`\\s*", "").replaceAll("`$", "").trim();
        log.info("Stage 2 - Generated SQL: {}", rawSql);

        // Stage 3: Execution & Security Validation
        if (!rawSql.toUpperCase().startsWith("SELECT")) {
            throw new CustomException("L?nh SQL sinh ra kh�ng h?p l? (Kh�ng ph?i l?nh SELECT).", HttpStatus.BAD_REQUEST);
        }
        String upperSql = rawSql.toUpperCase();
        if (upperSql.contains("INSERT ") || upperSql.contains("UPDATE ") || upperSql.contains("DELETE ") 
                || upperSql.contains("DROP ") || upperSql.contains("ALTER ")) {
            throw new CustomException("Ph�t hi?n l?nh SQL nguy hi?m!", HttpStatus.FORBIDDEN);
        }

        String dbResultJson = "[]";
        try {
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(rawSql);
            dbResultJson = objectMapper.writeValueAsString(resultList);
            log.info("Stage 3 - Execution Result: {}", dbResultJson);
        } catch (Exception e) {
            log.error("L?i khi ch?y SQL: {}", e.getMessage());
            return Map.of("answer", "R?t ti?c, t�i kh�ng th? tra c?u th�ng tin l�c n�y. L?i truy v?n Database.");
        }

        // Stage 4: NLG (Natural Language Generation)
        String nlgPrompt = String.format(
                "B?n l� m?t nh�n vi�n cham s�c kh�ch h�ng th�n thi?n c?a h�ng xe kh�ch.\n" +
"" +
                "Kh�ch h�ng h?i: '%s'\n" +
"" +
                "D? li?u tra c?u du?c t? h? th?ng (d?nh d?ng JSON): %s\n" +
"\n" +
"" +
                "D?a v�o d? li?u tr�n, h�y t�m t?t v� tr? l?i kh�ch h�ng b?ng ti?ng Vi?t m?t c�ch t? nhi�n, l?ch s? v� d? hi?u nh?t. N?u JSON r?ng ([]), h�y b�o v?i kh�ch l� kh�ng t�m th?y th�ng tin.",
                userQuestion, dbResultJson);

        String finalAnswer = chatModel.call(nlgPrompt);
        log.info("Stage 4 - Final Answer: {}", finalAnswer);

        return Map.of("answer", finalAnswer);
    }
}
