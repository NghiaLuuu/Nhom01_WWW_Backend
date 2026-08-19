package vn.edu.iuh.nhom01_www;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Nhom01WwwApplication {

    public static void main(String[] args) {
        SpringApplication.run(Nhom01WwwApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner schemaUpdater(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE customers ADD COLUMN version BIGINT DEFAULT 0");
            } catch (Exception e) {}
            try {
                jdbcTemplate.execute("ALTER TABLE staffs ADD COLUMN version BIGINT DEFAULT 0");
            } catch (Exception e) {}
            try {
                jdbcTemplate.execute("ALTER TABLE refresh_tokens ADD COLUMN version BIGINT DEFAULT 0");
            } catch (Exception e) {}
        };
    }
}
