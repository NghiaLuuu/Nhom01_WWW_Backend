package vn.edu.iuh.nhom01_www.cancelrequest.workflow;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.dto.CancellationDtos.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.model.CancelWorkflowDetails;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.repository.*;
import vn.edu.iuh.nhom01_www.cancelrequest.workflow.service.CancellationWorkflowService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.common.security.service.CurrentUserService;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.route.model.Route;
import vn.edu.iuh.nhom01_www.seat.model.Seat;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.ticket.repository.TicketRepository;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real ORM + PostgreSQL locks, with no Spring Boot startup, mail, Redis or application seeders. */
@EnabledIfSystemProperty(named = "cancellation.it.enabled", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancellationPostgresIntegrationTest {
    private SessionFactory factory;
    private EntityManager em;
    private TransactionTemplate tx;
    private JdbcTemplate jdbc;
    private CancellationWorkflowService service;
    private CurrentUserService currentUser;
    private BookingRepository bookings;
    private TicketRepository tickets;
    private PaymentRepository payments;
    private CancelRequestRepository requests;
    private CancelWorkflowDetailsRepository metadata;
    private CancellationQueries queries;
    private Customer owner;
    private Customer stranger;
    private Staff actor;
    private Long bookingId;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T03:00:00Z"), ZoneOffset.UTC);

    @BeforeAll
    void startIsolatedOrm() {
        String url = System.getProperty("cancellation.it.jdbc-url", "");
        if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/busbooking_cancellation_it")) {
            throw new IllegalStateException("Requires an isolated loopback busbooking_cancellation_it database");
        }
        String username = System.getProperty("cancellation.it.username");
        String password = System.getProperty("cancellation.it.password");
        jdbc = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).isEqualTo("busbooking_cancellation_it");
        Configuration config = new Configuration();
        for (Class<?> entity : List.of(Booking.class, Ticket.class, Payment.class, CancelRequest.class, CancelWorkflowDetails.class,
                Customer.class, Staff.class, Role.class, Permission.class, Route.class, Trip.class, Vehicle.class, Seat.class)) {
            config.addAnnotatedClass(entity);
        }
        factory = config.setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", username).setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").setProperty("hibernate.connection.pool_size", "6")
                .setProperty("hibernate.show_sql", "false").buildSessionFactory();
        em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        tx = new TransactionTemplate(new JpaTransactionManager(factory));
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(em);
        bookings = repositories.getRepository(BookingRepository.class);
        tickets = repositories.getRepository(TicketRepository.class);
        payments = repositories.getRepository(PaymentRepository.class);
        requests = repositories.getRepository(CancelRequestRepository.class);
        metadata = repositories.getRepository(CancelWorkflowDetailsRepository.class);
        queries = new CancellationQueries(em);
    }

    @BeforeEach
    void fixture() {
        jdbc.execute("TRUNCATE cancel_request_workflow_details, cancel_requests, payments, tickets, bookings, trips, seats, vehicles, routes, staff_permissions, staffs, customers, role_permissions, roles, permissions RESTART IDENTITY CASCADE");
        currentUser = mock(CurrentUserService.class);
        tx.executeWithoutResult(status -> {
            owner = Customer.builder().fullName("Khách sở hữu").email("owner@example.test").status("ACTIVE").password("not-a-real-password").build();
            stranger = Customer.builder().fullName("Khách khác").email("other@example.test").status("ACTIVE").password("not-a-real-password").build();
            em.persist(owner); em.persist(stranger);
            Role role = Role.builder().name("ROLE_ADMIN").build(); em.persist(role);
            actor = Staff.builder().fullName("Nhân viên xử lý").email("staff@example.test").staffCode("NV001").role(role).status("ACTIVE").password("not-a-real-password").build(); em.persist(actor);
            Route route = Route.builder().departureLocation("TP.HCM").arrivalLocation("Đà Lạt").basePrice(BigDecimal.valueOf(250000)).build(); em.persist(route);
            Vehicle vehicle = Vehicle.builder().licensePlate("51B-TEST").type("Giường nằm").capacity(2).build(); em.persist(vehicle);
            Trip trip = Trip.builder().route(route).vehicle(vehicle).status("PENDING").price(BigDecimal.valueOf(250000)).departureTime(LocalDateTime.of(2026, 11, 29, 20, 0)).build(); em.persist(trip);
            Booking booking = Booking.builder().customer(owner).trip(trip).status("PAID").totalPrice(BigDecimal.valueOf(500000)).build(); em.persist(booking);
            bookingId = booking.getId();
            for (int index = 1; index <= 2; index++) {
                Seat seat = Seat.builder().vehicle(vehicle).seatName("A0" + index).build(); em.persist(seat);
                em.persist(Ticket.builder().booking(booking).seat(seat).ticketCode("TEST-" + index).status("ISSUED").price(BigDecimal.valueOf(250000)).build());
            }
            em.persist(Payment.builder().booking(booking).amount(BigDecimal.valueOf(500000)).status("SUCCESS").paymentMethod("CASH").transactionId("PAYMENT-ORIGINAL").build());
        });
        when(currentUser.getCurrentStaff()).thenReturn(actor);
        when(currentUser.getCurrentCustomer()).thenReturn(owner);
        service = new CancellationWorkflowService(queries, requests, metadata, bookings, tickets, payments, currentUser, clock);
    }

    @AfterAll
    void closeOrm() { if (factory != null) factory.close(); }

    @Test
    void submissionPreservesPaidBookingAndTicketsAndStoresReasonSeparately() {
        RequestDetails result = submit();
        assertThat(result.request().requestReason()).isEqualTo("Khách thay đổi kế hoạch");
        assertThat(result.request().status()).isEqualTo("PENDING");
        assertThat(result.booking().status()).isEqualTo("PAID");
        assertThat(result.booking().tickets()).allSatisfy(ticket -> assertThat(ticket.status()).isEqualTo("ISSUED"));
        assertThat(result.request().version()).isZero();
        tx.executeWithoutResult(status -> assertThat(requests.findById(result.request().id()).orElseThrow().getRejectReason()).isNull());
    }

    @Test
    void approvalCancelsAllTicketsAndBookingWithoutRefundingOrEditingPayment() {
        RequestDetails pending = submit();
        RequestDetails result = tx.execute(status -> service.approve(pending.request().id(), new ApproveRequest(0L, true, "Đã xác nhận với khách")));
        assertThat(result.request().status()).isEqualTo("APPROVED");
        assertThat(result.request().processedBy()).isEqualTo(actor.getFullName());
        assertThat(result.request().version()).isEqualTo(1L);
        assertThat(result.request().requestReason()).isEqualTo("Khách thay đổi kế hoạch");
        assertThat(result.booking().status()).isEqualTo("CANCELLED");
        assertThat(result.booking().tickets()).allSatisfy(ticket -> assertThat(ticket.status()).isEqualTo("CANCELLED"));
        assertThat(result.financialNotice()).contains("không hoàn tiền");
        assertThat(result.decisionNote()).isEqualTo("Đã xác nhận với khách");
        assertPaymentUnchanged();
    }

    @Test
    void acceptsTheExistingProfileSubmissionWithoutInventingAReason() {
        RequestDetails result = tx.execute(status -> service.submitFromProfile(bookingId));
        assertThat(result.request().source()).isEqualTo("PROFILE");
        assertThat(result.request().requestReason()).isNull();
        assertThat(result.request().status()).isEqualTo("PENDING");
        assertThat(result.booking().status()).isEqualTo("PAID");
        when(currentUser.getCurrentCustomer()).thenReturn(stranger);
        assertThatThrownBy(() -> tx.execute(status -> service.submitFromProfile(bookingId))).hasMessageContaining("không có quyền");
        assertPaymentUnchanged();
    }

    @Test
    void acceptsUnusedLegacyTicketsThatTheExistingVerifierAlsoAccepts() {
        tx.executeWithoutResult(status -> tickets.findByBookingId(bookingId).forEach(ticket -> ticket.setStatus("UNUSED")));
        RequestDetails pending = submit();
        RequestDetails result = tx.execute(status -> service.approve(pending.request().id(), new ApproveRequest(0L, true, null)));
        assertThat(result.booking().tickets()).allSatisfy(ticket -> assertThat(ticket.status()).isEqualTo("CANCELLED"));
        assertPaymentUnchanged();
    }

    @Test
    void rejectionPreservesBookingTicketsOriginalReasonAndPayment() {
        RequestDetails pending = submit();
        RequestDetails result = tx.execute(status -> service.reject(pending.request().id(), new RejectRequest(0L, "Không đủ điều kiện theo yêu cầu khách cung cấp")));
        assertThat(result.request().status()).isEqualTo("REJECTED");
        assertThat(result.request().requestReason()).isEqualTo("Khách thay đổi kế hoạch");
        assertThat(result.request().rejectionReason()).isEqualTo("Không đủ điều kiện theo yêu cầu khách cung cấp");
        assertThat(result.booking().status()).isEqualTo("PAID");
        assertThat(result.booking().tickets()).allSatisfy(ticket -> assertThat(ticket.status()).isEqualTo("ISSUED"));
        assertPaymentUnchanged();
    }

    @Test
    void customerCannotSubmitOrReadAnotherCustomersRequest() {
        RequestDetails request = submit();
        when(currentUser.getCurrentCustomer()).thenReturn(stranger);
        assertThatThrownBy(() -> tx.execute(status -> service.submitByCustomer(new SubmitRequest(bookingId, "Không phải đơn của tôi")))).isInstanceOf(CustomException.class).hasMessageContaining("không có quyền");
        assertThatThrownBy(() -> tx.execute(status -> service.myDetails(request.request().id()))).hasMessageContaining("không có quyền");
        assertThat(tx.execute(status -> service.myRequests("", 0, 10)).content()).isEmpty();
    }

    @Test
    void preventsDuplicateRequestsEvenAfterRejection() {
        RequestDetails request = submit();
        tx.execute(status -> service.reject(request.request().id(), new RejectRequest(0L, "Khách xác nhận giữ vé")));
        assertThatThrownBy(this::submit).hasMessageContaining("đã có yêu cầu hủy");
        assertThat(tx.<Long>execute(status -> requests.count())).isEqualTo(1L);
    }

    @Test
    void refusesAlreadyProcessedAndStaleDecisions() {
        RequestDetails request = submit();
        assertThatThrownBy(() -> tx.execute(status -> service.approve(request.request().id(), new ApproveRequest(9L, true, null)))).hasMessageContaining("đã thay đổi");
        tx.execute(status -> service.approve(request.request().id(), new ApproveRequest(0L, true, null)));
        assertThatThrownBy(() -> tx.execute(status -> service.reject(request.request().id(), new RejectRequest(0L, "Từ chối lại")))).hasMessageContaining("đã được xử lý");
    }

    @Test
    void rechecksUsedTicketsBeforeApprovalButAllowsClosingTheRequestByRejection() {
        RequestDetails request = submit();
        tx.executeWithoutResult(status -> tickets.findByBookingId(bookingId).get(0).use());
        assertThatThrownBy(() -> tx.execute(status -> service.approve(request.request().id(), new ApproveRequest(0L, true, null)))).hasMessageContaining("đã sử dụng");
        assertThat(tx.execute(status -> service.staffDetails(request.request().id())).request().status()).isEqualTo("PENDING");
        tx.execute(status -> service.reject(request.request().id(), new RejectRequest(0L, "Khách đã sử dụng vé")));
        assertThat(tx.<String>execute(status -> bookings.findById(bookingId).orElseThrow().getStatus())).isEqualTo("PAID");
        assertPaymentUnchanged();
    }

    @Test
    void rechecksDepartureBeforeApproval() {
        RequestDetails request = submit();
        tx.executeWithoutResult(status -> bookings.findById(bookingId).orElseThrow().getTrip().setDepartureTime(LocalDateTime.of(2026, 11, 27, 9, 0)));
        assertThatThrownBy(() -> tx.execute(status -> service.approve(request.request().id(), new ApproveRequest(0L, true, null)))).hasMessageContaining("đã khởi hành");
        assertThat(tx.<String>execute(status -> bookings.findById(bookingId).orElseThrow().getStatus())).isEqualTo("PAID");
    }

    @Test
    void readsLegacyPendingRequestsAndPreservesTheirReasonOnApproval() {
        Long id = tx.execute(status -> {
            CancelRequest legacy = new CancelRequest(); legacy.setBooking(bookings.findById(bookingId).orElseThrow());
            legacy.setStatus("PENDING"); legacy.setRejectReason("Lý do được lưu trong luồng cũ");
            return requests.saveAndFlush(legacy).getId();
        });
        assertThat(tx.execute(status -> service.staffDetails(id)).request().source()).isEqualTo("LEGACY");
        RequestDetails result = tx.execute(status -> service.approve(id, new ApproveRequest(0L, true, null)));
        assertThat(result.request().requestReason()).isEqualTo("Lý do được lưu trong luồng cũ");
        assertThat(tx.<String>execute(status -> requests.findById(id).orElseThrow().getRejectReason())).isNull();
    }

    @Test
    void listsSearchesAndPaginatesWithoutLeakingOtherCustomers() {
        RequestDetails request = submit();
        assertThat(tx.execute(status -> service.staffList("Đà Lạt", "PENDING", 0, 10)).content()).hasSize(1);
        assertThat(tx.execute(status -> service.staffList("đÀ lẠt", "PENDING", 0, 10)).content()).hasSize(1);
        assertThat(tx.execute(status -> service.staffList("owner@example.test", "", 0, 10)).content()).hasSize(1);
        assertThat(tx.execute(status -> service.staffList("%", "", 0, 10)).content()).isEmpty();
        assertThat(tx.execute(status -> service.staffList("", "APPROVED", 0, 10)).content()).isEmpty();
        assertThat(tx.execute(status -> service.myRequests("", 0, 10)).content().get(0).id()).isEqualTo(request.request().id());
    }

    @Test
    void concurrentApprovalsHaveExactlyOneWinner() throws Exception {
        Long id = submit().request().id();
        assertConcurrent(() -> tx.execute(status -> service.approve(id, new ApproveRequest(0L, true, null))));
        assertThat(tx.execute(status -> service.staffDetails(id)).request().status()).isEqualTo("APPROVED");
        assertPaymentUnchanged();
    }

    @Test
    void concurrentSubmissionsCreateExactlyOneRequest() throws Exception {
        assertConcurrent(this::submit);
        assertThat(tx.<Long>execute(status -> requests.count())).isEqualTo(1L);
    }

    private void assertConcurrent(Callable<?> operation) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> task = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
            try { operation.call(); return true; }
            catch (CustomException expected) { return false; }
        };
        try {
            Future<Boolean> first = executor.submit(task); Future<Boolean> second = executor.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        } finally { executor.shutdownNow(); }
    }

    private RequestDetails submit() {
        return tx.execute(status -> service.submitByCustomer(new SubmitRequest(bookingId, "Khách thay đổi kế hoạch")));
    }

    private void assertPaymentUnchanged() {
        tx.executeWithoutResult(status -> {
            Payment payment = payments.findByBookingId(bookingId).orElseThrow();
            assertThat(payment.getStatus()).isEqualTo("SUCCESS");
            assertThat(payment.getAmount()).isEqualByComparingTo("500000");
            assertThat(payment.getTransactionId()).isEqualTo("PAYMENT-ORIGINAL");
            assertThat(payments.count()).isEqualTo(1);
        });
    }
}
