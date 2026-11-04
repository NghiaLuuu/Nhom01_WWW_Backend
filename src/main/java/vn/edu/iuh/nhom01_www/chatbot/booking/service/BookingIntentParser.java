package vn.edu.iuh.nhom01_www.chatbot.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.aivector.VietnameseLocationNormalizer;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.BookingIntent;
import vn.edu.iuh.nhom01_www.chatbot.dto.ChatbotRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingIntentParser {

    private final VietnameseLocationNormalizer locationNormalizer = new VietnameseLocationNormalizer();

    private static final Pattern CMD_SELECT_DEPARTURE = Pattern.compile("(?iu)^\\s*(?:SELECT_DEPARTURE|CHON_DIEM_DI)[:\\s]+(.+?)\\s*$");
    private static final Pattern CMD_SELECT_TRIP = Pattern.compile("(?iu)^\\s*(?:SELECT_TRIP|CHON_CHUYEN)[:\\s]+(\\d+)\\s*$");
    private static final Pattern CMD_MANUAL_SEAT_MAP = Pattern.compile("(?iu)^\\s*(?:MANUAL_SEAT_MAP|TU_CHON_GHE|OPEN_SEAT_MAP)(?:[:\\s]+(\\d+))?\\s*$");
    private static final Pattern CMD_SELECT_SEATS = Pattern.compile("(?iu)^\\s*(?:SELECT_SEATS|CHON_GHE)[:\\s]+(\\d+)[:\\s]+([a-zA-Z0-9,\\s]+)\\s*$");
    private static final Pattern CMD_SIMULATE_PAYMENT = Pattern.compile("(?iu)^\\s*(?:SIMULATE_PAYMENT|THANH_TOAN_DEMO)[:\\s]+(\\d+)(?:[:\\s]+([a-zA-Z0-9_\\-]+))?\\s*$");
    private static final Pattern CMD_CANCEL_BOOKING = Pattern.compile("(?iu)^\\s*(?:CANCEL_BOOKING|HUY_DAT_VE|CANCEL_PAYMENT)[:\\s]*(\\d+)?\\s*$");
    private static final Pattern NATURAL_TRIP_TIME_PATTERN = Pattern.compile("(\\d{1,2}[:h]\\d{2})");
    private static final Pattern TRIP_ID_NATURAL_PATTERN = Pattern.compile("(?iu)(?:chuyến|chuyen)[^\\d]*?(\\d{2,})");

    private static final Pattern ROUTE_PATTERN_1 = Pattern.compile(
            "(?iu)(?:từ|tu|from)\\s+([a-zA-Z0-9\\s\\p{L}]+?)\\s+(?:đến|den|đi|di|về|ve|to)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+mai|\\s+hôm|\\s+xe|\\s*$)");
    private static final Pattern ROUTE_PATTERN_2 = Pattern.compile(
            "(?iu)(?:vé|ve|xe)\\s+(?:đi|di)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+từ|\\s+tu)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+mai|\\s*$)");
    private static final Pattern ROUTE_PATTERN_DASH = Pattern.compile(
            "(?iu)([a-zA-Z0-9\\s\\p{L}]+?)\\s*[-–—]\\s*([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+mai|\\s*$)");
    private static final Pattern DESTINATION_ONLY_PATTERN = Pattern.compile(
            "(?iu)(?:vé|ve|xe)?\\s*(?:đi|di|đến|den|về|ve)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+tối|\\s+sáng|\\s+trưa|\\s+chiều|\\s+mai|\\s+hôm|\\s*$)");
    private static final Pattern DEPARTURE_ONLY_PATTERN = Pattern.compile(
            "(?iu)(?:khởi hành|khoi hanh|đi|di)?\\s*(?:từ|tu|from)\\s+([a-zA-Z0-9\\s\\p{L}]+?)(?:\\s+ngày|\\s+lúc|\\s+vào|\\s+tối|\\s+sáng|\\s+trưa|\\s+chiều|\\s+mai|\\s+hôm|\\s*$)");

    private static final Pattern PASSENGER_COUNT_PATTERN = Pattern.compile(
            "(?iu)(\\d+)\\s*(?:vé|ve|chỗ|cho|người|khách|ghế|ghe|ticket|seat)");
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?iu)(?:lúc|vào lúc|khoảng|lúc tầm)?\\s*(\\d{1,2}(?:h|:| giờ|g)(?:\\d{1,2})?|sáng|trưa|chiều|tối|đêm)");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?iu)(?:ngày|ngay)?\\s*(\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{4})?)");

    private static final Pattern SEAT_CODE_PATTERN = Pattern.compile(
            "(?iu)\\b([AB]\\d{2})\\b");

    public BookingIntent parseBookingIntent(String text, String rawHistory) {
        if (rawHistory != null && !rawHistory.isBlank()) {
            List<Map<String, String>> items = new ArrayList<>();
            String[] lines = rawHistory.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.toLowerCase(Locale.ROOT).startsWith("bot:") || trimmed.toLowerCase(Locale.ROOT).startsWith("trợ lý:")) {
                    items.add(Map.of("role", "assistant", "content", trimmed));
                } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("user:") || trimmed.toLowerCase(Locale.ROOT).startsWith("khách:")) {
                    items.add(Map.of("role", "user", "content", trimmed.substring(trimmed.indexOf(':') + 1).trim()));
                } else {
                    items.add(Map.of("role", "user", "content", trimmed));
                }
            }
            return parse(text, items);
        }
        return parse(text, Collections.emptyList());
    }

    public BookingIntent parse(String text, List<?> rawHistory) {
        if (text == null) {
            text = "";
        }
        String clean = text.trim();
        String lower = clean.toLowerCase(Locale.ROOT);

        BookingIntent.BookingIntentBuilder builder = BookingIntent.builder()
                .rawQuery(clean);

        // Check structured commands first
        Matcher depCmd = CMD_SELECT_DEPARTURE.matcher(clean);
        if (depCmd.find()) {
            builder.departureLocation(cleanLocationString(depCmd.group(1)));
            int inherited = inheritPassengerCount(rawHistory);
            if (inherited > 0) {
                builder.passengerCount(inherited);
            }
            if (rawHistory != null && !rawHistory.isEmpty()) {
                builder.arrivalLocation(inheritMissingField(null, rawHistory, false));
                builder.departureDate(inheritDate(rawHistory));
                builder.preferredTime(inheritTime(rawHistory));
            }
            return builder.build();
        }

        Matcher tripCmd = CMD_SELECT_TRIP.matcher(clean);
        if (tripCmd.find()) {
            builder.targetTripId(Long.parseLong(tripCmd.group(1)));
            int inherited = inheritPassengerCount(rawHistory);
            if (inherited > 0) {
                builder.passengerCount(inherited);
            }
            return builder.build();
        }

        Matcher manualSeatCmd = CMD_MANUAL_SEAT_MAP.matcher(clean);
        if (manualSeatCmd.find()) {
            builder.isManualSeatSelection(true);
            if (manualSeatCmd.group(1) != null) {
                builder.targetTripId(Long.parseLong(manualSeatCmd.group(1)));
            }
            int inherited = inheritPassengerCount(rawHistory);
            if (inherited > 0) {
                builder.passengerCount(inherited);
            }
            return builder.build();
        }

        Matcher seatsCmd = CMD_SELECT_SEATS.matcher(clean);
        if (seatsCmd.find()) {
            builder.targetTripId(Long.parseLong(seatsCmd.group(1)));
            String[] seatArr = seatsCmd.group(2).split("[,\\s]+");
            builder.targetSeatNames(Arrays.stream(seatArr).filter(s -> !s.isBlank()).map(String::trim).toList());
            int inherited = inheritPassengerCount(rawHistory);
            if (inherited > 0) {
                builder.passengerCount(inherited);
            }
            return builder.build();
        }

        Matcher payCmd = CMD_SIMULATE_PAYMENT.matcher(clean);
        if (payCmd.find()) {
            builder.isPaymentSimulation(true);
            builder.targetBookingId(Long.parseLong(payCmd.group(1)));
            if (payCmd.group(2) != null) {
                builder.targetTransactionId(payCmd.group(2));
            }
            return builder.build();
        }

        Matcher cancelCmd = CMD_CANCEL_BOOKING.matcher(clean);
        if (cancelCmd.find()) {
            builder.isRejection(true);
            if (cancelCmd.group(1) != null) {
                builder.targetBookingId(Long.parseLong(cancelCmd.group(1)));
            }
            return builder.build();
        }

        // Check prompt injection attempting to bypass confirmation
        if (lower.contains("bỏ qua xác nhận") || lower.contains("khỏi cần hỏi") || lower.contains("không cần xác nhận")
                || lower.contains("ignore confirmation") || lower.contains("skip confirmation")
                || lower.contains("bỏ qua bước xác nhận") || lower.contains("tự động xác nhận")) {
            builder.isPromptInjection(true);
            builder.promptInjectionReason("Phát hiện yêu cầu bỏ qua quy trình xác nhận đặt vé bắt buộc.");
            return builder.build();
        }

        // Check confirmation intent
        if (isUserConfirmation(lower)) {
            builder.isConfirmation(true);
            int inherited = inheritPassengerCount(rawHistory);
            if (inherited > 0) {
                builder.passengerCount(inherited);
            }
            return builder.build();
        }

        // Check rejection intent
        if (isUserRejection(lower)) {
            builder.isRejection(true);
            return builder.build();
        }

        // Check simulation intent expressed naturally
        if (lower.contains("mô phỏng thanh toán") || lower.contains("thanh toán mô phỏng") || lower.contains("thanh toán demo")) {
            builder.isPaymentSimulation(true);
            return builder.build();
        }

        // Check natural trip time selection (e.g. "chuyến 20:45", "chuyến lúc 20h45")
        Matcher naturalTime = NATURAL_TRIP_TIME_PATTERN.matcher(lower);
        if (naturalTime.find()) {
            builder.targetTripTime(naturalTime.group(1).replace('h', ':'));
        }

        Matcher naturalTripId = TRIP_ID_NATURAL_PATTERN.matcher(clean);
        if (naturalTripId.find()) {
            try {
                builder.targetTripId(Long.parseLong(naturalTripId.group(1)));
            } catch (Exception ignored) {}
        }

        // Check manual seat selection intent
        if (lower.contains("tự chọn vị trí ghế") || lower.contains("tự chọn ghế") || lower.contains("tôi muốn tự chọn")
                || lower.contains("chọn ghế thủ công") || lower.contains("xem sơ đồ ghế") || lower.contains("mở sơ đồ ghế")) {
            builder.isManualSeatSelection(true);
        }

        // Check adjacent seat intent
        if (lower.contains("cạnh nhau") || lower.contains("liền kề") || lower.contains("gần nhau")
                || lower.equals("có") || lower.equals("có chọn ghế cạnh nhau") || lower.equals("chọn ghế cạnh nhau")
                || lower.contains("chọn 2 ghế cạnh nhau") || lower.contains("hai ghế cạnh nhau")) {
            builder.wantsAdjacentSeats(true);
        }

        // Extract Passenger Count
        Matcher countMatcher = PASSENGER_COUNT_PATTERN.matcher(clean);
        if (countMatcher.find()) {
            try {
                int count = Integer.parseInt(countMatcher.group(1));
                builder.passengerCount(count > 0 ? count : 0);
            } catch (Exception ignored) {
            }
        } else {
            int inherited = inheritPassengerCount(rawHistory);
            if (inherited > 0) {
                builder.passengerCount(inherited);
            }
        }

        // Extract Route
        String dep = null;
        String arr = null;

        Matcher routeMatcher1 = ROUTE_PATTERN_1.matcher(clean);
        if (routeMatcher1.find()) {
            dep = cleanLocationString(routeMatcher1.group(1));
            arr = cleanLocationString(routeMatcher1.group(2));
        } else {
            Matcher routeMatcher2 = ROUTE_PATTERN_2.matcher(clean);
            if (routeMatcher2.find()) {
                arr = cleanLocationString(routeMatcher2.group(1));
                dep = cleanLocationString(routeMatcher2.group(2));
            } else {
                Matcher dashMatcher = ROUTE_PATTERN_DASH.matcher(clean);
                if (dashMatcher.find()) {
                    dep = cleanLocationString(dashMatcher.group(1));
                    arr = cleanLocationString(dashMatcher.group(2));
                }
            }
        }

        // Fallback to directional locations from normalizer
        if (dep == null || arr == null) {
            var directional = locationNormalizer.directionalLocations(clean);
            if (directional.isPresent()) {
                dep = directional.get().departure();
                arr = directional.get().arrival();
            }
        }

        // Single location extraction: departure only vs destination only
        if (dep == null && arr == null) {
            Matcher depMatcher = DEPARTURE_ONLY_PATTERN.matcher(clean);
            if (depMatcher.find()) {
                dep = cleanLocationString(depMatcher.group(1));
            }
            Matcher arrMatcher = DESTINATION_ONLY_PATTERN.matcher(clean);
            if (arrMatcher.find()) {
                arr = cleanLocationString(arrMatcher.group(1));
            }
        }

        // Check if user is simply replying with a city name when prompted for departure in multi-turn
        if (dep == null && arr == null && isCityAnswer(clean, rawHistory)) {
            dep = cleanLocationString(clean);
        }

        // Multi-turn context history inheritance (Only if not a fresh open search and not a cancellation)
        boolean isOpenSearch = lower.contains("các chuyến xe") || lower.contains("có những chuyến nào") || lower.contains("chuyến xe khởi hành");
        if (!isOpenSearch && rawHistory != null && !rawHistory.isEmpty()) {
            dep = inheritMissingField(dep, rawHistory, true);
            arr = inheritMissingField(arr, rawHistory, false);
        }

        builder.departureLocation(dep);
        builder.arrivalLocation(arr);

        // Extract Date
        LocalDate date = extractDate(clean);
        if (date == null && rawHistory != null && !rawHistory.isEmpty()) {
            date = inheritDate(rawHistory);
        }
        builder.departureDate(date);

        // Extract Preferred Time
        String prefTime = extractPreferredTime(clean);
        if (prefTime == null && rawHistory != null && !rawHistory.isEmpty()) {
            prefTime = inheritTime(rawHistory);
        }
        builder.preferredTime(prefTime);

        // Extract Vehicle Type
        String vehicleType = extractVehicleType(lower);
        builder.vehicleType(vehicleType);

        // Extract Seat Preferences & Seat codes (e.g. A01, A02)
        List<String> seatCodes = new ArrayList<>();
        Matcher seatMatcher = SEAT_CODE_PATTERN.matcher(clean);
        while (seatMatcher.find()) {
            seatCodes.add(seatMatcher.group(1).toUpperCase(Locale.ROOT));
        }
        builder.targetSeatNames(seatCodes);

        List<String> prefs = new ArrayList<>();
        if (lower.contains("tầng 1") || lower.contains("tang 1") || lower.contains("dưới") || lower.contains("tang duoi")) {
            prefs.add("Tầng 1");
        }
        if (lower.contains("tầng 2") || lower.contains("tang 2") || lower.contains("trên") || lower.contains("tang tren")) {
            prefs.add("Tầng 2");
        }
        if (lower.contains("cửa sổ") || lower.contains("cua so")) {
            prefs.add("Cạnh cửa sổ");
        }
        if (lower.contains("đầu") || lower.contains("dau xe")) {
            prefs.add("Ghế đầu");
        }
        builder.seatPreferences(prefs);

        return builder.build();
    }

    private boolean isUserConfirmation(String lower) {
        List<String> confirms = List.of(
                "tôi đồng ý", "đồng ý", "xác nhận", "xác nhận đặt vé", "đặt vé luôn", "đặt luôn đi",
                "ok đặt đi", "ok xác nhận", "chốt đơn", "chốt vé", "đồng ý đặt", "tôi xác nhận", "yes", "confirm",
                "xác nhận và thanh toán", "xac nhan va thanh toan"
        );
        return confirms.stream().anyMatch(c -> lower.equals(c) || lower.startsWith(c + " ") || lower.endsWith(" " + c));
    }

    private boolean isUserRejection(String lower) {
        List<String> rejects = List.of(
                "hủy", "không đặt nữa", "thôi không đặt", "thôi", "không đồng ý", "đổi ý", "hủy đơn", "cancel", "từ chối",
                "hủy thanh toán", "huy thanh toan"
        );
        return rejects.stream().anyMatch(r -> lower.equals(r) || lower.startsWith(r + " ") || lower.endsWith(" " + r));
    }

    private boolean isCityAnswer(String text, List<?> history) {
        if (history == null || history.isEmpty()) return false;
        // Check if last message asked for departure
        Object lastObj = history.get(history.size() - 1);
        String lastText = getMessageContent(lastObj);
        if (lastText != null && (lastText.contains("khởi hành từ") || lastText.contains("từ điểm nào") || lastText.contains("từ đâu") || lastText.contains("chọn điểm đi") || lastText.contains("điểm khởi hành"))) {
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("chuyến") || lower.contains("hủy") || lower.contains("ngày") || lower.contains("vé của tôi") || lower.contains("mai") || lower.contains("hôm nay")) {
                return false;
            }
            return text.length() <= 30 && !text.contains("?");
        }
        return false;
    }

    private boolean isUserMessage(Object msgObj) {
        if (msgObj instanceof ChatbotRequest.ChatMessage msg) {
            return !"assistant".equalsIgnoreCase(msg.getRole()) && !"bot".equalsIgnoreCase(msg.getRole());
        }
        if (msgObj instanceof Map<?, ?> m) {
            Object role = m.get("role");
            if (role != null) {
                return !"assistant".equalsIgnoreCase(role.toString()) && !"bot".equalsIgnoreCase(role.toString());
            }
            return true;
        }
        return false;
    }

    private String inheritMissingField(String current, List<?> history, boolean lookingForDeparture) {
        if (current != null && !current.isBlank()) return current;
        if (history == null || history.isEmpty()) return current;
        for (int i = history.size() - 1; i >= 0; i--) {
            Object item = history.get(i);
            // STRICT CONTEXT ISOLATION: Never inherit from assistant / bot greeting messages!
            if (!isUserMessage(item)) continue;
            String content = getMessageContent(item);
            if (content == null) continue;

            if (lookingForDeparture) {
                Matcher m = ROUTE_PATTERN_1.matcher(content);
                if (m.find()) return cleanLocationString(m.group(1));
                Matcher m2 = ROUTE_PATTERN_2.matcher(content);
                if (m2.find()) return cleanLocationString(m2.group(2));
                Matcher m3 = DEPARTURE_ONLY_PATTERN.matcher(content);
                if (m3.find()) return cleanLocationString(m3.group(1));
            } else {
                Matcher m = ROUTE_PATTERN_1.matcher(content);
                if (m.find()) return cleanLocationString(m.group(2));
                Matcher m2 = ROUTE_PATTERN_2.matcher(content);
                if (m2.find()) return cleanLocationString(m2.group(1));
                Matcher m3 = DESTINATION_ONLY_PATTERN.matcher(content);
                if (m3.find()) return cleanLocationString(m3.group(1));
            }
        }
        return current;
    }

    private LocalDate inheritDate(List<?> history) {
        if (history == null || history.isEmpty()) return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            Object item = history.get(i);
            if (!isUserMessage(item)) continue;
            String content = getMessageContent(item);
            if (content != null) {
                LocalDate d = extractDate(content);
                if (d != null) return d;
            }
        }
        return null;
    }

    private String inheritTime(List<?> history) {
        if (history == null || history.isEmpty()) return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            Object item = history.get(i);
            if (!isUserMessage(item)) continue;
            String content = getMessageContent(item);
            if (content != null) {
                String t = extractPreferredTime(content);
                if (t != null) return t;
            }
        }
        return null;
    }

    private int inheritPassengerCount(List<?> history) {
        if (history == null || history.isEmpty()) return 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            Object item = history.get(i);
            if (!isUserMessage(item)) continue;
            String content = getMessageContent(item);
            if (content != null) {
                Matcher m = PASSENGER_COUNT_PATTERN.matcher(content);
                if (m.find()) {
                    try {
                        int c = Integer.parseInt(m.group(1));
                        if (c > 0) return c;
                    } catch (Exception ignored) {}
                }
            }
        }
        return 0;
    }

    private String getMessageContent(Object msgObj) {
        if (msgObj instanceof ChatbotRequest.ChatMessage msg) {
            return msg.getContent();
        }
        if (msgObj instanceof Map<?, ?> m) {
            Object c = m.get("content");
            return c != null ? c.toString() : null;
        }
        return null;
    }

    private LocalDate extractDate(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();

        if (lower.contains("hôm nay") || lower.contains("hom nay") || lower.contains("today")) {
            return today;
        }
        if (lower.contains("ngày mai") || lower.contains("ngay mai") || lower.contains("mai") || lower.contains("tomorrow")) {
            return today.plusDays(1);
        }
        if (lower.contains("ngày mốt") || lower.contains("ngày kia") || lower.contains("ngay mot") || lower.contains("ngay kia")) {
            return today.plusDays(2);
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1).replace('-', '/');
            String[] parts = dateStr.split("/");
            try {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = parts.length > 2 ? Integer.parseInt(parts[2]) : today.getYear();
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private String extractPreferredTime(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        Matcher matcher = TIME_PATTERN.matcher(lower);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        if (lower.contains("sáng")) return "sáng";
        if (lower.contains("trưa")) return "trưa";
        if (lower.contains("chiều")) return "chiều";
        if (lower.contains("tối") || lower.contains("đêm")) return "tối";
        return null;
    }

    private String extractVehicleType(String lower) {
        if (lower.contains("limousine") || lower.contains("vip")) return "Limousine";
        if (lower.contains("giường nằm") || lower.contains("giuong nam")) return "Giường nằm";
        if (lower.contains("ghế ngồi") || lower.contains("ghe ngoi")) return "Ghế ngồi";
        return null;
    }

    private String cleanLocationString(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        s = s.replaceAll("(?iu)^(?:xe|vé|chuyến|đi|đến|từ|khoảng|lúc|giúp|tôi|cho)\\s+", "");
        s = s.replaceAll("(?iu)\\s+(?:lúc|vào|ngày|mai|tối|sáng|chiều|trưa|tầm).*$", "");
        return s.trim();
    }
}
