package vn.edu.iuh.nhom01_www.recommendation.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.aivector.VectorIngestionService;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
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
    private final UserSessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final UserActionRepository actionRepository;
    private final Optional<VectorIngestionService> vectorIngestionService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[DemoPersonaDataSeeder] Initializing rich demo routes, trips, and customer commuter personas...");
        boolean dataChanged = false;

        // 1. Ensure Reciprocal & Phú Yên Routes exist
        Route rHcmToPhuYen = getOrCreateRoute("TP.HCM", "Phú Yên", new BigDecimal("520"), 600, new BigDecimal("280000"));
        Route rPhuYenToHcm = getOrCreateRoute("Phú Yên", "TP.HCM", new BigDecimal("520"), 600, new BigDecimal("280000"));
        Route rNhaTrangToPhuYen = getOrCreateRoute("Nha Trang", "Phú Yên", new BigDecimal("120"), 150, new BigDecimal("140000"));
        Route rDaNangToPhuYen = getOrCreateRoute("Đà Nẵng", "Phú Yên", new BigDecimal("400"), 480, new BigDecimal("260000"));
        Route rDaLatToHcm = getOrCreateRoute("Đà Lạt", "TP.HCM", new BigDecimal("300"), 360, new BigDecimal("250000"));
        Route rNhaTrangToHcm = getOrCreateRoute("Nha Trang", "TP.HCM", new BigDecimal("430"), 500, new BigDecimal("350000"));
        Route rCanThoToHcm = getOrCreateRoute("Cần Thơ", "TP.HCM", new BigDecimal("170"), 200, new BigDecimal("160000"));

        List<Vehicle> vehicles = vehicleRepository.findAll();
        if (vehicles.isEmpty()) {
            log.warn("[DemoPersonaDataSeeder] No vehicles found, skipping trip seeding.");
            return;
        }

        Vehicle vLimousine = vehicles.stream().filter(v -> "Limousine".equalsIgnoreCase(v.getType())).findFirst().orElse(vehicles.get(0));
        Vehicle vGiuongNam = vehicles.stream().filter(v -> "Giường nằm".equalsIgnoreCase(v.getType())).findFirst().orElse(vehicles.get(0));

        // 2. Ensure Trips exist for Phú Yên and key return corridors
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
        if (tripRepository.findByRoute(rDaLatToHcm).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rDaLatToHcm, vLimousine, vGiuongNam, new BigDecimal("350000"), new BigDecimal("250000"));
        }
        if (tripRepository.findByRoute(rNhaTrangToHcm).isEmpty()) {
            dataChanged = true;
            seedTripsForRoute(rNhaTrangToHcm, vLimousine, vGiuongNam, new BigDecimal("420000"), new BigDecimal("350000"));
        }

        // 3. Seed Demo Commuter Customer Persona (Lưu Trung Nghĩa - Commuter TP.HCM <-> Phú Yên)
        Optional<Customer> nghiaCustomerOpt = customerRepository.findOptionalByEmail("luutrungnghia19012003@gmail.com");
        if (nghiaCustomerOpt.isPresent()) {
            Customer nghia = nghiaCustomerOpt.get();
            List<Booking> existingBookings = bookingRepository.findByCustomer_IdOrderByCreatedAtDesc(nghia.getId());
            boolean hasPhuYenBookings = existingBookings.stream().anyMatch(b ->
                    b.getTrip() != null && b.getTrip().getRoute() != null &&
                            (b.getTrip().getRoute().getDepartureLocation().contains("Phú Yên") || b.getTrip().getRoute().getArrivalLocation().contains("Phú Yên")));

            if (!hasPhuYenBookings) {
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

                // Seed user session with searches for Phú Yên
                UserSession session = sessionRepository.save(UserSession.builder()
                        .customerId(nghia.getId())
                        .anonymousId("anon-nghia-commuter")
                        .ipAddress("14.161.22.45")
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0")
                        .trafficSource("direct")
                        .startTime(LocalDateTime.now().minusHours(2))
                        .endTime(LocalDateTime.now().minusHours(1))
                        .totalDurationSeconds(300)
                        .build());

                PageView pv = pageViewRepository.save(PageView.builder()
                        .session(session)
                        .pageUrl("/search?from=TP.HCM&to=Phú+Yên")
                        .pageTitle("VEXE - Tìm vé xe Sài Gòn đi Phú Yên")
                        .enteredAt(LocalDateTime.now().minusHours(2))
                        .activeDurationSeconds(60)
                        .idleDurationSeconds(5)
                        .build());

                actionRepository.save(UserAction.builder()
                        .session(session)
                        .pageView(pv)
                        .actionType("SEARCH")
                        .payload("{\"departure\":\"TP.HCM\",\"destination\":\"Phú Yên\",\"type\":\"commuter\"}")
                        .createdAt(LocalDateTime.now().minusHours(2).plusSeconds(10))
                        .build());
            }
        }

        // 4. Ingest new routes & trips into pgvector if changes occurred
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

        Seat seat = trip.getVehicle().getSeats() != null && !trip.getVehicle().getSeats().isEmpty()
                ? trip.getVehicle().getSeats().get(0)
                : null;

        if (seat != null) {
            ticketRepository.save(Ticket.builder()
                    .ticketCode("VX" + System.currentTimeMillis() % 1000000)
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
