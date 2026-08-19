package vn.edu.iuh.nhom01_www.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try Customer First
        Customer customer = customerRepository.findByEmail(email);
        if (customer != null) {
            return new User(customer.getEmail(), customer.getPassword(), 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        }

        // Try Staff Second
        Staff staff = staffRepository.findByEmail(email);
        if (staff != null) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            // Add Role
            if (staff.getRole() != null) {
                authorities.add(new SimpleGrantedAuthority(staff.getRole().getName()));
                // Add Permissions from Role
                if (staff.getRole().getPermissions() != null) {
                    staff.getRole().getPermissions().forEach(permission -> {
                        authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                    });
                }
                // Add Individual Permissions
                if (staff.getPermissions() != null) {
                    staff.getPermissions().forEach(permission -> {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    });
                }
            }
            return new User(staff.getEmail(), staff.getPassword(), authorities);
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
