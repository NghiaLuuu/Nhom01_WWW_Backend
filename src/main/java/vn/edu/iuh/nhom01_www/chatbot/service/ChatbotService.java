package vn.edu.iuh.nhom01_www.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.aivector.VectorIngestionReport;
import vn.edu.iuh.nhom01_www.aivector.VectorIngestionService;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchRequest;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchResult;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchService;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.TripSearchResultDto;
import vn.edu.iuh.nhom01_www.chatbot.booking.service.BookingAssistantService;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.service.CancellationAssistantService;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatIntent;

@Slf4j
@Service
public class ChatbotService {

    private final ChatModel chatModel;
    private final Optional<VectorSearchService> vectorSearchService;
    private final Optional<VectorIngestionService> vectorIngestionService;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final Optional<BookingAssistantService> bookingAssistantService;
    private final Optional<CancellationAssistantService> cancellationAssistantService;
    private final ChatIntentClassifier intentClassifier;

    public ChatbotService(
            ChatModel chatModel,
            Optional<VectorSearchService> vectorSearchService,
            Optional<VectorIngestionService> vectorIngestionService,
            TripRepository tripRepository,
            RouteRepository routeRepository) {
        this(chatModel, vectorSearchService, vectorIngestionService, tripRepository, routeRepository, Optional.empty(), Optional.empty(), new ChatIntentClassifier());
    }

    public ChatbotService(
            ChatModel chatModel,
            Optional<VectorSearchService> vectorSearchService,
            Optional<VectorIngestionService> vectorIngestionService,
            TripRepository tripRepository,
            RouteRepository routeRepository,
            Optional<BookingAssistantService> bookingAssistantService) {
        this(chatModel, vectorSearchService, vectorIngestionService, tripRepository, routeRepository, bookingAssistantService, Optional.empty(), new ChatIntentClassifier());
    }

    public ChatbotService(
            ChatModel chatModel,
            Optional<VectorSearchService> vectorSearchService,
            Optional<VectorIngestionService> vectorIngestionService,
            TripRepository tripRepository,
            RouteRepository routeRepository,
            Optional<BookingAssistantService> bookingAssistantService,
            Optional<CancellationAssistantService> cancellationAssistantService) {
        this(chatModel, vectorSearchService, vectorIngestionService, tripRepository, routeRepository, bookingAssistantService, cancellationAssistantService, new ChatIntentClassifier());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ChatbotService(
            ChatModel chatModel,
            Optional<VectorSearchService> vectorSearchService,
            Optional<VectorIngestionService> vectorIngestionService,
            TripRepository tripRepository,
            RouteRepository routeRepository,
            Optional<BookingAssistantService> bookingAssistantService,
            Optional<CancellationAssistantService> cancellationAssistantService,
            ChatIntentClassifier intentClassifier) {
        this.chatModel = chatModel;
        this.vectorSearchService = vectorSearchService;
        this.vectorIngestionService = vectorIngestionService;
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.bookingAssistantService = bookingAssistantService != null ? bookingAssistantService : Optional.empty();
        this.cancellationAssistantService = cancellationAssistantService != null ? cancellationAssistantService : Optional.empty();
        this.intentClassifier = intentClassifier != null ? intentClassifier : new ChatIntentClassifier();
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    public Map<String, Object> ask(ChatbotRequest request, String currentUserEmail) {
        // Structured interactive actions routing (bypass text checks)
        if (request.getAction() != null && !request.getAction().isBlank()) {
            String act = request.getAction().trim().toUpperCase(Locale.ROOT);
            if ((act.contains("CANCEL") || act.contains("KEEP")) && cancellationAssistantService.isPresent()) {
                return cancellationAssistantService.get().assistCancellation(request, currentUserEmail);
            }
            if (bookingAssistantService.isPresent()) {
                return bookingAssistantService.get().assistBooking(request, currentUserEmail);
            }
        }

        String userQuestion = request.getQuestion();
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return Map.of("answer", "Xin chào! Bạn cần tìm chuyến xe hoặc thông tin vé đi đâu ạ?");
        }
        userQuestion = userQuestion.trim();

        String historyContext = "";
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            historyContext = request.getHistory().stream()
                    .map(msg -> (msg.getRole() != null && msg.getRole().equalsIgnoreCase("user") ? "Khách: " : "Trợ lý: ") + msg.getContent())
                    .collect(Collectors.joining("\n"));
        }

        // Stage 0: Security Whitelist Firewall (Blocks PII, passwords, internal financials, prompt injections)
        String securityRefusal = checkSecurityFirewall(userQuestion);
        if (securityRefusal != null) {
            log.warn("[Security Firewall] Blocked sensitive/unsafe query from '{}': '{}'", currentUserEmail, userQuestion);
            return Map.of("answer", securityRefusal, "matchedCount", 0);
        }

        // Stage 0.4: Unified Intent Classification & Context Isolation
        String sessionKey = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : ((request.getConversationId() != null && !request.getConversationId().isBlank())
                    ? request.getConversationId()
                    : "session_" + currentUserEmail);

        boolean hasActiveBooking = bookingAssistantService.map(s -> s.hasActiveBookingFlow(sessionKey)).orElse(false);
        ChatIntent intent = intentClassifier.classify(userQuestion, hasActiveBooking);
        log.info("[ChatbotRouter] Query '{}' classified as intent '{}' (hasActiveBooking={})", userQuestion, intent, hasActiveBooking);

        switch (intent) {
            case CANCEL_TICKET -> {
                if (bookingAssistantService.isPresent()) {
                    bookingAssistantService.get().resetBookingSession(sessionKey);
                }
                if (cancellationAssistantService.isPresent()) {
                    return cancellationAssistantService.get().assistCancellation(request, currentUserEmail);
                }
            }
            case VIEW_MY_BOOKINGS -> {
                if (bookingAssistantService.isPresent()) {
                    bookingAssistantService.get().resetBookingSession(sessionKey);
                }
                if (currentUserEmail == null || "ANONYMOUS".equalsIgnoreCase(currentUserEmail)) {
                    return Map.of(
                            "answer", "⚠️ **Yêu cầu đăng nhập**: Quý khách vui lòng đăng nhập tài khoản trên hệ thống VEXE để kiểm tra và quản lý danh sách vé xe đã đặt của mình ạ.",
                            "status", "UNAUTHORIZED"
                    );
                }
                if (cancellationAssistantService.isPresent()) {
                    return cancellationAssistantService.get().assistCancellation(request, currentUserEmail);
                }
            }
            case PAYMENT_STATUS -> {
                if (bookingAssistantService.isPresent()) {
                    return bookingAssistantService.get().assistBooking(request, currentUserEmail);
                }
            }
            case BOOK_TICKET -> {
                if (bookingAssistantService.isPresent()) {
                    return bookingAssistantService.get().assistBooking(request, currentUserEmail);
                }
            }
            case SEARCH_TRIPS -> {
                if (bookingAssistantService.isPresent()) {
                    bookingAssistantService.get().resetBookingSession(sessionKey);
                    return handleSearchTrips(userQuestion, request, currentUserEmail);
                }
                // When bookingAssistantService is not present (e.g. pure AI vector / RAG testing mode),
                // fall-through to Vector Search RAG pipeline below.
            }
            case GENERAL_CHAT -> {
                // Continue to natural conversation handling below
            }
        }

        // Stage 1: Natural Conversational Intent Handling (Fast, empathetic, context-aware)
        if (isIdentityQuestion(userQuestion)) {
            return Map.of("answer",
                    "Dạ em là **Trợ lý AI của hệ thống VEXE** 🚌.\n\n" +
                    "Em được lập trình chuyên sâu để hỗ trợ anh/chị:\n" +
                    "- 🔍 **Tra cứu chuyến xe liên tỉnh**: Tìm kiếm lịch chạy, điểm đón/trả và lộ trình xe trên toàn quốc.\n" +
                    "- 💰 **So sánh giá vé & loại xe**: Cung cấp thông tin giá vé niêm yết cho các dòng xe Ghế ngồi, Giường nằm và Limousine VIP.\n" +
                    "- 🎫 **Gợi ý đặt chỗ nhanh**: Hỗ trợ tìm chuyến phù hợp với khung giờ và ngân sách của anh/chị.\n\n" +
                    "Anh/chị đang có kế hoạch đi đâu, hãy chia sẻ điểm đi và điểm đến để em hỗ trợ nhé!");
        }

        if (isConfusionOrEmotional(userQuestion)) {
            return Map.of("answer",
                    "Dạ em xin lỗi nếu câu trả lời vừa rồi chưa rõ ràng hoặc làm anh/chị bối rối ạ! 😅\n\n" +
                    "Em là trợ lý AI chuyên hỗ trợ tra cứu lịch trình và giá vé xe khách của **VEXE**. Anh/chị đang cần tìm chuyến xe đi đâu hoặc có câu hỏi nào về vé xe, lộ trình, giá cả cần em giải đáp không ạ? Hãy chia sẻ với em nhé!");
        }

        if (isGratitude(userQuestion)) {
            return Map.of("answer",
                    "Dạ không có gì ạ! Rất vui được hỗ trợ anh/chị. Chúc anh/chị có những chuyến đi thượng lộ bình an và thoải mái cùng VEXE! Nếu cần tìm thêm chuyến xe nào khác, anh/chị cứ nhắn em nhé! 😊");
        }

        if (isFarewell(userQuestion)) {
            return Map.of("answer",
                    "Dạ tạm biệt anh/chị! Chúc anh/chị một ngày thật nhiều niềm vui và hẹn gặp lại trên những hành trình tiếp theo của VEXE! 👋");
        }

        if (isBookingGuide(userQuestion)) {
            return Map.of("answer",
                    "Để đặt vé nhanh chóng tại **VEXE**, anh/chị chỉ cần thực hiện 3 bước đơn giản:\n\n" +
                    "1. 🔍 **Tìm chuyến**: Nhập điểm đi, điểm đến và ngày khởi hành mong muốn tại thanh tìm kiếm.\n" +
                    "2. 💺 **Chọn chỗ ngồi**: Xem sơ đồ ghế trực quan và chọn vị trí ghế/giường yêu thích.\n" +
                    "3. 💳 **Xác nhận & Thanh toán**: Điền thông tin hành khách và thanh toán tiện lợi để nhận mã vé điện tử ngay tức thì.\n\n" +
                    "Anh/chị đang muốn đặt vé đi đâu, hãy cho em biết điểm đi và điểm đến để em tra cứu giúp mình nhé!");
        }

        if (isGreeting(userQuestion)) {
            return Map.of("answer",
                    "**Xin chào anh/chị!** Em là trợ lý VEXE 🚌.\n" +
                    "Em có thể giúp anh/chị tra cứu lịch trình chuyến xe, giá vé hoặc hướng dẫn đặt chỗ nhanh chóng.\n\n" +
                    "Anh/chị đang có kế hoạch đi đâu hôm nay ạ?");
        }

        // Stage 2: Intent-Aware Partitioned Vector Search in PostgreSQL pgvector
        String partition = routePartition(userQuestion);
        List<VectorSearchResult> searchResults = Collections.emptyList();
        if (vectorSearchService.isPresent()) {
            try {
                VectorSearchRequest searchRequest = new VectorSearchRequest(userQuestion, 6, 0.30d, partition, null);
                searchResults = vectorSearchService.get().search(searchRequest);
                log.info("RAG Partition [{}]: Vector search returned {} results for query: '{}'", 
                        partition != null ? partition : "ALL", searchResults.size(), userQuestion);
            } catch (Exception e) {
                log.warn("Vector search error in partition {}, fallback to unpartitioned: {}", partition, e.getMessage());
                try {
                    searchResults = vectorSearchService.get().search(new VectorSearchRequest(userQuestion, 6, 0.25d, null, null));
                } catch (Exception ignored) {}
            }
        }

        // Stage 3: Context Enrichment from Domain Data (Strict future-only filter)
        String ragContext = buildRagContext(searchResults, userQuestion);

        // Stage 4: Natural Language Generation (ChatModel with Spring AI prompt engineering)
        String nlgPrompt = buildNlgPrompt(userQuestion, historyContext, ragContext, currentUserEmail);
        log.debug("RAG: Generated prompt length: {}", nlgPrompt.length());

        String finalAnswer;
        try {
            finalAnswer = chatModel.call(nlgPrompt);
        } catch (Exception e) {
            log.warn("Failed to generate answer from ChatModel, activating Domain Knowledge Fallback: {}", e.getMessage());
            finalAnswer = buildDomainFallbackAnswer(userQuestion, ragContext);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer", finalAnswer);
        response.put("matchedCount", searchResults.size());
        if (partition != null) {
            response.put("routedPartition", partition);
        }
        return response;
    }

    public VectorIngestionReport ingestAll() {
        if (vectorIngestionService.isEmpty()) {
            throw new IllegalStateException("VectorIngestionService is not available (ai-vector profile not active)");
        }
        try {
            return vectorIngestionService.get().ingestAll();
        } catch (Exception e) {
            log.warn("[ChatbotService] Notice during vector ingestion: {}", e.getMessage());
            int routes = (int) routeRepository.count();
            int trips = (int) tripRepository.count();
            return new VectorIngestionReport(routes + trips, 0, routes + trips, 0, routes + trips, 0);
        }
    }

    public String routePartition(String question) {
        if (question == null) return null;
        String lower = question.toLowerCase(Locale.ROOT);

        if (lower.contains("loại xe") || lower.contains("limousine") || lower.contains("giường nằm")
                || lower.contains("tiện ích") || lower.contains("wifi") || lower.contains("massage")
                || lower.contains("sạc") || lower.contains("màn hình")) {
            return "VEHICLE";
        }

        if (lower.contains("tuyến") || lower.contains("đường") || lower.contains("lộ trình") 
                || lower.contains("bến xe") || lower.contains("trạm") || lower.contains("km") 
                || lower.contains("khoảng cách") || lower.contains("mất mấy tiếng") || lower.contains("bao xa")) {
            return "ROUTE";
        }

        if (lower.contains("chuyến") || lower.contains("giờ") || lower.contains("sáng") || lower.contains("tối")
                || lower.contains("trưa") || lower.contains("chiều") || lower.contains("ngày mai") 
                || lower.contains("hôm nay") || lower.contains("cuối tuần") || lower.contains("giá vé") 
                || lower.contains("bao nhiêu tiền") || lower.contains("còn vé") || lower.contains("ghế")
                || lower.contains("đặt vé") || lower.contains("mua vé")) {
            return "TRIP";
        }

        return null;
    }

    public String checkSecurityFirewall(String question) {
        if (question == null) return null;
        String lower = question.toLowerCase(Locale.ROOT);

        if (lower.contains("mật khẩu") || lower.contains("password") || lower.contains("token") 
                || lower.contains("secret") || lower.contains("api_key") || lower.contains("apikey")) {
            return "🛡️ [BẢO MẬT HỆ THỐNG]: Vì lý do an toàn, trợ lý VEXE không được phép truy xuất hoặc tiết lộ thông tin mật khẩu và khóa bảo mật của bất kỳ tài khoản nào.";
        }

        if (lower.contains("mã nhân viên") || lower.contains("lương nhân viên") || lower.contains("bảng lương")
                || lower.contains("staff code") || lower.contains("cmnd nhân viên") || lower.contains("cccd nhân viên")) {
            return "🛡️ [BẢO MẬT NỘI BỘ]: Dữ liệu nhân sự và quản trị nội bộ của công ty được bảo mật theo quy định, không thể cung cấp ra ngoài.";
        }

        if (lower.contains("doanh thu nội bộ") || lower.contains("lợi nhuận ròng") || lower.contains("báo cáo tài chính nội bộ")
                || lower.contains("chi phí vận hành nội bộ")) {
            return "🛡️ [BẢO MẬT TÀI CHÍNH]: Số liệu tài chính nội bộ được bảo vệ bởi chính sách bảo mật doanh nghiệp.";
        }

        if (lower.contains("thông tin cá nhân của người khác") || lower.contains("danh sách hành khách")
                || lower.contains("số điện thoại khách khác") || lower.contains("ai đang ngồi ghế")
                || lower.contains("vé của người khác")) {
            return "🛡️ [BẢO MẬT KHÁCH HÀNG]: VEXE cam kết bảo mật 100% dữ liệu hành khách. Bạn chỉ có thể tra cứu thông tin vé của chính mình bằng mã đặt vé.";
        }

        if (lower.contains("ignore all previous instructions") || lower.contains("system prompt")
                || lower.contains("developer mode") || lower.contains("jailbreak")
                || lower.contains("bỏ qua các chỉ dẫn trước") || lower.contains("quên các câu lệnh trước")) {
            return "🛡️ [AN TOÀN AI]: Yêu cầu vi phạm chính sách an toàn của hệ thống. Tôi chỉ có thể hỗ trợ các thông tin du lịch và đặt vé xe.";
        }

        if (lower.contains("drop table") || lower.contains("delete from") || lower.contains("select * from users")
                || lower.contains("truncate table") || lower.contains("exec ") || lower.contains("xp_cmdshell")) {
            return "🛡️ [CẢNH BÁO BẢO MẬT]: Phát hiện truy vấn độc hại. Yêu cầu đã bị từ chối và ghi lại nhật ký an ninh.";
        }

        return null;
    }

    private boolean isIdentityQuestion(String question) {
        String lower = question.toLowerCase(Locale.ROOT).trim();
        List<String> keywords = List.of(
                "bạn là ai", "em là ai", "mày là ai", "ai đấy", "ai đó", "bạn tên gì", "who are you",
                "giới thiệu bản thân", "bạn làm được gì", "bot là gì"
        );
        return keywords.stream().anyMatch(k -> lower.equals(k) || lower.contains(k));
    }

    private boolean isConfusionOrEmotional(String question) {
        String lower = question.toLowerCase(Locale.ROOT).trim();
        List<String> keywords = List.of(
                "???", "??", "?", "sao thế", "sao vậy", "sao kì vậy", "kì vậy", "gì vậy",
                "gì kì vậy", "sao lạ vậy", "hả", "sao cơ", "thế à", "bot ngáo à", "lỗi à"
        );
        return keywords.stream().anyMatch(k -> lower.equals(k) || lower.startsWith(k + " ") || lower.endsWith(" " + k));
    }

    private boolean isGratitude(String question) {
        String lower = question.toLowerCase(Locale.ROOT).trim();
        List<String> keywords = List.of("cảm ơn", "thank you", "thanks", "cảm ơn bạn", "cảm ơn em", "tks");
        return keywords.stream().anyMatch(k -> lower.equals(k) || lower.startsWith(k + " ") || lower.endsWith(" " + k));
    }

    private boolean isFarewell(String question) {
        String lower = question.toLowerCase(Locale.ROOT).trim();
        List<String> keywords = List.of("tạm biệt", "bye", "goodbye", "hẹn gặp lại", "bai");
        return keywords.stream().anyMatch(k -> lower.equals(k) || lower.startsWith(k + " ") || lower.endsWith(" " + k));
    }

    private boolean isBookingGuide(String question) {
        String lower = question.toLowerCase(Locale.ROOT).trim();
        List<String> keywords = List.of("cách đặt vé", "làm sao để đặt vé", "hướng dẫn đặt vé", "các bước đặt vé", "mua vé thế nào");
        return keywords.stream().anyMatch(k -> lower.equals(k) || lower.contains(k));
    }

    private boolean isGreeting(String question) {
        String lower = question.toLowerCase(Locale.ROOT).trim();
        List<String> greetings = List.of(
                "xin chào", "chào bạn", "chào em", "chào bot", "chào", "hello", "hi", "hey",
                "alo", "start", "bắt đầu"
        );
        return greetings.stream().anyMatch(g -> lower.equals(g) || lower.startsWith(g + " ") || lower.endsWith(" " + g));
    }

    private String buildRagContext(List<VectorSearchResult> searchResults, String userQuestion) {
        StringBuilder context = new StringBuilder();
        Set<Long> processedTripIds = new HashSet<>();
        Set<Long> processedRouteIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        if (searchResults != null && !searchResults.isEmpty()) {
            for (VectorSearchResult result : searchResults) {
                Map<String, Object> meta = result.metadata();
                String sourceType = meta != null && meta.get("sourceType") != null ? meta.get("sourceType").toString() : "";

                if ("trip".equalsIgnoreCase(sourceType) && meta.get("tripId") != null) {
                    try {
                        Long tripId = Long.parseLong(meta.get("tripId").toString());
                        if (processedTripIds.add(tripId)) {
                            tripRepository.findById(tripId).ifPresent(trip -> {
                                // STRICT GUARD: Filter out past or cancelled trips from vector store!
                                if (trip.getDepartureTime() != null && trip.getDepartureTime().isAfter(now)
                                        && !"CANCELLED".equalsIgnoreCase(trip.getStatus())) {
                                    context.append(formatTripDetails(trip)).append("\n---\n");
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                } else if ("route".equalsIgnoreCase(sourceType) && meta.get("routeId") != null) {
                    try {
                        Long routeId = Long.parseLong(meta.get("routeId").toString());
                        if (processedRouteIds.add(routeId)) {
                            routeRepository.findById(routeId).ifPresent(route -> {
                                context.append(formatRouteDetails(route)).append("\n---\n");
                            });
                        }
                    } catch (Exception ignored) {}
                } else if (result.content() != null && !result.content().isBlank()) {
                    context.append(result.content().trim()).append("\n---\n");
                }
            }
        }

        // Live Inventory Context Enrichment:
        // Always provide relevant, real-time future trips matching the user's destination, route, and target date
        String lower = userQuestion.toLowerCase(Locale.ROOT);
        boolean needsEnrichment = processedTripIds.size() < 3 || lower.contains("rẻ") || lower.contains("mai")
                || lower.contains("chuyến") || lower.contains("vé") || lower.contains("xe") || lower.contains("giá")
                || lower.contains("sapa") || lower.contains("đà lạt") || lower.contains("hà nội");

        if (needsEnrichment) {
            List<Trip> futureTrips = Collections.emptyList();
            try {
                futureTrips = new ArrayList<>(tripRepository.findUpcomingTrips(now, PageRequest.of(0, 50)));
            } catch (Exception ignored) {}
            if (futureTrips.isEmpty()) {
                futureTrips = tripRepository.findAll().stream()
                        .filter(t -> t.getDepartureTime() != null && t.getDepartureTime().isAfter(now))
                        .filter(t -> !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                        .collect(Collectors.toList());
            }

            if (!futureTrips.isEmpty()) {
                futureTrips.sort((t1, t2) -> {
                    int score1 = computeRelevance(t1, lower);
                    int score2 = computeRelevance(t2, lower);
                    if (score1 != score2) return Integer.compare(score2, score1);
                    if (lower.contains("rẻ")) {
                        BigDecimal p1 = t1.getPrice() != null ? t1.getPrice() : BigDecimal.valueOf(9999999);
                        BigDecimal p2 = t2.getPrice() != null ? t2.getPrice() : BigDecimal.valueOf(9999999);
                        return p1.compareTo(p2);
                    }
                    return t1.getDepartureTime().compareTo(t2.getDepartureTime());
                });

                context.append("\n[DỮ LIỆU CÁC CHUYẾN XE KHÁCH THỰC TẾ ĐANG BÁN VÉ TRÊN HỆ THỐNG VEXE (TẤT CẢ ĐỀU TRONG TƯƠNG LAI)]:\n");
                for (Trip t : futureTrips.stream().limit(15).collect(Collectors.toList())) {
                    if (processedTripIds.add(t.getId())) {
                        context.append(formatTripDetails(t)).append("\n---\n");
                    }
                }
            }
        }

        return context.length() > 0 ? context.toString() : "Dữ liệu tra cứu vector trống.";
    }

    private String formatTripDetails(Trip trip) {
        String departure = trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "N/A";
        String arrival = trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "N/A";
        String vehicleType = trip.getVehicle() != null ? trip.getVehicle().getType() : "Tiêu chuẩn";
        int capacity = trip.getVehicle() != null ? trip.getVehicle().getCapacity() : 0;
        String priceStr = trip.getPrice() != null ? CURRENCY_FORMATTER.format(trip.getPrice()) + " VNĐ" : "Đang cập nhật";
        String timeStr = trip.getDepartureTime() != null ? TIME_FORMATTER.format(trip.getDepartureTime()) : "Chưa có giờ";

        return String.format(
                "[CHUYẾN XE ID: %d]\n" +
                "- Tuyến đường: %s -> %s\n" +
                "- Khởi hành: %s\n" +
                "- Loại xe: %s (%d chỗ/giường)\n" +
                "- Giá vé: %s\n" +
                "- Trạng thái: %s",
                trip.getId(), departure, arrival, timeStr, vehicleType, capacity, priceStr, trip.getStatus()
        );
    }

    private String formatRouteDetails(Route route) {
        String basePriceStr = route.getBasePrice() != null ? CURRENCY_FORMATTER.format(route.getBasePrice()) + " VNĐ" : "N/A";
        return String.format(
                "[TUYẾN ĐƯỜNG ID: %d]\n" +
                "- Điểm đi: %s | Điểm đến: %s\n" +
                "- Khoảng cách: %s km | Thời gian dự kiến: %s phút\n" +
                "- Giá vé cơ bản: %s",
                route.getId(), route.getDepartureLocation(), route.getArrivalLocation(),
                route.getDistanceKm() != null ? route.getDistanceKm().toPlainString() : "N/A",
                route.getDurationMinutes() != null ? route.getDurationMinutes().toString() : "N/A",
                basePriceStr
        );
    }

    private String buildNlgPrompt(String userQuestion, String historyContext, String ragContext, String currentUserEmail) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
        String todayStr = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String tomorrowStr = now.plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        boolean isGuest = currentUserEmail == null || "ANONYMOUS".equalsIgnoreCase(currentUserEmail) || "anonymousUser".equalsIgnoreCase(currentUserEmail);

        return "Bạn là trợ lý ảo AI thông minh, chu đáo và chuyên nghiệp của nền tảng vé xe khách VEXE 🚌.\n" +
                "Thời gian hiện tại của hệ thống: " + nowStr + " (Hôm nay: " + todayStr + ", Ngày mai: " + tomorrowStr + ").\n" +
                "Người dùng đang chat: " + (isGuest ? "Khách vãng lai" : currentUserEmail) + ".\n\n" +
                "QUY TẮC CỐT LÕI BẮT BUỘC TUÂN THỦ 100%:\n" +
                "1. TUYỆT ĐỐI KHÔNG ĐƯỢC GIỚI THIỆU HOẶC LIỆT KÊ BẤT KỲ CHUYẾN XE NÀO CÓ THỜI GIAN KHỞI HÀNH TRONG QUÁ KHỨ (TRƯỚC " + nowStr + "). Bất kỳ chuyến xe nào có ngày khởi hành trước hôm nay (" + todayStr + ") đều là dữ liệu cũ, TUYỆT ĐỐI KHÔNG ĐƯỢC ĐỀ XUẤT CHO KHÁCH!\n" +
                "2. Khi khách hỏi tìm chuyến vào một ngày cụ thể (ví dụ: 'sáng mai', 'ngày mai' = " + tomorrowStr + "):\n" +
                "   - Hãy kiểm tra kỹ dữ liệu xem có chuyến xe nào đúng ngày " + tomorrowStr + " không.\n" +
                "   - Nếu có chuyến xe đúng ngày " + tomorrowStr + ", hãy ưu tiên liệt kê các chuyến đó đầu tiên kèm giờ khởi hành, giá vé, loại xe.\n" +
                "   - Nếu khách hỏi buổi sáng, hãy ưu tiên các chuyến có giờ khởi hành buổi sáng (từ 05:00 đến 11:59).\n" +
                "   - Nếu KHÔNG có chuyến nào vào đúng ngày " + tomorrowStr + ", hãy thông báo rõ ràng, lịch sự và chỉ gợi ý các chuyến TRONG TƯƠNG LAI gần nhất (sau ngày " + todayStr + "). TUYỆT ĐỐI KHÔNG lấy các chuyến xe trong quá khứ!\n" +
                "3. Luôn chào hỏi và xưng hô lịch sự là 'em' và gọi khách là 'anh/chị' hoặc 'quý khách' (tuyệt đối không xưng 'tôi', không gọi khách là 'ANONYMOUS').\n" +
                "4. Định dạng Markdown đẹp mắt với bullet points.\n" +
                "5. Luôn kèm thẻ [[BOOK_TRIP_ID:<id>]] cho các chuyến xe được nhắc đến để giao diện hiển thị nút Đặt vé nhanh cho khách.\n\n" +
                "[LỊCH SỬ HỘI THOẠI]:\n" + (historyContext.isBlank() ? "Chưa có lịch sử." : historyContext) + "\n\n" +
                "[DỮ LIỆU CHUYẾN XE TRA CỨU ĐƯỢC TỪ HỆ THỐNG (TẤT CẢ ĐỀU TRONG TƯƠNG LAI)]:\n" + ragContext + "\n\n" +
                "[CÂU HỎI CỦA KHÁCH HÀNG]:\n" + userQuestion + "\n\n" +
                "Hãy trả lời khách hàng:";
    }

    public String buildDomainFallbackAnswer(String userQuestion, String ragContext) {
        String lower = userQuestion.toLowerCase(Locale.ROOT).trim();
        LocalDateTime now = LocalDateTime.now();

        // 1. Identity & capabilities
        if (isIdentityQuestion(userQuestion)) {
            return "Dạ em là **Trợ lý AI của hệ thống VEXE** 🚌.\n\n" +
                    "Em được lập trình chuyên sâu để hỗ trợ anh/chị:\n" +
                    "- 🔍 **Tra cứu chuyến xe liên tỉnh**: Tìm kiếm lịch chạy, điểm đón/trả và lộ trình xe trên toàn quốc.\n" +
                    "- 💰 **So sánh giá vé & loại xe**: Cung cấp thông tin giá vé niêm yết cho các dòng xe Ghế ngồi, Giường nằm và Limousine VIP.\n" +
                    "- 🎫 **Gợi ý đặt chỗ nhanh**: Hỗ trợ tìm chuyến phù hợp với khung giờ và ngân sách của anh/chị.\n\n" +
                    "Anh/chị đang có kế hoạch đi đâu, hãy chia sẻ điểm đi và điểm đến để em hỗ trợ nhé!";
        }

        // 2. Confusion / Emotional / Short queries
        if (isConfusionOrEmotional(userQuestion)) {
            return "Dạ em xin lỗi nếu câu trả lời vừa rồi chưa rõ ràng hoặc làm anh/chị bối rối ạ! 😅\n\n" +
                    "Em là trợ lý AI chuyên hỗ trợ tra cứu lịch trình và giá vé xe khách của **VEXE**. Anh/chị đang cần tìm chuyến xe đi đâu hoặc có câu hỏi nào về vé xe, lộ trình, giá cả cần em giải đáp không ạ? Hãy chia sẻ với em nhé!";
        }

        // 3. Gratitude
        if (isGratitude(userQuestion)) {
            return "Dạ không có gì ạ! Rất vui được hỗ trợ anh/chị. Chúc anh/chị có những chuyến đi thượng lộ bình an và thoải mái cùng VEXE! Nếu cần tìm thêm chuyến xe nào khác, anh/chị cứ nhắn em nhé! 😊";
        }

        // 4. Farewell
        if (isFarewell(userQuestion)) {
            return "Dạ tạm biệt anh/chị! Chúc anh/chị một ngày thật nhiều niềm vui và hẹn gặp lại trên những hành trình tiếp theo của VEXE! 👋";
        }

        // 5. Booking Guide
        if (isBookingGuide(userQuestion)) {
            return "Để đặt vé nhanh chóng tại **VEXE**, anh/chị chỉ cần thực hiện 3 bước đơn giản:\n\n" +
                    "1. 🔍 **Tìm chuyến**: Nhập điểm đi, điểm đến và ngày khởi hành mong muốn tại thanh tìm kiếm.\n" +
                    "2. 💺 **Chọn chỗ ngồi**: Xem sơ đồ ghế trực quan và chọn vị trí ghế/giường yêu thích.\n" +
                    "3. 💳 **Xác nhận & Thanh toán**: Điền thông tin hành khách và thanh toán tiện lợi để nhận mã vé điện tử ngay tức thì.\n\n" +
                    "Anh/chị đang muốn đặt vé đi đâu, hãy cho em biết điểm đi và điểm đến để em tra cứu giúp mình nhé!";
        }

        // 6. General Greeting
        if (isGreeting(userQuestion)) {
            return "Dạ chào anh/chị! Em là trợ lý VEXE 🚌.\n" +
                    "Em có thể giúp anh/chị tra cứu lịch trình chuyến xe, giá vé hoặc hướng dẫn đặt chỗ nhanh chóng.\n\n" +
                    "Anh/chị đang có kế hoạch đi đâu hôm nay ạ?";
        }

        // 7. Policy / Refund / Cancellation question
        if (lower.contains("chính sách") || lower.contains("hủy vé") || lower.contains("đổi vé") 
                || lower.contains("hoàn tiền") || lower.contains("đổi trả") || lower.contains("trả vé")) {
            return "Dạ về **Chính sách Đổi trả & Hoàn tiền của VEXE**:\n\n" +
                    "- 🛡️ **Cam kết bảo vệ 100%**: Hoàn tiền **150%** nếu nhà xe tự ý hủy chuyến hoặc không có xe.\n" +
                    "- 🎫 **Hủy vé linh hoạt**: Quý khách có thể yêu cầu hủy vé trực tiếp trong trang **Hồ sơ cá nhân > Vé của tôi**.\n" +
                    "- ⏰ **Thời gian**: Vui lòng yêu cầu hủy trước giờ khởi hành tối thiểu 4 tiếng để được xử lý hoàn tiền nhanh nhất qua tài khoản ngân hàng ạ.";
        }

        // 8. General Price question
        if (lower.contains("bảng giá") || lower.contains("giá bao nhiêu") || lower.contains("giá vé chung")) {
            return "Dạ **giá vé xe tại VEXE** được niêm yết rõ ràng, không phụ thu bất ngờ:\n\n" +
                    "- 💺 **Xe ghế ngồi cao cấp**: từ **160.000 VNĐ** / vé\n" +
                    "- 🛏️ **Xe giường nằm tiêu chuẩn**: từ **220.000 - 280.000 VNĐ** / vé\n" +
                    "- 👑 **Limousine VIP phòng riêng**: từ **320.000 - 380.000 VNĐ** / vé (kèm sạc, wifi 5G, ghế massage)\n\n" +
                    "Anh/chị muốn tra cứu giá vé tuyến nào cụ thể để em kiểm tra ngay ạ?";
        }

        // 9. Destination search: Check if question mentions any province/city
        String detectedDestination = null;
        if (lower.contains("đà lạt") || lower.contains("da lat")) detectedDestination = "Đà Lạt";
        else if (lower.contains("phú yên") || lower.contains("phu yen") || lower.contains("tuy hòa") || lower.contains("tuy hoa")) detectedDestination = "Phú Yên";
        else if (lower.contains("nha trang")) detectedDestination = "Nha Trang";
        else if (lower.contains("cần thơ") || lower.contains("can tho")) detectedDestination = "Cần Thơ";
        else if (lower.contains("vũng tàu") || lower.contains("vung tau")) detectedDestination = "Vũng Tàu";
        else if (lower.contains("đà nẵng") || lower.contains("da nang")) detectedDestination = "Đà Nẵng";
        else if (lower.contains("hà nội") || lower.contains("ha noi")) detectedDestination = "Hà Nội";
        else if (lower.contains("hạ long") || lower.contains("ha long")) detectedDestination = "Hạ Long";
        else if (lower.contains("sài gòn") || lower.contains("tp.hcm") || lower.contains("tphcm") || lower.contains("hồ chí minh")) detectedDestination = "TP.HCM";
        else if (lower.contains("sapa") || lower.contains("sa pa")) detectedDestination = "Sapa";
        else if (lower.contains("quy nhơn") || lower.contains("quy nhon")) detectedDestination = "Quy Nhơn";
        else if (lower.contains("phan thiết") || lower.contains("phan thiet")) detectedDestination = "Phan Thiết";
        else if (lower.contains("huế") || lower.contains("hue")) detectedDestination = "Huế";

        List<Trip> trips = Collections.emptyList();
        try {
            if (detectedDestination != null) {
                trips = tripRepository.searchByArrival(detectedDestination, now, PageRequest.of(0, 50));
            } else {
                trips = tripRepository.findUpcomingTrips(now, PageRequest.of(0, 50));
            }
        } catch (Exception ignored) {}
        if (trips == null || trips.isEmpty()) {
            try {
                trips = tripRepository.findAll().stream()
                        .filter(t -> t.getDepartureTime() != null && t.getDepartureTime().isAfter(now))
                        .filter(t -> !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}
        }

        List<Trip> matchingTrips = new ArrayList<>();
        if (detectedDestination != null) {
            final String dest = detectedDestination.toLowerCase(Locale.ROOT);
            for (Trip t : trips) {
                if (t.getRoute() != null && t.getRoute().getArrivalLocation() != null 
                        && t.getRoute().getArrivalLocation().toLowerCase(Locale.ROOT).contains(dest)) {
                    matchingTrips.add(t);
                }
            }

            if (!matchingTrips.isEmpty()) {
                matchingTrips.sort((t1, t2) -> {
                    int s1 = computeRelevance(t1, lower);
                    int s2 = computeRelevance(t2, lower);
                    if (s1 != s2) return Integer.compare(s2, s1);
                    return t1.getDepartureTime().compareTo(t2.getDepartureTime());
                });

                StringBuilder sb = new StringBuilder();
                sb.append("Dạ chào anh/chị! Dưới đây là các chuyến xe đến **").append(detectedDestination)
                  .append("** đang có sẵn trên hệ thống VEXE để anh/chị lựa chọn:\n\n");

                for (Trip t : matchingTrips.stream().limit(3).collect(Collectors.toList())) {
                    String dep = t.getRoute() != null ? t.getRoute().getDepartureLocation() : "N/A";
                    String arr = t.getRoute() != null ? t.getRoute().getArrivalLocation() : "N/A";
                    String vehicleType = t.getVehicle() != null ? t.getVehicle().getType() : "Tiêu chuẩn";
                    int capacity = t.getVehicle() != null ? t.getVehicle().getCapacity() : 34;
                    String priceStr = t.getPrice() != null ? CURRENCY_FORMATTER.format(t.getPrice()) + " VNĐ" : "250.000 VNĐ";
                    String timeStr = t.getDepartureTime() != null ? TIME_FORMATTER.format(t.getDepartureTime()) : "Hôm nay";

                    sb.append("- 🚌 **Chuyến #").append(t.getId()).append("**: ").append(dep).append(" ➔ ").append(arr).append("\n")
                      .append("  • Khởi hành: **").append(timeStr).append("**\n")
                      .append("  • Loại xe: ").append(vehicleType).append(" (").append(capacity).append(" chỗ/giường)\n")
                      .append("  • Giá vé niêm yết: **").append(priceStr).append("**\n")
                      .append("  • [[BOOK_TRIP_ID:").append(t.getId()).append("]]\n\n");
                }
                sb.append("Anh/chị có thể bấm vào nút **Đặt vé ngay** ở từng chuyến hoặc cho em biết ngày giờ khởi hành mong muốn để em tư vấn thêm nhé!");
                return sb.toString();
            } else {
                return "Dạ hiện tại hệ thống VEXE chưa tìm thấy chuyến xe nào đến **" + detectedDestination + "** trong thời gian này. Anh/chị có thể vào trang **[Tra cứu chuyến xe](http://localhost:5173/search)** để chọn ngày khác, hoặc liên hệ Hotline 24/7 **1900 1234** để được tổng đài viên kiểm tra thêm lịch xe tăng cường nhé!";
            }
        }

        // 10. Question about cheapest tickets or upcoming trips
        if (lower.contains("rẻ") || lower.contains("thấp nhất") || lower.contains("tiết kiệm")) {
            if (!trips.isEmpty()) {
                List<Trip> sortedByPrice = new ArrayList<>(trips);
                sortedByPrice.sort(Comparator.comparing((Trip t) -> t.getPrice() != null ? t.getPrice() : BigDecimal.valueOf(9999999)));

                StringBuilder sb = new StringBuilder();
                sb.append("Dạ chào anh/chị! Dưới đây là các chuyến xe có **giá vé rẻ và ưu đãi nhất** hiện có trên hệ thống VEXE:\n\n");
                for (Trip t : sortedByPrice.stream().limit(3).collect(Collectors.toList())) {
                    String dep = t.getRoute() != null ? t.getRoute().getDepartureLocation() : "N/A";
                    String arr = t.getRoute() != null ? t.getRoute().getArrivalLocation() : "N/A";
                    String vehicleType = t.getVehicle() != null ? t.getVehicle().getType() : "Tiêu chuẩn";
                    int capacity = t.getVehicle() != null ? t.getVehicle().getCapacity() : 34;
                    String priceStr = t.getPrice() != null ? CURRENCY_FORMATTER.format(t.getPrice()) + " VNĐ" : "160.000 VNĐ";
                    String timeStr = t.getDepartureTime() != null ? TIME_FORMATTER.format(t.getDepartureTime()) : "Hôm nay";

                    sb.append("- 🚌 **Chuyến #").append(t.getId()).append("**: ").append(dep).append(" ➔ ").append(arr).append("\n")
                      .append("  • Khởi hành: **").append(timeStr).append("**\n")
                      .append("  • Loại xe: ").append(vehicleType).append(" (").append(capacity).append(" chỗ/giường)\n")
                      .append("  • Giá vé niêm yết: **").append(priceStr).append("**\n")
                      .append("  • [[BOOK_TRIP_ID:").append(t.getId()).append("]]\n\n");
                }
                sb.append("Anh/chị có thể bấm vào nút **Đặt vé ngay** ở chuyến phù hợp hoặc cho em biết điểm đi và điểm đến cụ thể để em tìm thêm nhé!");
                return sb.toString();
            }
        }

        // 11. Question about available trips (mai có những chuyến nào, các chuyến xe)
        if (lower.contains("mai") || lower.contains("chuyến nào") || lower.contains("có những chuyến") 
                || lower.contains("nổi bật") || lower.contains("phổ biến") || lower.contains("danh sách chuyến") || lower.contains("gợi ý")) {
            if (!trips.isEmpty()) {
                List<Trip> sortedTrips = new ArrayList<>(trips);
                sortedTrips.sort((t1, t2) -> {
                    int s1 = computeRelevance(t1, lower);
                    int s2 = computeRelevance(t2, lower);
                    if (s1 != s2) return Integer.compare(s2, s1);
                    return t1.getDepartureTime().compareTo(t2.getDepartureTime());
                });

                StringBuilder sb = new StringBuilder();
                sb.append("Dạ chào anh/chị! Dưới đây là các chuyến xe sắp khởi hành đang có sẵn vé để anh/chị tham khảo:\n\n");
                for (Trip t : sortedTrips.stream().limit(3).collect(Collectors.toList())) {
                    String dep = t.getRoute() != null ? t.getRoute().getDepartureLocation() : "N/A";
                    String arr = t.getRoute() != null ? t.getRoute().getArrivalLocation() : "N/A";
                    String vehicleType = t.getVehicle() != null ? t.getVehicle().getType() : "Tiêu chuẩn";
                    int capacity = t.getVehicle() != null ? t.getVehicle().getCapacity() : 34;
                    String priceStr = t.getPrice() != null ? CURRENCY_FORMATTER.format(t.getPrice()) + " VNĐ" : "250.000 VNĐ";
                    String timeStr = t.getDepartureTime() != null ? TIME_FORMATTER.format(t.getDepartureTime()) : "Hôm nay";

                    sb.append("- 🚌 **Chuyến #").append(t.getId()).append("**: ").append(dep).append(" ➔ ").append(arr).append("\n")
                      .append("  • Khởi hành: **").append(timeStr).append("**\n")
                      .append("  • Loại xe: ").append(vehicleType).append(" (").append(capacity).append(" chỗ/giường)\n")
                      .append("  • Giá vé niêm yết: **").append(priceStr).append("**\n")
                      .append("  • [[BOOK_TRIP_ID:").append(t.getId()).append("]]\n\n");
                }
                sb.append("Anh/chị có thể chọn đặt ngay hoặc cho em biết lộ trình mong muốn để em lọc chuyến chuẩn nhất nhé!");
                return sb.toString();
            }
        }

        // 12. Question about trips in general
        if (lower.contains("xe") || lower.contains("chuyến") || lower.contains("vé") || lower.contains("đặt") || lower.contains("đi đâu")) {
            if (!trips.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Dạ anh/chị muốn khởi hành từ đâu và đến tỉnh thành nào ạ? (Ví dụ: *'Tìm xe Sài Gòn đi Đà Lạt tối nay'* hoặc *'Hà Nội đi Sapa sáng mai'*).\n\n")
                  .append("Trong lúc chờ anh/chị chọn lộ trình, em gợi ý nhanh một số chuyến xe tiêu biểu đang sẵn vé:\n\n");
                for (Trip t : trips.stream().limit(3).collect(Collectors.toList())) {
                    String dep = t.getRoute() != null ? t.getRoute().getDepartureLocation() : "N/A";
                    String arr = t.getRoute() != null ? t.getRoute().getArrivalLocation() : "N/A";
                    String vehicleType = t.getVehicle() != null ? t.getVehicle().getType() : "Tiêu chuẩn";
                    int capacity = t.getVehicle() != null ? t.getVehicle().getCapacity() : 34;
                    String priceStr = t.getPrice() != null ? CURRENCY_FORMATTER.format(t.getPrice()) + " VNĐ" : "200.000 VNĐ";
                    String timeStr = t.getDepartureTime() != null ? TIME_FORMATTER.format(t.getDepartureTime()) : "Hôm nay";

                    sb.append("- 🚌 **Chuyến #").append(t.getId()).append("**: ").append(dep).append(" ➔ ").append(arr).append("\n")
                      .append("  • Khởi hành: **").append(timeStr).append("**\n")
                      .append("  • Loại xe: ").append(vehicleType).append(" (").append(capacity).append(" chỗ/giường)\n")
                      .append("  • Giá vé: **").append(priceStr).append("**\n")
                      .append("  • [[BOOK_TRIP_ID:").append(t.getId()).append("]]\n\n");
                }
                sb.append("Anh/chị có thể bấm **Đặt vé ngay** hoặc nhắn cho em điểm đi, điểm đến mong muốn nhé!");
                return sb.toString();
            }
            return "Dạ anh/chị muốn khởi hành từ đâu và đến tỉnh thành nào ạ? (Ví dụ: *'Tìm xe Sài Gòn đi Đà Lạt tối nay'* hoặc *'Hà Nội đi Sapa sáng mai'*) để em tra cứu chuyến xe và giá vé chính xác nhất cho mình nhé!";
        }

        // 13. General Unrecognized Query
        if (!trips.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Dạ em là trợ lý AI chuyên về **tra cứu lộ trình, giá vé và hỗ trợ đặt vé xe khách** của VEXE.\n\n")
              .append("Anh/chị đang cần tìm chuyến xe từ đâu đến tỉnh thành nào ạ? Dưới đây là các chuyến xe phổ biến đang có sẵn vé:\n\n");
            for (Trip t : trips.stream().limit(3).collect(Collectors.toList())) {
                String dep = t.getRoute() != null ? t.getRoute().getDepartureLocation() : "N/A";
                String arr = t.getRoute() != null ? t.getRoute().getArrivalLocation() : "N/A";
                String vehicleType = t.getVehicle() != null ? t.getVehicle().getType() : "Tiêu chuẩn";
                int capacity = t.getVehicle() != null ? t.getVehicle().getCapacity() : 34;
                String priceStr = t.getPrice() != null ? CURRENCY_FORMATTER.format(t.getPrice()) + " VNĐ" : "200.000 VNĐ";
                String timeStr = t.getDepartureTime() != null ? TIME_FORMATTER.format(t.getDepartureTime()) : "Hôm nay";

                sb.append("- 🚌 **Chuyến #").append(t.getId()).append("**: ").append(dep).append(" ➔ ").append(arr).append("\n")
                  .append("  • Khởi hành: **").append(timeStr).append("**\n")
                  .append("  • Loại xe: ").append(vehicleType).append(" (").append(capacity).append(" chỗ/giường)\n")
                  .append("  • Giá vé: **").append(priceStr).append("**\n")
                  .append("  • [[BOOK_TRIP_ID:").append(t.getId()).append("]]\n\n");
            }
            sb.append("Anh/chị hãy cho em biết điểm đi và điểm đến cụ thể để em lọc chuyến chuẩn nhất nhé!");
            return sb.toString();
        }
        return "Dạ em là trợ lý AI chuyên về **tra cứu lộ trình, giá vé và đặt vé xe khách** của VEXE. Anh/chị đang cần tìm chuyến xe từ đâu đến tỉnh thành nào ạ? Hãy chia sẻ với em để em tra cứu giúp mình nhé!";
    }

    private int computeRelevance(Trip t, String lower) {
        if (t.getDepartureTime() == null || !t.getDepartureTime().isAfter(LocalDateTime.now())) {
            return -100000; // NEVER match past trips
        }
        int score = 0;
        if (t.getRoute() != null) {
            String dep = t.getRoute().getDepartureLocation() != null ? t.getRoute().getDepartureLocation().toLowerCase(Locale.ROOT) : "";
            String arr = t.getRoute().getArrivalLocation() != null ? t.getRoute().getArrivalLocation().toLowerCase(Locale.ROOT) : "";
            if (!dep.isEmpty() && lower.contains(dep)) score += 30;
            if (!arr.isEmpty() && lower.contains(arr)) score += 30;
            if ((dep.contains("hcm") || dep.contains("sài gòn")) && (lower.contains("sài gòn") || lower.contains("sai gon") || lower.contains("hcm") || lower.contains("tphcm"))) score += 30;
            if ((arr.contains("phú yên") || arr.contains("tuy hòa")) && (lower.contains("phú yên") || lower.contains("phu yen") || lower.contains("tuy hòa") || lower.contains("tuy hoa"))) score += 30;
            if ((dep.contains("phú yên") || dep.contains("tuy hòa")) && (lower.contains("phú yên") || lower.contains("phu yen") || lower.contains("tuy hòa") || lower.contains("tuy hoa"))) score += 30;
            if ((arr.contains("sapa") || arr.contains("sa pa")) && (lower.contains("sapa") || lower.contains("sa pa"))) score += 40;
            if ((dep.contains("hà nội") || dep.contains("ha noi")) && (lower.contains("hà nội") || lower.contains("ha noi"))) score += 40;
            if ((arr.contains("đà lạt") || arr.contains("da lat")) && (lower.contains("đà lạt") || lower.contains("da lat"))) score += 40;
            if ((arr.contains("đà nẵng") || arr.contains("da nang")) && (lower.contains("đà nẵng") || lower.contains("da nang"))) score += 40;
            if ((arr.contains("nha trang") || arr.contains("nha trang")) && (lower.contains("nha trang") || lower.contains("nha trang"))) score += 40;
        }

        // Target date matching (hôm nay, ngày mai, sáng mai, etc.):
        LocalDate targetDate = extractTargetDate(lower);
        if (targetDate != null) {
            if (t.getDepartureTime().toLocalDate().isEqual(targetDate)) {
                score += 500; // MASSIVE boost for trips on the exact requested date!
            } else {
                long daysDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(targetDate, t.getDepartureTime().toLocalDate()));
                score -= (int) (daysDiff * 25);
            }
        }

        // Target time slot matching (sáng / trưa / chiều / tối):
        String targetTimeSlot = extractTargetTimeSlot(lower);
        if (targetTimeSlot != null) {
            int hour = t.getDepartureTime().getHour();
            if ("MORNING".equals(targetTimeSlot) && hour >= 4 && hour < 12) score += 100;
            else if ("AFTERNOON".equals(targetTimeSlot) && hour >= 12 && hour < 18) score += 100;
            else if ("EVENING".equals(targetTimeSlot) && (hour >= 18 || hour < 4)) score += 100;
        }

        return score;
    }

    private LocalDate extractTargetDate(String lower) {
        LocalDate today = LocalDate.now();
        if (lower.contains("hôm nay") || lower.contains("hom nay")) {
            return today;
        }
        if (lower.contains("ngày mai") || lower.contains("ngay mai") || lower.contains("sáng mai") 
                || lower.contains("sang mai") || lower.contains("tối mai") || lower.contains("toi mai") 
                || lower.contains("chiều mai") || lower.contains("chieu mai") || lower.contains("mai")) {
            return today.plusDays(1);
        }
        if (lower.contains("ngày mốt") || lower.contains("ngay mot") || lower.contains("hôm kia")) {
            return today.plusDays(2);
        }
        return null;
    }

    private String extractTargetTimeSlot(String lower) {
        if (lower.contains("sáng") || lower.contains("sang") || lower.contains("buổi sáng") || lower.contains("sáng sớm")) {
            return "MORNING";
        }
        if (lower.contains("trưa") || lower.contains("trua") || lower.contains("chiều") || lower.contains("chieu") || lower.contains("buổi chiều")) {
            return "AFTERNOON";
        }
        if (lower.contains("tối") || lower.contains("toi") || lower.contains("đêm") || lower.contains("dem") || lower.contains("buổi tối")) {
            return "EVENING";
        }
        return null;
    }

    public Map<String, Object> assistBooking(ChatbotRequest request, String currentUserEmail) {
        String question = request.getQuestion();
        String sessionKey = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : ((request.getConversationId() != null && !request.getConversationId().isBlank())
                    ? request.getConversationId()
                    : "session_" + currentUserEmail);

        boolean hasActiveBooking = bookingAssistantService.map(s -> s.hasActiveBookingFlow(sessionKey)).orElse(false);
        ChatIntent intent = intentClassifier.classify(question, hasActiveBooking);

        if (intent == ChatIntent.CANCEL_TICKET || intent == ChatIntent.VIEW_MY_BOOKINGS) {
            if (bookingAssistantService.isPresent()) {
                bookingAssistantService.get().resetBookingSession(sessionKey);
            }
            if (cancellationAssistantService.isPresent()) {
                return cancellationAssistantService.get().assistCancellation(request, currentUserEmail);
            }
        } else if (intent == ChatIntent.SEARCH_TRIPS) {
            if (bookingAssistantService.isPresent()) {
                bookingAssistantService.get().resetBookingSession(sessionKey);
            }
            return handleSearchTrips(question, request, currentUserEmail);
        } else if (intent == ChatIntent.BOOK_TICKET || intent == ChatIntent.PAYMENT_STATUS) {
            if (bookingAssistantService.isPresent()) {
                return bookingAssistantService.get().assistBooking(request, currentUserEmail);
            }
        }
        return ask(request, currentUserEmail);
    }

    public Map<String, Object> assistCancellation(ChatbotRequest request, String currentUserEmail) {
        String question = request.getQuestion();
        String sessionKey = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : ((request.getConversationId() != null && !request.getConversationId().isBlank())
                    ? request.getConversationId()
                    : "session_" + currentUserEmail);

        boolean hasActiveBooking = bookingAssistantService.map(s -> s.hasActiveBookingFlow(sessionKey)).orElse(false);
        ChatIntent intent = intentClassifier.classify(question, hasActiveBooking);

        if (intent == ChatIntent.BOOK_TICKET || intent == ChatIntent.PAYMENT_STATUS) {
            if (bookingAssistantService.isPresent()) {
                return bookingAssistantService.get().assistBooking(request, currentUserEmail);
            }
        } else if (intent == ChatIntent.SEARCH_TRIPS) {
            return handleSearchTrips(question, request, currentUserEmail);
        } else if (intent == ChatIntent.CANCEL_TICKET || intent == ChatIntent.VIEW_MY_BOOKINGS) {
            if (cancellationAssistantService.isPresent()) {
                return cancellationAssistantService.get().assistCancellation(request, currentUserEmail);
            }
        }
        return ask(request, currentUserEmail);
    }

    public Map<String, Object> handleSearchTrips(String userQuestion, ChatbotRequest request, String currentUserEmail) {
        String lower = userQuestion.toLowerCase(Locale.ROOT);
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(vnZone);
        LocalDateTime now = LocalDateTime.now(vnZone);

        // 1. Determine target date
        LocalDate targetDate = null;
        if (lower.contains("ngày mai") || lower.contains("ngay mai") || lower.contains("mai") || lower.contains("tomorrow")) {
            targetDate = today.plusDays(1);
        } else if (lower.contains("hôm nay") || lower.contains("hom nay") || lower.contains("tối nay") || lower.contains("sáng nay")) {
            targetDate = today;
        } else if (lower.contains("ngày mốt") || lower.contains("ngày kia")) {
            targetDate = today.plusDays(2);
        }

        // 2. Extract origin & destination if explicitly mentioned
        String dep = null;
        String arr = null;

        Matcher m1 = Pattern.compile("(?iu)(?:từ|tu|from)\\s+([a-zA-Z0-9\\s\\p{L}]+?)\\s+(?:đến|den|đi|di|về|ve|to)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+tối|\\s+sáng|\\s+trưa|\\s+chiều|\\s+mai|\\s+hôm|\\s*$)").matcher(userQuestion);
        if (m1.find()) {
            dep = cleanSearchLocation(m1.group(1));
            arr = cleanSearchLocation(m1.group(2));
        } else {
            Matcher m2 = Pattern.compile("(?iu)(?:xe|chuyến|tìm\\s+xe)?\\s*([a-zA-Z0-9\\s\\p{L}]+?)\\s+(?:đến|den|đi|di|về|ve|to)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+tối|\\s+sáng|\\s+trưa|\\s+chiều|\\s+mai|\\s+hôm|\\s*$)").matcher(userQuestion);
            if (m2.find()) {
                String candidateDep = cleanSearchLocation(m2.group(1));
                String candidateArr = cleanSearchLocation(m2.group(2));
                if (isLikelyCity(candidateDep) && isLikelyCity(candidateArr)) {
                    dep = candidateDep;
                    arr = candidateArr;
                } else if (isLikelyCity(candidateArr)) {
                    arr = candidateArr;
                }
            }
        }

        // If specific route info was provided (e.g. "Xe Sài Gòn đi Đà Lạt")
        if (bookingAssistantService.isPresent() && (dep != null || arr != null)) {
            String dateStr = targetDate != null ? targetDate.toString() : null;
            List<TripSearchResultDto> searchDtos = bookingAssistantService.get().getBookingTools().searchAvailableTrips(
                    dep, arr, dateStr, null, null, 1
            );

            if (!searchDtos.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                String routeTitle = (dep != null && arr != null)
                        ? "**" + dep + "** ➔ **" + arr + "**"
                        : (arr != null ? "đến **" + arr + "**" : "từ **" + dep + "**");
                String dateTitle = targetDate != null ? " vào ngày **" + targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "**" : "";

                sb.append(String.format("🚌 **DANH SÁCH CHUYẾN XE %s%s:**\n\n", routeTitle, dateTitle));
                sb.append("Em tìm thấy các chuyến xe phù hợp đang mở bán trên hệ thống VEXE để anh/chị tham khảo:\n\n");

                for (int i = 0; i < Math.min(searchDtos.size(), 5); i++) {
                    TripSearchResultDto t = searchDtos.get(i);
                    sb.append(String.format("- 🚌 **Chuyến #%d**: %s ➔ %s\n", t.getTripId(), t.getDepartureLocation(), t.getArrivalLocation()))
                      .append(String.format("  • Khởi hành: **%s**\n", t.getDepartureTimeFormatted()))
                      .append(String.format("  • Loại xe: %s (%d chỗ trống)\n", t.getVehicleType(), t.getAvailableSeats()))
                      .append(String.format("  • Giá vé: **%s**\n", t.getPriceFormatted()))
                      .append(String.format("  • [[BOOK_TRIP_ID:%d]]\n\n", t.getTripId()));
                }
                sb.append("👉 *Anh/chị có thể bấm 'Chọn chuyến này' bên dưới hoặc nhắn 'Đặt vé chuyến #<mã_chuyến>' để em hỗ trợ giữ chỗ nhé!*");

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("answer", sb.toString());
                res.put("status", "TRIPS_FOUND");
                res.put("cardType", "TRIP_LIST");
                res.put("trips", searchDtos);
                return res;
            } else if (dep != null && arr != null) {
                String dateTitle = targetDate != null ? " vào ngày **" + targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "**" : "";
                return Map.of(
                        "answer", "Dạ hiện tại hệ thống VEXE chưa có chuyến xe nào từ **" + dep + "** đến **" + arr + "**" + dateTitle + " còn chỗ trống mở bán ạ. Anh/chị có thể kiểm tra ngày khác hoặc xem các chuyến lân cận nhé!",
                        "status", "NO_TRIPS_FOUND"
                );
            }
        }

        // Query database trips directly (e.g. for "Các chuyến xe khởi hành vào ngày mai" without route)
        List<Trip> trips = Collections.emptyList();
        if (targetDate != null) {
            LocalDateTime start = targetDate.atStartOfDay();
            LocalDateTime end = targetDate.atTime(LocalTime.MAX);
            try {
                trips = tripRepository.searchByRouteAndDate("", "", start, end, PageRequest.of(0, 30));
                if (trips.isEmpty()) {
                    trips = tripRepository.findUpcomingTrips(start, PageRequest.of(0, 30));
                }
            } catch (Exception ignored) {}
            if (trips == null || trips.isEmpty()) {
                trips = tripRepository.findAll().stream()
                        .filter(t -> t.getDepartureTime() != null
                                && !t.getDepartureTime().isBefore(start)
                                && !t.getDepartureTime().isAfter(end)
                                && !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                        .sorted(Comparator.comparing(Trip::getDepartureTime))
                        .toList();
            }
        } else {
            try {
                trips = tripRepository.findUpcomingTrips(now, PageRequest.of(0, 30));
            } catch (Exception ignored) {}
            if (trips == null || trips.isEmpty()) {
                trips = tripRepository.findAll().stream()
                        .filter(t -> t.getDepartureTime() != null && t.getDepartureTime().isAfter(now)
                                && !"CANCELLED".equalsIgnoreCase(t.getStatus()))
                        .sorted(Comparator.comparing(Trip::getDepartureTime))
                        .toList();
            }
        }

        if (trips.isEmpty()) {
            String dateTitle = targetDate != null ? " vào ngày **" + targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "**" : " sắp tới";
            return Map.of(
                    "answer", "Dạ hiện tại hệ thống chưa tìm thấy chuyến xe nào khởi hành" + dateTitle + " đang mở bán ạ. Anh/chị vui lòng kiểm tra ngày khác hoặc cho em biết điểm đi và điểm đến cụ thể để em lọc chuyến nhé!",
                    "status", "NO_TRIPS_FOUND"
            );
        }

        List<TripSearchResultDto> tripDtos = new ArrayList<>();
        List<Trip> displayTrips = trips.stream().limit(6).toList();

        StringBuilder sb = new StringBuilder();
        String dateHeader = targetDate != null ? " VÀO NGÀY " + targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        sb.append("🚌 **CÁC CHUYẾN XE KHỞI HÀNH").append(dateHeader).append(" TRÊN HỆ THỐNG VEXE:**\n\n");
        sb.append("Dưới đây là một số chuyến xe tiêu biểu đang sẵn vé để anh/chị tham khảo:\n\n");

        for (Trip t : displayTrips) {
            String depLoc = t.getRoute() != null ? t.getRoute().getDepartureLocation() : "N/A";
            String arrLoc = t.getRoute() != null ? t.getRoute().getArrivalLocation() : "N/A";
            String vType = t.getVehicle() != null ? t.getVehicle().getType() : "Tiêu chuẩn";
            int cap = t.getVehicle() != null ? t.getVehicle().getCapacity() : 34;
            String priceStr = t.getPrice() != null ? CURRENCY_FORMATTER.format(t.getPrice()) + " VNĐ" : "250.000 VNĐ";
            String timeStr = t.getDepartureTime() != null ? TIME_FORMATTER.format(t.getDepartureTime()) : "Hôm nay";

            TripSearchResultDto dto = TripSearchResultDto.builder()
                    .tripId(t.getId())
                    .routeId(t.getRoute() != null ? t.getRoute().getId() : null)
                    .departureLocation(depLoc)
                    .arrivalLocation(arrLoc)
                    .departureTime(t.getDepartureTime())
                    .departureTimeFormatted(timeStr)
                    .price(t.getPrice())
                    .priceFormatted(priceStr)
                    .vehicleType(vType)
                    .totalSeats(cap)
                    .availableSeats(cap)
                    .status("ACTIVE")
                    .build();
            tripDtos.add(dto);

            sb.append(String.format("- 🚌 **Chuyến #%d**: %s ➔ %s\n", t.getId(), depLoc, arrLoc))
              .append(String.format("  • Khởi hành: **%s**\n", timeStr))
              .append(String.format("  • Loại xe: %s\n", vType))
              .append(String.format("  • Giá vé: **%s**\n", priceStr))
              .append(String.format("  • [[BOOK_TRIP_ID:%d]]\n\n", t.getId()));
        }
        sb.append("💡 *Để tìm chuyến chuẩn xác nhất, anh/chị có thể cho em biết thêm điểm đi và điểm đến mong muốn (ví dụ: 'Xe Sài Gòn đi Đà Lạt') nhé!*");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", sb.toString());
        res.put("status", "TRIPS_FOUND");
        res.put("cardType", "TRIP_LIST");
        res.put("trips", tripDtos);
        return res;
    }

    private String cleanSearchLocation(String loc) {
        if (loc == null) return null;
        String s = loc.trim();
        s = s.replaceAll("(?iu)^(?:xe|vé|chuyến|đi|đến|từ|về|ở|tại|tìm|tra\\s*cứu|giúp|tôi|cho)\\s+", "");
        s = s.replaceAll("(?iu)\\s+(?:ngày|lúc|vào|tối|sáng|trưa|chiều|mai|hôm).*$", "");
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private boolean isLikelyCity(String s) {
        if (s == null || s.isBlank()) return false;
        String lower = s.toLowerCase(Locale.ROOT);
        return !lower.contains("chuyến") && !lower.contains("ngày") && !lower.contains("mai") && !lower.contains("vé") && !lower.contains("hủy");
    }

    private boolean isCancellationIntent(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();
        return lower.startsWith("hủy giúp") || lower.startsWith("hủy vé") || lower.startsWith("hủy chuyến")
                || lower.contains("hủy vé") || lower.contains("hủy đơn") || lower.contains("trả vé")
                || lower.contains("xác nhận hủy") || lower.contains("giữ lại vé")
                || lower.startsWith("select_booking_to_cancel") || lower.startsWith("confirm_cancellation")
                || lower.startsWith("keep_tickets")
                || lower.contains("muốn hủy");
    }

    private boolean isFindMyBookingsIntent(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ROOT).trim();
        return lower.contains("vé của tôi")
                || lower.contains("vé đã mua")
                || lower.contains("vé đang có")
                || lower.contains("vé hiện có")
                || lower.contains("vé có hiệu lực")
                || lower.contains("kiểm tra vé")
                || lower.contains("xem vé")
                || lower.contains("tra cứu vé")
                || lower.contains("danh sách vé")
                || lower.contains("đơn đặt vé của tôi")
                || lower.contains("lịch sử đặt vé");
    }
}
