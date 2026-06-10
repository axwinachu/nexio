package com.nexio.nexio.analytics.controller;

import com.nexio.nexio.analytics.dto.AnalyticsResponse;
import com.nexio.nexio.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Application analytics")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get application analytics")
    @GetMapping
    public AnalyticsResponse getAnalytics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return analyticsService.getAnalytics(userId);
    }
}
