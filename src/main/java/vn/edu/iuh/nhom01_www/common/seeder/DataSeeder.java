/**
 * Lệnh chạy Seeder trực tiếp từ Terminal (bấm Ctrl+C để thoát sau khi chạy xong):
 * .\mvnw spring-boot:run "-Dspring-boot.run.arguments=--seed"
 */
package vn.edu.iuh.nhom01_www.common.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.chatbot.model.TableMetadata;
import vn.edu.iuh.nhom01_www.chatbot.repository.TableMetadataRepository;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.trip.repository.TripRepository;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;
import vn.edu.iuh.nhom01_www.vehicle.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

        private final RoleRepository roleRepository;
        private final PermissionRepository permissionRepository;
        private final StaffRepository staffRepository;
        private final CustomerRepository customerRepository;
        private final RouteRepository routeRepository;
        private final VehicleRepository vehicleRepository;
        private final TripRepository tripRepository;
        private final BookingRepository bookingRepository;
        private final PaymentRepository paymentRepository;
        private final TicketRepository ticketRepository;
        private final TableMetadataRepository tableMetadataRepository;
        private final PasswordEncoder passwordEncoder;
        private final java.util.Optional<vn.edu.iuh.nhom01_www.aivector.VectorIngestionService> vectorIngestionService;

        private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                boolean shouldSeed = Arrays.stream(args).anyMatch(arg -> arg.contains("seed"));
                if (!shouldSeed) {
                        return;
                }

                try {
                        log.info("--- BẮT ĐẦU CHẠY SEEDER DỮ LIỆU MẪU LỚN ---");
                        clearDatabase();
                        seedData();
                        log.info("--- HOÀN TẤT SEEDER DỮ LIỆU MẪU LỚN. CHẠY THÀNH CÔNG! ---");
                } catch (Exception e) {
                        log.error("--- LỖI KHI CHẠY SEEDER, DỪNG CHẠY VÀ KHÔI PHỤC (ROLLBACK): {} ---", e.getMessage());
                        throw e;
                }
        }

        private void clearDatabase() {
                log.info("Đang xóa dữ liệu cũ (Sử dụng TRUNCATE CASCADE để tránh lỗi khóa ngoại)...");
                try {
                    jdbcTemplate.execute(
                                    "TRUNCATE TABLE refresh_tokens, cancel_requests, payments, tickets, bookings, trips, seats, vehicles, routes, staff_permissions, staffs, customers, role_permissions, roles, permissions, table_metadata, audit_logs RESTART IDENTITY CASCADE");
                } catch (Exception e) {
                    log.warn("Lỗi khi TRUNCATE: {}", e.getMessage());
                }
                log.info("Xóa dữ liệu cũ thành công!");
        }

        private void seedData() {
                // 1. Permissions & Roles
                log.info("1. Đang tạo Permissions và Roles...");
                Permission pStaffManage = permissionRepository
                                .save(Permission.builder().code("STAFF_MANAGE").description("Quản lý nhân viên")
                                                .build());
                Permission pCustomerManage = permissionRepository
                                .save(Permission.builder().code("CUSTOMER_MANAGE").description("Quản lý khách hàng")
                                                .build());
                Permission pTripManage = permissionRepository
                                .save(Permission.builder().code("TRIP_MANAGE").description("Quản lý chuyến xe")
                                                .build());
                Permission pTicketManage = permissionRepository
                                .save(Permission.builder().code("TICKET_MANAGE").description("Quản lý vé xe").build());
                Permission pManageTicket = permissionRepository
                                .save(Permission.builder().code("MANAGE_TICKET").description("Quản lý đặt vé").build());
                Permission pStats = permissionRepository
                                .save(Permission.builder().code("VIEW_STATISTICS").description("Xem thống kê").build());
                Permission pRouteManage = permissionRepository
                                .save(Permission.builder().code("ROUTE_MANAGE").description("Quản lý tuyến đường")
                                                .build());
                Permission pVehicleManage = permissionRepository
                                .save(Permission.builder().code("VEHICLE_MANAGE").description("Quản lý xe & tài xế")
                                                .build());

                Set<Permission> adminPerms = new HashSet<>(Arrays.asList(pStaffManage, pCustomerManage, pTripManage,
                                pTicketManage, pManageTicket, pStats, pRouteManage, pVehicleManage));
                Set<Permission> staffPerms = new HashSet<>(Arrays.asList(pTripManage, pTicketManage, pManageTicket));

                Role roleAdmin = roleRepository
                                .save(Role.builder().name("ROLE_ADMIN").description("Quản trị viên hệ thống")
                                                .permissions(adminPerms).build());
                Role roleStaff = roleRepository.save(
                                Role.builder().name("ROLE_STAFF").description("Nhân viên quản lý")
                                                .permissions(staffPerms).build());
                roleRepository.save(
                                Role.builder().name("ROLE_CUSTOMER").description("Khách hàng")
                                                .permissions(new HashSet<>()).build());

                // 2. Users
                log.info("2. Đang tạo Users...");
                String defaultPassword = passwordEncoder.encode("123456");

                staffRepository.save(
                                Staff.builder().fullName("Admin VEXE").email("admin@vexe.vn").phoneNumber("0901234567")
                                                .password(defaultPassword).status("ACTIVE")
                                                .dateOfBirth(LocalDate.of(1985, 1, 1)).staffCode("STAFF001")
                                                .role(roleAdmin).build());
                staffRepository.save(Staff.builder().fullName("Nguyễn Quản Lý").email("manager@vexe.vn")
                                .phoneNumber("0901234568").password(defaultPassword).status("ACTIVE")
                                .dateOfBirth(LocalDate.of(1988, 5, 15)).staffCode("STAFF002").role(roleAdmin).build());

                staffRepository.save(Staff.builder().fullName("Nhân viên soát vé mobile").email("staff@vexe.vn")
                                .phoneNumber("0901234569").password(defaultPassword).status("ACTIVE")
                                .staffCode("STAFF_MOBILE").role(roleStaff).build());

                for (int i = 3; i <= 7; i++) {
                        staffRepository.save(Staff.builder().fullName("Nhân viên " + i).email("staff" + i + "@vexe.vn")
                                        .phoneNumber("090987654" + i).password(defaultPassword).status("ACTIVE")
                                        .staffCode("STAFF00" + i)
                                        .role(roleStaff).build());
                }

                List<Customer> customers = new ArrayList<>();
                customers.add(customerRepository
                                .save(Customer.builder().fullName("Lưu Trung Nghĩa").email("luutrungnghia19012003@gmail.com")
                                                .phoneNumber("0909999999")
                                                .password(defaultPassword).status("ACTIVE").address("TP.HCM")
                                                .build()));
                customers.add(customerRepository
                                .save(Customer.builder().fullName("Lê Văn Khách").email("khach1@gmail.com")
                                                .phoneNumber("0912345678")
                                                .password(defaultPassword).status("ACTIVE").address("Quận 1, TP.HCM")
                                                .build()));
                for (int i = 2; i <= 20; i++) {
                        customers.add(customerRepository.save(Customer.builder().fullName("Khách hàng " + i)
                                        .email("khach" + i + "@gmail.com")
                                        .phoneNumber("09123456" + String.format("%02d", i))
                                        .password(defaultPassword).status("ACTIVE").address("TP.HCM").build()));
                }

                // 3. Routes
                log.info("3. Đang tạo Tuyến đường...");
                List<Route> routes = new ArrayList<>();
                routes.add(routeRepository.save(
                                Route.builder().departureLocation("TP.HCM").arrivalLocation("Đà Lạt")
                                                .distanceKm(new BigDecimal("300"))
                                                .durationMinutes(360).basePrice(new BigDecimal("250000")).status(true)
                                                .build()));
                routes.add(routeRepository.save(
                                Route.builder().departureLocation("Hà Nội").arrivalLocation("Sapa")
                                                .distanceKm(new BigDecimal("315"))
                                                .durationMinutes(330).basePrice(new BigDecimal("300000")).status(true)
                                                .build()));
                routes.add(routeRepository.save(
                                Route.builder().departureLocation("Đà Nẵng").arrivalLocation("Huế")
                                                .distanceKm(new BigDecimal("100"))
                                                .durationMinutes(120).basePrice(new BigDecimal("150000")).status(true)
                                                .build()));
                routes.add(routeRepository.save(Route.builder().departureLocation("TP.HCM").arrivalLocation("Nha Trang")
                                .distanceKm(new BigDecimal("430")).durationMinutes(500)
                                .basePrice(new BigDecimal("350000")).status(true)
                                .build()));
                routes.add(routeRepository.save(Route.builder().departureLocation("Hà Nội").arrivalLocation("Hải Phòng")
                                .distanceKm(new BigDecimal("120")).durationMinutes(100)
                                .basePrice(new BigDecimal("120000")).status(true)
                                .build()));
                routes.add(routeRepository.save(
                                Route.builder().departureLocation("TP.HCM").arrivalLocation("Cần Thơ")
                                                .distanceKm(new BigDecimal("170"))
                                                .durationMinutes(200).basePrice(new BigDecimal("160000")).status(true)
                                                .build()));
                routes.add(routeRepository.save(Route.builder().departureLocation("Đà Nẵng").arrivalLocation("Quy Nhơn")
                                .distanceKm(new BigDecimal("320")).durationMinutes(360)
                                .basePrice(new BigDecimal("280000")).status(true)
                                .build()));
                routes.add(routeRepository.save(Route.builder().departureLocation("Đà Lạt").arrivalLocation("Nha Trang")
                                .distanceKm(new BigDecimal("140")).durationMinutes(180)
                                .basePrice(new BigDecimal("180000")).status(true)
                                .build()));
                routes.add(routeRepository.save(Route.builder().departureLocation("TP.HCM").arrivalLocation("Vũng Tàu")
                                .distanceKm(new BigDecimal("100")).durationMinutes(120)
                                .basePrice(new BigDecimal("150000")).status(true)
                                .build()));
                routes.add(routeRepository.save(
                                Route.builder().departureLocation("Hà Nội").arrivalLocation("Hạ Long")
                                                .distanceKm(new BigDecimal("160"))
                                                .durationMinutes(150).basePrice(new BigDecimal("200000")).status(true)
                                                .build()));

                // 4. Vehicles
                log.info("4. Đang tạo Xe...");
                List<Vehicle> vehicles = new ArrayList<>();
                String[] types = { "Giường nằm", "Limousine", "Ghế ngồi" };
                int[] capacities = { 34, 22, 45 };
                for (int i = 1; i <= 20; i++) {
                        int typeIdx = i % 3;
                        String plate = String.format("%02dB-%03d.%02d", 50 + (i % 30), 100 + i, i);
                        Vehicle v = createVehicle(plate, types[typeIdx], capacities[typeIdx]);
                        vehicles.add(vehicleRepository.save(v));
                }

                // 5. Trips
                log.info("5. Đang tạo Chuyến xe...");
                List<Trip> trips = new ArrayList<>();
                Random rand = new Random();
                LocalDateTime baseTime = LocalDateTime.now().minusDays(5).withHour(6).withMinute(0).withSecond(0)
                                .withNano(0);

                for (int i = 0; i < 50; i++) {
                        Route route = routes.get(rand.nextInt(routes.size()));
                        Vehicle vehicle = vehicles.get(rand.nextInt(vehicles.size()));
                        LocalDateTime deptTime = baseTime.plusDays(rand.nextInt(15)).plusHours(rand.nextInt(16));
                        String status = deptTime.isBefore(LocalDateTime.now()) ? "COMPLETED" : "PENDING";
                        BigDecimal price = route.getBasePrice()
                                        .multiply(new BigDecimal(
                                                        vehicle.getType().equals("Limousine") ? "1.5" : "1.0"));

                        trips.add(tripRepository.save(Trip.builder()
                                        .route(route)
                                        .vehicle(vehicle)
                                        .departureTime(deptTime)
                                        .price(price)
                                        .status(status)
                                        .build()));
                }

                // 6. Bookings, Tickets, Payments
                log.info("6. Đang tạo Dữ liệu Đặt vé...");
                for (int i = 0; i < 150; i++) {
                        Customer customer = customers.get(rand.nextInt(customers.size()));
                        Trip trip = trips.get(rand.nextInt(trips.size()));

                        List<Seat> availableSeats = trip.getVehicle().getSeats();
                        int numSeats = rand.nextInt(3) + 1; // 1-3 seats
                        List<Seat> bookedSeats = new ArrayList<>();
                        for (int j = 0; j < numSeats; j++) {
                                bookedSeats.add(availableSeats.get(rand.nextInt(availableSeats.size()))); // Might
                                                                                                          // duplicate
                                                                                                          // but it's
                                                                                                          // mock
                        }

                        BigDecimal total = trip.getPrice().multiply(new BigDecimal(numSeats));
                        boolean isPaid = rand.nextBoolean() || trip.getStatus().equals("COMPLETED");
                        String status = isPaid ? "PAID" : (rand.nextBoolean() ? "CANCELLED" : "PENDING_PAYMENT");

                        Booking booking = bookingRepository.save(Booking.builder()
                                        .customer(customer)
                                        .trip(trip)
                                        .status(status)
                                        .totalPrice(total)
                                        .build());

                        if (isPaid) {
                                paymentRepository.save(Payment.builder()
                                                .booking(booking)
                                                .amount(total)
                                                .paymentMethod(rand.nextBoolean() ? "VNPAY" : "MOMO")
                                                .status("SUCCESS")
                                                .transactionId(UUID.randomUUID().toString())
                                                .build());
                        }

                        int ticketIdx = 1;
                        for (Seat seat : bookedSeats) {
                                String tCode = "VX" + String.format("%04d%02d", i + 1, ticketIdx++);
                                ticketRepository.save(Ticket.builder()
                                                .ticketCode(tCode)
                                                .booking(booking)
                                                .seat(seat)
                                                .status(isPaid ? "ISSUED" : "RESERVED")
                                                .price(trip.getPrice())
                                                .build());
                        }
                }

                seedMobileVerificationCases(routes.get(0), vehicles.get(0), vehicles.get(1), customers.get(0));

                // 7. Chatbot Metadata
                log.info("7. Đang tạo Dữ liệu Schema cho Chatbot...");
                tableMetadataRepository.save(TableMetadata.builder().tableName("routes")
                                .shortDescription("Chứa thông tin các tuyến đường xe chạy")
                                .schemaDefinition(
                                                "CREATE TABLE routes (id BIGINT PRIMARY KEY, departure_location VARCHAR(255), arrival_location VARCHAR(255), base_price DECIMAL(38,2), distance_km DECIMAL(38,2), duration_minutes INT);")
                                .build());
                tableMetadataRepository.save(TableMetadata.builder().tableName("vehicles")
                                .shortDescription("Chứa thông tin xe và biển số")
                                .schemaDefinition(
                                                "CREATE TABLE vehicles (id BIGINT PRIMARY KEY, license_plate VARCHAR(255), type VARCHAR(255), capacity INT);")
                                .build());
                tableMetadataRepository.save(TableMetadata.builder().tableName("trips")
                                .shortDescription("Chứa thông tin các chuyến xe đang và sắp chạy")
                                .schemaDefinition(
                                                "CREATE TABLE trips (id BIGINT PRIMARY KEY, route_id BIGINT, vehicle_id BIGINT, departure_time TIMESTAMP, price DECIMAL(38,2), status VARCHAR(255));")
                                .build());
                tableMetadataRepository.save(TableMetadata.builder().tableName("customers")
                                .shortDescription("Chứa thông tin khách hàng, dùng để đối chiếu email người dùng đang đăng nhập")
                                .schemaDefinition(
                                                "CREATE TABLE customers (id UUID PRIMARY KEY, email VARCHAR(255), full_name VARCHAR(255), phone_number VARCHAR(255));")
                                .build());
                tableMetadataRepository.save(TableMetadata.builder().tableName("bookings")
                                .shortDescription("Chứa lịch sử đặt vé (hóa đơn) của khách hàng (customer_id)")
                                .schemaDefinition(
                                                "CREATE TABLE bookings (id BIGINT PRIMARY KEY, customer_id UUID, trip_id BIGINT, status VARCHAR(255), total_price DECIMAL(38,2), created_at TIMESTAMP);")
                                .build());
                tableMetadataRepository.save(TableMetadata.builder().tableName("tickets")
                                .shortDescription("Chứa thông tin chi tiết từng vé (ghế ngồi) thuộc về một booking")
                                .schemaDefinition(
                                                "CREATE TABLE tickets (id BIGINT PRIMARY KEY, booking_id BIGINT, seat_id BIGINT, price DECIMAL(38,2), status VARCHAR(255));")
                                .build());

                // 8. Vector Ingestion
                vectorIngestionService.ifPresent(service -> {
                        log.info("8. Đang nạp toàn bộ dữ liệu mẫu vào pgvector (VectorStore)...");
                        try {
                                service.ingestAll();
                                log.info("Nạp dữ liệu vector vào pgvector thành công!");
                        } catch (Exception e) {
                                log.warn("Không thể nạp dữ liệu vector trong seeder: {}", e.getMessage());
                        }
                });
        }

        private void seedMobileVerificationCases(Route route, Vehicle todayVehicle, Vehicle wrongDayVehicle,
                        Customer customer) {
                LocalDateTime todayDeparture = LocalDate.now().atTime(23, 59);
                LocalDateTime wrongDayDeparture = LocalDate.now().plusDays(1).atTime(10, 0);

                Trip todayTrip = tripRepository.save(Trip.builder()
                                .route(route)
                                .vehicle(todayVehicle)
                                .departureTime(todayDeparture)
                                .price(route.getBasePrice())
                                .status("PENDING")
                                .build());
                Trip wrongDayTrip = tripRepository.save(Trip.builder()
                                .route(route)
                                .vehicle(wrongDayVehicle)
                                .departureTime(wrongDayDeparture)
                                .price(route.getBasePrice())
                                .status("PENDING")
                                .build());

                Booking validBooking = createMobileTestBooking(customer, todayTrip, "PAID");
                Booking usedBooking = createMobileTestBooking(customer, todayTrip, "PAID");
                Booking unpaidBooking = createMobileTestBooking(customer, todayTrip, "PENDING_PAYMENT");
                Booking wrongDayBooking = createMobileTestBooking(customer, wrongDayTrip, "PAID");

                createMobileTestPayment(validBooking, "SEED-MOBILE-VALID");
                createMobileTestPayment(usedBooking, "SEED-MOBILE-USED");
                createMobileTestPayment(wrongDayBooking, "SEED-MOBILE-WRONG-DAY");

                List<Seat> todaySeats = todayVehicle.getSeats();
                ticketRepository.save(Ticket.builder()
                                .ticketCode("QR-VALID-TODAY")
                                .booking(validBooking)
                                .seat(todaySeats.get(0))
                                .status("ISSUED")
                                .price(todayTrip.getPrice())
                                .build());
                ticketRepository.save(Ticket.builder()
                                .ticketCode("QR-ALREADY-USED")
                                .booking(usedBooking)
                                .seat(todaySeats.get(1))
                                .status("USED")
                                .usedAt(LocalDateTime.now().minusMinutes(5))
                                .price(todayTrip.getPrice())
                                .build());
                ticketRepository.save(Ticket.builder()
                                .ticketCode("QR-UNPAID")
                                .booking(unpaidBooking)
                                .seat(todaySeats.get(2))
                                .status("ISSUED")
                                .price(todayTrip.getPrice())
                                .build());
                ticketRepository.save(Ticket.builder()
                                .ticketCode("QR-WRONG-DAY")
                                .booking(wrongDayBooking)
                                .seat(wrongDayVehicle.getSeats().get(0))
                                .status("ISSUED")
                                .price(wrongDayTrip.getPrice())
                                .build());
        }

        private Booking createMobileTestBooking(Customer customer, Trip trip, String status) {
                return bookingRepository.save(Booking.builder()
                                .customer(customer)
                                .trip(trip)
                                .status(status)
                                .totalPrice(trip.getPrice())
                                .build());
        }

        private void createMobileTestPayment(Booking booking, String transactionId) {
                paymentRepository.save(Payment.builder()
                                .booking(booking)
                                .amount(booking.getTotalPrice())
                                .paymentMethod("CASH")
                                .status("SUCCESS")
                                .transactionId(transactionId)
                                .build());
        }

        private Vehicle createVehicle(String licensePlate, String type, int capacity) {
                Vehicle vehicle = Vehicle.builder()
                                .licensePlate(licensePlate)
                                .type(type)
                                .capacity(capacity)
                                .status("ACTIVE")
                                .build();

                List<Seat> seats = new ArrayList<>();
                for (int i = 1; i <= capacity; i++) {
                        String seatName = (i <= capacity / 2 ? "A" : "B")
                                        + (i <= capacity / 2 ? i : (i - capacity / 2));
                        seats.add(Seat.builder().seatName(seatName).vehicle(vehicle).build());
                }
                vehicle.setSeats(seats);
                return vehicle;
        }
}
