package vn.edu.iuh.nhom01_www.common.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    public Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException("Người dùng chưa đăng nhập!", HttpStatus.UNAUTHORIZED);
        }
        return authentication;
    }

    public Customer getCurrentCustomer() {
        Authentication authentication = getAuthentication();
        String email = authentication.getName();
        return customerRepository.findOptionalByEmail(email)
                .orElseThrow(() -> new CustomException("Không tìm thấy thông tin khách hàng!", HttpStatus.NOT_FOUND));
    }

    public Optional<Customer> getOptionalCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return customerRepository.findOptionalByEmail(authentication.getName());
    }

    public Staff getCurrentStaff() {
        Authentication authentication = getAuthentication();
        String email = authentication.getName();
        Staff staff = staffRepository.findByEmail(email);
        if (staff == null) {
            throw new CustomException("Không tìm thấy thông tin nhân viên!", HttpStatus.NOT_FOUND);
        }
        return staff;
    }

    public boolean isStaffOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                            || a.getAuthority().equalsIgnoreCase("ROLE_STAFF")
                            || a.getAuthority().equalsIgnoreCase("ADMIN")
                            || a.getAuthority().equalsIgnoreCase("STAFF")
                            || a.getAuthority().equalsIgnoreCase("TICKET_MANAGE"));
    }
}
