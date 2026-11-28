package vn.edu.iuh.nhom01_www.cancelrequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.edu.iuh.nhom01_www.cancelrequest.controller.CancelRequestController;
import vn.edu.iuh.nhom01_www.cancelrequest.dto.CancelRequestResponse;
import vn.edu.iuh.nhom01_www.cancelrequest.service.CancelRequestService;
import vn.edu.iuh.nhom01_www.common.config.SecurityConfig;
import vn.edu.iuh.nhom01_www.common.exception.GlobalExceptionHandler;
import vn.edu.iuh.nhom01_www.common.security.JwtAuthenticationFilter;
import vn.edu.iuh.nhom01_www.common.security.JwtUtils;
import vn.edu.iuh.nhom01_www.common.security.UserDetailsServiceImpl;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MVC + the real security filter chain, without loading the application or touching its database. */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = CancelRequestControllerTest.TestConfig.class)
@TestPropertySource(properties = "jwt.expiration=3600000")
class CancelRequestControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired CancelRequestService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private CancelRequestResponse response(String status) {
        return new CancelRequestResponse(20L, null, "Khách thay đổi lịch trình", status,
                "REJECTED".equals(status) ? "Không đủ điều kiện" : null, null, null, null);
    }

    @Test
    void customerCanSendTheExistingFrontendPayload() throws Exception {
        when(service.submitCancelRequest(100L, "Khách thay đổi lịch trình")).thenReturn(response("PENDING"));
        mvc.perform(post("/api/cancel-requests").with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":100,\"reason\":\"Khách thay đổi lịch trình\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void staffReceivesThePendingQueueInsteadOfAConstantEmptyList() throws Exception {
        when(service.getRequests("PENDING")).thenReturn(List.of(response("PENDING")));
        mvc.perform(get("/api/cancel-requests").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(20));
        verify(service).getRequests("PENDING");
    }

    @Test
    void adminCanFilterHistoryWithoutChangingTheArrayResponseContract() throws Exception {
        when(service.getRequests("ALL")).thenReturn(List.of(response("APPROVED")));
        mvc.perform(get("/api/cancel-requests").param("status", "ALL").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        verify(service).getRequests("ALL");
    }

    @Test
    void staffCanApproveWithoutASuppliedOrRandomStaffId() throws Exception {
        when(service.resolveCancelRequest(20L, true, null)).thenReturn(response("APPROVED"));
        mvc.perform(patch("/api/cancel-requests/20/resolve").with(user("staff").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isApproved\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        verify(service).resolveCancelRequest(20L, true, null);
    }

    @Test
    void staffCanRejectUsingTheExistingFrontendPayload() throws Exception {
        when(service.resolveCancelRequest(20L, false, "Không đủ điều kiện")).thenReturn(response("REJECTED"));
        mvc.perform(patch("/api/cancel-requests/20/resolve").with(user("staff").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isApproved\":false,\"rejectReason\":\"Không đủ điều kiện\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void missingDecisionDoesNotSilentlyRejectTheRequest() throws Exception {
        mvc.perform(patch("/api/cancel-requests/20/resolve").with(user("staff").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void invalidCustomerSubmissionIsRejectedBeforeCallingTheService() throws Exception {
        mvc.perform(post("/api/cancel-requests").with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bookingId\":-1,\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void customerCannotReadStaffQueueOrApproveRequests() throws Exception {
        mvc.perform(get("/api/cancel-requests").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/cancel-requests/20/resolve").with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isApproved\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void staffCannotImpersonateACustomerToSubmitCancellation() throws Exception {
        mvc.perform(post("/api/cancel-requests").with(user("staff").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bookingId\":100,\"reason\":\"Lý do\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void customerCannotBypassTheRequestQueueByCancellingATicketDirectly() throws Exception {
        mvc.perform(post("/api/tickets/1000/cancel").with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Lý do\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousRequestsRequireLogin() throws Exception {
        mvc.perform(get("/api/cancel-requests")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cancel-requests").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":100,\"reason\":\"Lý do\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, CancelRequestController.class, GlobalExceptionHandler.class})
    static class TestConfig implements WebMvcConfigurer {
        @Bean CancelRequestService cancelRequestService() { return mock(CancelRequestService.class); }
        @Bean UserDetailsServiceImpl userDetailsService() { return mock(UserDetailsServiceImpl.class); }
        @Bean JwtUtils jwtUtils() { return mock(JwtUtils.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwt, UserDetailsServiceImpl users) {
            return new JwtAuthenticationFilter(jwt, users);
        }
        @Bean LocalValidatorFactoryBean requestValidator() { return new LocalValidatorFactoryBean(); }
        @Override public org.springframework.validation.Validator getValidator() { return requestValidator(); }
    }
}
