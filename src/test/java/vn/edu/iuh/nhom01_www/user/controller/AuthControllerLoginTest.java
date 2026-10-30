package vn.edu.iuh.nhom01_www.user.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import vn.edu.iuh.nhom01_www.user.dto.JwtResponse;
import vn.edu.iuh.nhom01_www.user.dto.LoginApiResponse;
import vn.edu.iuh.nhom01_www.user.dto.LoginRequest;
import vn.edu.iuh.nhom01_www.user.dto.LoginUserResponse;
import vn.edu.iuh.nhom01_www.user.service.AuthService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerLoginTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginKeepsLegacyDataAndAddsTopLevelMobileFields() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@vexe.vn");
        request.setPassword("password123");
        LoginUserResponse user = LoginUserResponse.builder()
                .id(UUID.randomUUID())
                .username("admin@vexe.vn")
                .role("admin")
                .build();
        JwtResponse jwt = JwtResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .user(user)
                .build();
        when(authService.login(request)).thenReturn(jwt);

        ResponseEntity<LoginApiResponse> result = authController.login(request);

        assertNotNull(result.getBody());
        assertEquals("access-token", result.getBody().getToken());
        assertEquals(user, result.getBody().getUser());
        assertEquals("access-token", result.getBody().getData().getAccessToken());
        assertEquals("refresh-token", result.getBody().getData().getRefreshToken());
        assertEquals("Bearer", result.getBody().getData().getTokenType());
    }
}
