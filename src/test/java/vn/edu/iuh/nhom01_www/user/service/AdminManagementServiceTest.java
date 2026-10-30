package vn.edu.iuh.nhom01_www.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.trip.model.Trip;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {
    @Mock CustomerRepository customers;
    @Mock StaffRepository staffs;
    @Mock BookingRepository bookings;
    @InjectMocks AdminManagementService service;

    @Test void returnsPaginatedSafeCustomerViews() {
        var customer = Customer.builder().id(UUID.randomUUID()).email("test@example.com").password("hash").build();
        when(customers.findAll(org.mockito.ArgumentMatchers.<Specification<Customer>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer), AdminManagementService.pageable(0, 20), 1));
        var result = service.customers("test", "ACTIVE", 0, 20);
        assertEquals(1, result.totalElements());
        assertEquals(customer.getId(), result.content().get(0).id());
        assertEquals("test@example.com", result.content().get(0).email());
    }

    @Test void rejectsInvalidPaginationAndStatus() {
        assertThrows(CustomException.class, () -> service.customers("", null, -1, 20));
        assertThrows(CustomException.class, () -> service.staffs("", null, 0, 101));
        assertThrows(CustomException.class, () -> service.customers("", "DELETED", 0, 20));
        verifyNoInteractions(customers, staffs);
    }

    @Test void missingCustomerIsNotFound() {
        UUID id = UUID.randomUUID();
        when(customers.findById(id)).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> service.customerBookings(id, 0, 20));
        verifyNoInteractions(bookings);
    }

    @Test void historyUsesRequestedCustomerIdAndSafeBookingView() {
        UUID id = UUID.randomUUID();
        when(customers.findById(id)).thenReturn(Optional.of(Customer.builder().id(id).build()));
        var booking = Booking.builder().id(1L).status("PAID").totalPrice(BigDecimal.TEN)
                .trip(Trip.builder().id(2L).build()).build();
        when(bookings.findByCustomer_Id(eq(id), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(booking)));
        var result = service.customerBookings(id, 0, 20);
        assertEquals(2L, result.content().get(0).tripId());
        assertEquals(BigDecimal.TEN, result.content().get(0).totalPrice());
    }
}
