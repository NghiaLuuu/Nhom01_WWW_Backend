package vn.edu.iuh.nhom01_www.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findByName("ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().name("ADMIN").description("Administrator").build());
        }
        if (roleRepository.findByName("CUSTOMER_CARE_STAFF").isEmpty()) {
            roleRepository.save(Role.builder().name("CUSTOMER_CARE_STAFF").description("Nhân viên chăm sóc khách hàng").build());
        }
    }
}
