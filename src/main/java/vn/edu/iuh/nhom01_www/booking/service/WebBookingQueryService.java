package vn.edu.iuh.nhom01_www.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.dto.WebBookingView;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebBookingQueryService {
    private final BookingRepository bookings;
    private final CancelRequestRepository cancelRequests;

    public List<WebBookingView> history(UUID customerId) {
        return views(bookings.findByCustomer_IdOrderByCreatedAtDesc(customerId));
    }

    public List<WebBookingView> management() {
        return views(bookings.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()));
    }

    private List<WebBookingView> views(List<Booking> rows) {
        if (rows.isEmpty()) return List.of();
        var requests = cancelRequests.findByBooking_IdIn(rows.stream().map(Booking::getId).toList()).stream()
                .collect(Collectors.toMap(r -> r.getBooking().getId(), r -> r));
        return rows.stream().map(b -> WebBookingView.from(b, requests.get(b.getId()))).toList();
    }
}
