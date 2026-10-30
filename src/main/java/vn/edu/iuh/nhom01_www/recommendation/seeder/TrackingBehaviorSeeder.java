package vn.edu.iuh.nhom01_www.recommendation.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.tracking.model.PageView;
import vn.edu.iuh.nhom01_www.tracking.model.UserAction;
import vn.edu.iuh.nhom01_www.tracking.model.UserSession;
import vn.edu.iuh.nhom01_www.tracking.repository.PageViewRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserActionRepository;
import vn.edu.iuh.nhom01_www.tracking.repository.UserSessionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class TrackingBehaviorSeeder implements CommandLineRunner {

    private final UserSessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final UserActionRepository actionRepository;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        long currentCount = sessionRepository.count();
        if (currentCount >= 40) {
            log.info("[TrackingBehaviorSeeder] Database already populated with {} sessions. Skipping seeder.", currentCount);
            return;
        }

        log.info("[TrackingBehaviorSeeder] Seeding rich behavior tracking data for 3 personas...");
        seedAllPersonas();
        log.info("[TrackingBehaviorSeeder] Behavior tracking data seeded successfully! Total sessions: {}", sessionRepository.count());
    }

    public void seedAllPersonas() {
        seedDalatLimousinePersona(25);
        seedNhaTrangFamilyPersona(20);
        seedBudgetSaverPersona(20);
    }

    private void seedDalatLimousinePersona(int count) {
        String[] sources = {"facebook", "zalo", "direct", "instagram"};
        String[] campaigns = {"tet-dalat-2026", "san-ve-dem-dalat", "limousine-vip"};
        String[] titles = {
                "VEXE - Đặt vé xe Limousine Sài Gòn Đà Lạt",
                "Chi tiết Chuyến #1: Sài Gòn - Đà Lạt 21:00 (Limousine 34 phòng)",
                "Chọn ghế xe Limousine giường nằm VIP Đà Lạt",
                "Chính sách hoàn 150% vé xe du lịch Đà Lạt"
        };
        String[] urls = {
                "/search?from=TP.HCM&to=Đà+Lạt&type=limousine",
                "/detail/1",
                "/seat-selection/1",
                "/faq"
        };

        for (int i = 0; i < count; i++) {
            LocalDateTime start = LocalDateTime.now().minusHours(random.nextInt(72)).minusMinutes(random.nextInt(60));
            int totalActive = 120 + random.nextInt(240);

            UserSession session = sessionRepository.save(UserSession.builder()
                    .anonymousId("anon-dalat-" + (1000 + i))
                    .ipAddress("14.161." + random.nextInt(250) + "." + random.nextInt(250))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0")
                    .trafficSource(sources[random.nextInt(sources.length)])
                    .utmCampaign(campaigns[random.nextInt(campaigns.length)])
                    .startTime(start)
                    .endTime(start.plusSeconds(totalActive + 180))
                    .totalDurationSeconds(totalActive)
                    .build());

            // Pageviews
            for (int p = 0; p < urls.length; p++) {
                int activeSec = 20 + random.nextInt(45);
                int idleSec = (p == 1) ? (180 + random.nextInt(120)) : random.nextInt(30); // Simulated idle on detail page

                PageView pv = pageViewRepository.save(PageView.builder()
                        .session(session)
                        .pageUrl(urls[p])
                        .pageTitle(titles[p])
                        .previousPageUrl(p > 0 ? urls[p - 1] : "/")
                        .enteredAt(start.plusSeconds(p * 50L))
                        .leftAt(start.plusSeconds((p + 1) * 50L))
                        .activeDurationSeconds(activeSec)
                        .idleDurationSeconds(idleSec)
                        .build());

                // Actions
                if (p == 0) {
                    actionRepository.save(UserAction.builder()
                            .session(session)
                            .pageView(pv)
                            .actionType("SEARCH")
                            .payload("{\"departure\":\"TP.HCM\",\"destination\":\"Đà Lạt\",\"vehicle\":\"Limousine\",\"time\":\"21:00\"}")
                            .createdAt(start.plusSeconds(15))
                            .build());
                } else if (p == 1) {
                    actionRepository.save(UserAction.builder()
                            .session(session)
                            .pageView(pv)
                            .actionType("VIEW_DETAIL")
                            .payload("{\"tripId\":1,\"price\":250000,\"vehicle\":\"Limousine 34 phòng VIP\"}")
                            .createdAt(start.plusSeconds(70))
                            .build());
                } else if (p == 2) {
                    actionRepository.save(UserAction.builder()
                            .session(session)
                            .pageView(pv)
                            .actionType("SELECT_SEAT")
                            .payload("{\"seats\":[\"A04\",\"A05\"],\"total\":500000}")
                            .createdAt(start.plusSeconds(120))
                            .build());
                }
            }
        }
    }

    private void seedNhaTrangFamilyPersona(int count) {
        String[] sources = {"google", "facebook", "direct"};
        String[] campaigns = {"nhatrang-he-2026", "ve-xe-gia-dinh", "nghi-duong-bien"};
        String[] urls = {
                "/search?from=TP.HCM&to=Nha+Trang&departureTime=morning",
                "/detail/4",
                "/seat-selection/4"
        };
        String[] titles = {
                "Tìm vé xe Sài Gòn đi Nha Trang ban ngày",
                "Chuyến #4: TP.HCM - Nha Trang 08:30 (Giường nằm 40 chỗ)",
                "Chọn 3 ghế cho gia đình"
        };

        for (int i = 0; i < count; i++) {
            LocalDateTime start = LocalDateTime.now().minusHours(random.nextInt(48)).minusMinutes(random.nextInt(60));
            int totalActive = 90 + random.nextInt(180);

            UserSession session = sessionRepository.save(UserSession.builder()
                    .anonymousId("anon-nhatrang-" + (2000 + i))
                    .ipAddress("113.190." + random.nextInt(250) + "." + random.nextInt(250))
                    .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X)")
                    .trafficSource(sources[random.nextInt(sources.length)])
                    .utmCampaign(campaigns[random.nextInt(campaigns.length)])
                    .startTime(start)
                    .endTime(start.plusSeconds(totalActive + 60))
                    .totalDurationSeconds(totalActive)
                    .build());

            for (int p = 0; p < urls.length; p++) {
                PageView pv = pageViewRepository.save(PageView.builder()
                        .session(session)
                        .pageUrl(urls[p])
                        .pageTitle(titles[p])
                        .previousPageUrl(p > 0 ? urls[p - 1] : "/")
                        .enteredAt(start.plusSeconds(p * 40L))
                        .activeDurationSeconds(25 + random.nextInt(35))
                        .idleDurationSeconds(random.nextInt(40))
                        .build());

                if (p == 0) {
                    actionRepository.save(UserAction.builder()
                            .session(session)
                            .pageView(pv)
                            .actionType("SEARCH")
                            .payload("{\"departure\":\"TP.HCM\",\"destination\":\"Nha Trang\",\"preference\":\"Gia đình, xuất phát sáng\"}")
                            .createdAt(start.plusSeconds(10))
                            .build());
                } else if (p == 1) {
                    actionRepository.save(UserAction.builder()
                            .session(session)
                            .pageView(pv)
                            .actionType("FILTER_PRICE")
                            .payload("{\"minPrice\":250000,\"maxPrice\":450000}")
                            .createdAt(start.plusSeconds(50))
                            .build());
                }
            }
        }
    }

    private void seedBudgetSaverPersona(int count) {
        String[] sources = {"tiktok", "direct", "google"};
        String[] campaigns = {"san-deal-sinh-vien", "ve-re-ve-que", "tiet-kiem"};
        String[] urls = {
                "/search?sort=price_asc",
                "/search?to=Vũng+Tàu&price=under200k",
                "/promotions"
        };
        String[] titles = {
                "Danh sách vé giá rẻ nhất",
                "Chuyến đi Vũng Tàu giá dưới 150k",
                "Mã giảm giá vé xe sinh viên"
        };

        for (int i = 0; i < count; i++) {
            LocalDateTime start = LocalDateTime.now().minusHours(random.nextInt(96)).minusMinutes(random.nextInt(60));
            int totalActive = 60 + random.nextInt(120);

            UserSession session = sessionRepository.save(UserSession.builder()
                    .anonymousId("anon-budget-" + (3000 + i))
                    .ipAddress("27.72." + random.nextInt(250) + "." + random.nextInt(250))
                    .userAgent("Mozilla/5.0 (Linux; Android 14; SM-G998B)")
                    .trafficSource(sources[random.nextInt(sources.length)])
                    .utmCampaign(campaigns[random.nextInt(campaigns.length)])
                    .startTime(start)
                    .endTime(start.plusSeconds(totalActive + 30))
                    .totalDurationSeconds(totalActive)
                    .build());

            for (int p = 0; p < urls.length; p++) {
                PageView pv = pageViewRepository.save(PageView.builder()
                        .session(session)
                        .pageUrl(urls[p])
                        .pageTitle(titles[p])
                        .previousPageUrl(p > 0 ? urls[p - 1] : "/")
                        .enteredAt(start.plusSeconds(p * 30L))
                        .activeDurationSeconds(15 + random.nextInt(25))
                        .idleDurationSeconds(random.nextInt(20))
                        .build());

                if (p == 0) {
                    actionRepository.save(UserAction.builder()
                            .session(session)
                            .pageView(pv)
                            .actionType("FILTER_PRICE")
                            .payload("{\"sort\":\"price_asc\",\"maxPrice\":180000}")
                            .createdAt(start.plusSeconds(12))
                            .build());
                }
            }
        }
    }
}

