package vn.edu.iuh.nhom01_www.ticket.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.iuh.nhom01_www.ticket.service.TicketService;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketNoShowScheduler {

    private final TicketService ticketService;

    /**
     * Quét tự động mỗi 60 giây để cập nhật trạng thái các vé chưa lên xe (ISSUED)
     * của những chuyến xe đã qua giờ chốt vé sang VẮNG MẶT (NO_SHOW).
     */
    @Scheduled(fixedRate = 60000)
    public void scheduleNoShowAutoUpdate() {
        try {
            int count = ticketService.autoMarkPastDepartureNoShows();
            if (count > 0) {
                log.info("[TicketNoShowScheduler] Đã tự động cập nhật {} vé sang trạng thái NO_SHOW (Vắng mặt).", count);
            }
        } catch (Exception e) {
            log.error("[TicketNoShowScheduler] Lỗi khi tự động quét vé No-Show: {}", e.getMessage());
        }
    }
}
