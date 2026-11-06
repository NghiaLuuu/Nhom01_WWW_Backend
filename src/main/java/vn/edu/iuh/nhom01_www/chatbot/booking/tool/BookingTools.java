package vn.edu.iuh.nhom01_www.chatbot.booking.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchRequest;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchResult;
import vn.edu.iuh.nhom01_www.aivector.VectorSearchService;
import vn.edu.iuh.nhom01_www.aivector.VietnameseLocationNormalizer;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.service.BookingService;
import vn.edu.iuh.nhom01_www.booking.service.SeatHoldRedisService;
import vn.edu.iuh.nhom01_www.chatbot.booking.dto.*;
import vn.edu.iuh.nhom01_www.chatbot.booking.service.BookingDraftManager;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentConfirmResponse;
import vn.edu.iuh.nhom01_www.payment.demo.dto.DemoPaymentQrResponse;
import vn.edu.iuh.nhom01_www.payment.demo.processor.DemoPaymentProcessor;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.trip.service.TripBookingAvailabilityService;
import vn.edu.iuh.nhom01_www.user.model.Customer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BookingTools {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final BookingService bookingService;
    private final TripBookingAvailabilityService tripBookingAvailabilityService;
    private final SeatHoldRedisService seatHoldRedisService;
    private final CurrentUserService currentUserService;
    private final BookingDraftManager bookingDraftManager;
    private final Optional<VectorSearchService> vectorSearchService;
    private final Optional<DemoPaymentProcessor> demoPaymentProcessor;

    private final VietnameseLocationNormalizer locationNormalizer = new VietnameseLocationNormalizer();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    public BookingTools(
            TripRepository tripRepository,
            RouteRepository routeRepository,
            SeatRepository seatRepository,
            TicketRepository ticketRepository,
            BookingService bookingService,
            TripBookingAvailabilityService tripBookingAvailabilityService,
            SeatHoldRedisService seatHoldRedisService,
            CurrentUserService currentUserService,
            BookingDraftManager bookingDraftManager,
            Optional<VectorSearchService> vectorSearchService) {
        this(tripRepository, routeRepository, seatRepository, ticketRepository, bookingService,
                tripBookingAvailabilityService, seatHoldRedisService, currentUserService,
                bookingDraftManager, vectorSearchService, Optional.empty());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public BookingTools(
            TripRepository tripRepository,
            RouteRepository routeRepository,
            SeatRepository seatRepository,
            TicketRepository ticketRepository,
            BookingService bookingService,
            TripBookingAvailabilityService tripBookingAvailabilityService,
            SeatHoldRedisService seatHoldRedisService,
            CurrentUserService currentUserService,
            BookingDraftManager bookingDraftManager,
            Optional<VectorSearchService> vectorSearchService,
            Optional<DemoPaymentProcessor> demoPaymentProcessor) {
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
        this.bookingService = bookingService;
        this.tripBookingAvailabilityService = tripBookingAvailabilityService;
        this.seatHoldRedisService = seatHoldRedisService;
        this.currentUserService = currentUserService;
        this.bookingDraftManager = bookingDraftManager;
        this.vectorSearchService = vectorSearchService;
        this.demoPaymentProcessor = demoPaymentProcessor != null ? demoPaymentProcessor : Optional.empty();
    }

    @Tool(name = "listAvailableDepartureLocations", description = "Truy vấn danh sách các điểm khởi hành thực tế có chuyến xe đi đến điểm đến mong muốn vào ngày đã chọn và còn đủ chỗ trống.")
    public List<DepartureOptionDto> listAvailableDepartureLocations(
            @ToolParam(description = "Điểm đến cần đến (ví dụ: Đà Lạt, Nha Trang, Vũng Tàu)") String destination,
            @ToolParam(description = "Ngày khởi hành định dạng YYYY-MM-DD (nếu không cung cấp thì mặc định là ngày mai)", required = false) String departureDate,
            @ToolParam(description = "Số lượng hành khách / số vé cần đặt (mặc định 1)", required = false) Integer passengerCount) {

        if (destination == null || destination.isBlank()) {
            return Collections.emptyList();
        }

        int minSeats = (passengerCount != null && passengerCount > 0) ? passengerCount : 1;
        LocalDate targetDate;
        if (departureDate != null && !departureDate.isBlank()) {
            try {
                targetDate = LocalDate.parse(departureDate.trim());
            } catch (Exception e) {
                targetDate = LocalDate.now().plusDays(1);
            }
        } else {
            targetDate = LocalDate.now().plusDays(1);
        }

        // Search trips to destination on targetDate
        List<Trip> candidates = findCandidateTrips("", destination, targetDate.toString());
        LocalDateTime now = LocalDateTime.now();

        // Filter valid trips
        Map<String, List<Trip>> groupedByDeparture = new LinkedHashMap<>();
        for (Trip trip : candidates) {
            if (trip.getStatus() != null && (trip.getStatus().equalsIgnoreCase("CANCELLED") || trip.getStatus().equalsIgnoreCase("COMPLETED"))) {
                continue;
            }
            if (trip.getDepartureTime() == null || trip.getDepartureTime().isBefore(now)) {
                continue;
            }
            int availableSeats = tripBookingAvailabilityService.getAvailableSeatCount(trip);
            if (availableSeats < minSeats) {
                continue;
            }
            if (!tripBookingAvailabilityService.isWebBookingOpen(trip)) {
                continue;
            }

            String dep = (trip.getRoute() != null && trip.getRoute().getDepartureLocation() != null)
                    ? trip.getRoute().getDepartureLocation() : "Khác";
            String normDep = locationNormalizer.normalize(dep);

            groupedByDeparture.computeIfAbsent(normDep, k -> new ArrayList<>()).add(trip);
        }

        List<DepartureOptionDto> options = new ArrayList<>();
        DateTimeFormatter timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Map.Entry<String, List<Trip>> entry : groupedByDeparture.entrySet()) {
            List<Trip> trips = entry.getValue();
            if (trips.isEmpty()) continue;

            // Pick the best display name from the routes (prefer canonical ones)
            String canonicalName = trips.get(0).getRoute().getDepartureLocation();
            for (Trip t : trips) {
                String candidateName = t.getRoute().getDepartureLocation();
                if (candidateName.contains("TP.") || candidateName.contains("Thành phố") || candidateName.length() > canonicalName.length()) {
                    canonicalName = candidateName;
                }
            }

            int activeCount = trips.size();
            int totalSeats = trips.stream().mapToInt(tripBookingAvailabilityService::getAvailableSeatCount).sum();
            BigDecimal minPrice = trips.stream().map(Trip::getPrice).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            LocalDateTime earliest = trips.stream().map(Trip::getDepartureTime).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
            String earliestStr = earliest != null ? earliest.format(timeOnlyFormatter) : "N/A";

            List<String> vehicleTypes = trips.stream()
                    .map(t -> t.getVehicle() != null ? t.getVehicle().getType() : "Tiêu chuẩn")
                    .distinct()
                    .toList();

            options.add(DepartureOptionDto.builder()
                    .departureLocation(canonicalName)
                    .arrivalLocation(destination)
                    .departureDate(targetDate.toString())
                    .activeTripCount(activeCount)
                    .totalAvailableSeats(totalSeats)
                    .minPrice(minPrice)
                    .minPriceFormatted(CURRENCY_FORMATTER.format(minPrice) + " VNĐ")
                    .earliestDepartureTimeFormatted(earliestStr)
                    .vehicleTypes(vehicleTypes)
                    .command("SELECT_DEPARTURE:" + canonicalName)
                    .build());
        }

        // Sort by trip count DESC, then minPrice ASC
        options.sort(Comparator.comparingInt(DepartureOptionDto::getActiveTripCount).reversed()
                .thenComparing(DepartureOptionDto::getMinPrice));

        return options;
    }

    @Tool(name = "searchAvailableTrips", description = "Tìm kiếm danh sách các chuyến xe thực tế còn chỗ dựa theo điểm đi, điểm đến, ngày khởi hành và khung giờ mong muốn.")
    public List<TripSearchResultDto> searchAvailableTrips(
            @ToolParam(description = "Điểm khởi hành, ví dụ: Sài Gòn, TP.HCM, Hà Nội, Đà Lạt") String departureLocation,
            @ToolParam(description = "Điểm đến, ví dụ: Đà Lạt, Nha Trang, Cần Thơ, Vũng Tàu") String arrivalLocation,
            @ToolParam(description = "Ngày khởi hành định dạng YYYY-MM-DD (ví dụ: 2026-11-03)", required = false) String departureDate,
            @ToolParam(description = "Khung giờ hoặc giờ mong muốn (ví dụ: 21:00, sáng, tối)", required = false) String preferredTime,
            @ToolParam(description = "Loại xe (ví dụ: Giường nằm, Limousine, Ghế ngồi)", required = false) String vehicleType) {
        return searchAvailableTrips(departureLocation, arrivalLocation, departureDate, preferredTime, vehicleType, 1);
    }

    public List<TripSearchResultDto> searchAvailableTrips(
            String departureLocation,
            String arrivalLocation,
            String departureDate,
            String preferredTime,
            String vehicleType,
            Integer passengerCount) {

        log.info("[BookingTool:searchAvailableTrips] dep='{}', arr='{}', date='{}', time='{}', vehicle='{}', passengers={}",
                departureLocation, arrivalLocation, departureDate, preferredTime, vehicleType, passengerCount);

        List<Trip> candidates = findCandidateTrips(departureLocation, arrivalLocation, departureDate);

        // Filter and enrich
        LocalDateTime now = LocalDateTime.now();
        List<TripSearchResultDto> results = new ArrayList<>();
        int minSeats = (passengerCount != null && passengerCount > 0) ? passengerCount : 1;

        for (Trip trip : candidates) {
            if (trip.getStatus() != null && (trip.getStatus().equalsIgnoreCase("CANCELLED") || trip.getStatus().equalsIgnoreCase("COMPLETED"))) {
                continue;
            }
            if (trip.getDepartureTime() == null || trip.getDepartureTime().isBefore(now)) {
                continue;
            }

            int availableSeats = tripBookingAvailabilityService.getAvailableSeatCount(trip);
            if (availableSeats < minSeats) {
                continue;
            }

            boolean webOpen = tripBookingAvailabilityService.isWebBookingOpen(trip);

            // Filter preferred time if specified (e.g. 21:00 or "tối" -> >= 18:00)
            if (preferredTime != null && !preferredTime.isBlank()) {
                if (!matchesPreferredTime(trip.getDepartureTime().toLocalTime(), preferredTime)) {
                    continue;
                }
            }

            // Filter vehicle type if specified
            if (vehicleType != null && !vehicleType.isBlank()) {
                String vType = trip.getVehicle() != null ? trip.getVehicle().getType() : "";
                if (!matchesVehicleType(vType, vehicleType)) {
                    continue;
                }
            }

            TripSearchResultDto dto = TripSearchResultDto.builder()
                    .tripId(trip.getId())
                    .routeId(trip.getRoute() != null ? trip.getRoute().getId() : null)
                    .departureLocation(trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "")
                    .arrivalLocation(trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "")
                    .departureTime(trip.getDepartureTime())
                    .departureTimeFormatted(trip.getDepartureTime().format(TIME_FORMATTER))
                    .price(trip.getPrice())
                    .priceFormatted(CURRENCY_FORMATTER.format(trip.getPrice()) + " VNĐ")
                    .vehicleType(trip.getVehicle() != null ? trip.getVehicle().getType() : "Tiêu chuẩn")
                    .licensePlate(trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : "")
                    .totalSeats(trip.getVehicle() != null ? trip.getVehicle().getCapacity() : 40)
                    .availableSeats(availableSeats)
                    .webBookingOpen(webOpen)
                    .status(trip.getStatus() != null ? trip.getStatus() : "ACTIVE")
                    .build();

            results.add(dto);
        }

        // Sort by departure time ASC
        results.sort(Comparator.comparing(TripSearchResultDto::getDepartureTime));
        return results;
    }

    @Tool(name = "getTripDetails", description = "Lấy thông tin chi tiết một chuyến xe theo tripId gồm lộ trình, xe, giá và số chỗ trống.")
    public TripSearchResultDto getTripDetails(
            @ToolParam(description = "Mã định danh ID chuyến xe") Long tripId) {

        if (tripId == null) {
            throw new CustomException("Vui lòng cung cấp mã ID chuyến xe!", HttpStatus.BAD_REQUEST);
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe có mã ID: " + tripId, HttpStatus.NOT_FOUND));

        int availableSeats = tripBookingAvailabilityService.getAvailableSeatCount(trip);
        boolean webOpen = tripBookingAvailabilityService.isWebBookingOpen(trip);

        return TripSearchResultDto.builder()
                .tripId(trip.getId())
                .routeId(trip.getRoute() != null ? trip.getRoute().getId() : null)
                .departureLocation(trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "")
                .arrivalLocation(trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "")
                .departureTime(trip.getDepartureTime())
                .departureTimeFormatted(trip.getDepartureTime().format(TIME_FORMATTER))
                .price(trip.getPrice())
                .priceFormatted(CURRENCY_FORMATTER.format(trip.getPrice()) + " VNĐ")
                .vehicleType(trip.getVehicle() != null ? trip.getVehicle().getType() : "Tiêu chuẩn")
                .licensePlate(trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : "")
                .totalSeats(trip.getVehicle() != null ? trip.getVehicle().getCapacity() : 40)
                .availableSeats(availableSeats)
                .webBookingOpen(webOpen)
                .status(trip.getStatus() != null ? trip.getStatus() : "ACTIVE")
                .build();
    }

    @Tool(name = "getAvailableSeats", description = "Lấy danh sách các ghế còn trống thực tế của chuyến xe để người dùng lựa chọn.")
    public List<AvailableSeatDto> getAvailableSeats(
            @ToolParam(description = "Mã ID chuyến xe") Long tripId) {

        if (tripId == null) {
            throw new CustomException("Vui lòng cung cấp mã ID chuyến xe!", HttpStatus.BAD_REQUEST);
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe có mã ID: " + tripId, HttpStatus.NOT_FOUND));

        if (trip.getVehicle() == null) {
            return Collections.emptyList();
        }

        List<Seat> allSeats = seatRepository.findByVehicleId(trip.getVehicle().getId());
        List<Long> bookedSeatIds = ticketRepository.findBookedSeatIdsByTripId(tripId);
        List<Long> heldSeatIds = seatHoldRedisService.getHeldSeatIdsByTripId(tripId);

        Set<Long> unavailableSeatIds = new HashSet<>(bookedSeatIds);
        unavailableSeatIds.addAll(heldSeatIds);

        List<AvailableSeatDto> availableSeats = allSeats.stream()
                .filter(seat -> !unavailableSeatIds.contains(seat.getId()))
                .sorted(Comparator.comparing(Seat::getFloor).thenComparing((s1, s2) -> compareSeatNames(s1.getSeatName(), s2.getSeatName())))
                .map(seat -> AvailableSeatDto.builder()
                        .seatId(seat.getId())
                        .seatName(seat.getSeatName())
                        .floor(seat.getFloor())
                        .status("AVAILABLE")
                        .build())
                .collect(Collectors.toList());

        return availableSeats;
    }

    @Tool(name = "getTripSeatMap", description = "Lấy sơ đồ toàn bộ ghế thực tế của chuyến xe (bao gồm ghế trống, ghế đã đặt và ghế đang giữ).")
    public TripSeatMapDto getTripSeatMap(
            @ToolParam(description = "Mã ID chuyến xe") Long tripId,
            @ToolParam(description = "Số lượng khách cần đặt vé", required = false) Integer passengerCount) {

        if (tripId == null) {
            throw new CustomException("Vui lòng cung cấp mã ID chuyến xe!", HttpStatus.BAD_REQUEST);
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe có mã ID: " + tripId, HttpStatus.NOT_FOUND));

        if (trip.getVehicle() == null) {
            throw new CustomException("Chuyến xe này chưa được cấu hình phương tiện!", HttpStatus.BAD_REQUEST);
        }

        List<Seat> allSeats = new ArrayList<>(seatRepository.findByVehicleId(trip.getVehicle().getId()));
        List<Long> bookedSeatIds = ticketRepository.findBookedSeatIdsByTripId(tripId);
        List<Long> heldSeatIds = seatHoldRedisService.getHeldSeatIdsByTripId(tripId);

        Set<Long> bookedSet = new HashSet<>(bookedSeatIds != null ? bookedSeatIds : Collections.emptyList());
        Set<Long> heldSet = new HashSet<>(heldSeatIds != null ? heldSeatIds : Collections.emptyList());

        allSeats.sort(Comparator.comparing(Seat::getFloor)
                .thenComparing((s1, s2) -> compareSeatNames(s1.getSeatName(), s2.getSeatName())));

        List<AvailableSeatDto> seatDtos = new ArrayList<>();
        int availableCount = 0;
        int bookedCount = 0;
        int heldCount = 0;

        for (Seat seat : allSeats) {
            String status = "AVAILABLE";
            if (bookedSet.contains(seat.getId())) {
                status = "BOOKED";
                bookedCount++;
            } else if (heldSet.contains(seat.getId())) {
                status = "HELD";
                heldCount++;
            } else {
                availableCount++;
            }

            seatDtos.add(AvailableSeatDto.builder()
                    .seatId(seat.getId())
                    .seatName(seat.getSeatName())
                    .floor(seat.getFloor())
                    .status(status)
                    .price(trip.getPrice())
                    .priceFormatted(CURRENCY_FORMATTER.format(trip.getPrice()) + " VNĐ")
                    .build());
        }

        String routeName = (trip.getRoute() != null)
                ? trip.getRoute().getDepartureLocation() + " -> " + trip.getRoute().getArrivalLocation()
                : "Chuyến #" + tripId;

        int count = (passengerCount != null && passengerCount > 0) ? passengerCount : 1;

        return TripSeatMapDto.builder()
                .tripId(trip.getId())
                .routeName(routeName)
                .departureLocation(trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "")
                .arrivalLocation(trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "")
                .departureTimeFormatted(trip.getDepartureTime().format(TIME_FORMATTER))
                .vehicleType(trip.getVehicle().getType())
                .unitPrice(trip.getPrice())
                .unitPriceFormatted(CURRENCY_FORMATTER.format(trip.getPrice()) + " VNĐ")
                .passengerCount(count)
                .totalSeats(allSeats.size())
                .availableCount(availableCount)
                .bookedCount(bookedCount)
                .heldCount(heldCount)
                .seats(seatDtos)
                .build();
    }

    @Tool(name = "suggestAdjacentSeats", description = "Đề xuất các ghế trống cạnh nhau trong cùng một tầng dựa theo sơ đồ ghế thực tế của chuyến xe.")
    public List<AvailableSeatDto> suggestAdjacentSeats(
            @ToolParam(description = "Mã ID chuyến xe") Long tripId,
            @ToolParam(description = "Số lượng ghế cần tìm liền kề (thường là 2)") int seatCount,
            @ToolParam(description = "Tầng mong muốn: 1 hoặc 2 (tùy chọn)", required = false) Integer preferredFloor) {

        if (seatCount <= 0) seatCount = 2;
        List<AvailableSeatDto> availableSeats = getAvailableSeats(tripId);
        if (availableSeats.isEmpty() || availableSeats.size() < seatCount) {
            return Collections.emptyList();
        }

        Map<Integer, List<AvailableSeatDto>> byFloor = availableSeats.stream()
                .collect(Collectors.groupingBy(AvailableSeatDto::getFloor));

        List<Integer> floorOrder = new ArrayList<>();
        if (preferredFloor != null && preferredFloor > 0) {
            floorOrder.add(preferredFloor);
            floorOrder.add(preferredFloor == 1 ? 2 : 1);
        } else {
            floorOrder.add(1);
            floorOrder.add(2);
        }

        for (Integer floor : floorOrder) {
            List<AvailableSeatDto> floorSeats = byFloor.getOrDefault(floor, Collections.emptyList());
            if (floorSeats.size() < seatCount) continue;

            // Strategy 1: Consecutive seats with identical prefix (e.g. A1 & A2, or B3 & B4)
            Map<String, List<AvailableSeatDto>> byPrefix = floorSeats.stream()
                    .collect(Collectors.groupingBy(s -> extractSeatPrefix(s.getSeatName())));

            for (Map.Entry<String, List<AvailableSeatDto>> entry : byPrefix.entrySet()) {
                List<AvailableSeatDto> prefixSeats = new ArrayList<>(entry.getValue());
                if (prefixSeats.size() < seatCount) continue;
                prefixSeats.sort(Comparator.comparingInt(s -> extractSeatDigits(s.getSeatName())));

                for (int i = 0; i <= prefixSeats.size() - seatCount; i++) {
                    boolean consecutive = true;
                    for (int j = 0; j < seatCount - 1; j++) {
                        int num1 = extractSeatDigits(prefixSeats.get(i + j).getSeatName());
                        int num2 = extractSeatDigits(prefixSeats.get(i + j + 1).getSeatName());
                        if (num2 - num1 != 1) {
                            consecutive = false;
                            break;
                        }
                    }
                    if (consecutive) {
                        return new ArrayList<>(prefixSeats.subList(i, i + seatCount));
                    }
                }
            }

            // Strategy 2: Paired seats on same row across side-by-side columns (e.g. A1 & B1)
            if (seatCount == 2) {
                Map<Integer, List<AvailableSeatDto>> byRowNum = floorSeats.stream()
                        .collect(Collectors.groupingBy(s -> extractSeatDigits(s.getSeatName())));
                List<Integer> sortedRowNums = new ArrayList<>(byRowNum.keySet());
                Collections.sort(sortedRowNums);

                for (Integer rowNum : sortedRowNums) {
                    List<AvailableSeatDto> rowSeats = byRowNum.get(rowNum);
                    if (rowSeats != null && rowSeats.size() >= 2) {
                        rowSeats.sort(Comparator.comparing(AvailableSeatDto::getSeatName));
                        return new ArrayList<>(rowSeats.subList(0, 2));
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    public static int compareSeatNames(String name1, String name2) {
        if (name1 == null && name2 == null) return 0;
        if (name1 == null) return -1;
        if (name2 == null) return 1;

        String prefix1 = extractSeatPrefix(name1);
        String prefix2 = extractSeatPrefix(name2);
        int prefixCmp = prefix1.compareToIgnoreCase(prefix2);
        if (prefixCmp != 0) {
            return prefixCmp;
        }

        int num1 = extractSeatDigits(name1);
        int num2 = extractSeatDigits(name2);
        return Integer.compare(num1, num2);
    }

    private static String extractSeatPrefix(String name) {
        if (name == null) return "";
        return name.replaceAll("[0-9]", "").trim().toUpperCase(Locale.ROOT);
    }

    private static int extractSeatDigits(String name) {
        if (name == null) return 999;
        String digits = name.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 999;
        }
    }

    @Tool(name = "calculateBookingPrice", description = "Tính toán tổng tiền vé dựa trên đơn giá chuyến xe và số lượng ghế đặt.")
    public Map<String, Object> calculateBookingPrice(
            @ToolParam(description = "Mã ID chuyến xe") Long tripId,
            @ToolParam(description = "Số lượng vé hoặc ghế cần đặt") int seatCount) {

        if (tripId == null || seatCount <= 0) {
            throw new CustomException("Tham số tính giá không hợp lệ!", HttpStatus.BAD_REQUEST);
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe!", HttpStatus.NOT_FOUND));

        BigDecimal unitPrice = trip.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(seatCount));

        Map<String, Object> calc = new LinkedHashMap<>();
        calc.put("tripId", tripId);
        calc.put("seatCount", seatCount);
        calc.put("unitPrice", unitPrice);
        calc.put("unitPriceFormatted", CURRENCY_FORMATTER.format(unitPrice) + " VNĐ");
        calc.put("totalPrice", totalPrice);
        calc.put("totalPriceFormatted", CURRENCY_FORMATTER.format(totalPrice) + " VNĐ");
        return calc;
    }

    @Tool(name = "prepareBooking", description = "Chuẩn bị bản tóm tắt đặt vé và kiểm tra sơ bộ tình trạng ghế trước khi người dùng xác nhận. KHÔNG tạo booking chính thức ở bước này.")
    public BookingDraftDto prepareBooking(
            @ToolParam(description = "Mã ID chuyến xe") Long tripId,
            @ToolParam(description = "Danh sách ID các ghế muốn đặt") List<Long> seatIds) {

        if (tripId == null) {
            throw new CustomException("Vui lòng cung cấp mã ID chuyến xe!", HttpStatus.BAD_REQUEST);
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new CustomException("Vui lòng chọn ít nhất một ghế để chuẩn bị đơn đặt vé!", HttpStatus.BAD_REQUEST);
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException("Không tìm thấy chuyến xe!", HttpStatus.NOT_FOUND));

        // Validate web booking open
        tripBookingAvailabilityService.ensureWebBookingOpen(trip);

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new CustomException("Một hoặc nhiều ghế được chọn không tồn tại trên hệ thống!", HttpStatus.BAD_REQUEST);
        }

        // Validate seats belong to vehicle
        for (Seat seat : seats) {
            if (!seat.getVehicle().getId().equals(trip.getVehicle().getId())) {
                throw new CustomException("Ghế " + seat.getSeatName() + " không thuộc phương tiện của chuyến xe này!", HttpStatus.BAD_REQUEST);
            }
        }

        // Check if seats already booked in DB
        List<Ticket> bookedTickets = ticketRepository.findBookedTicketsByTripIdAndSeatIds(tripId, seatIds);
        if (!bookedTickets.isEmpty()) {
            List<String> bookedNames = bookedTickets.stream().map(t -> t.getSeat().getSeatName()).toList();
            throw new CustomException("Ghế (" + String.join(", ", bookedNames) + ") đã có người mua!", HttpStatus.CONFLICT);
        }

        // Check if seats currently held in Redis
        List<Long> heldSeatIds = seatHoldRedisService.getHeldSeatIdsByTripId(tripId);
        List<String> heldNames = seats.stream()
                .filter(s -> heldSeatIds.contains(s.getId()))
                .map(Seat::getSeatName)
                .toList();
        if (!heldNames.isEmpty()) {
            throw new CustomException("Ghế (" + String.join(", ", heldNames) + ") đang trong thời gian giữ chỗ của khách hàng khác!", HttpStatus.CONFLICT);
        }

        Customer currentCustomer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = currentCustomer != null ? currentCustomer.getId() : null;

        BigDecimal totalPrice = trip.getPrice().multiply(BigDecimal.valueOf(seats.size()));
        String idempotencyKey = UUID.randomUUID().toString();
        List<String> seatNames = seats.stream().map(Seat::getSeatName).sorted().toList();

        String routeName = (trip.getRoute() != null)
                ? trip.getRoute().getDepartureLocation() + " -> " + trip.getRoute().getArrivalLocation()
                : "Chuyến #" + tripId;

        String summary = String.format(
                "📋 **TÓM TẮT ĐƠN ĐẶT VÉ CHỜ XÁC NHẬN**\n" +
                "- Tuyến: %s\n" +
                "- Giờ khởi hành: %s\n" +
                "- Xe: %s\n" +
                "- Ghế chọn (%d ghế): %s\n" +
                "- Đơn giá: %s VNĐ/vé\n" +
                "- **Tổng thanh toán: %s VNĐ**\n\n" +
                "⚠️ Quý khách vui lòng kiểm tra kỹ và **XÁC NHẬN** để hệ thống tạo đơn và giữ chỗ trong 5 phút.",
                routeName,
                trip.getDepartureTime().format(TIME_FORMATTER),
                trip.getVehicle() != null ? trip.getVehicle().getType() : "Tiêu chuẩn",
                seats.size(),
                String.join(", ", seatNames),
                CURRENCY_FORMATTER.format(trip.getPrice()),
                CURRENCY_FORMATTER.format(totalPrice)
        );

        BookingDraftDto draft = BookingDraftDto.builder()
                .idempotencyKey(idempotencyKey)
                .customerId(customerId)
                .tripId(tripId)
                .routeName(routeName)
                .departureLocation(trip.getRoute() != null ? trip.getRoute().getDepartureLocation() : "")
                .arrivalLocation(trip.getRoute() != null ? trip.getRoute().getArrivalLocation() : "")
                .departureTime(trip.getDepartureTime())
                .departureTimeFormatted(trip.getDepartureTime().format(TIME_FORMATTER))
                .seatIds(seatIds)
                .seatNames(seatNames)
                .pricePerTicket(trip.getPrice())
                .totalPrice(totalPrice)
                .totalPriceFormatted(CURRENCY_FORMATTER.format(totalPrice) + " VNĐ")
                .vehicleType(trip.getVehicle() != null ? trip.getVehicle().getType() : "")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .requiresConfirmation(true)
                .summaryMessage(summary)
                .build();

        bookingDraftManager.saveDraft(draft);
        return draft;
    }

    @Tool(name = "confirmBooking", description = "Chính thức tạo đơn đặt vé khi người dùng ĐÃ XÁC NHẬN ĐỒNG Ý rõ ràng. Bắt buộc người dùng phải đăng nhập tài khoản.")
    public BookingConfirmationResultDto confirmBooking(
            @ToolParam(description = "Mã ID chuyến xe") Long tripId,
            @ToolParam(description = "Danh sách ID các ghế muốn đặt") List<Long> seatIds,
            @ToolParam(description = "Mã xác nhận idempotencyKey từ bước prepareBooking", required = false) String idempotencyKey) {

        log.info("[BookingTool:confirmBooking] tripId={}, seatIds={}, idempotencyKey={}", tripId, seatIds, idempotencyKey);

        // Security check: Customer MUST be authenticated. NEVER trust userId from LLM!
        Customer customer = currentUserService.getOptionalCurrentCustomer()
                .orElseThrow(() -> new CustomException("Quý khách cần đăng nhập tài khoản trước khi xác nhận đặt vé!", HttpStatus.UNAUTHORIZED));

        // Check idempotency retry
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<BookingConfirmationResultDto> existing = bookingDraftManager.getIdempotentResult(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent retry detected for key: {}. Returning existing booking without duplicate.", idempotencyKey);
                BookingConfirmationResultDto res = existing.get();
                res.setAlreadyExisting(true);
                return res;
            }
        }

        // Validate draft if exists
        Optional<BookingDraftDto> draftOpt = bookingDraftManager.getDraft(customer.getId(), idempotencyKey);
        if (draftOpt.isEmpty()) {
            draftOpt = bookingDraftManager.getLatestDraftForCustomer(customer.getId());
        }

        if (draftOpt.isPresent()) {
            BookingDraftDto draft = draftOpt.get();
            if (tripId == null) tripId = draft.getTripId();
            if (seatIds == null || seatIds.isEmpty()) seatIds = draft.getSeatIds();
        }

        if (tripId == null || seatIds == null || seatIds.isEmpty()) {
            throw new CustomException("Thông tin chuyến hoặc ghế chưa đầy đủ để tạo đơn!", HttpStatus.BAD_REQUEST);
        }

        // Call the real business booking service
        Booking booking = bookingService.createBooking(customer.getId(), tripId, seatIds);

        List<String> seatNames = booking.getSeats();
        String departureTimeStr = (booking.getTrip() != null && booking.getTrip().getDepartureTime() != null)
                ? booking.getTrip().getDepartureTime().format(TIME_FORMATTER) : "";
        String routeName = (booking.getTrip() != null && booking.getTrip().getRoute() != null)
                ? booking.getTrip().getRoute().getDepartureLocation() + " -> " + booking.getTrip().getRoute().getArrivalLocation()
                : "Chuyến #" + tripId;

        BookingConfirmationResultDto result = BookingConfirmationResultDto.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status("PENDING_PAYMENT")
                .totalPrice(booking.getTotalPrice())
                .totalPriceFormatted(CURRENCY_FORMATTER.format(booking.getTotalPrice()) + " VNĐ")
                .tripId(tripId)
                .routeName(routeName)
                .departureTimeFormatted(departureTimeStr)
                .seatNames(seatNames)
                .holdMinutes(5)
                .paymentInstructions("Đơn đặt vé đã tạo thành công và giữ chỗ trong 5 phút. Vui lòng thanh toán trực tuyến qua cổng VNPay để nhận vé điện tử.")
                .isAlreadyExisting(false)
                .build();

        // Register idempotency
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            bookingDraftManager.registerIdempotentResult(idempotencyKey, result);
            bookingDraftManager.removeDraft(customer.getId(), idempotencyKey);
        }

        return result;
    }

    @Tool(name = "getBookingStatus", description = "Tra cứu tình trạng một đơn đặt vé của khách hàng theo mã ID đơn đặt vé.")
    public Map<String, Object> getBookingStatus(
            @ToolParam(description = "Mã ID đơn đặt vé") Long bookingId) {

        if (bookingId == null) {
            throw new CustomException("Vui lòng cung cấp mã ID đơn đặt vé!", HttpStatus.BAD_REQUEST);
        }

        Customer customer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = customer != null ? customer.getId() : null;

        Booking booking = bookingService.getBookingById(bookingId, customerId);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("bookingId", booking.getId());
        res.put("bookingCode", booking.getBookingCode());
        res.put("status", booking.getStatus());
        res.put("totalPrice", booking.getTotalPrice());
        res.put("totalPriceFormatted", CURRENCY_FORMATTER.format(booking.getTotalPrice()) + " VNĐ");
        res.put("seats", booking.getSeats());
        if (booking.getTrip() != null) {
            res.put("tripId", booking.getTrip().getId());
            if (booking.getTrip().getRoute() != null) {
                res.put("route", booking.getTrip().getRoute().getDepartureLocation() + " -> " + booking.getTrip().getRoute().getArrivalLocation());
            }
            if (booking.getTrip().getDepartureTime() != null) {
                res.put("departureTime", booking.getTrip().getDepartureTime().format(TIME_FORMATTER));
            }
        }
        res.put("createdAt", booking.getCreatedAt() != null ? booking.getCreatedAt().format(TIME_FORMATTER) : "");
        return res;
    }

    @Tool(name = "createDemoPayment", description = "Tạo mã QR và thông tin giao dịch thanh toán mô phỏng (Demo Payment) cho đơn đặt vé.")
    public DemoPaymentQrResponse createDemoPayment(
            @ToolParam(description = "Mã ID đơn đặt vé") Long bookingId) {
        if (demoPaymentProcessor.isEmpty()) {
            throw new CustomException("Dịch vụ thanh toán demo không khả dụng!", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Customer customer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = customer != null ? customer.getId() : null;
        return demoPaymentProcessor.get().createDemoPayment(bookingId, customerId);
    }

    @Tool(name = "confirmDemoPayment", description = "Xác nhận thanh toán mô phỏng thành công cho đơn đặt vé và phát hành vé điện tử.")
    public DemoPaymentConfirmResponse confirmDemoPayment(
            @ToolParam(description = "Mã ID đơn đặt vé") Long bookingId,
            @ToolParam(description = "Mã giao dịch transactionId", required = false) String transactionId) {
        if (demoPaymentProcessor.isEmpty()) {
            throw new CustomException("Dịch vụ thanh toán demo không khả dụng!", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Customer customer = currentUserService.getOptionalCurrentCustomer().orElse(null);
        UUID customerId = customer != null ? customer.getId() : null;
        return demoPaymentProcessor.get().processPayment(bookingId, transactionId, customerId);
    }

    // Helper methods
    private List<Trip> findCandidateTrips(String dep, String arr, String date) {
        String depNorm = locationNormalizer.normalize(dep);
        String arrNorm = locationNormalizer.normalize(arr);

        List<Trip> allTrips = Collections.emptyList();
        try {
            allTrips = tripRepository.findUpcomingTrips(java.time.LocalDateTime.now().minusMinutes(15), PageRequest.of(0, 200));
        } catch (Exception ignored) {}
        if (allTrips == null || allTrips.isEmpty()) {
            allTrips = tripRepository.findAll();
        }
        List<Trip> matched = new ArrayList<>();

        LocalDate targetDate = null;
        if (date != null && !date.isBlank()) {
            try {
                targetDate = LocalDate.parse(date.trim());
            } catch (Exception ignored) {}
        }

        for (Trip trip : allTrips) {
            if (trip.getRoute() == null) continue;
            String tripDepNorm = locationNormalizer.normalize(trip.getRoute().getDepartureLocation());
            String tripArrNorm = locationNormalizer.normalize(trip.getRoute().getArrivalLocation());

            boolean depMatch = depNorm.isBlank() || tripDepNorm.contains(depNorm) || depNorm.contains(tripDepNorm);
            boolean arrMatch = arrNorm.isBlank() || tripArrNorm.contains(arrNorm) || arrNorm.contains(tripArrNorm);

            if (depMatch && arrMatch) {
                if (targetDate != null) {
                    if (trip.getDepartureTime() != null && trip.getDepartureTime().toLocalDate().isEqual(targetDate)) {
                        matched.add(trip);
                    }
                } else {
                    matched.add(trip);
                }
            }
        }

        // Semantic fallback via VectorSearchService if direct normalization found no trips
        if (matched.isEmpty() && vectorSearchService.isPresent() && (dep != null || arr != null)) {
            try {
                String vectorQuery = (dep != null ? dep : "") + " " + (arr != null ? arr : "");
                VectorSearchRequest searchReq = new VectorSearchRequest(vectorQuery, 5, 0.25d, "ROUTE", null);
                List<VectorSearchResult> vResults = vectorSearchService.get().search(searchReq);
                Set<Long> routeIds = vResults.stream()
                        .map(r -> {
                            Object id = r.metadata().get("routeId");
                            if (id instanceof Number n) return n.longValue();
                            if (id instanceof String s) {
                                try { return Long.parseLong(s); } catch (Exception e) { return null; }
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (Long rId : routeIds) {
                    List<Trip> byRoute = tripRepository.findByRoute_Id(rId);
                    if (targetDate != null) {
                        final LocalDate finalTargetDate = targetDate;
                        matched.addAll(byRoute.stream()
                                .filter(t -> t.getDepartureTime() != null && t.getDepartureTime().toLocalDate().isEqual(finalTargetDate))
                                .toList());
                    } else {
                        matched.addAll(byRoute);
                    }
                }
            } catch (Exception e) {
                log.warn("Vector semantic lookup fallback encountered notice: {}", e.getMessage());
            }
        }

        return matched;
    }

    private boolean matchesPreferredTime(LocalTime tripTime, String preferredTime) {
        if (preferredTime == null || preferredTime.isBlank() || tripTime == null) return true;
        String p = preferredTime.toLowerCase(Locale.ROOT).trim();

        if (p.contains("sáng") || p.contains("morning")) {
            return tripTime.getHour() >= 4 && tripTime.getHour() < 12;
        }
        if (p.contains("trưa") || p.contains("noon")) {
            return tripTime.getHour() >= 11 && tripTime.getHour() < 14;
        }
        if (p.contains("chiều") || p.contains("afternoon")) {
            return tripTime.getHour() >= 13 && tripTime.getHour() < 18;
        }
        if (p.contains("tối") || p.contains("đêm") || p.contains("evening") || p.contains("night")) {
            return tripTime.getHour() >= 18 || tripTime.getHour() < 4;
        }

        // Try exact hour matching, e.g. "21", "21:00", "21h"
        try {
            String digits = p.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                int hour = Integer.parseInt(digits);
                if (hour <= 24) {
                    return Math.abs(tripTime.getHour() - hour) <= 2; // within 2 hour window
                }
            }
        } catch (Exception ignored) {}

        return true;
    }

    private boolean matchesVehicleType(String actualType, String requestedType) {
        if (requestedType == null || requestedType.isBlank() || actualType == null) return true;
        String a = actualType.toLowerCase(Locale.ROOT);
        String r = requestedType.toLowerCase(Locale.ROOT);
        if (r.contains("limousine") && a.contains("limousine")) return true;
        if (r.contains("giường nằm") && a.contains("giường")) return true;
        if (r.contains("ghế ngồi") && a.contains("ghế")) return true;
        return a.contains(r) || r.contains(a);
    }
}
