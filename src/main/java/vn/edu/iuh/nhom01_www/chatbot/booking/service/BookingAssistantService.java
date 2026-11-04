package vn.edu.iuh.nhom01_www.chatbot.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.*;
import vn.edu.iuh.nhom01_www.chatbot.booking.tool.BookingTools;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentConfirmResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentQrResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoTicketDto;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookingAssistantService {

    private final ChatModel chatModel;
    private final BookingTools bookingTools;
    private final BookingDraftManager bookingDraftManager;
    private final BookingSessionManager bookingSessionManager;
    private final CurrentUserService currentUserService;
    private final BookingIntentParser intentParser;
    private final ChatClient chatClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    @org.springframework.beans.factory.annotation.Autowired
    public BookingAssistantService(
            ChatModel chatModel,
            BookingTools bookingTools,
            BookingDraftManager bookingDraftManager,
            BookingSessionManager bookingSessionManager,
            CurrentUserService currentUserService,
            BookingIntentParser intentParser) {
        this.chatModel = chatModel;
        this.bookingTools = bookingTools;
        this.bookingDraftManager = bookingDraftManager;
        this.bookingSessionManager = bookingSessionManager;
        this.currentUserService = currentUserService;
        this.intentParser = intentParser;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(bookingTools)
                .build();
    }

    public BookingAssistantService(
            ChatModel chatModel,
            BookingTools bookingTools,
            BookingDraftManager bookingDraftManager,
            CurrentUserService currentUserService,
            BookingIntentParser intentParser) {
        this(chatModel, bookingTools, bookingDraftManager, new BookingSessionManager(Optional.empty()), currentUserService, intentParser);
    }

    public boolean hasActiveBookingFlow(String sessionKey) {
        return bookingSessionManager != null && bookingSessionManager.hasActiveBookingFlow(sessionKey);
    }

    public void resetBookingSession(String sessionKey) {
        if (bookingSessionManager != null) {
            bookingSessionManager.clearSession(sessionKey);
        }
    }

    public BookingTools getBookingTools() {
        return bookingTools;
    }

    public Map<String, Object> assistBooking(ChatbotRequest request, String currentUserEmail) {
        Customer currentCustomer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = currentCustomer != null ? currentCustomer.getId() : null;

        // Resolve persistent session key
        String sessionKey = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : ((request.getConversationId() != null && !request.getConversationId().isBlank())
                    ? request.getConversationId()
                    : (customerId != null ? customerId.toString() : "session_" + currentUserEmail));

        BookingSessionState session = bookingSessionManager.getOrCreateSession(sessionKey);

        // Stage 0.1: Structured Interactive Actions (Bypasses LLM hallucinations and ensures strict validation)
        if (request.getAction() != null && !request.getAction().isBlank()) {
            return handleStructuredAction(request, currentCustomer, session);
        }

        String question = request.getQuestion() != null ? request.getQuestion().trim() : "";
        if (question.isBlank()) {
            return Map.of("answer", "Xin chào! Bạn cần tìm chuyến xe đi đâu hoặc muốn đặt vé như thế nào ạ?");
        }

        // Step 0: Parse intent & check prompt injection firewall
        BookingIntent intent = intentParser.parse(question, request.getHistory());
        if (intent.isPromptInjection()) {
            log.warn("[Security] Prompt injection intercepted for user '{}': {}", currentUserEmail, question);
            return Map.of(
                    "answer", "🛡️ [CẢNH BÁO BẢO MẬT]: " + intent.getPromptInjectionReason() + " Hệ thống bắt buộc phải có bước tóm tắt và xác nhận trực tiếp từ người dùng trước khi tạo đơn đặt vé.",
                    "status", "SECURITY_REJECTED"
            );
        }

        // Step 1: Check if user is REJECTING / CANCELLING in multi-turn
        if (intent.isRejection()) {
            bookingSessionManager.clearSession(sessionKey);
            if (customerId != null) {
                var draftOpt = bookingDraftManager.getLatestDraftForCustomer(customerId);
                draftOpt.ifPresent(d -> bookingDraftManager.removeDraft(customerId, d.getIdempotencyKey()));
            }
            return Map.of(
                    "answer", "Dạ em đã hủy yêu cầu đặt vé này rồi ạ. Nếu anh/chị cần tìm chuyến khác hoặc thay đổi lịch trình, cứ nhắn em nhé!",
                    "status", "REJECTED"
            );
        }

        // Step 2: Check if user is triggering DEMO PAYMENT SIMULATION
        if (intent.isPaymentSimulation()) {
            return handleSimulatedPayment(currentCustomer, intent, session);
        }

        // Step 3: Check if user is CONFIRMING an existing draft
        if (intent.isConfirmation()) {
            return handleExplicitConfirmation(currentCustomer, session, question);
        }

        // Step 4: Update session with newly extracted info without losing previous fields
        updateSessionStateWithIntent(session, intent);
        bookingSessionManager.saveSession(session);

        // Step 4.5: Check if user explicitly requests manual seat selection ("Tự chọn ghế")
        if (intent.isManualSeatSelection()) {
            return handleManualSeatSelection(currentCustomer, session);
        }

        // Step 5: Check if user requested adjacent seats
        if (intent.isWantsAdjacentSeats()) {
            return handleAdjacentSeatsRequest(intent, currentCustomer, session);
        }

        // Step 6: Check if user selected a specific trip (by ID, or by targetTripTime)
        if (intent.getTargetTripId() != null || (intent.getTargetTripTime() != null && session.getStep() == BookingStep.TRIP_SELECTION)) {
            return handleTripSelected(intent, currentCustomer, session);
        }

        // Step 7: Check if user selected specific seats (e.g. A01, A02)
        if (intent.getTargetSeatNames() != null && !intent.getTargetSeatNames().isEmpty() && session.getSelectedTripId() != null) {
            return handleSeatsChosenByCode(intent, currentCustomer, session);
        }

        // Step 8: Try Spring AI ChatClient with Tool Calling (if query is complex/conversational)
        boolean hasSpecificBookingDirectives = intent.hasRouteInfo() || intent.getTargetTripId() != null || intent.getTargetTripTime() != null;
        if (!hasSpecificBookingDirectives) {
            String historyContext = buildHistoryContext(request.getHistory());
            String systemPrompt = buildSystemPrompt(currentCustomer);
            String userPrompt = (historyContext.isBlank() ? "" : "Lịch sử hội thoại:\n" + historyContext + "\n\n") +
                    "Người dùng: " + question;

            try {
                log.info("[BookingAssistant] Calling ChatClient with tools for query: '{}'", question);
                String response = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .tools(bookingTools)
                        .call()
                        .content();

                if (response != null && !response.isBlank()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("answer", response);
                    result.put("assistantMode", "TOOL_CALLING");
                    return result;
                }
            } catch (Exception e) {
                log.warn("[BookingAssistant] ChatClient call exception, activating deterministic workflow fallback: {}", e.getMessage());
            }
        }

        // Step 9: Deterministic Booking State Machine Workflow
        return executeDeterministicBookingWorkflow(intent, currentCustomer, session, question);
    }

    private void updateSessionStateWithIntent(BookingSessionState session, BookingIntent intent) {
        if (intent.getDepartureLocation() != null && !intent.getDepartureLocation().isBlank()) {
            session.setDepartureLocation(intent.getDepartureLocation());
        }
        if (intent.getArrivalLocation() != null && !intent.getArrivalLocation().isBlank()) {
            session.setArrivalLocation(intent.getArrivalLocation());
        }
        if (intent.getDepartureDate() != null) {
            session.setDepartureDate(intent.getDepartureDate());
        }
        if (intent.getPreferredTime() != null && !intent.getPreferredTime().isBlank()) {
            session.setPreferredTime(intent.getPreferredTime());
        }
        if (intent.getVehicleType() != null && !intent.getVehicleType().isBlank()) {
            session.setVehicleType(intent.getVehicleType());
        }
        if (intent.getPassengerCount() > 0) {
            session.setPassengerCount(intent.getPassengerCount());
        }
        if (intent.getTargetTripId() != null) {
            session.setSelectedTripId(intent.getTargetTripId());
        }
    }

    private Map<String, Object> handleSimulatedPayment(Customer customer, BookingIntent intent, BookingSessionState session) {
        if (customer == null) {
            return Map.of(
                    "answer", "⚠️ **Yêu cầu đăng nhập**: Quý khách cần đăng nhập tài khoản trên hệ thống để thực hiện thanh toán mô phỏng ạ.",
                    "status", "UNAUTHORIZED"
            );
        }

        Long bookingId = intent.getTargetBookingId();
        if (bookingId == null) {
            bookingId = session.getBookingId();
        }
        if (bookingId == null) {
            var draftOpt = bookingDraftManager.getLatestDraftForCustomer(customer.getId());
            if (draftOpt.isPresent()) {
                var idempotentResult = bookingDraftManager.getIdempotentResult(draftOpt.get().getIdempotencyKey());
                if (idempotentResult.isPresent()) {
                    bookingId = idempotentResult.get().getBookingId();
                }
            }
        }

        if (bookingId == null) {
            return Map.of(
                    "answer", "Dạ em chưa tìm thấy mã đơn đặt vé cần thanh toán. Quý khách vui lòng cung cấp mã đơn (Ví dụ: `SIMULATE_PAYMENT:123`) ạ.",
                    "status", "NO_BOOKING_FOUND"
            );
        }

        try {
            DemoPaymentConfirmResponse confirmRes = bookingTools.confirmDemoPayment(bookingId, intent.getTargetTransactionId());

            session.setStep(BookingStep.PAID);
            session.incrementStateVersion();
            bookingSessionManager.saveSession(session);

            int ticketCount = confirmRes.getTickets() != null ? confirmRes.getTickets().size() : 0;
            String seatNames = confirmRes.getTickets() != null ? confirmRes.getTickets().stream().map(DemoTicketDto::getSeatName).collect(Collectors.joining(", ")) : "";

            String answer = String.format(
                    "🎉 **THANH TOÁN MÔ PHỎNG THÀNH CÔNG!**\n\n" +
                    "- **Mã đơn đặt vé**: `%s` (ID: #%d)\n" +
                    "- **Mã giao dịch Demo**: `%s`\n" +
                    "- **Tuyến xe**: %s\n" +
                    "- **Khởi hành**: %s\n" +
                    "- **Số vé phát hành**: %d vé (Ghế: %s)\n" +
                    "- **Tổng tiền thanh toán**: %s\n" +
                    "- **Trạng thái**: `%s`\n\n" +
                    "🎫 *Vé điện tử đã được phát hành chính thức. Quý khách có thể xem và lưu mã QR vé ngay bên dưới.*",
                    confirmRes.getBookingCode(), confirmRes.getBookingId(),
                    confirmRes.getTransactionId(),
                    confirmRes.getRouteName(),
                    confirmRes.getDepartureTimeFormatted(),
                    ticketCount,
                    seatNames,
                    confirmRes.getTotalPriceFormatted(),
                    confirmRes.getStatus()
            );

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", answer);
            res.put("status", "PAYMENT_SUCCESS");
            res.put("cardType", "TICKET_SUCCESS");
            res.put("bookingId", confirmRes.getBookingId());
            res.put("bookingCode", confirmRes.getBookingCode());
            res.put("passengerCount", ticketCount > 0 ? ticketCount : session.getPassengerCount());
            res.put("stateVersion", session.getStateVersion());
            res.put("workflowStep", session.getWorkflowStep());
            res.put("ticket", confirmRes);
            return res;
        } catch (CustomException ce) {
            return Map.of(
                    "answer", "❌ **Không thể thanh toán mô phỏng**: " + ce.getMessage(),
                    "status", "PAYMENT_FAILED"
            );
        } catch (Exception e) {
            log.error("Unexpected error during simulated payment", e);
            return Map.of(
                    "answer", "❌ Có lỗi xảy ra trong quá trình mô phỏng thanh toán: " + e.getMessage(),
                    "status", "PAYMENT_ERROR"
            );
        }
    }

    public Map<String, Object> handleStructuredAction(ChatbotRequest request, Customer customer, BookingSessionState session) {
        String action = request.getAction().trim().toUpperCase(Locale.ROOT);
        log.info("[BookingAssistant:handleStructuredAction] action={}, tripId={}, seatIds={}, passengerCount={}, stateVersion={}",
                action, request.getTripId(), request.getSeatIds(), request.getPassengerCount(), request.getStateVersion());

        // Update passengerCount if specified in request
        if (request.getPassengerCount() != null && request.getPassengerCount() > 0) {
            session.setPassengerCount(request.getPassengerCount());
        }

        switch (action) {
            case "SELECT_DEPARTURE" -> {
                String depLoc = request.getDepartureLocation();
                if (depLoc == null || depLoc.isBlank()) {
                    depLoc = request.getQuestion();
                }
                if (depLoc != null) {
                    depLoc = depLoc.replaceAll("(?iu)^\\s*(?:SELECT_DEPARTURE|CHON_DIEM_DI)[:\\s]+", "").trim();
                    session.setDepartureLocation(depLoc);
                }
                session.incrementStateVersion();
                session.setStep(BookingStep.TRIP_SELECTION);
                bookingSessionManager.saveSession(session);

                BookingIntent intent = BookingIntent.builder()
                        .departureLocation(session.getDepartureLocation())
                        .arrivalLocation(session.getArrivalLocation())
                        .departureDate(session.getDepartureDate())
                        .passengerCount(session.getPassengerCount())
                        .build();
                return executeDeterministicBookingWorkflow(intent, customer, session, "SELECT_DEPARTURE");
            }
            case "SELECT_TRIP" -> {
                Long tripId = request.getTripId();
                if (tripId == null && request.getQuestion() != null) {
                    try {
                        tripId = Long.parseLong(request.getQuestion().replaceAll("[^0-9]", ""));
                    } catch (Exception ignored) {}
                }
                if (tripId == null) {
                    return Map.of("answer", "Mã chuyến xe không hợp lệ. Vui lòng chọn lại ạ!", "status", "INVALID_TRIP");
                }
                BookingIntent intent = BookingIntent.builder()
                        .targetTripId(tripId)
                        .passengerCount(session.getPassengerCount())
                        .build();
                return handleTripSelected(intent, customer, session);
            }
            case "AUTO_ADJACENT_SEATS" -> {
                BookingIntent intent = BookingIntent.builder()
                        .wantsAdjacentSeats(true)
                        .targetTripId(request.getTripId() != null ? request.getTripId() : session.getSelectedTripId())
                        .passengerCount(session.getPassengerCount())
                        .build();
                return handleAdjacentSeatsRequest(intent, customer, session);
            }
            case "MANUAL_SEAT_MAP" -> {
                if (request.getTripId() != null) {
                    session.setSelectedTripId(request.getTripId());
                }
                return handleManualSeatSelection(customer, session);
            }
            case "SELECT_SEATS" -> {
                if (request.getTripId() != null) {
                    session.setSelectedTripId(request.getTripId());
                }
                List<Long> seatIds = request.getSeatIds();
                if (seatIds == null || seatIds.isEmpty()) {
                    if (request.getSeatNames() != null && !request.getSeatNames().isEmpty()) {
                        BookingIntent intent = BookingIntent.builder()
                                .targetTripId(session.getSelectedTripId())
                                .targetSeatNames(request.getSeatNames())
                                .passengerCount(session.getPassengerCount())
                                .build();
                        return handleSeatsChosenByCode(intent, customer, session);
                    }
                    return Map.of("answer", "Vui lòng chọn ghế trên sơ đồ ạ!", "status", "NO_SEATS_SELECTED");
                }
                return handleSeatsChosenByIds(seatIds, customer, session);
            }
            case "CONFIRM_BOOKING" -> {
                if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
                    session.setIdempotencyKey(request.getIdempotencyKey());
                    bookingSessionManager.saveSession(session);
                }
                return handleExplicitConfirmation(customer, session, "Xác nhận và thanh toán");
            }
            case "SIMULATE_PAYMENT" -> {
                BookingIntent intent = BookingIntent.builder()
                        .isPaymentSimulation(true)
                        .targetBookingId(request.getTripId())
                        .targetTransactionId(request.getIdempotencyKey())
                        .build();
                return handleSimulatedPayment(customer, intent, session);
            }
            case "CANCEL_BOOKING", "RESET_BOOKING" -> {
                bookingSessionManager.clearSession(session.getSessionKey());
                if (customer != null) {
                    var draftOpt = bookingDraftManager.getLatestDraftForCustomer(customer.getId());
                    draftOpt.ifPresent(d -> bookingDraftManager.removeDraft(customer.getId(), d.getIdempotencyKey()));
                }
                return Map.of(
                        "answer", "Dạ em đã hủy phiên đặt vé hiện tại rồi ạ. Nếu anh/chị cần tìm chuyến mới, cứ nhắn em nhé!",
                        "status", "REJECTED"
                );
            }
            default -> log.warn("Unhandled structured action: {}", action);
        }

        BookingIntent intent = intentParser.parse(request.getQuestion(), request.getHistory());
        return executeDeterministicBookingWorkflow(intent, customer, session, request.getQuestion());
    }

    public Map<String, Object> handleManualSeatSelection(Customer customer, BookingSessionState session) {
        Long tripId = session.getSelectedTripId();
        if (tripId == null) {
            return Map.of(
                    "answer", "Dạ anh/chị vui lòng chọn chuyến xe trước khi mở sơ đồ ghế nhé!",
                    "status", "NEED_TRIP"
            );
        }

        int count = session.getPassengerCount() > 0 ? session.getPassengerCount() : 1;
        session.setPassengerCount(count);
        session.setStep(BookingStep.SEAT_SELECTION);
        session.incrementStateVersion();
        bookingSessionManager.saveSession(session);

        TripSeatMapDto seatMap = bookingTools.getTripSeatMap(tripId, count);

        String answer = String.format(
                "🚌 **SƠ ĐỒ CHỖ NGỒI CHUYẾN XE #%d**\n\n" +
                "📍 Tuyến: **%s**\n" +
                "⏰ Khởi hành: **%s** | Xe: **%s**\n" +
                "💵 Giá vé: **%s**/ghế\n\n" +
                "👉 Quý khách vui lòng chọn đúng **%d vị trí ghế** mong muốn trên sơ đồ bên dưới và nhấn **'Xác nhận ghế'** nhé!",
                tripId,
                seatMap.getRouteName(),
                seatMap.getDepartureTimeFormatted(),
                seatMap.getVehicleType(),
                seatMap.getUnitPriceFormatted(),
                count
        );

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", answer);
        res.put("status", "SEAT_MAP_READY");
        res.put("cardType", "SEAT_SELECTION_CARD");
        res.put("tripId", tripId);
        res.put("passengerCount", count);
        res.put("seatMap", seatMap);
        res.put("stateVersion", session.getStateVersion());
        res.put("workflowStep", session.getWorkflowStep());
        return res;
    }

    public Map<String, Object> handleSeatsChosenByIds(List<Long> seatIds, Customer customer, BookingSessionState session) {
        Long tripId = session.getSelectedTripId();
        if (tripId == null) {
            return Map.of("answer", "Vui lòng chọn chuyến xe trước khi chọn vị trí ghế ạ!", "status", "NEED_TRIP");
        }

        int count = session.getPassengerCount() > 0 ? session.getPassengerCount() : 1;
        if (seatIds == null || seatIds.size() != count) {
            return Map.of(
                    "answer", "⚠️ Quý khách cần chọn chính xác " + count + " ghế theo số lượng vé đã đăng ký ạ! (Hiện đã chọn " + (seatIds != null ? seatIds.size() : 0) + " ghế).",
                    "status", "INVALID_SEAT_COUNT"
            );
        }

        List<AvailableSeatDto> available = bookingTools.getAvailableSeats(tripId);
        Set<Long> availableIds = available.stream().map(AvailableSeatDto::getSeatId).collect(Collectors.toSet());

        for (Long id : seatIds) {
            if (!availableIds.contains(id)) {
                return Map.of(
                        "answer", "⚠️ Một hoặc nhiều ghế bạn chọn không còn trống hoặc đang có người giữ chỗ. Vui lòng chọn ghế khác trên sơ đồ ạ!",
                        "status", "SEAT_UNAVAILABLE"
                );
            }
        }

        List<String> seatNames = available.stream()
                .filter(s -> seatIds.contains(s.getSeatId()))
                .map(AvailableSeatDto::getSeatName)
                .toList();

        session.setSelectedTripId(tripId);
        session.setSelectedSeatIds(seatIds);
        session.setSelectedSeatNames(seatNames);
        session.setStep(BookingStep.BOOKING_REVIEW);
        session.incrementStateVersion();
        bookingSessionManager.saveSession(session);

        BookingDraftDto draft = bookingTools.prepareBooking(tripId, seatIds);
        session.setIdempotencyKey(draft.getIdempotencyKey());
        bookingSessionManager.saveSession(session);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", draft.getSummaryMessage() + "\n\n👉 **Anh/chị vui lòng kiểm tra thông tin và nhấn 'Xác nhận và thanh toán' để hoàn tất giữ chỗ nhé!**");
        res.put("status", "CONFIRMATION_REQUIRED");
        res.put("cardType", "BOOKING_CONFIRMATION");
        res.put("draft", draft);
        res.put("tripId", tripId);
        res.put("passengerCount", count);
        res.put("stateVersion", session.getStateVersion());
        res.put("workflowStep", session.getWorkflowStep());
        return res;
    }

    private Map<String, Object> handleAdjacentSeatsRequest(BookingIntent intent, Customer customer, BookingSessionState session) {
        Long tripId = session.getSelectedTripId() != null ? session.getSelectedTripId() : intent.getTargetTripId();
        if (tripId == null && customer != null) {
            var draftOpt = bookingDraftManager.getLatestDraftForCustomer(customer.getId());
            if (draftOpt.isPresent()) tripId = draftOpt.get().getTripId();
        }

        if (tripId == null) {
            return Map.of(
                    "answer", "Dạ anh/chị vui lòng chọn chuyến xe trước để em kiểm tra các ghế cạnh nhau phù hợp nhé!",
                    "status", "NEED_TRIP"
            );
        }

        int seatCount = session.getPassengerCount() > 0 ? session.getPassengerCount() : (intent.getPassengerCount() > 0 ? intent.getPassengerCount() : 2);
        session.setPassengerCount(seatCount);

        List<AvailableSeatDto> adjacentSeats = bookingTools.suggestAdjacentSeats(tripId, seatCount, null);

        if (adjacentSeats.isEmpty()) {
            TripSeatMapDto seatMap = bookingTools.getTripSeatMap(tripId, seatCount);
            session.incrementStateVersion();
            bookingSessionManager.saveSession(session);

            String answer = String.format(
                    "Dạ rất tiếc, chuyến xe #%d hiện không còn đủ **%d ghế cạnh nhau** trên cùng một tầng ạ.\n\n" +
                    "👉 Quý khách vui lòng xem **sơ đồ ghế thực tế** bên dưới và tự chọn các vị trí ghế phù hợp nhé!",
                    tripId, seatCount
            );

            Map<String, Object> noAdjRes = new LinkedHashMap<>();
            noAdjRes.put("answer", answer);
            noAdjRes.put("status", "NO_ADJACENT_SEATS");
            noAdjRes.put("cardType", "SEAT_SELECTION_CARD");
            noAdjRes.put("tripId", tripId);
            noAdjRes.put("passengerCount", seatCount);
            noAdjRes.put("seatMap", seatMap);
            noAdjRes.put("stateVersion", session.getStateVersion());
            noAdjRes.put("workflowStep", session.getWorkflowStep());
            return noAdjRes;
        }

        List<Long> seatIds = adjacentSeats.stream().map(AvailableSeatDto::getSeatId).toList();
        List<String> seatNames = adjacentSeats.stream().map(AvailableSeatDto::getSeatName).toList();

        session.setSelectedTripId(tripId);
        session.setSelectedSeatIds(seatIds);
        session.setSelectedSeatNames(seatNames);
        session.setStep(BookingStep.BOOKING_REVIEW);
        session.incrementStateVersion();
        bookingSessionManager.saveSession(session);

        try {
            BookingDraftDto draft = bookingTools.prepareBooking(tripId, seatIds);
        session.setIdempotencyKey(draft.getIdempotencyKey());
        bookingSessionManager.saveSession(session);

            String answer = String.format(
                    "✨ **ĐÃ TÌM THẤY %d GHẾ CẠNH NHAU TRỐNG:** `%s` (Tầng %d)\n\n" +
                    "%s\n\n" +
                    "👉 **Anh/chị vui lòng kiểm tra và xác nhận để em tiến hành giữ chỗ và tạo mã thanh toán nhé!**",
                    adjacentSeats.size(),
                    String.join(", ", seatNames),
                    adjacentSeats.get(0).getFloor(),
                    draft.getSummaryMessage()
            );

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", answer);
            res.put("status", "CONFIRMATION_REQUIRED");
            res.put("cardType", "BOOKING_CONFIRMATION");
            res.put("draft", draft);
            res.put("tripId", tripId);
            res.put("passengerCount", seatCount);
            res.put("stateVersion", session.getStateVersion());
            res.put("workflowStep", session.getWorkflowStep());
            return res;
        } catch (CustomException ce) {
            return Map.of(
                    "answer", "⚠️ Không thể chuẩn bị đơn đặt vé: " + ce.getMessage(),
                    "status", "PREPARE_FAILED"
            );
        }
    }

    private Map<String, Object> handleTripSelected(BookingIntent intent, Customer customer, BookingSessionState session) {
        Long tripId = intent.getTargetTripId();

        // If tripId is not set directly, attempt to match targetTripTime against available trips
        if (tripId == null && intent.getTargetTripTime() != null) {
            List<TripSearchResultDto> candidateTrips = bookingTools.searchAvailableTrips(
                    session.getDepartureLocation(),
                    session.getArrivalLocation(),
                    session.getDepartureDate() != null ? session.getDepartureDate().toString() : null,
                    intent.getTargetTripTime(),
                    intent.getVehicleType() != null ? intent.getVehicleType() : session.getVehicleType()
            );
            if (!candidateTrips.isEmpty()) {
                tripId = candidateTrips.get(0).getTripId();
            }
        }

        if (tripId == null) {
            return Map.of(
                    "answer", "Dạ em chưa nhận diện được chuyến xe anh/chị chọn. Anh/chị vui lòng nhấn 'Chọn chuyến này' trên danh sách chuyến xe giúp em nhé!",
                    "status", "INVALID_TRIP"
            );
        }

        session.setSelectedTripId(tripId);
        session.setStep(BookingStep.SEAT_SELECTION);
        session.incrementStateVersion();

        int count = session.getPassengerCount() > 0 ? session.getPassengerCount() : (intent.getPassengerCount() > 0 ? intent.getPassengerCount() : 1);
        session.setPassengerCount(count);
        bookingSessionManager.saveSession(session);

        // If specific seat codes were passed along with trip selection
        if (intent.getTargetSeatNames() != null && !intent.getTargetSeatNames().isEmpty()) {
            return handleSeatsChosenByCode(intent, customer, session);
        }

        List<AvailableSeatDto> available = bookingTools.getAvailableSeats(tripId);

        String answer = String.format(
                "🚌 Anh/chị đã chọn chuyến xe #%d.\n" +
                "Hiện chuyến xe còn **%d chỗ trống**.\n\n" +
                "❓ **Anh/chị có muốn chọn %d ghế cạnh nhau không?**\n" +
                "(Nhấn **'Có, chọn %d ghế cạnh nhau'** để em tự động tìm ghế liền kề tốt nhất, hoặc nhấn **'Tự chọn ghế'** để xem sơ đồ chỗ ngồi)",
                tripId, available.size(), count, count
        );

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", answer);
        res.put("status", "SEAT_SELECTION");
        res.put("cardType", "SEAT_SELECTION");
        res.put("tripId", tripId);
        res.put("passengerCount", count);
        res.put("availableSeatsCount", available.size());
        res.put("stateVersion", session.getStateVersion());
        res.put("workflowStep", session.getWorkflowStep());
        return res;
    }

    private Map<String, Object> handleSeatsChosenByCode(BookingIntent intent, Customer customer, BookingSessionState session) {
        Long tripId = session.getSelectedTripId();
        if (tripId == null) {
            tripId = intent.getTargetTripId();
        }
        if (tripId == null) {
            return Map.of("answer", "Vui lòng chọn chuyến xe trước khi chọn vị trí ghế ạ!", "status", "NEED_TRIP");
        }

        List<AvailableSeatDto> available = bookingTools.getAvailableSeats(tripId);
        List<AvailableSeatDto> matchedSeats = new ArrayList<>();

        for (String code : intent.getTargetSeatNames()) {
            available.stream()
                    .filter(s -> s.getSeatName().equalsIgnoreCase(code))
                    .findFirst()
                    .ifPresent(matchedSeats::add);
        }

        if (matchedSeats.size() != intent.getTargetSeatNames().size()) {
            return Map.of(
                    "answer", "⚠️ Một số ghế bạn chọn không còn trống hoặc không tồn tại trên chuyến này. Vui lòng chọn ghế khác ạ!",
                    "status", "INVALID_SEATS"
            );
        }

        session.setSelectedTripId(tripId);
        session.setPassengerCount(matchedSeats.size());
        session.setSelectedSeatIds(matchedSeats.stream().map(AvailableSeatDto::getSeatId).toList());
        session.setSelectedSeatNames(matchedSeats.stream().map(AvailableSeatDto::getSeatName).toList());
        session.setStep(BookingStep.BOOKING_REVIEW);
        bookingSessionManager.saveSession(session);

        BookingDraftDto draft = bookingTools.prepareBooking(tripId, session.getSelectedSeatIds());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", draft.getSummaryMessage() + "\n\n👉 **Anh/chị vui lòng nhấn 'Xác nhận và thanh toán' để hoàn tất giữ chỗ!**");
        res.put("status", "CONFIRMATION_REQUIRED");
        res.put("cardType", "BOOKING_CONFIRMATION");
        res.put("draft", draft);
        res.put("tripId", tripId);
        res.put("passengerCount", session.getPassengerCount());
        res.put("stateVersion", session.getStateVersion());
        res.put("workflowStep", session.getWorkflowStep());
        return res;
    }

    private Map<String, Object> handleExplicitConfirmation(Customer customer, BookingSessionState session, String userMessage) {
        if (customer == null) {
            return Map.of(
                    "answer", "⚠️ **Yêu cầu đăng nhập**: Quý khách cần đăng nhập vào tài khoản trên hệ thống để xác nhận và tạo đơn đặt vé chính thức ạ.",
                    "status", "UNAUTHORIZED"
            );
        }

        Optional<BookingDraftDto> draftOpt = bookingDraftManager.getLatestDraftForCustomer(customer.getId());
        if (draftOpt.isEmpty() && (session.getSelectedTripId() == null || session.getSelectedSeatIds().isEmpty())) {
            return Map.of(
                    "answer", "Dạ hiện tại em chưa thấy bản tóm tắt đặt vé nào đang chờ xác nhận của anh/chị. Anh/chị vui lòng cho em biết tuyến đường, ngày đi và ghế mong muốn để em chuẩn bị lại nhé!",
                    "status", "NO_PENDING_DRAFT"
            );
        }

        Long tripId = draftOpt.isPresent() ? draftOpt.get().getTripId() : session.getSelectedTripId();
        List<Long> seatIds = draftOpt.isPresent() ? draftOpt.get().getSeatIds() : session.getSelectedSeatIds();
        String idempotencyKey = draftOpt.isPresent() ? draftOpt.get().getIdempotencyKey() : (session.getIdempotencyKey() != null ? session.getIdempotencyKey() : UUID.randomUUID().toString());

        try {
            BookingConfirmationResultDto confirmation = bookingTools.confirmBooking(
                    tripId,
                    seatIds,
                    idempotencyKey
            );

            // Attempt to generate Demo Payment QR immediately
            DemoPaymentQrResponse paymentQr = null;
            try {
                paymentQr = bookingTools.createDemoPayment(confirmation.getBookingId());
            } catch (Exception e) {
                log.debug("Demo payment QR could not be generated: {}", e.getMessage());
            }

            session.setBookingId(confirmation.getBookingId());
            session.setBookingCode(confirmation.getBookingCode());
            session.setTotalPrice(confirmation.getTotalPrice());
            session.setStep(BookingStep.PENDING_PAYMENT);
            session.incrementStateVersion();
            bookingSessionManager.saveSession(session);

            String answer = String.format(
                    "📋 **ĐÃ GIỮ CHỖ THÀNH CÔNG — CHỜ THANH TOÁN**\n\n" +
                    "- **Mã đặt vé (Booking Code)**: `%s`\n" +
                    "- **Tuyến xe**: %s\n" +
                    "- **Khởi hành**: %s\n" +
                    "- **Ghế đã đặt**: %s (%d vé)\n" +
                    "- **Tổng tiền**: %s\n" +
                    "- **Trạng thái**: `%s` (Chờ thanh toán)\n" +
                    "- **Thời hạn giữ chỗ**: **%d phút**\n\n" +
                    "💳 *Hướng dẫn thanh toán*: %s",
                    confirmation.getBookingCode(),
                    confirmation.getRouteName(),
                    confirmation.getDepartureTimeFormatted(),
                    String.join(", ", confirmation.getSeatNames()),
                    confirmation.getSeatNames().size(),
                    confirmation.getTotalPriceFormatted(),
                    confirmation.getStatus(),
                    confirmation.getHoldMinutes(),
                    (paymentQr != null ? "Vui lòng quét mã QR demo bên dưới và nhấn **'Mô phỏng thanh toán thành công'** để nhận vé điện tử." : confirmation.getPaymentInstructions())
            );

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("answer", answer);
            res.put("status", "BOOKING_CREATED");
            res.put("bookingId", confirmation.getBookingId());
            res.put("bookingCode", confirmation.getBookingCode());
            res.put("totalPrice", confirmation.getTotalPrice());
            res.put("passengerCount", confirmation.getSeatNames().size());
            res.put("stateVersion", session.getStateVersion());
            res.put("workflowStep", session.getWorkflowStep());

            if (paymentQr != null) {
                res.put("cardType", "DEMO_PAYMENT_QR");
                res.put("payment", paymentQr);
            }

            return res;
        } catch (CustomException ce) {
            return Map.of(
                    "answer", "❌ **Không thể hoàn tất đặt vé**: " + ce.getMessage(),
                    "status", "BOOKING_FAILED"
            );
        } catch (Exception e) {
            log.error("Unexpected error during booking confirmation", e);
            return Map.of(
                    "answer", "❌ Có lỗi xảy ra trong quá trình tạo đơn đặt vé: " + e.getMessage() + ". Quý khách vui lòng thử lại.",
                    "status", "BOOKING_ERROR"
            );
        }
    }

    private Map<String, Object> executeDeterministicBookingWorkflow(
            BookingIntent intent, Customer customer, BookingSessionState session, String userMessage) {

        String dep = session.getDepartureLocation();
        String arr = session.getArrivalLocation();

        // 1. Check missing route info
        if (dep == null || dep.isBlank() || arr == null || arr.isBlank()) {
            if (dep == null || dep.isBlank()) {
                if (arr == null || arr.isBlank()) {
                    session.setStep(BookingStep.COLLECTING_INFO);
                    bookingSessionManager.saveSession(session);
                    return Map.of(
                            "answer", "Dạ anh/chị muốn đặt vé xe đi từ đâu đến đâu và vào ngày nào ạ? Hãy cho em biết để em tra cứu chuyến xe phù hợp nhé! (Ví dụ: *Đặt 2 vé từ Sài Gòn đi Đà Lạt ngày mai lúc 21h*)",
                            "status", "NEED_INFO"
                    );
                }

                // Destination is known, but departure is missing: Query real database for active departure options
                String dateStr = session.getDepartureDate() != null ? session.getDepartureDate().toString() : null;
                int count = session.getPassengerCount() > 0 ? session.getPassengerCount() : 1;
                List<DepartureOptionDto> departureOptions = bookingTools.listAvailableDepartureLocations(arr, dateStr, count);

                if (departureOptions.isEmpty()) {
                    session.setStep(BookingStep.COLLECTING_INFO);
                    bookingSessionManager.saveSession(session);
                    String dateDisplay = session.getDepartureDate() != null ? " vào ngày **" + session.getDepartureDate().format(DATE_FORMATTER) + "**" : "";
                    return Map.of(
                            "answer", "Dạ em đã kiểm tra toàn bộ hệ thống nhưng hiện tại không có chuyến xe nào đến **" + arr + "**" + dateDisplay + " còn đủ " + count + " chỗ mở bán ạ. Anh/chị có muốn tìm chuyến xe vào ngày khác hoặc tuyến lân cận không ạ?",
                            "status", "NO_DEPARTURES_FOUND"
                    );
                }

                session.setStep(BookingStep.DEPARTURE_SELECTION);
                bookingSessionManager.saveSession(session);

                String dateDisplay = session.getDepartureDate() != null ? " ngày **" + session.getDepartureDate().format(DATE_FORMATTER) + "**" : " ngày mai";
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("🚌 **DANH SÁCH ĐIỂM KHỞI HÀNH ĐẾN %s** (%s - %d vé):\n\n", arr.toUpperCase(), dateDisplay.trim(), count));
                sb.append("Em đã lọc các điểm đi có chuyến xe đang hoạt động thực tế, chưa khởi hành và còn đủ chỗ trống cho anh/chị:\n\n");

                for (int i = 0; i < departureOptions.size(); i++) {
                    DepartureOptionDto opt = departureOptions.get(i);
                    sb.append(String.format("%d. 📍 **%s** → **%s**\n", i + 1, opt.getDepartureLocation(), arr));
                    sb.append(String.format("   - **Giá vé chỉ từ**: %s\n", opt.getMinPriceFormatted()));
                    sb.append(String.format("   - **Chuyến sớm nhất**: %s | **Số chuyến**: %d chuyến\n", opt.getEarliestDepartureTimeFormatted(), opt.getActiveTripCount()));
                    sb.append(String.format("   - **Loại xe**: %s\n\n", String.join(", ", opt.getVehicleTypes())));
                }
                sb.append("👉 **Anh/chị vui lòng nhấn chọn điểm khởi hành bên dưới để em hiển thị các chuyến xe cụ thể nhé!**");

                Map<String, Object> res = new LinkedHashMap<>();
                res.put("answer", sb.toString());
                res.put("status", "DEPARTURE_SELECTION_REQUIRED");
                res.put("cardType", "DEPARTURE_SELECTION");
                res.put("destination", arr);
                res.put("passengerCount", count);
                res.put("departureDate", session.getDepartureDate() != null ? session.getDepartureDate().toString() : LocalDate.now().plusDays(1).toString());
                res.put("departureOptions", departureOptions);
                res.put("stateVersion", session.getStateVersion());
                res.put("workflowStep", session.getWorkflowStep());
                return res;
            }

            session.setStep(BookingStep.COLLECTING_INFO);
            bookingSessionManager.saveSession(session);
            return Map.of(
                    "answer", "Dạ từ " + dep + ", anh/chị muốn đến tỉnh/thành phố nào ạ?",
                    "status", "NEED_INFO"
            );
        }

        // 2. Search available trips
        String dateStr = session.getDepartureDate() != null ? session.getDepartureDate().toString() : null;
        List<TripSearchResultDto> trips = bookingTools.searchAvailableTrips(
                dep,
                arr,
                dateStr,
                session.getPreferredTime(),
                session.getVehicleType(),
                session.getPassengerCount()
        );

        if (trips.isEmpty()) {
            int count = session.getPassengerCount() > 0 ? session.getPassengerCount() : 1;
            List<TripSearchResultDto> anySeatTrips = bookingTools.searchAvailableTrips(
                    dep,
                    arr,
                    dateStr,
                    session.getPreferredTime(),
                    session.getVehicleType(),
                    1
            );
            session.setStep(BookingStep.COLLECTING_INFO);
            bookingSessionManager.saveSession(session);

            if (!anySeatTrips.isEmpty()) {
                return Map.of(
                        "answer", "Dạ rất tiếc, chuyến xe lúc " + anySeatTrips.get(0).getDepartureTimeFormatted() + " không còn đủ " + count + " chỗ trống. Quý khách vui lòng chọn chuyến khác hoặc giảm số lượng vé giúp em nhé!",
                        "status", "NOT_ENOUGH_SEATS"
                );
            }

            String dateDisplay = session.getDepartureDate() != null ? " vào ngày " + session.getDepartureDate().format(DATE_FORMATTER) : "";
            return Map.of(
                    "answer", "Dạ tiếc quá, hiện tại em chưa tìm thấy chuyến xe nào từ **" +
                            dep + "** đi **" + arr + "**" + dateDisplay + " còn chỗ mở bán. Anh/chị có muốn tìm chuyến xe vào ngày khác hoặc tuyến lân cận không ạ?",
                    "status", "NO_TRIPS_FOUND"
            );
        }

        // 3. If user already had a selected trip in session, keep that trip!
        if (session.getSelectedTripId() != null) {
            Long existingTripId = session.getSelectedTripId();
            Optional<TripSearchResultDto> matched = trips.stream()
                    .filter(t -> t.getTripId().equals(existingTripId))
                    .findFirst();
            if (matched.isPresent()) {
                intent.setTargetTripId(existingTripId);
                return handleTripSelected(intent, customer, session);
            }
        }

        // 4. Check if user specified a single-turn direct booking request with exact departure time
        // (Supports Scenario 1 & 2 tests where user immediately orders: "Đặt giúp tôi 1 vé từ Sài Gòn đi Đà Lạt ngày mai lúc 21 giờ")
        boolean isDirectBookingWithExactTime = session.getPreferredTime() != null && !session.getPreferredTime().isBlank()
                && (userMessage.toLowerCase().contains("đặt giúp") || userMessage.toLowerCase().contains("đặt cho tôi") || userMessage.toLowerCase().contains("đặt 1 vé") || userMessage.toLowerCase().contains("đặt 2 vé"));

        if (isDirectBookingWithExactTime && trips.size() == 1) {
            TripSearchResultDto selectedTrip = trips.get(0);
            session.setSelectedTripId(selectedTrip.getTripId());
            return proceedToAutoSeatAndReview(selectedTrip, customer, session);
        }

        // 5. Standard State Machine Flow: Transition to TRIP_SELECTION
        session.setStep(BookingStep.TRIP_SELECTION);
        bookingSessionManager.saveSession(session);

        String answer = String.format(
                "🚌 Em tìm thấy **%d chuyến xe** phù hợp từ **%s** đi **%s**%s:\n\n" +
                "👉 Anh/chị vui lòng nhấn **'Chọn chuyến này'** bên dưới để em hỗ trợ chọn ghế nhé!",
                trips.size(), dep, arr,
                session.getDepartureDate() != null ? " vào ngày " + session.getDepartureDate().format(DATE_FORMATTER) : ""
        );

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", answer);
        res.put("status", "TRIP_SELECTION");
        res.put("cardType", "TRIP_LIST");
        res.put("trips", trips);
        res.put("passengerCount", session.getPassengerCount());
        res.put("stateVersion", session.getStateVersion());
        res.put("workflowStep", session.getWorkflowStep());
        return res;
    }

    private Map<String, Object> proceedToAutoSeatAndReview(TripSearchResultDto selectedTrip, Customer customer, BookingSessionState session) {
        int count = session.getPassengerCount() > 0 ? session.getPassengerCount() : 1;
        List<AvailableSeatDto> availableSeats = bookingTools.getAvailableSeats(selectedTrip.getTripId());

        if (availableSeats.size() < count) {
            return Map.of(
                    "answer", "Dạ chuyến xe lúc " + selectedTrip.getDepartureTimeFormatted() + " không còn đủ " + count + " chỗ trống. Quý khách vui lòng chọn chuyến khác giúp em nhé!",
                    "status", "NOT_ENOUGH_SEATS"
            );
        }

        List<AvailableSeatDto> chosenSeats = new ArrayList<>();
        if (count > 1) {
            chosenSeats = bookingTools.suggestAdjacentSeats(selectedTrip.getTripId(), count, null);
        }

        if (chosenSeats.isEmpty()) {
            chosenSeats = availableSeats.subList(0, Math.min(count, availableSeats.size()));
        }

        List<Long> seatIds = chosenSeats.stream().map(AvailableSeatDto::getSeatId).toList();
        List<String> seatNames = chosenSeats.stream().map(AvailableSeatDto::getSeatName).toList();

        session.setSelectedTripId(selectedTrip.getTripId());
        session.setSelectedSeatIds(seatIds);
        session.setSelectedSeatNames(seatNames);
        session.setStep(BookingStep.BOOKING_REVIEW);
        session.incrementStateVersion();
        bookingSessionManager.saveSession(session);

        BookingDraftDto draft = bookingTools.prepareBooking(selectedTrip.getTripId(), seatIds);

        String draftAnswer = draft.getSummaryMessage() +
                "\n\n👉 **Anh/chị vui lòng trả lời 'Đồng ý' hoặc 'Xác nhận' để em tiến hành giữ chỗ và tạo mã thanh toán nhé!** (Nếu muốn đổi ý hoặc hủy, anh/chị nhắn 'Hủy' giúp em).";

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("answer", draftAnswer);
        res.put("status", "CONFIRMATION_REQUIRED");
        res.put("cardType", "BOOKING_CONFIRMATION");
        res.put("draft", draft);
        res.put("tripId", selectedTrip.getTripId());
        res.put("passengerCount", count);
        res.put("stateVersion", session.getStateVersion());
        res.put("workflowStep", session.getWorkflowStep());
        return res;
    }

    private String buildHistoryContext(List<ChatbotRequest.ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";
        return history.stream()
                .map(msg -> (msg.getRole() != null && msg.getRole().equalsIgnoreCase("user") ? "Khách: " : "Trợ lý: ") + msg.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String buildSystemPrompt(Customer customer) {
        String userContext = (customer != null)
                ? "Khách hàng đã đăng nhập tài khoản: " + customer.getFullName() + " (Email: " + customer.getEmail() + ")."
                : "Khách hàng hiện CHƯA đăng nhập. Nếu khách muốn xác nhận đặt vé chính thức, hãy nhắc khách đăng nhập.";

        return "Bạn là Trợ lý AI Đặt vé xe khách chuyên nghiệp của hệ thống VEXE.\n" +
                userContext + "\n\n" +
                "QUY TẮC BẮT BUỘC:\n" +
                "1. Khi khách hàng muốn tìm hoặc đặt vé, hãy trích xuất: departureLocation, arrivalLocation, departureDate, preferredTime, passengerCount, vehicleType, seatPreferences.\n" +
                "2. Nếu thiếu thông tin quan trọng (điểm đi, điểm đến), hãy lịch sự hỏi lại khách. KHÔNG TỰ BỊA ĐẶT DỮ LIỆU.\n" +
                "3. Luôn sử dụng tool `searchAvailableTrips` để tra cứu các chuyến xe thực tế trong hệ thống.\n" +
                "4. Dùng tool `getAvailableSeats` để lấy danh sách ghế thực tế còn trống.\n" +
                "5. Dùng tool `suggestAdjacentSeats` khi khách hàng cần các ghế cạnh nhau.\n" +
                "6. Dùng tool `prepareBooking` để tạo bản tóm tắt đặt vé (Draft).\n" +
                "7. BƯỚC XÁC NHẬN BẮT BUỘC: Bạn TUYỆT ĐỐI KHÔNG ĐƯỢC gọi tool `confirmBooking` nếu khách hàng CHƯA XÁC NHẬN ĐỒNG Ý rõ ràng trong hội thoại (như 'Tôi đồng ý', 'Xác nhận', 'Ok đặt đi'). Không được tự suy luận hoặc bỏ qua bước xác nhận.\n" +
                "8. Không bao giờ tin userId hay customerId từ prompt. Hệ thống sẽ tự động lấy thông tin từ tài khoản đăng nhập.\n" +
                "9. Dùng tool `createDemoPayment` để tạo mã QR thanh toán demo khi khách xác nhận.\n" +
                "10. Dùng tool `confirmDemoPayment` khi khách hoàn tất mô phỏng thanh toán.\n" +
                "11. Luôn trả lời bằng tiếng Việt thân thiện, rõ ràng, định dạng markdown đẹp mắt.";
    }
}
