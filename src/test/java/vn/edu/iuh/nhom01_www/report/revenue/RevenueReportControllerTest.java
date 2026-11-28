package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import vn.edu.iuh.nhom01_www.common.config.SecurityConfig;
import vn.edu.iuh.nhom01_www.common.exception.GlobalExceptionHandler;
import vn.edu.iuh.nhom01_www.common.security.JwtAuthenticationFilter;
import vn.edu.iuh.nhom01_www.common.security.JwtUtils;
import vn.edu.iuh.nhom01_www.common.security.UserDetailsServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the existing security configuration without starting JPA, Redis, mail or AI. */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = RevenueReportControllerTest.TestConfig.class)
@TestPropertySource(properties = "jwt.expiration=3600000")
class RevenueReportControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired RevenueReportService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor viewer() {
        return user("report-viewer").roles("STAFF").authorities(new SimpleGrantedAuthority("VIEW_STATISTICS"));
    }

    private RevenueReportResponse report() {
        return new RevenueReportResponse(null, null, "MONTH", "REAL", "PAYMENT_CREATED_AT", "Asia/Ho_Chi_Minh",
                new RevenueReportResponse.Totals(new BigDecimal("250000"), 1, 0, 0, 0, 1, 1, 1, 2, 1, 1),
                List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void viewerCanReadSummaryAndTheDefaultCohortIsRealPayments() throws Exception {
        when(service.summary(any())).thenReturn(report());
        mvc.perform(get("/api/admin/revenue-reports").with(viewer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totals.successfulAmount").value(250000))
                .andExpect(jsonPath("$.data.timeBasis").value("PAYMENT_CREATED_AT"));
        var filter = ArgumentCaptor.forClass(RevenueReportFilter.class);
        verify(service).summary(filter.capture());
        assertEquals("REAL", filter.getValue().scope());
        assertNull(filter.getValue().fromDate());
    }

    @Test
    void inclusiveDateRangeGroupingAndRouteArePassedToService() throws Exception {
        when(service.summary(any())).thenReturn(report());
        mvc.perform(get("/api/admin/revenue-reports").with(viewer())
                        .param("fromDate", "2026-11-01").param("toDate", "2026-11-30")
                        .param("groupBy", "DAY").param("scope", "DEMO").param("routeId", "3"))
                .andExpect(status().isOk());
        var filter = ArgumentCaptor.forClass(RevenueReportFilter.class);
        verify(service).summary(filter.capture());
        assertEquals(LocalDate.of(2026, 11, 30), filter.getValue().toDate());
        assertEquals("DEMO", filter.getValue().scope());
        assertEquals("DAY", filter.getValue().groupBy());
        assertEquals(3L, filter.getValue().routeId());
    }

    @Test
    void transactionEndpointUsesTheSameDateAndScopeFilters() throws Exception {
        when(service.transactions(any(), eq(1), eq(15), eq("VX123"), eq("SUCCESS"), eq("AMOUNT_DESC")))
                .thenReturn(new RevenueTransactionPage(List.of(), 31, 1, 15, 3));
        mvc.perform(get("/api/admin/revenue-reports/transactions").with(viewer())
                        .param("fromDate", "2026-11-01").param("toDate", "2026-11-30").param("scope", "DEMO")
                        .param("page", "1").param("search", "VX123").param("status", "SUCCESS").param("sort", "AMOUNT_DESC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(31))
                .andExpect(jsonPath("$.data.totalPages").value(3));
        var filter = ArgumentCaptor.forClass(RevenueReportFilter.class);
        verify(service).transactions(filter.capture(), eq(1), eq(15), eq("VX123"), eq("SUCCESS"), eq("AMOUNT_DESC"));
        assertEquals("DEMO", filter.getValue().scope());
        assertEquals(LocalDate.of(2026, 11, 1), filter.getValue().fromDate());
    }

    @Test
    void bookingStatusIsIndependentFromPaymentStatusAndKeepsTheExistingContract() throws Exception {
        when(service.transactions(any(), eq(0), eq(15), isNull(), eq("SUCCESS"), eq("NEWEST"), eq("CANCELLED")))
                .thenReturn(new RevenueTransactionPage(List.of(), 1, 0, 15, 1));
        mvc.perform(get("/api/admin/revenue-reports/transactions").with(viewer())
                        .param("status", "SUCCESS").param("bookingStatus", "CANCELLED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        verify(service).transactions(any(), eq(0), eq(15), isNull(), eq("SUCCESS"), eq("NEWEST"), eq("CANCELLED"));
    }

    @Test
    void userWithoutStatisticsPermissionGets403RatherThanLegacyGeneric500() throws Exception {
        mvc.perform(get("/api/admin/revenue-reports").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/admin/revenue-reports/transactions").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousRequestsRequireLogin() throws Exception {
        mvc.perform(get("/api/admin/revenue-reports")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void invalidDateAndNumericParametersAre400WithoutModifyingGlobalAdvice() throws Exception {
        mvc.perform(get("/api/admin/revenue-reports").with(viewer()).param("fromDate", "31-11-2026"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/revenue-reports/transactions").with(viewer()).param("page", "not-a-number"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void reversedDateRangeIsRejectedBeforeServiceInvocation() throws Exception {
        mvc.perform(get("/api/admin/revenue-reports").with(viewer()).param("fromDate", "2026-12-01").param("toDate", "2026-11-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }

    @Test
    void unavailableDatabaseReturns503WithoutLeakingSqlOrCredentials() throws Exception {
        when(service.summary(any())).thenThrow(new DataAccessResourceFailureException("internal SQL and credential details"));
        mvc.perform(get("/api/admin/revenue-reports").with(viewer()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(content().string(not(containsString("credential"))));
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, RevenueReportController.class, GlobalExceptionHandler.class})
    static class TestConfig {
        @Bean RevenueReportService service() { return mock(RevenueReportService.class); }
        @Bean UserDetailsServiceImpl users() { return mock(UserDetailsServiceImpl.class); }
        @Bean JwtUtils jwt() { return mock(JwtUtils.class); }
        @Bean JwtAuthenticationFilter jwtFilter(JwtUtils jwt, UserDetailsServiceImpl users) {
            return new JwtAuthenticationFilter(jwt, users);
        }
    }
}
