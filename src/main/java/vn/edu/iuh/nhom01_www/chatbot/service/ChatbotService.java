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

        // Stage 0: Pre-execution Guardrail
        String guardrailPrompt = String.format(
                "Kiểm tra xem câu hỏi sau có chứa nội dung bậy bạ, nhạy cảm, độc hại, hoặc yêu cầu lấy thông tin cá nhân/nhạy cảm của người khác không. " +
                "Câu hỏi: '%s'. Trả lời 'SAFE' nếu an toàn, hoặc trả lời một câu từ chối lịch sự nếu vi phạm.",
                userQuestion);
        String guardrailResult = chatModel.call(guardrailPrompt).trim();
        log.info("Stage 0 - Guardrail Result: {}", guardrailResult);
        if (!"SAFE".equalsIgnoreCase(guardrailResult)) {
            return Map.of("answer", guardrailResult);
        }

        // 1. Fetch Metadata
        List<TableMetadata> allMetadata = tableMetadataRepository.findAll();
        if (allMetadata.isEmpty()) {
            return Map.of("answer", "Hệ thống chưa được cấu hình Metadata để trả lời câu hỏi.");
        }

        String metadataContext = allMetadata.stream()
                .map(m -> "- Bảng '" + m.getTableName() + "': " + m.getShortDescription())
                .collect(Collectors.joining("\n"));

        // Stage 1: Routing
        String routingPrompt = String.format(
                "Người dùng hỏi: '%s'.\n" +
                "Dưới đây là các bảng dữ liệu trong hệ thống:\n" +
                "%s\n" +
                "Dựa vào câu hỏi, hãy cho biết cần truy vấn dữ liệu từ những bảng nào? " +
                "Hãy trả lời CHỈ BẰNG TÊN CÁC BẢNG cách nhau bằng dấu phẩy, không giải thích gì thêm.",
                userQuestion, metadataContext);
        
        String chosenTablesStr = chatModel.call(routingPrompt).trim();
        log.info("Stage 1 - Chosen Tables: {}", chosenTablesStr);

        // Prepare schema for SQL Generation
        String schemaContext = allMetadata.stream()
                .filter(m -> chosenTablesStr.contains(m.getTableName()))
                .map(m -> m.getSchemaDefinition())
                .collect(Collectors.joining("\n\n"));

        if (schemaContext.isEmpty()) {
            return Map.of("answer", "Xin lỗi, tôi không tìm thấy dữ liệu phù hợp để trả lời câu hỏi của bạn.");
        }

        // Stage 2 & 3: SQL Generation & Execution Loop
        String dbResultJson = "[]";
        boolean sqlSuccess = false;
        String rawSql = "";
        String lastError = "";

        for (int i = 1; i <= 5; i++) {
            log.info("Stage 2 & 3 - Attempt {}/5", i);
            String sqlPrompt;
            if (i == 1) {
                sqlPrompt = String.format(
                        "Dựa vào schema CSDL PostgreSQL sau:\n" +
                        "%s\n\n" +
                        "Tối hậu thư bảo mật: KHÔNG ĐƯỢC tạo truy vấn lấy dữ liệu cá nhân (tên, email, sđt, mật khẩu) của người dùng khác. " +
                        "Chỉ được tra cứu các thông tin PUBLIC (tuyến đường, chuyến xe, giá vé, lịch trình) hoặc thông tin cá nhân của CHÍNH người dùng hiện tại (Email: %s). " +
                        "Nếu câu hỏi yêu cầu lấy thông tin riêng tư của người khác, hãy trả về chính xác câu lệnh: SELECT 'VIOLATION' as error;\n\n" +
                        "QUAN TRỌNG: Khi tìm kiếm theo chuỗi (như tên địa điểm, tên người), KHÔNG ĐƯỢC DÙNG toán tử '=', MÀ BẮT BUỘC phải dùng toán tử ILIKE với ký tự đại diện '%%' ở hai đầu để tìm kiếm tương đối và không phân biệt hoa thường. Ví dụ: ILIKE '%%Đà Nẵng%%' hoặc ILIKE '%%Huế%%'.\n\n" +
                        "Hãy viết MỘT câu lệnh SQL SELECT duy nhất để trả lời câu hỏi của người dùng: '%s'.\n" +
                        "LƯU Ý QUAN TRỌNG: Chỉ trả về câu lệnh SQL thô, không dùng code block, không giải thích.",
                        schemaContext, currentUserEmail, userQuestion);
            } else {
                sqlPrompt = String.format(
                        "Câu lệnh SQL trước đó bạn viết: '%s' đã gây ra lỗi Database sau: '%s'.\n" +
                        "Dựa vào schema:\n%s\n\n" +
                        "Hãy sửa lại câu lệnh SQL cho đúng. CHỈ trả về câu lệnh SQL thô, không dùng code block, không giải thích.",
                        rawSql, lastError, schemaContext);
            }

            rawSql = chatModel.call(sqlPrompt).trim();
            rawSql = rawSql.replaceAll("^`sql\\s*", "").replaceAll("^`\\s*", "").replaceAll("`$", "").trim();
            log.info("Stage 2 - Generated SQL: {}", rawSql);

            // Security Validation
            if (!rawSql.toUpperCase().startsWith("SELECT")) {
                throw new CustomException("Lệnh SQL sinh ra không hợp lệ (Không phải lệnh SELECT).", HttpStatus.BAD_REQUEST);
            }
            if (rawSql.toUpperCase().contains("'VIOLATION'")) {
                return Map.of("answer", "Xin lỗi, tôi không thể cung cấp thông tin cá nhân của người khác vì lý do bảo mật.");
            }
            String upperSql = rawSql.toUpperCase();
            if (upperSql.contains("INSERT ") || upperSql.contains("UPDATE ") || upperSql.contains("DELETE ") 
                    || upperSql.contains("DROP ") || upperSql.contains("ALTER ")) {
                throw new CustomException("Phát hiện lệnh SQL nguy hiểm!", HttpStatus.FORBIDDEN);
            }

            try {
                List<Map<String, Object>> resultList = jdbcTemplate.queryForList(rawSql);
                dbResultJson = objectMapper.writeValueAsString(resultList);
                log.info("Stage 3 - Execution Result: {}", dbResultJson);
                sqlSuccess = true;
                break; // Break the loop on success
            } catch (Exception e) {
                lastError = e.getMessage();
                log.error("Stage 3 - SQL Execution Error on attempt {}: {}", i, lastError);
            }
        }

        if (!sqlSuccess) {
            return Map.of("answer", "Xin lỗi, tôi đang gặp khó khăn khi tra cứu dữ liệu cho câu hỏi này. Vui lòng thử lại với cách hỏi khác.");
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
