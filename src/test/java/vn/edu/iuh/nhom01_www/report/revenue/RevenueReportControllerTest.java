package vn.edu.iuh.nhom01_www.report.revenue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.iuh.nhom01_www.common.exception.CustomException;
import vn.edu.iuh.nhom01_www.report.revenue.controller.RevenueReportController;
import vn.edu.iuh.nhom01_www.report.revenue.controller.RevenueReportExceptionHandler;
import vn.edu.iuh.nhom01_www.report.revenue.dto.RevenueReportDtos.*;
import vn.edu.iuh.nhom01_www.report.revenue.service.RevenueReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RevenueReportControllerTest {
    private RevenueReportService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RevenueReportService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RevenueReportController(service))
                .setControllerAdvice(new RevenueReportExceptionHandler()).build();
    }

    @Test
    void parsesFiltersAndPreservesLargeDecimalAmountsAsJsonStrings() throws Exception {
        var from = LocalDate.of(2026, 11, 1);
        var to = from.plusDays(2);
        var report = new Report(new Filter(from, to, GroupBy.DAY, 7L, "CASH"), "VND", "Asia/Ho_Chi_Minh", "PAYMENT_CREATED_AT",
                "2026-11-03T12:00:00+07:00", false,
                new Summary(new BigDecimal("900719925474099312345.75"), 1, 1, BigDecimal.ZERO, 0, 0),
                List.of(), List.of(), List.of(), List.of());
        when(service.report(from, to, "DAY", 7L, "CASH")).thenReturn(report);
        mvc.perform(get("/api/admin/reports/revenue").param("from", from.toString()).param("to", to.toString())
                        .param("groupBy", "DAY").param("routeId", "7").param("paymentMethod", "CASH"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.amount").isString())
                .andExpect(jsonPath("$.data.summary.amount").value("900719925474099312345.75"))
                .andExpect(jsonPath("$.data.refundDataAvailable").value(false))
                .andExpect(jsonPath("$.data.filter.from").value("2026-11-01"));
        verify(service).report(from, to, "DAY", 7L, "CASH");
    }

    @Test
    void returnsOptionsUsingTheExistingApiEnvelope() throws Exception {
        when(service.options()).thenReturn(new Options(List.of(new RouteOption(1L, "TP.HCM", "Đà Lạt")), List.of("CASH")));
        mvc.perform(get("/api/admin/reports/revenue/options")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routes[0].id").value(1)).andExpect(jsonPath("$.data.paymentMethods[0]").value("CASH"));
    }

    @Test
    void rejectsMalformedDatesAndRouteIds() throws Exception {
        mvc.perform(get("/api/admin/reports/revenue").param("from", "2026-02-30"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/admin/reports/revenue").param("routeId", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void preservesBusinessValidationErrors() throws Exception {
        when(service.report(any(), any(), anyString(), any(), any()))
                .thenThrow(new CustomException("Khoảng ngày không hợp lệ.", HttpStatus.BAD_REQUEST));
        mvc.perform(get("/api/admin/reports/revenue")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Khoảng ngày không hợp lệ."));
    }

    @Test
    void returnsForbiddenRatherThanAnInternalError() throws Exception {
        when(service.options()).thenThrow(new AccessDeniedException("denied"));
        mvc.perform(get("/api/admin/reports/revenue/options")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cần quyền Admin hoặc VIEW_STATISTICS để xem báo cáo."));
    }

    @Test
    void doesNotLeakSqlOrDatabaseErrorsToClients() throws Exception {
        when(service.options()).thenThrow(new DataAccessResourceFailureException("secret JDBC SQL details"));
        mvc.perform(get("/api/admin/reports/revenue/options")).andExpect(status().isServiceUnavailable())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret JDBC"))));
    }
}
