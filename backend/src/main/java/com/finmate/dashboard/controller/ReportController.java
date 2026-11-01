package com.finmate.dashboard.controller;

import com.finmate.common.security.UserPrincipal;
import com.finmate.dashboard.dto.MonthlyReportDto;
import com.finmate.dashboard.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Monthly financial summaries and PDF export")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get structured monthly financial report JSON")
    public ResponseEntity<MonthlyReportDto> getMonthlyReport(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "month", required = false) String month) {
        return ResponseEntity.ok(reportService.generateMonthlyReport(currentUser.getId(), month));
    }

    @GetMapping("/monthly/pdf")
    @Operation(summary = "Download formatted monthly financial report as PDF")
    public ResponseEntity<byte[]> downloadMonthlyReportPdf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "month", required = false) String month) {
        byte[] pdfBytes = reportService.generateMonthlyReportPdf(currentUser.getId(), month);
        String targetMonth = (month != null && !month.isBlank()) ? month : "current";
        String filename = "FinMate-Report-" + targetMonth + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
