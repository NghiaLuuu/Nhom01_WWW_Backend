package vn.edu.iuh.nhom01_www.recommendation.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.iuh.nhom01_www.aivector.VectorIngestionService;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.repository.PageViewRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserActionRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserSessionRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class DemoPersonaDataSeeder implements CommandLineRunner {

    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final SeatRepository seatRepository;
    private final UserSessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final UserActionRepository actionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final Optional<VectorIngestionService> vectorIngestionService;

    @Override
    public void run(String... args) {
        log.info("[DemoPersonaDataSeeder] Initializing rich demo routes, trips, and customer personas...");
        boolean dataChanged = false;
        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            Boolean res = txTemplate.execute(status -> seedData());
            dataChanged = Boolean.TRUE.equals(res);
        } catch (Exception e) {
            log.error("[DemoPersonaDataSeeder] Error during seedData: ", e);
        }

        // 6. Ingest new routes & trips into pgvector if changes occurred (Outside @Transactional)
        if (dataChanged && vectorIngestionService.isPresent()) {
            try {
                log.info("[DemoPersonaDataSeeder] Re-indexing new trips and routes into pgvector vector_store...");
                vectorIngestionService.get().ingestAll();
            } catch (Exception e) {
                log.warn("[DemoPersonaDataSeeder] pgvector ingestion notice: {}", e.getMessage());
            }
        }

        log.info("[DemoPersonaDataSeeder] Ready! Total routes in system: {}, Total trips: {}",
                routeRepository.count(), tripRepository.count());
    }

    @Transactional
    public boolean seedData() {
        boolean dataChanged = false;

        // 1. Ensure Reciprocal & Popular Routes exist
        Route rHcmToDalat = getOrCreateRoute("TP.HCM", "Đà Lạt", new BigDecimal("300"), 360, new BigDecimal("250000"));
        Route rDalatToHcm = getOrCreateRoute("Đà Lạt", "TP.HCM", new BigDecimal("300"), 360, new BigDecimal("250000"));
        Route rHcmToPhuYen = getOrCreateRoute("TP.HCM", "Phú Yên", new BigDecimal("520"), 600, new BigDecimal("280000"));
        Route rPhuYenToHcm = getOrCreateRoute("Phú Yên", "TP.HCM", new BigDecimal("520"), 600, new BigDecimal("280000"));
        Route rNhaTrangToPhuYen = getOrCreateRoute("Nha Trang", "Phú Yên", new BigDecimal("120"), 150, new BigDecimal("140000"));
        Route rDaNangToPhuYen = getOrCreateRoute("Đà Nẵng", "Phú Yên", new BigDecimal("400"), 480, new BigDecimal("260000"));
        Route rNhaTrangToHcm = getOrCreateRoute("Nha Trang", "TP.HCM", new BigDecimal("430"), 500, new BigDecimal("350000"));
        Route rCanThoToHcm = getOrCreateRoute("Cần Thơ", "TP.HCM", new BigDecimal("170"), 200, new BigDecimal("160000"));

        List<Vehicle> vehicles = vehicleRepository.findAll();
        if (vehicles.isEmpty()) {
            log.warn("[DemoPersonaDataSeeder] No vehicles found, skipping trip seeding.");
            return false;
        }

        Vehicle vLimousine = vehicles.stream()
                .filter(v -> "Limousine".equalsIgnoreCase(v.getType()) || (v.getType() != null && v.getType().toLowerCase().contains("limousine")))
                .findFirst().orElse(vehicles.get(0));
        Vehicle vGiuongNam = vehicles.stream()
                .filter(v -> "Giường nằm".equalsIgnoreCase(v.getType()) || (v.getType() != null && v.getType().toLowerCase().contains("giường")))
                .findFirst().orElse(vehicles.get(0));

        // 2. Ensure Trips exist for Đà Lạt, Phú Yên and key return corridors
        if (tripRepository.findByRoute(rHcmToDalat).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rHcmToDalat, vLimousine, vGiuongNam, new BigDecimal("350000"), new BigDecimal("250000"));
        }
        if (tripRepository.findByRoute(rDalatToHcm).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rDalatToHcm, vLimousine, vGiuongNam, new BigDecimal("350000"), new BigDecimal("250000"));
        }
        if (tripRepository.findByRoute(rHcmToPhuYen).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rHcmToPhuYen, vLimousine, vGiuongNam, new BigDecimal("380000"), new BigDecimal("280000"));
        }
        if (tripRepository.findByRoute(rPhuYenToHcm).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rPhuYenToHcm, vLimousine, vGiuongNam, new BigDecimal("380000"), new BigDecimal("280000"));
        }
        if (tripRepository.findByRoute(rNhaTrangToPhuYen).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rNhaTrangToPhuYen, vGiuongNam, vGiuongNam, new BigDecimal("160000"), new BigDecimal("140000"));
        }
        if (tripRepository.findByRoute(rDaNangToPhuYen).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rDaNangToPhuYen, vGiuongNam, vGiuongNam, new BigDecimal("280000"), new BigDecimal("260000"));
        }
        if (tripRepository.findByRoute(rNhaTrangToHcm).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rNhaTrangToHcm, vLimousine, vGiuongNam, new BigDecimal("420000"), new BigDecimal("350000"));
        }

        // 3. Seed Demo Account 1: Hoàng Kim Oanh (VIP Limousine Đà Lạt)
        Customer hoangKim = getOrCreateCustomer(
                "Hoàng Kim Oanh",
                "hoang.kim.dalat@gmail.com",
                "0938123456",
                "123456",
                "Quận 1, TP. Hồ Chí Minh"
        );
        List<Booking> hkBookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(hoangKim.getId());
        if (hkBookings.size() < 6) {
            dataChanged = true;
            log.info("[DemoPersonaDataSeeder] Seeding 7 past VIP Limousine bookings for Hoàng Kim Oanh...");
            List<Trip> hcmToDalatTrips = tripRepository.findByRoute(rHcmToDalat);
            List<Trip> dalatToHcmTrips = tripRepository.findByRoute(rDalatToHcm);

            if (!hcmToDalatTrips.isEmpty() && !dalatToHcmTrips.isEmpty()) {
                Trip tHcmDl = hcmToDalatTrips.get(0);
                Trip tDlHcm = dalatToHcmTrips.get(0);

                createPastCompletedBooking(hoangKim, tHcmDl, LocalDateTime.now().minusDays(50));
                createPastCompletedBooking(hoangKim, tDlHcm, LocalDateTime.now().minusDays(47));
                createPastCompletedBooking(hoangKim, tHcmDl, LocalDateTime.now().minusDays(35));
                createPastCompletedBooking(hoangKim, tDlHcm, LocalDateTime.now().minusDays(32));
                createPastCompletedBooking(hoangKim, tHcmDl, LocalDateTime.now().minusDays(20));
                createPastCompletedBooking(hoangKim, tDlHcm, LocalDateTime.now().minusDays(17));
                createPastCompletedBooking(hoangKim, tHcmDl, LocalDateTime.now().minusDays(5));
            }

            // Tracking session: searches & filters for Đà Lạt Limousine
            UserSession sessionHk = sessionRepository.save(UserSession.builder()
                    .customerId(hoangKim.getId())
                    .anonymousId("anon-hoangkim-dalat-vip")
                    .ipAddress("113.161.45.89")
                    .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148")
                    .trafficSource("facebook")
                    .utmCampaign("dalat-limousine-vip")
                    .startTime(LocalDateTime.now().minusHours(3))
                    .endTime(LocalDateTime.now().minusHours(2).minusMinutes(10))
                    .totalDurationSeconds(3000)
                    .build());

            PageView pvHk1 = pageViewRepository.save(PageView.builder()
                    .session(sessionHk)
                    .pageUrl("/search?from=TP.HCM&to=Đà+Lạt&vehicle=Limousine")
                    .pageTitle("VEXE - Đặt Vé Xe Limousine VIP Sài Gòn đi Đà Lạt")
                    .enteredAt(LocalDateTime.now().minusHours(3))
                    .activeDurationSeconds(120)
                    .idleDurationSeconds(10)
                    .build());

            actionRepository.save(UserAction.builder()
                    .session(sessionHk)
                    .pageView(pvHk1)
                    .actionType("SEARCH")
                    .payload("{\"departure\":\"TP.HCM\",\"destination\":\"Đà Lạt\",\"vehicleType\":\"Limousine VIP\"}")
                    .createdAt(LocalDateTime.now().minusHours(3).plusSeconds(15))
                    .build());

            actionRepository.save(UserAction.builder()
                    .session(sessionHk)
                    .pageView(pvHk1)
                    .actionType("FILTER_VEHICLE")
                    .payload("{\"type\":\"Limousine 34 phòng\",\"class\":\"VIP\"}")
                    .createdAt(LocalDateTime.now().minusHours(3).plusSeconds(45))
                    .build());

            PageView pvHk2 = pageViewRepository.save(PageView.builder()
                    .session(sessionHk)
                    .pageUrl("/checkout")
                    .pageTitle("VEXE - Thanh Toán Đặt Vé Limousine")
                    .enteredAt(LocalDateTime.now().minusHours(3).plusMinutes(2))
                    .activeDurationSeconds(80)
                    .idleDurationSeconds(5)
                    .build());

            actionRepository.save(UserAction.builder()
                    .session(sessionHk)
                    .pageView(pvHk2)
                    .actionType("SELECT_SEAT")
                    .payload("{\"seat\":\"VIP-A01\",\"price\":350000}")
                    .createdAt(LocalDateTime.now().minusHours(3).plusMinutes(2).plusSeconds(30))
                    .build());
        }

        // 4. Seed Demo Account 2: Trần Minh Tuấn (Commuter TP.HCM <-> Phú Yên)
        Customer minhTuan = getOrCreateCustomer(
                "Trần Minh Tuấn",
                "minh.tuan.phuyen@gmail.com",
                "0909654321",
                "123456",
                "TP. Tuy Hòa, Tỉnh Phú Yên"
        );
        List<Booking> mtBookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(minhTuan.getId());
        if (mtBookings.size() < 6) {
            dataChanged = true;
            log.info("[DemoPersonaDataSeeder] Seeding 8 past commuter bookings for Trần Minh Tuấn (TP.HCM <-> Phú Yên)...");
            List<Trip> hcmToPyTrips = tripRepository.findByRoute(rHcmToPhuYen);
            List<Trip> pyToHcmTrips = tripRepository.findByRoute(rPhuYenToHcm);

            if (!hcmToPyTrips.isEmpty() && !pyToHcmTrips.isEmpty()) {
                Trip tHcmPy = hcmToPyTrips.get(0);
                Trip tPyHcm = pyToHcmTrips.get(0);

                createPastCompletedBooking(minhTuan, tHcmPy, LocalDateTime.now().minusDays(52));
                createPastCompletedBooking(minhTuan, tPyHcm, LocalDateTime.now().minusDays(49));
                createPastCompletedBooking(minhTuan, tHcmPy, LocalDateTime.now().minusDays(38));
                createPastCompletedBooking(minhTuan, tPyHcm, LocalDateTime.now().minusDays(35));
                createPastCompletedBooking(minhTuan, tHcmPy, LocalDateTime.now().minusDays(24));
                createPastCompletedBooking(minhTuan, tPyHcm, LocalDateTime.now().minusDays(21));
                createPastCompletedBooking(minhTuan, tHcmPy, LocalDateTime.now().minusDays(10));
                createPastCompletedBooking(minhTuan, tPyHcm, LocalDateTime.now().minusDays(7));
            }

            // Tracking session: searches for Phú Yên commuter
            UserSession sessionMt = sessionRepository.save(UserSession.builder()
                    .customerId(minhTuan.getId())
                    .anonymousId("anon-minhtuan-phuyen-commuter")
                    .ipAddress("14.161.22.77")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0")
                    .trafficSource("direct")
                    .startTime(LocalDateTime.now().minusHours(4))
                    .endTime(LocalDateTime.now().minusHours(3).minusMinutes(20))
                    .totalDurationSeconds(2400)
                    .build());

            PageView pvMt = pageViewRepository.save(PageView.builder()
                    .session(sessionMt)
                    .pageUrl("/search?from=TP.HCM&to=Phú+Yên")
                    .pageTitle("VEXE - Tìm vé xe Sài Gòn đi Phú Yên")
                    .enteredAt(LocalDateTime.now().minusHours(4))
                    .activeDurationSeconds(150)
                    .idleDurationSeconds(10)
                    .build());

            actionRepository.save(UserAction.builder()
                    .session(sessionMt)
                    .pageView(pvMt)
                    .actionType("SEARCH")
                    .payload("{\"departure\":\"TP.HCM\",\"destination\":\"Phú Yên\",\"type\":\"commuter\",\"return\":\"weekend\"}")
                    .createdAt(LocalDateTime.now().minusHours(4).plusSeconds(20))
                    .build());
        }

        // 5. Existing Commuter Persona (Lưu Trung Nghĩa - Commuter TP.HCM <-> Phú Yên)
        Optional<Customer> nghiaCustomerOpt = customerRepository.findOptionalByEmail("luutrungnghia19012003@gmail.com");
        if (nghiaCustomerOpt.isPresent()) {
            Customer nghia = nghiaCustomerOpt.get();
            List<Booking> existingBookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(nghia.getId());
            if (existingBookings.size() < 4) {
                dataChanged = true;
                log.info("[DemoPersonaDataSeeder] Seeding commuter booking history for Lưu Trung Nghĩa (TP.HCM <-> Phú Yên)...");
                List<Trip> hcmToPyTrips = tripRepository.findByRoute(rHcmToPhuYen);
                List<Trip> pyToHcmTrips = tripRepository.findByRoute(rPhuYenToHcm);

                if (!hcmToPyTrips.isEmpty() && !pyToHcmTrips.isEmpty()) {
                    createPastCompletedBooking(nghia, hcmToPyTrips.get(0), LocalDateTime.now().minusDays(20));
                    createPastCompletedBooking(nghia, pyToHcmTrips.get(0), LocalDateTime.now().minusDays(15));
                    createPastCompletedBooking(nghia, hcmToPyTrips.get(0), LocalDateTime.now().minusDays(7));
                    createPastCompletedBooking(nghia, pyToHcmTrips.get(0), LocalDateTime.now().minusDays(2));
                }
            }
        }

        return dataChanged;
    }

    private Customer getOrCreateCustomer(String fullName, String email, String phone, String rawPassword, String address) {
        Customer customer = customerRepository.findByEmail(email);
        if (customer == null) {
            customer = Customer.builder()
                    .fullName(fullName)
                    .email(email)
                    .phoneNumber(phone)
                    .password(passwordEncoder.encode(rawPassword))
                    .address(address)
                    .status("ACTIVE")
                    .build();
            return customerRepository.save(customer);
        }
        customer.setFullName(fullName);
        customer.setPhoneNumber(phone);
        customer.setPassword(passwordEncoder.encode(rawPassword));
        customer.setStatus("ACTIVE");
        return customerRepository.save(customer);
    }

    private Route getOrCreateRoute(String from, String to, BigDecimal distance, Integer duration, BigDecimal basePrice) {
        return routeRepository.findByDepartureLocationIgnoreCaseAndArrivalLocationIgnoreCase(from, to)
                .orElseGet(() -> routeRepository.save(Route.builder()
                        .departureLocation(from)
                        .arrivalLocation(to)
                        .distanceKm(distance)
                        .durationMinutes(duration)
                        .basePrice(basePrice)
                        .status(true)
                        .build()));
    }

    private void seedTripsForRoute(Route route, Vehicle v1, Vehicle v2, BigDecimal price1, BigDecimal price2) {
        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(18).withMinute(30).withSecond(0).withNano(0);

        for (int day = 0; day < 5; day++) {
            LocalDateTime t1 = base.plusDays(day).withHour(18).withMinute(30);
            LocalDateTime t2 = base.plusDays(day).withHour(20).withMinute(45);

            tripRepository.save(Trip.builder()
                    .route(route)
                    .vehicle(v1)
                    .departureTime(t1)
                    .price(price1)
                    .status("PENDING")
                    .build());

            tripRepository.save(Trip.builder()
                    .route(route)
                    .vehicle(v2)
                    .departureTime(t2)
                    .price(price2)
                    .status("PENDING")
                    .build());
        }
    }

    private void createPastCompletedBooking(Customer customer, Trip trip, LocalDateTime bookingTime) {
        Booking booking = bookingRepository.save(Booking.builder()
                .customer(customer)
                .trip(trip)
                .status("PAID")
                .totalPrice(trip.getPrice())
                .createdAt(bookingTime)
                .build());

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .amount(trip.getPrice())
                .paymentMethod("VNPAY")
                .status("SUCCESS")
                .transactionId(UUID.randomUUID().toString())
                .createdAt(bookingTime)
                .build());

        List<Seat> seats = seatRepository.findByVehicleId(trip.getVehicle().getId());
        Seat seat = (seats != null && !seats.isEmpty()) ? seats.get(0) : null;

        if (seat != null) {
            ticketRepository.save(Ticket.builder()
                    .ticketCode("VX" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                    .booking(booking)
                    .seat(seat)
                    .status("USED")
                    .price(trip.getPrice())
                    .usedAt(bookingTime.plusHours(10))
                    .createdAt(bookingTime)
                    .build());
        }
    }
}
