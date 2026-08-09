package vn.edu.iuh.nhom01_www.payment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.nhom01_www.booking.model.Booking;
import vn.edu.iuh.nhom01_www.booking.repository.BookingRepository;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.payment.model.Payment;
import vn.edu.iuh.nhom01_www.payment.repository.PaymentRepository;
import vn.edu.iuh.nhom01_www.payment.service.PaymentService;
import vn.edu.iuh.nhom01_www.ticket.model.Ticket;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public Payment processPaymentWebhook(Long bookingId, String transactionId, String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Booking not found", HttpStatus.NOT_FOUND));

        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new CustomException("Booking is not pending payment", HttpStatus.BAD_REQUEST);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalPrice())
                .paymentMethod("PAYOS")
                .status("SUCCESS".equalsIgnoreCase(status) ? "SUCCESS" : "FAILED")
                .transactionId(transactionId)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        if ("SUCCESS".equals(payment.getStatus())) {
            booking.markAsPaid();
            if (booking.getTickets() != null) {
                for (Ticket ticket : booking.getTickets()) {
                    ticket.issue();
                }
            }
            bookingRepository.save(booking);
        }

        return savedPayment;
    }
}
