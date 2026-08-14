package vn.edu.iuh.nhom01_www.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
            // Log error
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}
