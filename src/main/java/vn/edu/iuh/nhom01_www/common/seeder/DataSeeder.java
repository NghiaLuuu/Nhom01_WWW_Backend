package vn.edu.iuh.nhom01_www.common.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.chatbot.model.TableMetadata;
import vn.edu.iuh.nhom01_www.chatbot.repository.TableMetadataRepository;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.route.repository.RouteRepository;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.seat.repository.SeatRepository;
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
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final CancelRequestRepository cancelRequestRepository;
    private final TableMetadataRepository tableMetadataRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("--- BẮT ĐẦU CHẠY SEEDER DỮ LIỆU MẪU ---");
        clearDatabase();
        seedData();
        log.info("--- HOÀN TẤT SEEDER DỮ LIỆU MẪU ---");
    }

    private void clearDatabase() {
        log.info("Đang xóa dữ liệu cũ (Xóa theo thứ tự phụ thuộc khóa ngoại)...");
        cancelRequestRepository.deleteAll();
        paymentRepository.deleteAll();
        ticketRepository.deleteAll();
        bookingRepository.deleteAll();
        tripRepository.deleteAll();
        seatRepository.deleteAll();
        vehicleRepository.deleteAll();
        routeRepository.deleteAll();
        staffRepository.deleteAll();
        customerRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
        tableMetadataRepository.deleteAll();
        log.info("Xóa dữ liệu cũ thành công!");
    }

    private void seedData() {
        // 1. Permissions & Roles
        log.info("Đang tạo Permissions và Roles...");
        Permission pTrip = permissionRepository.save(Permission.builder().code("MANAGE_TRIP").description("Quản lý chuyến xe").build());
        Permission pUser = permissionRepository.save(Permission.builder().code("MANAGE_USER").description("Quản lý người dùng").build());
        Permission pRoute = permissionRepository.save(Permission.builder().code("MANAGE_ROUTE").description("Quản lý tuyến đường").build());
        Permission pTicket = permissionRepository.save(Permission.builder().code("MANAGE_TICKET").description("Quản lý đặt vé").build());

        Set<Permission> adminPerms = new HashSet<>(Arrays.asList(pTrip, pUser, pRoute, pTicket));
        Set<Permission> staffPerms = new HashSet<>(Arrays.asList(pTrip, pTicket));

        Role roleAdmin = roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Quản trị viên hệ thống").permissions(adminPerms).build());
        Role roleStaff = roleRepository.save(Role.builder().name("ROLE_STAFF").description("Nhân viên quản lý").permissions(staffPerms).build());
        Role roleCustomer = roleRepository.save(Role.builder().name("ROLE_CUSTOMER").description("Khách hàng").permissions(new HashSet<>()).build());

        // 2. Users
        log.info("Đang tạo Users...");
        String defaultPassword = passwordEncoder.encode("password123");
        
        staffRepository.save(Staff.builder()
                .fullName("Nguyễn Văn Admin")
                .email("admin@vexe.vn")
                .phoneNumber("0901234567")
                .password(defaultPassword)
                .status("ACTIVE")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .staffCode("STAFF001")
                .role(roleAdmin)
                .build());

        staffRepository.save(Staff.builder()
                .fullName("Trần Thị Staff")
                .email("staff@vexe.vn")
                .phoneNumber("0909876543")
                .password(defaultPassword)
                .status("ACTIVE")
                .staffCode("STAFF002")
                .role(roleStaff)
                .build());

        customerRepository.save(Customer.builder()
                .fullName("Lê Văn Khách")
                .email("khach1@gmail.com")
                .phoneNumber("0912345678")
                .password(defaultPassword)
                .status("ACTIVE")
                .address("Quận 1, TP.HCM")
                .build());

        // 3. Routes
        log.info("Đang tạo Tuyến đường...");
        Route route1 = routeRepository.save(Route.builder().departureLocation("TP.HCM").arrivalLocation("Đà Lạt").distanceKm(new BigDecimal("300")).durationMinutes(360).basePrice(new BigDecimal("250000")).status(true).build());
        Route route2 = routeRepository.save(Route.builder().departureLocation("Hà Nội").arrivalLocation("Sapa").distanceKm(new BigDecimal("315")).durationMinutes(330).basePrice(new BigDecimal("300000")).status(true).build());
        Route route3 = routeRepository.save(Route.builder().departureLocation("Đà Nẵng").arrivalLocation("Huế").distanceKm(new BigDecimal("100")).durationMinutes(120).basePrice(new BigDecimal("150000")).status(true).build());
        Route route4 = routeRepository.save(Route.builder().departureLocation("TP.HCM").arrivalLocation("Nha Trang").distanceKm(new BigDecimal("430")).durationMinutes(500).basePrice(new BigDecimal("350000")).status(true).build());

        // 4. Vehicles
        log.info("Đang tạo Xe...");
        Vehicle v1 = createVehicle("51B-123.45", "Sleeper", 34);
        Vehicle v2 = createVehicle("29B-987.65", "Sleeper", 36);
        Vehicle v3 = createVehicle("49B-555.55", "Limousine", 22);
        v1 = vehicleRepository.save(v1);
        v2 = vehicleRepository.save(v2);
        v3 = vehicleRepository.save(v3);

        // 5. Trips
        log.info("Đang tạo Chuyến xe...");
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        
        tripRepository.save(Trip.builder().route(route1).vehicle(v1).departureTime(tomorrow).price(new BigDecimal("250000")).status("PENDING").build());
        tripRepository.save(Trip.builder().route(route1).vehicle(v3).departureTime(tomorrow.plusHours(4)).price(new BigDecimal("400000")).status("PENDING").build());
        tripRepository.save(Trip.builder().route(route2).vehicle(v2).departureTime(tomorrow.plusDays(1)).price(new BigDecimal("300000")).status("PENDING").build());
        tripRepository.save(Trip.builder().route(route4).vehicle(v1).departureTime(tomorrow.plusDays(2)).price(new BigDecimal("350000")).status("PENDING").build());

        // 6. Chatbot Metadata
        log.info("Đang tạo Dữ liệu Schema cho Chatbot...");
        tableMetadataRepository.save(TableMetadata.builder()
                .tableName("routes")
                .shortDescription("Chứa thông tin các tuyến đường xe chạy")
                .schemaDefinition("CREATE TABLE routes (id BIGINT PRIMARY KEY, departure_location VARCHAR(255), arrival_location VARCHAR(255), base_price DECIMAL(38,2), distance_km DECIMAL(38,2), duration_minutes INT);")
                .build());
                
        tableMetadataRepository.save(TableMetadata.builder()
                .tableName("vehicles")
                .shortDescription("Chứa thông tin xe và biển số")
                .schemaDefinition("CREATE TABLE vehicles (id BIGINT PRIMARY KEY, license_plate VARCHAR(255), type VARCHAR(255), capacity INT);")
                .build());

        tableMetadataRepository.save(TableMetadata.builder()
                .tableName("trips")
                .shortDescription("Chứa thông tin các chuyến xe đang và sắp chạy")
                .schemaDefinition("CREATE TABLE trips (id BIGINT PRIMARY KEY, route_id BIGINT, vehicle_id BIGINT, departure_time TIMESTAMP, price DECIMAL(38,2), status VARCHAR(255));")
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
            String seatName = (i <= capacity / 2 ? "A" : "B") + (i <= capacity / 2 ? i : (i - capacity / 2));
            seats.add(Seat.builder().seatName(seatName).status("AVAILABLE").vehicle(vehicle).build());
        }
        vehicle.setSeats(seats);
        return vehicle;
    }
}
