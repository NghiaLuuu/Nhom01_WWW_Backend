package vn.edu.iuh.nhom01_www.report.revenue.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.nhom01_www.common.dto.ApiResponse;
import vn.edu.iuh.nhom01_www.report.revenue.dto.RevenueReportDtos.Options;
import vn.edu.iuh.nhom01_www.report.revenue.dto.RevenueReportDtos.Report;
import vn.edu.iuh.nhom01_www.report.revenue.service.RevenueReportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports/revenue")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('VIEW_STATISTICS')")
public class RevenueReportController {
    private final RevenueReportService service;

    public RevenueReportController(RevenueReportService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Report> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String groupBy,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String paymentMethod) {
        return ApiResponse.success(service.report(from, to, groupBy, routeId, paymentMethod), "Báo cáo khoản thu ghi nhận thành công.");
    }

    @GetMapping("/options")
    public ApiResponse<Options> options() {
        return ApiResponse.success(service.options());
    }
}
