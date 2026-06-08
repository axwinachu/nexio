package com.nexio.nexio.dashboard.controller;

import com.nexio.nexio.dashboard.dto.DashboardSummary;
import com.nexio.nexio.dashboard.dto.JobApplicationResponse;
import com.nexio.nexio.dashboard.facade.DashboardFacade;
import com.nexio.nexio.dashboard.service.DashboardService;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Analytics and job application summaries")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {
    private final DashboardFacade dashboardFacade;

    @Operation(summary = "Get summary counts for all statuses")
    @GetMapping("/summary")
    public DashboardSummary getSummary(HttpServletRequest request){
        Long userId=(Long) request.getAttribute("userId");
        return dashboardFacade.getSummary(userId);
    }

    @Operation(summary = "Get all APPLIED job applications")
    @GetMapping("/applied")
    public List<JobApplicationResponse> getApplied(HttpServletRequest request){
        Long userId=(Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId,ApplicationStatus.APPLIED);
    }

    @Operation(summary = "Get all ASSESSMENT job applications")
    @GetMapping("/assessment")
    public List<JobApplicationResponse> getAssessment(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.ASSESSMENT);
    }

    @Operation(summary = "Get all INTERVIEW job applications")
    @GetMapping("/interview")
    public List<JobApplicationResponse> getInterview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.INTERVIEW);
    }

    @Operation(summary = "Get all OFFER job applications")
    @GetMapping("/offer")
    public List<JobApplicationResponse> getOffer(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.OFFER);
    }

    @Operation(summary = "Get all REJECTED job applications")
    @GetMapping("/rejected")
    public List<JobApplicationResponse> getRejected(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.REJECTED);
    }

    @Operation(summary = "Get job applications by any status dynamically")
    @GetMapping("/category/{status}")
    public List<JobApplicationResponse> getByCategory(
            HttpServletRequest request,
            @PathVariable ApplicationStatus status) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, status);
    }

}
