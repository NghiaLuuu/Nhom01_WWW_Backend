package vn.edu.iuh.nhom01_www;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import vn.edu.iuh.nhom01_www.aivector.VectorIngestionService;
import vn.edu.iuh.nhom01_www.chatbot.model.TableMetadata;
import vn.edu.iuh.nhom01_www.chatbot.repository.TableMetadataRepository;

import java.util.Optional;

@SpringBootApplication
@EnableScheduling
public class Nhom01WwwApplication {

    public static void main(String[] args) {
        SpringApplication.run(Nhom01WwwApplication.class, args);
    }

    @Bean
    public CommandLineRunner autoSeedChatbotMetadata(TableMetadataRepository repo, JdbcTemplate jdbcTemplate) {
        return args -> {
            // Đảm bảo bảng tickets luôn có 2 cột audit created_at và updated_at
            try {
                jdbcTemplate.execute("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6)");
                jdbcTemplate.execute("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6)");
            } catch (Exception ignored) {
            }

            // Translate legacy vehicle types already stored in the database.
            jdbcTemplate.update("UPDATE vehicles SET type = CASE type "
                    + "WHEN 'Seater' THEN 'Ghế ngồi' "
                    + "WHEN 'Sleeper' THEN 'Giường nằm' "
                    + "ELSE type END "
                    + "WHERE type IN ('Seater', 'Sleeper')");

            // Chuyển toàn bộ trạng thái hết hạn giữ chỗ (EXPIRED / RELEASED) sang Đã hủy (CANCELLED)
            jdbcTemplate.update("UPDATE bookings SET status = 'CANCELLED' WHERE status = 'EXPIRED'");
            jdbcTemplate.update("UPDATE tickets SET status = 'CANCELLED' WHERE status = 'RELEASED'");

            if (repo.findAll().stream().noneMatch(m -> m.getTableName().equals("bookings"))) {
                repo.save(TableMetadata.builder().tableName("customers")
                        .shortDescription("Chứa thông tin khách hàng, dùng để đối chiếu email người dùng đang đăng nhập")
                        .schemaDefinition("CREATE TABLE customers (id UUID PRIMARY KEY, email VARCHAR(255), full_name VARCHAR(255), phone_number VARCHAR(255));")
                        .build());
                repo.save(TableMetadata.builder().tableName("bookings")
                        .shortDescription("Chứa lịch sử đặt vé (hóa đơn) của khách hàng (customer_id)")
                        .schemaDefinition("CREATE TABLE bookings (id BIGINT PRIMARY KEY, customer_id UUID, trip_id BIGINT, status VARCHAR(255), total_price DECIMAL(38,2), created_at TIMESTAMP);")
                        .build());
                repo.save(TableMetadata.builder().tableName("tickets")
                        .shortDescription("Chứa thông tin chi tiết từng vé (ghế ngồi) thuộc về một booking")
                        .schemaDefinition("CREATE TABLE tickets (id BIGINT PRIMARY KEY, booking_id BIGINT, seat_id BIGINT, price DECIMAL(38,2), status VARCHAR(255));")
                        .build());
                System.out.println("--- ĐÃ TỰ ĐỘNG BỔ SUNG TABLE METADATA (BOOKINGS, CUSTOMERS, TICKETS) CHO CHATBOT ---");
            }
        };
    }

    @Bean
    public CommandLineRunner autoIngestVectors(Optional<VectorIngestionService> ingestionService, JdbcTemplate jdbcTemplate) {
        return args -> {
            if (ingestionService.isPresent()) {
                try {
                    Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
                    if (count == null || count == 0) {
                        System.out.println("--- BẢNG VECTOR_STORE ĐANG TRỐNG, TỰ ĐỘNG CHẠY INGESTALL()... ---");
                        ingestionService.get().ingestAll();
                        System.out.println("--- TỰ ĐỘNG INGEST DỮ LIỆU VECTOR VÀO POSTGRESQL PGVECTOR THÀNH CÔNG! ---");
                    }
                } catch (Exception e) {
                    System.err.println("--- CHƯA THỂ TỰ ĐỘNG INGEST VECTOR: " + e.getMessage() + " ---");
                }
            }
        };
    }
}
