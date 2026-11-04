package vn.edu.iuh.nhom01_www.chatbot.cancellation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.dto.*;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.tool.CancellationTools;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CancellationAssistantService {

    private final ChatModel chatModel;
    private final CancellationTools cancellationTools;
    private final CancellationPreviewManager previewManager;
    private final CancellationIntentParser intentParser;
    private final CurrentUserService currentUserService;
    private final CancellationPolicyService policyService;
    private final Optional<BookingService> bookingService;
    private final ChatClient chatClient;

    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public CancellationAssistantService(
            ChatModel chatModel,
            CancellationTools cancellationTools,
            CancellationPreviewManager previewManager,
            CancellationIntentParser intentParser,
            CurrentUserService currentUserService) {
        this(chatModel, cancellationTools, previewManager, intentParser, currentUserService, new CancellationPolicyService(), Optional.empty());
    }

    public CancellationAssistantService(
            ChatModel chatModel,
            CancellationTools cancellationTools,
            CancellationPreviewManager previewManager,
            CancellationIntentParser intentParser,
            CurrentUserService currentUserService,
            Optional<BookingService> bookingService) {
        this(chatModel, cancellationTools, previewManager, intentParser, currentUserService, new CancellationPolicyService(), bookingService);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CancellationAssistantService(
            ChatModel chatModel,
            CancellationTools cancellationTools,
            CancellationPreviewManager previewManager,
            CancellationIntentParser intentParser,
            CurrentUserService currentUserService,
            CancellationPolicyService policyService,
            Optional<BookingService> bookingService) {
        this.chatModel = chatModel;
        this.cancellationTools = cancellationTools;
        this.previewManager = previewManager;
        this.intentParser = intentParser;
        this.currentUserService = currentUserService;
        this.policyService = policyService != null ? policyService : new CancellationPolicyService();
        this.bookingService = bookingService != null ? bookingService : Optional.empty();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(cancellationTools)
                .build();
    }

    public Map<String, Object> assistCancellation(ChatbotRequest request, String currentUserEmail) {
        String question = request.getQuestion() != null ? request.getQuestion().trim() : "";
        if (question.isBlank()) {
            return Map.of("answer", "Xin chào! Bạn đang muốn tra cứu hoặc hủy đơn đặt vé nào ạ?");
        }

        Customer currentCustomer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        if (currentCustomer == null) {
            return Map.of(
                    "answer", "⚠️ **Yêu cầu đăng nhập**: Quý khách cần đăng nhập vào tài khoản trên hệ thống để tra cứu và thực hiện hủy vé xe an toàn ạ.",
                    "status", "UNAUTHORIZED"
            );
        }

        // Step 0: Parse intent & check prompt injection
        CancellationIntent intent = intentParser.parse(question, request.getHistory());
        if (intent.isPromptInjection()) {
            log.warn("[Security] Cancellation prompt injection intercepted for user '{}': {}", currentUserEmail, question);
            return Map.of(
                    "answer", "🛡️ [CẢNH BÁO BẢO MẬT]: " + intent.getPromptInjectionReason() + " Hệ thống bắt buộc phải hiển thị bản tóm tắt chính sách hoàn tiền và nhận sự xác nhận trực tiếp từ người dùng trước khi tiến hành hủy vé.",
                    "status", "SECURITY_REJECTED"
            );
        }

        // Step 0.5: User wants to look up active tickets (FIND_MY_BOOKINGS)
        if (intent.isFindMyBookings() || question.toUpperCase(Locale.ROOT).contains("VIEW_MY_BOOKINGS") || question.toUpperCase(Locale.ROOT).contains("MY_BOOKINGS")) {
            boolean isExplicitCancelQuery = question.toLowerCase(Locale.ROOT).contains("hủy")
                    || question.toLowerCase(Locale.ROOT).contains("huy")
                    || question.toLowerCase(Locale.ROOT).contains("trả")
                    || question.toLowerCase(Locale.ROOT).contains("hiệu lực");
            if (isExplicitCancelQuery) {
                return handleFindMyBookings(currentCustomer, intent);
            }
            return handleViewMyBookings(currentCustomer);
        }

        // Step 1: User chooses to keep tickets
        if (intent.isKeepTickets()) {
            previewManager.getLatestPreviewForCustomer(currentCustomer.getId())
                    .ifPresent(p -> previewManager.removePreview(p.getConfirmationToken()));
            return Map.of(
                    "answer", "Dạ em đã giữ lại vé cho anh/chị rồi ạ. Vé vẫn có hiệu lực bình thường để lên xe theo đúng lịch trình. Nếu cần hỗ trợ thêm thông tin gì khác, anh/chị cứ nhắn em nhé! 🚌✨",
                    "status", "KEEP_TICKETS"
            );
        }

        // Step 2: User confirms cancellation
        if (intent.isConfirmation()) {
            return handleExplicitCancellation(currentCustomer, intent);
        }

        // Step 3: User selected a specific booking or preview command
        if (intent.getTargetBookingId() != null) {
            return handleBookingSelected(currentCustomer, intent);
        }

        // Step 4: Try Spring AI Tool Calling
        String historyContext = buildHistoryContext(request.getHistory());
        String systemPrompt = buildSystemPrompt(currentCustomer);
        String userPrompt = (historyContext.isBlank() ? "" : "Lịch sử hội thoại:\n" + historyContext + "\n\n") +
                "Người dùng: " + question;

        try {
            log.info("[CancellationAssistant] Calling ChatClient with tools for query: '{}'", question);
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(cancellationTools)
                    .call()
                    .content();

            if (response != null && !response.isBlank()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("answer", response);
                result.put("assistantMode", "TOOL_CALLING");
                return result;
            }
        } catch (Exception e) {
            log.warn("[CancellationAssistant] ChatClient call exception, activating deterministic workflow fallback: {}", e.getMessage());
        }

        // Step 5: Deterministic Cancellation Workflow Fallback
        return executeDeterministicCancellationWorkflow(intent, currentCustomer);
    }

    private Map<String, Object> handleExplicitCancellation(Customer customer, CancellationIntent intent) {
        String token = intent.getConfirmationToken();
        try {
            CancellationResultDto result = cancellationTools.confirmCancellation(token, intent.getTargetBookingId(), intent.getTargetTicketIds());

            String answer = String.format(
                    "🎉 **HỦY VÉ THÀNH CÔNG!**\n\n" +
                    "- **Mã đơn đặt vé**: `%s` (ID: #%d)\n" +
                    "- **Ghế đã hủy**: **%s** (%s)\n" +
                    (result.getRemainingSeats() != null && !result.getRemainingSeats().isEmpty() ? "- **Ghế tiếp tục giữ**: `" + String.join(", ", result.getRemainingSeats()) + "`\n" : "") +
                    "- **Phí hủy áp dụng**: %s\n" +
                    "- **Số tiền hoàn trả**: **%s**\n" +
                    "- **Trạng thái hoàn tiền**: `%s` (Mã GD hoàn: `%s`)\n" +
                    "- **Thời gian thực hiện**: %s\n\n" +
                    "ℹ️ *Chỗ ngồi đã được tự động giải phóng trên sơ đồ xe. Tiền hoàn mô phỏng đã được ghi nhận thành công.*",
                    result.getBookingCode(), result.getBookingId(),
                    String.join(", ", result.getCancelledSeats()),
                    result.isPartialCancellation() ? "Hủy một phần" : "Hủy toàn bộ",
                    result.getCancellationFeeFormatted(),
                    result.getRefundAmountFormatted(),
                    result.getRefundStatus(),
                    result.getRefundTransactionId() != null ? result.getRefundTransactionId() : "N/A",
                    result.getCancelledAtFormatted()
            );

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", answer);
            res.put("status", "CANCELLATION_SUCCESS");
            res.put("cardType", "CANCELLATION_SUCCESS");
            res.put("result", result);
            return res;
        } catch (CustomException ce) {
            return Map.of(
                    "answer", "❌ **Không thể hoàn tất hủy vé**: " + ce.getMessage(),
                    "status", "CANCELLATION_FAILED"
            );
        } catch (Exception e) {
            log.error("Unexpected error during cancellation confirmation", e);
            return Map.of(
                    "answer", "❌ Có lỗi xảy ra trong quá trình hủy vé: " + e.getMessage() + ". Quý khách vui lòng thử lại.",
                    "status", "CANCELLATION_ERROR"
            );
        }
    }

    private Map<String, Object> handleBookingSelected(Customer customer, CancellationIntent intent) {
        Long bookingId = intent.getTargetBookingId();
        try {
            CancellationPreviewDto preview = cancellationTools.previewCancellation(bookingId, intent.getTargetTicketIds(), "Yêu cầu hủy qua Chatbot AI");

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", preview.getSummaryMessage() + "\n\n👉 **Anh/chị vui lòng nhấn nút 'Xác nhận hủy vé' bên dưới để em xử lý nhé!**");
            res.put("status", "PREVIEW_REQUIRED");
            res.put("cardType", "CANCELLATION_PREVIEW");
            res.put("preview", preview);
            return res;
        } catch (CustomException ce) {
            return Map.of(
                    "answer", "⚠️ Không thể chuẩn bị hủy đơn đặt vé #" + bookingId + ": " + ce.getMessage(),
                    "status", "PREVIEW_FAILED"
            );
        }
    }

    private Map<String, Object> handleFindMyBookings(Customer customer, CancellationIntent intent) {
        String keyword = null;
        if (intent.getTargetBookingCode() != null && !intent.getTargetBookingCode().isBlank()) {
            keyword = intent.getTargetBookingCode();
        } else if (intent.getTargetBookingId() != null) {
            keyword = String.valueOf(intent.getTargetBookingId());
        }

        List<MyBookingSummaryDto> myBookings = cancellationTools.findMyBookingsFiltered(
                intent.getDepartureKeyword(),
                intent.getDestinationKeyword(),
                intent.getDateKeyword(),
                keyword
        );

        if (myBookings.isEmpty()) {
            StringBuilder filterDesc = new StringBuilder();
            if (keyword != null) {
                filterDesc.append(" với mã đơn **#").append(keyword).append("**");
            }
            if (intent.getDepartureKeyword() != null && intent.getDestinationKeyword() != null) {
                filterDesc.append(" từ **").append(intent.getDepartureKeyword()).append("** đến **").append(intent.getDestinationKeyword()).append("**");
            } else if (intent.getDestinationKeyword() != null) {
                filterDesc.append(" đến **").append(intent.getDestinationKeyword()).append("**");
            } else if (intent.getDepartureKeyword() != null) {
                filterDesc.append(" từ **").append(intent.getDepartureKeyword()).append("**");
            }
            if (intent.getDateKeyword() != null) {
                filterDesc.append(" ngày **").append(intent.getDateKeyword().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("**");
            }

            return Map.of(
                    "answer", "Dạ em đã kiểm tra tài khoản của anh/chị (" + customer.getFullName() + ") nhưng hiện tại không tìm thấy vé xe nào" + filterDesc + " đang có hiệu lực ạ. Anh/chị có thể vào trang **Vé của tôi** để xem toàn bộ lịch sử đặt vé nhé! 🎫",
                    "status", "NO_BOOKINGS_FOUND"
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 **DANH SÁCH VÉ XE ĐANG CÓ HIỆU LỰC CỦA ANH/CHỊ:**\n\n");
        for (int i = 0; i < myBookings.size(); i++) {
            MyBookingSummaryDto b = myBookings.get(i);
            sb.append(String.format("%d. **Mã đơn: `%s`** (ID: #%d)\n", i + 1, b.getBookingCode(), b.getBookingId()));
            sb.append(String.format("   - **Tuyến**: %s\n", b.getRouteName()));
            sb.append(String.format("   - **Khởi hành**: %s\n", b.getDepartureTimeFormatted()));
            sb.append(String.format("   - **Số vé / Ghế**: %d vé (%s)\n", b.getTicketCount(), String.join(", ", b.getSeatNames())));
            sb.append(String.format("   - **Tổng tiền**: %s\n", b.getTotalPriceFormatted()));
            sb.append(String.format("   - **Trạng thái**: `%s`%s\n\n",
                    b.getStatus(),
                    b.isCancellable() ? " *(Đủ điều kiện hủy)*" : " *(Không thể hủy)*"));
        }
        sb.append("💡 *Để xem chi tiết vé hoặc hủy vé, anh/chị có thể nhấn nút \"Xem vé điện tử\" hoặc \"Hủy vé\" bên dưới mỗi đơn đặt vé nhé!*");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", sb.toString());
        res.put("status", "BOOKINGS_FOUND");
        res.put("cardType", "BOOKING_LOOKUP");
        res.put("bookings", myBookings);
        return res;
    }

    public Map<String, Object> handleViewMyBookings(Customer customer) {
        if (customer == null) {
            return Map.of(
                    "answer", "⚠️ **Yêu cầu đăng nhập**: Quý khách vui lòng đăng nhập tài khoản trên hệ thống VEXE để kiểm tra và quản lý danh sách vé xe đã đặt của mình ạ.",
                    "status", "UNAUTHORIZED"
            );
        }

        // 1. Query real data from existing BookingService
        List<Booking> allBookings;
        if (bookingService.isPresent()) {
            allBookings = bookingService.get().getMyBookingHistory(customer.getId());
        } else {
            allBookings = cancellationTools.getBookingRepository().findByCustomer_IdOrderByCreatedAtDesc(customer.getId());
        }

        if (allBookings == null || allBookings.isEmpty()) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", "Dạ em đã kiểm tra tài khoản của anh/chị (" + (customer.getFullName() != null ? customer.getFullName() : customer.getEmail()) + ") nhưng hiện tại chưa có đơn đặt vé nào trên hệ thống ạ. Anh/chị có thể tìm chuyến và đặt vé bất cứ lúc nào nhé! 🎫");
            res.put("status", "NO_BOOKINGS_FOUND");
            res.put("cardType", "MY_BOOKINGS");
            res.put("bookings", Collections.emptyList());
            res.put("totalBookingsCount", 0);
            res.put("hasMore", false);
            return res;
        }

        // 2. Sort by createdAt DESC (order of booking creation, NOT trip departure time)
        List<Booking> sortedBookings = new ArrayList<>(allBookings);
        sortedBookings.sort((b1, b2) -> {
            if (b1.getCreatedAt() != null && b2.getCreatedAt() != null) {
                int c = b2.getCreatedAt().compareTo(b1.getCreatedAt());
                if (c != 0) return c;
            } else if (b1.getCreatedAt() != null) {
                return -1;
            } else if (b2.getCreatedAt() != null) {
                return 1;
            }
            if (b1.getId() != null && b2.getId() != null) {
                return b2.getId().compareTo(b1.getId());
            }
            return 0;
        });

        int totalCount = sortedBookings.size();

        // 3. Take strictly 3 most recent bookings without status filtering
        List<Booking> top3 = sortedBookings.stream().limit(3).toList();

        List<MyBookingSummaryDto> dtos = new ArrayList<>();
        for (Booking b : top3) {
            String dep = (b.getTrip() != null && b.getTrip().getRoute() != null)
                    ? b.getTrip().getRoute().getDepartureLocation() : "";
            String arr = (b.getTrip() != null && b.getTrip().getRoute() != null)
                    ? b.getTrip().getRoute().getArrivalLocation() : "";
            String routeName = (!dep.isBlank() && !arr.isBlank())
                    ? dep + " → " + arr
                    : "Chuyến #" + (b.getTrip() != null ? b.getTrip().getId() : "N/A");

            LocalDateTime depTime = (b.getTrip() != null) ? b.getTrip().getDepartureTime() : null;
            String depTimeFormatted = depTime != null ? depTime.format(TIME_FORMATTER) : "N/A";
            String vehicleType = (b.getTrip() != null && b.getTrip().getVehicle() != null)
                    ? b.getTrip().getVehicle().getType() : "";

            List<String> seats = b.getSeats() != null ? b.getSeats() : Collections.emptyList();
            String totalPriceFormatted = b.getTotalPrice() != null
                    ? CURRENCY_FORMATTER.format(b.getTotalPrice()) + " VNĐ"
                    : "0 VNĐ";

            String status = b.getStatus() != null ? b.getStatus() : "UNKNOWN";
            boolean canViewTicket = "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);

            String createdAtFormatted = b.getCreatedAt() != null
                    ? b.getCreatedAt().format(CREATED_AT_FORMATTER)
                    : "N/A";

            boolean isCancellable = false;
            String nonCancellableReason = null;
            try {
                CancellationEligibilityDto eligibility = policyService.evaluateEligibility(b, null);
                isCancellable = eligibility.isEligible();
                nonCancellableReason = eligibility.getReason();
            } catch (Exception e) {
                isCancellable = false;
                nonCancellableReason = "Không hỗ trợ hủy trực tuyến";
            }

            dtos.add(MyBookingSummaryDto.builder()
                    .bookingId(b.getId())
                    .bookingCode(b.getBookingCode())
                    .tripId(b.getTrip() != null ? b.getTrip().getId() : null)
                    .routeName(routeName)
                    .departureLocation(dep)
                    .arrivalLocation(arr)
                    .departureTime(depTime)
                    .departureTimeFormatted(depTimeFormatted)
                    .vehicleType(vehicleType)
                    .seatNames(seats)
                    .ticketCount(seats.size())
                    .totalPrice(b.getTotalPrice())
                    .totalPriceFormatted(totalPriceFormatted)
                    .status(status)
                    .createdAt(b.getCreatedAt())
                    .createdAtFormatted(createdAtFormatted)
                    .canViewTicket(canViewTicket)
                    .isCancellable(isCancellable)
                    .nonCancellableReason(nonCancellableReason)
                    .build());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📋 **DANH SÁCH %d ĐƠN ĐẶT VÉ GẦN NHẤT CỦA ANH/CHỊ:**\n\n", dtos.size()));
        for (int i = 0; i < dtos.size(); i++) {
            MyBookingSummaryDto b = dtos.get(i);
            sb.append(String.format("%d. **Mã đơn: `%s`** (ID: #%d)\n", i + 1, b.getBookingCode(), b.getBookingId()));
            sb.append(String.format("   - **Tuyến**: %s\n", b.getRouteName()));
            sb.append(String.format("   - **Khởi hành**: %s\n", b.getDepartureTimeFormatted()));
            sb.append(String.format("   - **Ghế**: %s\n", b.getSeatNames().isEmpty() ? "Chưa có ghế" : String.join(", ", b.getSeatNames())));
            sb.append(String.format("   - **Tổng tiền**: %s\n", b.getTotalPriceFormatted()));
            sb.append(String.format("   - **Trạng thái**: `%s`\n\n", b.getStatus()));
        }
        if (totalCount > 3) {
            sb.append(String.format("👉 *Anh/chị có tổng cộng %d đơn đặt vé. Vui lòng nhấn nút **\"Xem thêm\"** bên dưới để xem toàn bộ lịch sử đặt vé tại trang Quản lý vé nhé!*", totalCount));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", sb.toString());
        res.put("status", "BOOKINGS_FOUND");
        res.put("cardType", "MY_BOOKINGS");
        res.put("bookings", dtos);
        res.put("totalBookingsCount", totalCount);
        res.put("hasMore", totalCount > 3);
        return res;
    }

    public Map<String, Object> handleViewMyBookings(String currentUserEmail) {
        Customer currentCustomer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        if (currentCustomer == null) {
            return Map.of(
                    "answer", "⚠️ **Yêu cầu đăng nhập**: Quý khách vui lòng đăng nhập tài khoản trên hệ thống VEXE để kiểm tra và quản lý danh sách vé xe đã đặt của mình ạ.",
                    "status", "UNAUTHORIZED"
            );
        }
        return handleViewMyBookings(currentCustomer);
    }

    private Map<String, Object> executeDeterministicCancellationWorkflow(CancellationIntent intent, Customer customer) {
        List<MyBookingSummaryDto> myBookings = cancellationTools.findMyBookingsFiltered(
                intent.getDepartureKeyword(),
                intent.getDestinationKeyword(),
                intent.getDateKeyword(),
                null
        );
        if (myBookings.isEmpty()) {
            StringBuilder filterDesc = new StringBuilder();
            if (intent.getDepartureKeyword() != null && intent.getDestinationKeyword() != null) {
                filterDesc.append(" từ **").append(intent.getDepartureKeyword()).append("** đến **").append(intent.getDestinationKeyword()).append("**");
            } else if (intent.getDestinationKeyword() != null) {
                filterDesc.append(" đến **").append(intent.getDestinationKeyword()).append("**");
            } else if (intent.getDepartureKeyword() != null) {
                filterDesc.append(" từ **").append(intent.getDepartureKeyword()).append("**");
            }
            if (intent.getDateKeyword() != null) {
                filterDesc.append(" ngày **").append(intent.getDateKeyword().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("**");
            }

            if (intent.getDestinationKeyword() != null && intent.getDepartureKeyword() == null) {
                return Map.of(
                        "answer", "Dạ em không tìm thấy vé đến **" + intent.getDestinationKeyword() + "** trong danh sách vé của anh/chị ạ.",
                        "status", "NO_BOOKINGS_FOUND"
                );
            }

            return Map.of(
                    "answer", "Dạ em đã kiểm tra tài khoản của anh/chị nhưng không tìm thấy đơn đặt vé nào" +
                            (filterDesc.length() > 0 ? filterDesc.toString() : "") + " đang có hiệu lực. Anh/chị có muốn kiểm tra lại danh sách vé đã mua không ạ?",
                    "status", "NO_BOOKINGS_FOUND"
            );
        }

        // If multiple bookings found, ask user to pick
        if (myBookings.size() > 1) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", "Dạ em tìm thấy **" + myBookings.size() + " đơn đặt vé** phù hợp trong tài khoản của anh/chị. Anh/chị vui lòng chọn đúng đơn cần hủy bên dưới nhé:");
            res.put("status", "MULTIPLE_BOOKINGS_FOUND");
            res.put("cardType", "BOOKING_LOOKUP");
            res.put("bookings", myBookings);
            return res;
        }

        // Exactly 1 booking found
        MyBookingSummaryDto booking = myBookings.get(0);
        if (!booking.isCancellable()) {
            return Map.of(
                    "answer", "⚠️ Đơn đặt vé **" + booking.getBookingCode() + "** (" + booking.getRouteName() + ") hiện không đủ điều kiện để hủy vé trực tuyến: *" + booking.getNonCancellableReason() + "*",
                    "status", "NOT_ELIGIBLE"
            );
        }

        // Generate Preview
        try {
            CancellationPreviewDto preview = cancellationTools.previewCancellation(booking.getBookingId(), null, "Yêu cầu hủy qua Chatbot AI");

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", preview.getSummaryMessage() + "\n\n👉 **Anh/chị vui lòng kiểm tra kỹ và nhấn 'Xác nhận hủy vé' để hoàn tất!**");
            res.put("status", "PREVIEW_REQUIRED");
            res.put("cardType", "CANCELLATION_PREVIEW");
            res.put("preview", preview);
            return res;
        } catch (CustomException ce) {
            return Map.of(
                    "answer", "⚠️ Không thể chuẩn bị hủy vé: " + ce.getMessage(),
                    "status", "PREVIEW_FAILED"
            );
        }
    }

    private String buildHistoryContext(List<ChatbotRequest.ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";
        return history.stream()
                .map(msg -> (msg.getRole() != null && msg.getRole().equalsIgnoreCase("user") ? "Khách: " : "Trợ lý: ") + msg.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String buildSystemPrompt(Customer customer) {
        return "Bạn là Trợ lý AI Hủy vé xe khách chuyên nghiệp của hệ thống VEXE.\n" +
                "Khách hàng đã đăng nhập tài khoản: " + customer.getFullName() + " (Email: " + customer.getEmail() + ").\n\n" +
                "QUY TẮC BẮT BUỘC:\n" +
                "1. Khi khách hàng muốn hủy vé xe, sử dụng tool `findMyBookings` để tìm các đơn đặt vé của khách.\n" +
                "2. Nếu có nhiều đơn đặt vé, yêu cầu khách hàng xác định đúng đơn.\n" +
                "3. Luôn sử dụng tool `checkCancellationEligibility` và `previewCancellation` để tính toán số tiền hoàn và phí hủy theo đúng chính sách backend. KHÔNG TỰ BỊA ĐẶT PHÍ HỦY.\n" +
                "4. TUYỆT ĐỐI KHÔNG ĐƯỢC gọi `confirmCancellation` nếu khách hàng chưa nhấn nút hoặc chưa đồng ý xác nhận rõ ràng trong hội thoại.\n" +
                "5. Luôn trả lời bằng tiếng Việt thân thiện, rõ ràng, định dạng markdown đẹp mắt.";
    }
}
