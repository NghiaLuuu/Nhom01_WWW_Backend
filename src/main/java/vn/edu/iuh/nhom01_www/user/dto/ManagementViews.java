package vn.edu.iuh.nhom01_www.user.dto;

import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ManagementViews {
    private ManagementViews() {}

    public record CustomerView(UUID id, String fullName, String email, String phoneNumber,
                               LocalDate dateOfBirth, String address, String status, LocalDateTime createdAt) {
        public static CustomerView from(Customer c) {
            return new CustomerView(c.getId(), c.getFullName(), c.getEmail(), c.getPhoneNumber(),
                    c.getDateOfBirth(), c.getAddress(), c.getStatus(), c.getCreatedAt());
        }
    }

    public record StaffView(UUID id, String fullName, String email, String phoneNumber,
                            LocalDate dateOfBirth, String staffCode, String position, String workStation,
                            LocalDate hireDate, String status, Long roleId, String roleName, List<String> permissions) {
        public static StaffView from(Staff s) {
            return new StaffView(s.getId(), s.getFullName(), s.getEmail(), s.getPhoneNumber(), s.getDateOfBirth(),
                    s.getStaffCode(), s.getPosition(), s.getWorkStation(), s.getHireDate(), s.getStatus(),
                    s.getRole() == null ? null : s.getRole().getId(),
                    s.getRole() == null ? null : s.getRole().getName(),
                    s.getPermissions() == null ? List.of() : List.copyOf(s.getPermissions()));
        }
    }

    public record BookingView(Long id, Long tripId, String status, BigDecimal totalPrice, LocalDateTime createdAt) {
        public static BookingView from(Booking b) {
            return new BookingView(b.getId(), b.getTrip().getId(), b.getStatus(), b.getTotalPrice(), b.getCreatedAt());
        }
    }
}
