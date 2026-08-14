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

    public Object ask(ChatbotRequest request, String currentUserEmail) {
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
            return Map.of("answer", "Xin l?i, ti khng tm th?y d? li?u ph h?p d? tr? l?i cu h?i c?a b?n.");
        }

        // Stage 2: SQL Generation
        String sqlPrompt = String.format(
                "Dựa vào schema CSDL PostgreSQL sau:\n" +
                "%s\n\n" +
                "Tối hậu thư bảo mật: KHÔNG ĐƯỢC tạo truy vấn lấy dữ liệu cá nhân (tên, email, sđt, mật khẩu) của người dùng khác. " +
                "Chỉ được tra cứu các thông tin PUBLIC (tuyến đường, chuyến xe, giá vé, lịch trình) hoặc thông tin cá nhân của CHÍNH người dùng hiện tại (Email: %s). " +
                "Nếu câu hỏi yêu cầu lấy thông tin riêng tư của người khác, hãy trả về chính xác câu lệnh: SELECT 'VIOLATION' as error;\n\n" +
                "Hãy viết MỘT câu lệnh SQL SELECT duy nhất để trả lời câu hỏi của người dùng: '%s'.\n" +
                "LƯU Ý QUAN TRỌNG: Chỉ trả về câu lệnh SQL thô, không dùng code block, không giải thích.",
                schemaContext, currentUserEmail, userQuestion);
        
        String rawSql = chatModel.call(sqlPrompt).trim();
        rawSql = rawSql.replaceAll("^`sql\\s*", "").replaceAll("^`\\s*", "").replaceAll("`$", "").trim();
        log.info("Stage 2 - Generated SQL: {}", rawSql);

        // Stage 3: Execution & Security Validation
        if (!rawSql.toUpperCase().startsWith("SELECT")) {
            throw new CustomException("Lệnh SQL sinh ra không hợp lệ (Không phải lệnh SELECT).", HttpStatus.BAD_REQUEST);
        }
        if (rawSql.toUpperCase().contains("'VIOLATION'")) {
            return Map.of("answer", "Xin lỗi, tôi không thể cung cấp thông tin cá nhân của người khác vì lý do bảo mật.");
        }
        String upperSql = rawSql.toUpperCase();
        if (upperSql.contains("INSERT ") || upperSql.contains("UPDATE ") || upperSql.contains("DELETE ") 
                || upperSql.contains("DROP ") || upperSql.contains("ALTER ")) {
            throw new CustomException("Pht hi?n l?nh SQL nguy hi?m!", HttpStatus.FORBIDDEN);
        }

        String dbResultJson = "[]";
        try {
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(rawSql);
            dbResultJson = objectMapper.writeValueAsString(resultList);
            log.info("Stage 3 - Execution Result: {}", dbResultJson);
        } catch (Exception e) {
            log.error("L?i khi ch?y SQL: {}", e.getMessage());
            return Map.of("answer", "R?t ti?c, ti khng th? tra c?u thng tin lc ny. L?i truy v?n Database.");
        }

        // Stage 4: NLG (Natural Language Generation)
        String nlgPrompt = String.format(
                "Bạn là một nhân viên chăm sóc khách hàng thân thiện của hãng xe khách.\n" +
                "Khách hàng hỏi: '%s'\n" +
                "Dữ liệu tra cứu được từ hệ thống (định dạng JSON): %s\n\n" +
                "Dựa vào dữ liệu trên, hãy tóm tắt và trả lời khách hàng bằng tiếng Việt một cách tự nhiên. Nếu JSON rỗng ([]), hãy báo là không tìm thấy thông tin.\n" +
                "QUAN TRỌNG: Nếu khách hàng có ý định muốn ĐẶT VÉ (mua vé, chọn ghế, thanh toán) cho một chuyến xe cụ thể và bạn tìm thấy 'id' của chuyến xe đó trong JSON, " +
                "hãy thêm chính xác thẻ [[BOOK_TRIP_ID:<id>]] vào CUỐI câu trả lời của bạn. Thay <id> bằng mã id của chuyến xe.",
                userQuestion, dbResultJson);

        String finalAnswer = chatModel.call(nlgPrompt);
        log.info("Stage 4 - Final Answer: {}", finalAnswer);

        return Map.of("answer", finalAnswer);
    }
}
