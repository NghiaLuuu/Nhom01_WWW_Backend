package vn.edu.iuh.nhom01_www.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.common.dto.PageResponse;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.user.dto.ManagementViews.*;
import vn.edu.iuh.nhom01_www.user.model.BaseUser;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagementService {
    private final CustomerRepository customers;
    private final StaffRepository staffs;
    private final BookingRepository bookings;

    public PageResponse<CustomerView> customers(String keyword, String status, int page, int size) {
        return PageResponse.from(customers.findAll(search(keyword, status, false), pageable(page, size)).map(CustomerView::from));
    }

    public PageResponse<StaffView> staffs(String keyword, String status, int page, int size) {
        return PageResponse.from(staffs.findAll(search(keyword, status, true), pageable(page, size)).map(StaffView::from));
    }

    public CustomerView customer(UUID id) {
        return CustomerView.from(customers.findById(id).orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND)));
    }

    public StaffView staff(UUID id) {
        return StaffView.from(staffs.findById(id).orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND)));
    }

    public PageResponse<BookingView> customerBookings(UUID id, int page, int size) {
        customer(id);
        return PageResponse.from(bookings.findByCustomer_Id(id, pageable(page, size)).map(BookingView::from));
    }

    public static PageRequest pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new CustomException("page must be >= 0 and size must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private <T extends BaseUser> Specification<T> search(String keyword, String status, boolean staff) {
        if (keyword != null && keyword.length() > 200) {
            throw new CustomException("keyword must not exceed 200 characters", HttpStatus.BAD_REQUEST);
        }
        if (status != null && !status.isBlank() && !java.util.Set.of("ACTIVE", "LOCKED", "PENDING_VERIFICATION").contains(status.toUpperCase(Locale.ROOT))) {
            throw new CustomException("Invalid account status", HttpStatus.BAD_REQUEST);
        }
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (keyword != null && !keyword.isBlank()) {
                String escaped = keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
                String like = "%" + escaped + "%";
                var match = cb.or(cb.like(cb.lower(root.get("fullName")), like, '!'),
                        cb.like(cb.lower(root.get("email")), like, '!'), cb.like(root.get("phoneNumber"), like, '!'));
                if (staff) match = cb.or(match, cb.like(cb.lower(root.get("staffCode")), like, '!'));
                predicate = cb.and(predicate, match);
            }
            if (status != null && !status.isBlank()) predicate = cb.and(predicate, cb.equal(root.get("status"), status.toUpperCase(Locale.ROOT)));
            return predicate;
        };
    }
}
