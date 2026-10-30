package vn.edu.iuh.nhom01_www.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực OTP - Đăng ký tài khoản");
            helper.setText("Chào bạn,\n\nMã xác thực OTP của bạn là: " + otp + "\n\nMã này sẽ hết hạn trong 5 phút.\n\nTrân trọng,\nĐội ngũ hỗ trợ.");
            
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendBookingConfirmationEmail(String toEmail, String customerName, Long bookingId,
                                             String departure, String destination, String departureTime,
                                             String licensePlate, String vehicleType, String seatNames,
                                             String totalPrice, List<String> ticketCodes) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🚌 [VEXE] Xác nhận đặt vé thành công - Đơn hàng #" + bookingId);

            StringBuilder ticketCodeListHtml = new StringBuilder();
            if (ticketCodes != null) {
                for (String code : ticketCodes) {
                    ticketCodeListHtml.append("<li style='margin-bottom: 4px; font-weight: bold; color: #1e40af;'>").append(code).append("</li>");
                }
            }

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                    <div style="background-color: #2563eb; color: white; padding: 20px; text-align: center;">
                        <h2 style="margin: 0;">VEXE - Vé Xe Điện Tử</h2>
                        <p style="margin: 5px 0 0 0; font-size: 14px;">Xác nhận đặt vé và thanh toán thành công</p>
                    </div>
                    <div style="padding: 24px;">
                        <p>Kính gửi quý khách <strong>%s</strong>,</p>
                        <p>Đơn đặt vé <strong>#%d</strong> của quý khách đã được thanh toán thành công. Dưới đây là thông tin chi tiết vé xe điện tử:</p>
                        
                        <div style="background-color: #f8fafc; border-left: 4px solid #2563eb; padding: 16px; margin: 20px 0; border-radius: 4px;">
                            <p style="margin: 4px 0;"><strong>Tuyến đường:</strong> %s ➔ %s</p>
                            <p style="margin: 4px 0;"><strong>Thời gian xuất bến:</strong> %s</p>
                            <p style="margin: 4px 0;"><strong>Phương tiện:</strong> %s (%s)</p>
                            <p style="margin: 4px 0;"><strong>Ghế ngồi:</strong> <span style="color: #059669; font-weight: bold;">%s</span></p>
                            <p style="margin: 4px 0;"><strong>Tổng thanh toán:</strong> <span style="color: #dc2626; font-weight: bold; font-size: 16px;">%s VNĐ</span></p>
                        </div>

                        <div>
                            <h4 style="margin-bottom: 8px; color: #1e293b;">Danh sách mã vé điện tử:</h4>
                            <ul style="padding-left: 20px; margin-top: 0;">
                                %s
                            </ul>
                        </div>

                        <p style="font-size: 13px; color: #64748b; margin-top: 24px;">
                            * Quý khách vui lòng có mặt tại bến xe trước giờ xuất bến 30 phút và xuất trình mã vé hoặc mã QR trên ứng dụng cho nhân viên soát vé.
                        </p>
                    </div>
                    <div style="background-color: #f1f5f9; padding: 16px; text-align: center; font-size: 12px; color: #64748b;">
                        Mọi thắc mắc xin vui lòng liên hệ tổng đài hỗ trợ 1900-xxxx. Chúc quý khách có một chuyến đi thượng lộ bình an!
                    </div>
                </div>
                """.formatted(
                    customerName != null ? customerName : "Quý khách",
                    bookingId,
                    departure != null ? departure : "",
                    destination != null ? destination : "",
                    departureTime != null ? departureTime : "",
                    licensePlate != null ? licensePlate : "",
                    vehicleType != null ? vehicleType : "",
                    seatNames != null ? seatNames : "",
                    totalPrice != null ? totalPrice : "0",
                    ticketCodeListHtml.toString()
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send booking confirmation email to " + toEmail + ": " + e.getMessage());
        }
    }
}
