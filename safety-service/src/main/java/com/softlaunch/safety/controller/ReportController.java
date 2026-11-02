package com.softlaunch.safety.controller;

import com.softlaunch.safety.dto.CreateReportRequest;
import com.softlaunch.safety.dto.MyReportResponse;
import com.softlaunch.safety.dto.ReportReceipt;
import com.softlaunch.safety.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/safety/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportReceipt create(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody CreateReportRequest request) {
        return reportService.create(me, request);
    }

    @GetMapping("/mine")
    public List<MyReportResponse> mine(@RequestHeader("X-User-Id") UUID me) {
        return reportService.mine(me);
    }
}
