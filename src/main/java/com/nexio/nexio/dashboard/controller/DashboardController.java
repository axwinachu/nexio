package com.nexio.nexio.dashboard.controller;

import com.nexio.nexio.dashboard.dto.DashboardSummary;
import com.nexio.nexio.dashboard.dto.JobApplicationResponse;
import com.nexio.nexio.dashboard.facade.DashboardFacade;
import com.nexio.nexio.dashboard.service.DashboardService;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
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
public class DashboardController {
    private final DashboardFacade dashboardFacade;

    @GetMapping("/summary")
    public DashboardSummary getSummary(HttpServletRequest request){
        Long userId=(Long) request.getAttribute("userId");
        return dashboardFacade.getSummary(userId);
    }

    @GetMapping("/applied")
    public List<JobApplicationResponse> getApplied(HttpServletRequest request){
        Long userId=(Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId,ApplicationStatus.APPLIED);
    }

    @GetMapping("/assessment")
    public List<JobApplicationResponse> getAssessment(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.ASSESSMENT);
    }
    @GetMapping("/interview")
    public List<JobApplicationResponse> getInterview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.INTERVIEW);
    }

    @GetMapping("/offer")
    public List<JobApplicationResponse> getOffer(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.OFFER);
    }

    @GetMapping("/rejected")
    public List<JobApplicationResponse> getRejected(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, ApplicationStatus.REJECTED);
    }

    @GetMapping("/category/{status}")
    public List<JobApplicationResponse> getByCategory(
            HttpServletRequest request,
            @PathVariable ApplicationStatus status) {
        Long userId = (Long) request.getAttribute("userId");
        return dashboardFacade.getByStatus(userId, status);
    }

}
