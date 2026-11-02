package com.example.tax.statistics.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.statistics.service.StatisticsService;
import com.example.tax.statistics.vo.DashboardVO;
import com.example.tax.statistics.vo.OverviewVO;
import com.example.tax.statistics.vo.StatusStat;
import com.example.tax.statistics.vo.TaxpayerTypeStat;
import com.example.tax.statistics.vo.TrendStat;
import com.example.tax.statistics.vo.TypeStat;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@PreAuthorize("hasAuthority('statistics:read')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewVO> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long typeId) {
        return ApiResponse.ok(statisticsService.overview(from, to, typeId));
    }

    @GetMapping("/declarations/trend")
    public ApiResponse<List<TrendStat>> trend(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long typeId) {
        return ApiResponse.ok(statisticsService.trend(months, from, to, typeId));
    }

    @GetMapping("/declarations/by-status")
    public ApiResponse<List<StatusStat>> byStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long typeId) {
        return ApiResponse.ok(statisticsService.byStatus(from, to, typeId));
    }

    @GetMapping("/declarations/by-type")
    public ApiResponse<List<TypeStat>> byType(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long typeId) {
        return ApiResponse.ok(statisticsService.byType(from, to, typeId));
    }

    @GetMapping("/declarations/by-taxpayer-type")
    public ApiResponse<List<TaxpayerTypeStat>> byTaxpayerType(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long typeId) {
        return ApiResponse.ok(statisticsService.byTaxpayerType(from, to, typeId));
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardVO> dashboard() {
        return ApiResponse.ok(statisticsService.dashboard());
    }
}
