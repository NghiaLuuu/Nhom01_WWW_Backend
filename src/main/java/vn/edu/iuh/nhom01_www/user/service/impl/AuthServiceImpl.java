package vn.edu.iuh.nhom01_www.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.iuh.nhom01_www.common.security.JwtUtils;
import vn.edu.iuh.nhom01_www.common.security.UserDetailsServiceImpl;
import vn.edu.iuh.nhom01_www.common.security.model.RefreshToken;
import vn.edu.iuh.nhom01_www.common.security.service.RefreshTokenService;
import vn.edu.iuh.nhom01_www.common.service.EmailService;
import vn.edu.iuh.nhom01_www.user.dto.JwtResponse;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.dto.RegisterRequest;
import vn.edu.iuh.nhom01_www.user.dto.TokenRefreshRequest;
import vn.edu.iuh.nhom01_www.user.dto.VerifyOtpRequest;
import vn.edu.iuh.nhom01_www.user.model.Customer;
import vn.edu.iuh.nhom01_www.user.repository.CustomerRepository;
import vn.edu.iuh.nhom01_www.user.service.AuthService;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Object register(RegisterRequest request) {
        Customer existing = customerRepository.findByEmail(request.getEmail());
        if (existing != null && "ACTIVE".equals(existing.getStatus())) {
            throw new CustomException("Email is already in use", HttpStatus.BAD_REQUEST);
        }
        
        if (existing == null) {
            existing = Customer.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .fullName(request.getFullName())
                    .status("PENDING_VERIFICATION")
                    .build();
            customerRepository.save(existing);
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set("OTP:" + request.getEmail(), otp, Duration.ofMinutes(5));
        emailService.sendOtpEmail(request.getEmail(), otp);
        
        return "Registration initiated. Please check your email for the OTP.";
    }

    @Override
    public Object verifyOtp(VerifyOtpRequest request) {
        String storedOtp = redisTemplate.opsForValue().get("OTP:" + request.getEmail());
        if (storedOtp == null) {
            throw new CustomException("OTP expired or not found", HttpStatus.BAD_REQUEST);
        }
        if (!storedOtp.equals(request.getOtp())) {
            throw new CustomException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }

        Customer customer = customerRepository.findByEmail(request.getEmail());
        if (customer != null) {
            customer.setStatus("ACTIVE");
            customerRepository.save(customer);
            redisTemplate.delete("OTP:" + request.getEmail());
            return "Account verified successfully";
        }
        throw new CustomException("User not found", HttpStatus.NOT_FOUND);
    }

    @Override
    public JwtResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail());
        if (customer != null && "PENDING_VERIFICATION".equals(customer.getStatus())) {
            throw new CustomException("Account not verified. Please verify OTP first.", HttpStatus.FORBIDDEN);
        }
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());
        
        return JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .build();
    }

    @Override
    public JwtResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(refreshToken -> {
                    String userEmail = refreshToken.getUserEmail();
                    
                    // Strict rotation: delete old token
                    refreshTokenService.deleteByToken(refreshToken);
                    
                    // Issue new refresh token
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(userEmail);
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                    String newAccessToken = jwtUtils.generateToken(userDetails);
                    
                    return JwtResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .tokenType("Bearer")
                            .build();
                })
                .orElseThrow(() -> new CustomException("Refresh token is not in database!", HttpStatus.FORBIDDEN));
    }
}
