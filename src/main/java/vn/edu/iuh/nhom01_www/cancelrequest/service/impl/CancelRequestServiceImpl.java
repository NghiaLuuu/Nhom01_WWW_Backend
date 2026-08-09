package vn.edu.iuh.nhom01_www.cancelrequest.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.model.CancelRequest;
import vn.edu.iuh.nhom01_www.cancelrequest.repository.CancelRequestRepository;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;
import vn.edu.iuh.nhom01_www.user.model.Staff;
import vn.edu.iuh.nhom01_www.user.repository.StaffRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelRequestServiceImpl implements CancelRequestService {

    private final CancelRequestRepository cancelRequestRepository;
    private final BookingRepository bookingRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public CancelRequest submitCancelRequest(Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Booking not found", HttpStatus.NOT_FOUND));

        if (!"PAID".equals(booking.getStatus())) {
            throw new CustomException("Only PAID bookings can be cancelled", HttpStatus.BAD_REQUEST);
        }

        CancelRequest cancelRequest = CancelRequest.builder()
                .booking(booking)
                .status("PENDING")
                .rejectReason(reason)
                .build();

        return cancelRequestRepository.save(cancelRequest);
    }

    @Override
    @Transactional
    public CancelRequest resolveCancelRequest(Long cancelRequestId, UUID staffId, boolean isApproved, String rejectReason) {
        CancelRequest request = cancelRequestRepository.findById(cancelRequestId)
                .orElseThrow(() -> new CustomException("Cancel Request not found", HttpStatus.NOT_FOUND));

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new CustomException("Staff not found", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(request.getStatus())) {
            throw new CustomException("Cancel Request is already resolved", HttpStatus.BAD_REQUEST);
        }

        if (isApproved) {
            request.approve(staff);
            Booking booking = request.getBooking();
            booking.cancelBooking();
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.cancel();
                }
            }
        } else {
            request.reject(staff, rejectReason);
        }

        return cancelRequestRepository.save(request);
    }
}
