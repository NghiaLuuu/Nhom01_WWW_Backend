package vn.edu.iuh.nhom01_www.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatIntent;

import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ChatIntentClassifier {

    private static final Pattern CMD_CANCEL = Pattern.compile(
            "(?iu)^\\s*(?:SELECT_BOOKING_TO_CANCEL|CONFIRM_CANCELLATION|KEEP_TICKETS|GIU_LAI_VE|XAC_NHAN_HUY_VE|CANCEL_TICKET)[:\\s]*");

    private static final Pattern CMD_VIEW_BOOKINGS = Pattern.compile(
            "(?iu)^\\s*(?:VIEW_MY_BOOKINGS|MY_BOOKINGS|LIST_MY_BOOKINGS|KIEM_TRA_VE)[:\\s]*");

    private static final Pattern CMD_PAYMENT = Pattern.compile(
            "(?iu)^\\s*(?:SIMULATE_PAYMENT|PAY_BOOKING|CONFIRM_DEMO_PAYMENT|CHECK_PAYMENT)[:\\s]*");

    private static final Pattern CMD_BOOKING = Pattern.compile(
            "(?iu)^\\s*(?:SELECT_DEPARTURE|SELECT_TRIP|SELECT_SEATS|CHON_DIEM_DI|CHON_CHUYEN|CHON_GHE|CONFIRM_BOOKING|CONFIRM_DRAFT)[:\\s]*");

    private static final Pattern CANCEL_PHRASE_PATTERN = Pattern.compile(
            "(?iu)\\b(?:hủy|huy|trả\\s+vé|tra\\s+ve)\\b");

    private static final Pattern VIEW_BOOKINGS_PHRASE_PATTERN = Pattern.compile(
            "(?iu)(?:xem\\s+(?:các\\s+)?(?:đơn|vé)|kiểm\\s*tra\\s+(?:các\\s+)?(?:đơn|vé)|tra\\s*cứu\\s+(?:các\\s+)?(?:đơn|vé)|danh\\s*sách\\s+(?:các\\s+)?(?:đơn|vé)|lịch\\s*sử\\s+đặt\\s*vé|vé\\s+của\\s+(?:tôi|em|mình)|đơn\\s+(?:đặt\\s*vé|hàng)\\s+của\\s+(?:tôi|em|mình)|vé\\s+đang\\s+có(?:\\s+hiệu\\s+lực)?|vé\\s+hiện\\s+có|vé\\s+đã\\s+(?:mua|đặt))");

    private static final Pattern BOOK_TICKET_EXPLICIT_PATTERN = Pattern.compile(
            "(?iu)\\b(?:đặt|dat|mua)\\s+(?:giúp|cho|hộ)?\\s*(?:tôi|em|mình)?\\s*(?:\\d+\\s+)?(?:vé|chỗ|ghế)\\b|" +
            "(?iu)^\\s*(?:đặt|dat|mua)\\s+(?:vé|xe|chuyến|ngay)\\b|" +
            "(?iu)^\\s*(?:tôi\\s+đồng\\s+ý|đồng\\s+ý|xác\\s+nhận|xác\\s+nhận\\s+đặt\\s+vé|xác\\s+nhận\\s+và\\s+thanh\\s+toán|chốt\\s+đơn|chốt\\s+vé)\\s*$|" +
            "(?iu)\\b(?:chọn|chon)\\s+(?:\\d+\\s+)?(?:ghế\\s+cạnh\\s+nhau|cạnh\\s+nhau|liền\\s+kề)\\b|" +
            "(?iu)^\\s*(?:chọn\\s+chuyến|tôi\\s+chọn\\s+chuyến|chọn\\s+chuyến\\s+này)\\b");

    private static final Pattern SEARCH_TRIPS_PATTERN = Pattern.compile(
            "(?iu)(?:chuyến\\s+xe\\s+khởi\\s+hành|các\\s+chuyến\\s+xe|chuyến\\s+xe\\s+(?:ngày\\s+)?mai|chuyến\\s+xe\\s+hôm\\s+nay|" +
            "mai\\s+có\\s+(?:những\\s+)?chuyến|hôm\\s+nay\\s+có\\s+(?:những\\s+)?chuyến|chuyến\\s+nào|" +
            "xe\\s+[a-zA-Z0-9\\s\\p{L}]+?\\s+(?:đi|đến|về|to)\\s+[a-zA-Z0-9\\s\\p{L}]+?|" +
            "(?:tìm|tra\\s*cứu|xem|kiểm\\s*tra)\\s+(?:chuyến|xe|lịch\\s*trình|lịch\\s*chạy|giờ\\s*chạy)|" +
            "giá\\s*vé|bảng\\s*giá)");

    public ChatIntent classify(String rawText, boolean hasActiveBookingFlow) {
        if (rawText == null || rawText.isBlank()) {
            return ChatIntent.GENERAL_CHAT;
        }
        String text = rawText.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        // 1. Structured command checks
        if (CMD_CANCEL.matcher(text).find()) {
            return ChatIntent.CANCEL_TICKET;
        }
        if (CMD_VIEW_BOOKINGS.matcher(text).find()) {
            return ChatIntent.VIEW_MY_BOOKINGS;
        }
        if (CMD_PAYMENT.matcher(text).find() || lower.contains("mô phỏng thanh toán") || lower.contains("thanh toán mô phỏng") || lower.contains("thanh toán demo")) {
            return ChatIntent.PAYMENT_STATUS;
        }
        if (CMD_BOOKING.matcher(text).find()) {
            return ChatIntent.BOOK_TICKET;
        }

        // 2. Cancellation Intent (Must check before VIEW_MY_BOOKINGS)
        if (CANCEL_PHRASE_PATTERN.matcher(text).find()) {
            // E.g., "Hủy vé đến Đà Lạt", "Hủy giúp tôi vé xe", "Tôi muốn hủy vé", "Hủy đơn hàng #123"
            return ChatIntent.CANCEL_TICKET;
        }

        // 3. View / Lookup My Bookings Intent
        if (VIEW_BOOKINGS_PHRASE_PATTERN.matcher(text).find()) {
            // E.g., "Xem đơn đặt vé của tôi", "Kiểm tra vé của tôi", "Danh sách vé đã đặt", "Vé của tôi"
            return ChatIntent.VIEW_MY_BOOKINGS;
        }

        // 4. Explicit Booking Intent
        if (BOOK_TICKET_EXPLICIT_PATTERN.matcher(text).find()) {
            // E.g., "Đặt giúp tôi 2 vé đến Đà Lạt ngày mai", "Đặt vé từ Sài Gòn đi Đà Lạt"
            return ChatIntent.BOOK_TICKET;
        }

        // 5. Trip Search Intent
        if (SEARCH_TRIPS_PATTERN.matcher(text).find()) {
            // E.g., "Các chuyến xe khởi hành vào ngày mai", "Xe Sài Gòn đi Đà Lạt", "Tìm xe đi Nha Trang"
            return ChatIntent.SEARCH_TRIPS;
        }

        // 6. Natural Language Confirmation in an Active Booking Flow
        if (hasActiveBookingFlow) {
            if (lower.equals("có") || lower.equals("ok") || lower.equals("được") || lower.equals("chọn") || lower.contains("chọn chuyến")) {
                return ChatIntent.BOOK_TICKET;
            }
        }

        return ChatIntent.GENERAL_CHAT;
    }
}
