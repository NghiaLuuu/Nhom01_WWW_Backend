package vn.edu.iuh.nhom01_www.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.chatbot.model.TableMetadata;
import vn.edu.iuh.nhom01_www.chatbot.repository.TableMetadataRepository;
import vn.edu.iuh.nhom01_www.role.model.Permission;
import vn.edu.iuh.nhom01_www.role.model.Role;
import vn.edu.iuh.nhom01_www.role.repository.PermissionRepository;
import vn.edu.iuh.nhom01_www.role.repository.RoleRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TableMetadataRepository tableMetadataRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed Permissions
        List<String> permissionCodes = Arrays.asList(
                "STAFF_MANAGE", "CUSTOMER_MANAGE", "TRIP_MANAGE", "ROUTE_MANAGE", "VIEW_STATISTICS", "TICKET_MANAGE"
        );
        
        Set<Permission> allPermissions = new HashSet<>();

        for (String code : permissionCodes) {
            Permission permission = permissionRepository.findByCode(code)
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .code(code)
                            .description("Permission for " + code)
                            .build()));
            allPermissions.add(permission);
        }

        // Seed Roles
        if (roleRepository.findByName("ADMIN").isEmpty()) {
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .description("Administrator")
                    .permissions(allPermissions)
                    .build();
            roleRepository.save(adminRole);
        }

        if (roleRepository.findByName("CUSTOMER_CARE_STAFF").isEmpty()) {
            Set<Permission> staffPermissions = new HashSet<>();
            permissionRepository.findByCode("TICKET_MANAGE").ifPresent(staffPermissions::add);

            Role staffRole = Role.builder()
                    .name("CUSTOMER_CARE_STAFF")
                    .description("Nhân viên cham sóc khách hàng")
                    .permissions(staffPermissions)
                    .build();
            roleRepository.save(staffRole);
        }

        // Seed TableMetadata for Chatbot
        if (tableMetadataRepository.count() == 0) {
            tableMetadataRepository.save(TableMetadata.builder()
                    .tableName("trips")
                    .shortDescription("Ch?a thông tin các chuy?n xe khách: ngày kh?i hành, giá vé, tr?ng thái chuy?n, và ID tuy?n du?ng.")
                    .schemaDefinition("TABLE trips (id BIGINT PRIMARY KEY, departure_time TIMESTAMP, arrival_time TIMESTAMP, price DOUBLE PRECISION, status VARCHAR, route_id BIGINT, vehicle_id BIGINT)")
                    .build());

            tableMetadataRepository.save(TableMetadata.builder()
                    .tableName("routes")
                    .shortDescription("Ch?a thông tin các tuy?n du?ng: di?m di, di?m d?n, kho?ng cách, th?i gian di chuy?n.")
                    .schemaDefinition("TABLE routes (id BIGINT PRIMARY KEY, departure_location VARCHAR, arrival_location VARCHAR, distance DOUBLE PRECISION, duration DOUBLE PRECISION)")
                    .build());

            tableMetadataRepository.save(TableMetadata.builder()
                    .tableName("vehicles")
                    .shortDescription("Ch?a thông tin xe: bi?n s? xe, lo?i xe, s? lu?ng gh?.")
                    .schemaDefinition("TABLE vehicles (id BIGINT PRIMARY KEY, license_plate VARCHAR, vehicle_type VARCHAR, capacity INT)")
                    .build());
        }
    }
}
