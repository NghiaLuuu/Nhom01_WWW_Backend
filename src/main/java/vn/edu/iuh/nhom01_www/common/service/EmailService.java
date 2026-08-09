package vn.edu.iuh.nhom01_www.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác th?c OTP - Ðang ký tài kho?n");
        message.setText("Chào b?n,\n\nMã xác th?c OTP c?a b?n là: " + otp + "\n\nMã này s? h?t h?n trong 5 phút.\n\nTrân tr?ng,\nÐ?i ngu h? tr?.");
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log error
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}
