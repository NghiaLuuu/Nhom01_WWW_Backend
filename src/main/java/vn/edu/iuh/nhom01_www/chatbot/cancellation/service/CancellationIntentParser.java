package vn.edu.iuh.nhom01_www.chatbot.cancellation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.chatbot.cancellation.dto.CancellationIntent;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CancellationIntentParser {

    private static final Pattern CMD_SELECT_BOOKING = Pattern.compile("(?iu)^\\s*(?:SELECT_BOOKING_TO_CANCEL|CHON_DON_HUY)[:\\s]+(\\d+)\\s*$");
    private static final Pattern CMD_CONFIRM_CANCEL = Pattern.compile("(?iu)^\\s*(?:CONFIRM_CANCELLATION|XAC_NHAN_HUY_VE)[:\\s]+([a-zA-Z0-9\\-]+)(?:[:\\s]+(\\d+))?\\s*$");
    private static final Pattern CMD_KEEP_TICKETS = Pattern.compile("(?iu)^\\s*(?:KEEP_TICKETS|GIU_LAI_VE|KHONG_HUY)\\s*$");

    private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("(?iu)\\b(BK[a-zA-Z0-9]+|VXDEMO[a-zA-Z0-9]+)\\b");

    // Strictly requires explicit '#' or keywords like 'mã đơn', 'đơn hàng', 'đơn số'. NEVER match date digits like 21/11!
    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile(
            "(?iu)(?:#(\\d{1,8})|(?:mã\\s+(?:đơn|đặt\\s+vé)|đơn\\s+(?:hàng|số|đặt\\s+vé)|booking|đơn)\\s*[:#]?\\s*(\\d{1,8}))(?![/\\-:])\\b");

    private static final Pattern TICKET_COUNT_PATTERN = Pattern.compile("(?iu)(\\d+)\\s*(?:vé|ve|ghế|chỗ)");
    private static final Pattern SEAT_CODE_PATTERN = Pattern.compile("(?iu)\\b([AB]\\d{2})\\b");

    private static final Pattern ROUTE_PATTERN_FROM_TO = Pattern.compile(
            "(?iu)(?:từ|tu|from)\\s+([a-zA-Z0-9\\s\\p{L}]+?)\\s+(?:đến|den|đi|di|về|ve|to)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+tối|\\s+sáng|\\s+trưa|\\s+chiều|\\s+mai|\\s+hôm|\\s*$)");
    private static final Pattern ROUTE_PATTERN_DASH = Pattern.compile(
            "(?iu)([a-zA-Z0-9\\s\\p{L}]+?)\\s*[-–—→]\\s*([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s*$)");
    private static final Pattern ROUTE_PATTERN_TO = Pattern.compile(
            "(?iu)(?:vé|chuyến|xe)?\\s*([a-zA-Z0-9\\s\\p{L}]+?)\\s+(?:đến|den|đi|di|về|ve|to)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+tối|\\s+sáng|\\s+trưa|\\s+chiều|\\s+mai|\\s+hôm|\\s*$)");
    private static final Pattern DESTINATION_ONLY_PATTERN = Pattern.compile(
            "(?iu)(?:đi|đến|to|về)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+tối|\\s+sáng|\\s+trưa|\\s+chiều|\\s+mai|\\s+hôm|\\s*$)");

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?iu)(?:ngày|ngay)?\\s*(\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{4})?)");

    public CancellationIntent parse(String text, List<?> history) {
        if (text == null) text = "";
        String clean = text.trim();
        String lower = clean.toLowerCase(Locale.ROOT);

        CancellationIntent.CancellationIntentBuilder builder = CancellationIntent.builder()
                .rawQuery(clean);

        // Check structured commands
        Matcher selMatcher = CMD_SELECT_BOOKING.matcher(clean);
        if (selMatcher.find()) {
            builder.targetBookingId(Long.parseLong(selMatcher.group(1)));
            return builder.build();
        }

        Matcher confMatcher = CMD_CONFIRM_CANCEL.matcher(clean);
        if (confMatcher.find()) {
            builder.isConfirmation(true);
            builder.confirmationToken(confMatcher.group(1));
            if (confMatcher.group(2) != null) {
                builder.targetBookingId(Long.parseLong(confMatcher.group(2)));
            }
            return builder.build();
        }

        Matcher keepMatcher = CMD_KEEP_TICKETS.matcher(clean);
        if (keepMatcher.find()) {
            builder.isKeepTickets(true);
            return builder.build();
        }

        // Check prompt injection
        if (lower.contains("bỏ qua xác nhận") || lower.contains("khỏi cần hỏi") || lower.contains("không cần xác nhận")
                || lower.contains("ignore confirmation") || lower.contains("skip confirmation")
                || lower.contains("bỏ qua bước xác nhận") || lower.contains("tự động hủy")
                || lower.contains("hủy luôn không cần hỏi")) {
            builder.isPromptInjection(true);
            builder.promptInjectionReason("Phát hiện yêu cầu bỏ qua quy trình xác nhận hủy vé bắt buộc.");
            return builder.build();
        }

        // Check confirmation phrases
        if (isUserConfirmCancel(lower)) {
            builder.isConfirmation(true);
            return builder.build();
        }

        // Check keep tickets phrases
        if (isUserKeepTickets(lower)) {
            builder.isKeepTickets(true);
            return builder.build();
        }

        // Check if user is asking to look up their active tickets (FIND_MY_BOOKINGS)
        if (isFindMyBookingsIntent(lower)) {
            builder.isFindMyBookings(true);
        }

        // Check if user specified cancel all
        if (lower.contains("toàn bộ") || lower.contains("hủy hết") || lower.contains("hủy tất cả")) {
            builder.cancelAll(true);
        }

        // Extract ticket count
        Matcher countMatcher = TICKET_COUNT_PATTERN.matcher(clean);
        if (countMatcher.find()) {
            try {
                int count = Integer.parseInt(countMatcher.group(1));
                builder.ticketCount(count);
            } catch (Exception ignored) {}
        }

        // Extract booking code (e.g. BK7632, VXDEMO123)
        Matcher codeMatcher = BOOKING_CODE_PATTERN.matcher(clean);
        if (codeMatcher.find()) {
            builder.targetBookingCode(codeMatcher.group(1).toUpperCase(Locale.ROOT));
        }

        // Extract booking ID (never mistake date numbers for booking IDs)
        Matcher idMatcher = BOOKING_ID_PATTERN.matcher(clean);
        if (idMatcher.find()) {
            String numStr = idMatcher.group(1) != null ? idMatcher.group(1) : idMatcher.group(2);
            if (numStr != null) {
                try {
                    builder.targetBookingId(Long.parseLong(numStr));
                } catch (Exception ignored) {}
            }
        }

        // Extract seat codes (e.g. A01, A02, VIP8, VIP2)
        List<String> seats = new ArrayList<>();
        Matcher seatMatcher = SEAT_CODE_PATTERN.matcher(clean);
        while (seatMatcher.find()) {
            seats.add(seatMatcher.group(1).toUpperCase(Locale.ROOT));
        }
        // Also match custom VIP seats like VIP8, VIP2
        Matcher vipMatcher = Pattern.compile("(?iu)\\b(VIP\\d+)\\b").matcher(clean);
        while (vipMatcher.find()) {
            seats.add(vipMatcher.group(1).toUpperCase(Locale.ROOT));
        }
        if (!seats.isEmpty()) {
            builder.targetSeatNames(seats);
        }

        // Extract Route (Departure and Destination)
        String dep = null;
        String arr = null;

        Matcher fromToMatcher = ROUTE_PATTERN_FROM_TO.matcher(clean);
        if (fromToMatcher.find()) {
            dep = cleanLocation(fromToMatcher.group(1));
            arr = cleanLocation(fromToMatcher.group(2));
        } else {
            Matcher dashMatcher = ROUTE_PATTERN_DASH.matcher(clean);
            if (dashMatcher.find()) {
                dep = cleanLocation(dashMatcher.group(1));
                arr = cleanLocation(dashMatcher.group(2));
            } else {
                Matcher toMatcher = ROUTE_PATTERN_TO.matcher(clean);
                if (toMatcher.find()) {
                    dep = cleanLocation(toMatcher.group(1));
                    arr = cleanLocation(toMatcher.group(2));
                } else {
                    Matcher destOnly = DESTINATION_ONLY_PATTERN.matcher(clean);
                    if (destOnly.find()) {
                        arr = cleanLocation(destOnly.group(1));
                    }
                }
            }
        }

        builder.departureKeyword(dep);
        builder.destinationKeyword(arr);

        // Extract explicit Date (e.g. ngày 21/11, 21/11/2026, 21-11)
        LocalDate date = extractDate(clean);
        if (date == null) {
            // Check relative dates
            if (lower.contains("ngày mai") || lower.contains("mai") || lower.contains("tối mai") || lower.contains("sáng mai")) {
                date = LocalDate.now().plusDays(1);
            } else if (lower.contains("hôm nay")) {
                date = LocalDate.now();
            } else if (lower.contains("ngày mốt") || lower.contains("ngày kia")) {
                date = LocalDate.now().plusDays(2);
            }
        }
        builder.dateKeyword(date);

        if (lower.contains("tối")) builder.timeKeyword("tối");
        else if (lower.contains("sáng")) builder.timeKeyword("sáng");
        else if (lower.contains("chiều")) builder.timeKeyword("chiều");
        else if (lower.contains("trưa")) builder.timeKeyword("trưa");

        return builder.build();
    }

    public boolean isFindMyBookingsIntent(String lower) {
        if (lower == null) return false;
        boolean hasFindKeyword = lower.contains("kiểm tra vé") || lower.contains("kiểm tra các vé")
                || lower.contains("vé của tôi") || lower.contains("vé của em") || lower.contains("vé của mình")
                || lower.contains("xem vé") || lower.contains("danh sách vé") || lower.contains("vé đã đặt")
                || lower.contains("vé đã mua") || lower.contains("vé hiện có") || lower.contains("vé đang có hiệu lực")
                || lower.contains("tôi có vé nào") || lower.contains("các vé đang có hiệu lực")
                || lower.contains("tra cứu vé") || lower.contains("tra cứu đơn đặt vé")
                || lower.contains("view_my_bookings") || lower.contains("my_bookings")
                || lower.contains("list_my_bookings") || lower.contains("kiem_tra_ve")
                || lower.contains("đơn đặt vé") || lower.contains("lịch sử đặt vé")
                || lower.contains("đơn của tôi") || lower.contains("đơn của em") || lower.contains("đơn của mình");
        boolean hasCancelKeyword = lower.contains("hủy") || lower.contains("huy") || lower.contains("trả vé") || lower.contains("tra ve");
        return hasFindKeyword && !hasCancelKeyword;
    }

    private LocalDate extractDate(String text) {
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1).replace('-', '/');
            String[] parts = dateStr.split("/");
            try {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                LocalDate today = LocalDate.now();
                int year = parts.length > 2 ? Integer.parseInt(parts[2]) : today.getYear();
                if (parts.length <= 2 && (month < today.getMonthValue() || (month == today.getMonthValue() && day < today.getDayOfMonth()))) {
                    year = today.getYear() + 1;
                }
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "hủy", "huy", "trả", "tra", "giúp", "giup", "tôi", "toi", "em", "mình", "minh",
            "cho", "hộ", "ho", "vé", "ve", "đơn", "don", "chuyến", "chuyen", "xe",
            "đặt", "dat", "booking", "cần", "can", "muốn", "muon", "kiểm tra", "kiem tra",
            "xem", "lấy", "lay", "bỏ", "bo"
    );

    private boolean isLikelyLocation(String name) {
        if (name == null || name.isBlank()) return false;
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        String[] tokens = trimmed.split("\\s+");
        boolean allStopWords = true;
        for (String t : tokens) {
            if (!STOP_WORDS.contains(t)) {
                allStopWords = false;
                break;
            }
        }
        return !allStopWords;
    }

    private String cleanLocation(String loc) {
        if (loc == null) return null;
        String s = loc.trim();
        s = s.replaceAll("(?iu)^(?:(?:xe|vé|chuyến|đi|đến|từ|về|hủy|giúp|tôi|cho|em|mình|hộ|đơn|bán|mua)\\s+)+", "");
        s = s.replaceAll("(?iu)\\s+(?:ngày|lúc|vào|tối|sáng|trưa|chiều|mai|hôm).*$", "");
        s = s.trim();
        if (s.isEmpty() || !isLikelyLocation(s)) {
            return null;
        }
        return s;
    }

    private boolean isUserConfirmCancel(String lower) {
        List<String> confirms = List.of(
                "xác nhận hủy", "xác nhận hủy vé", "tôi đồng ý hủy", "đồng ý hủy", "hủy luôn đi",
                "đồng ý hủy vé", "tôi xác nhận hủy vé", "yes, cancel", "confirm cancel"
        );
        return confirms.stream().anyMatch(c -> lower.equals(c) || lower.startsWith(c + " ") || lower.endsWith(" " + c));
    }

    private boolean isUserKeepTickets(String lower) {
        List<String> keeps = List.of(
                "giữ lại vé", "không hủy nữa", "thôi không hủy", "giữ vé", "thôi", "đổi ý", "quay lại", "đừng hủy", "cancel"
        );
        return keeps.stream().anyMatch(k -> lower.equals(k) || lower.startsWith(k + " ") || lower.endsWith(" " + k));
    }
}
