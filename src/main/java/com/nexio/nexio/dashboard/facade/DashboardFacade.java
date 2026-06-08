package com.nexio.nexio.dashboard.facade;

import com.nexio.nexio.dashboard.dto.DashboardSummary;
import com.nexio.nexio.dashboard.dto.JobApplicationResponse;
import com.nexio.nexio.dashboard.mapper.DashboardMapper;
import com.nexio.nexio.dashboard.service.DashboardService;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardFacade {
    private final DashboardService dashboardService;
    private final DashboardMapper mapper;
    @Transactional
    public DashboardSummary getSummary(Long userId) {
        long total = dashboardService.countByUserId(userId);
        long applied=dashboardService.countByUserIdStatus(userId,ApplicationStatus.APPLIED);
        long assessment=dashboardService.countByUserIdStatus(userId,ApplicationStatus.ASSESSMENT);
        long interview=dashboardService.countByUserIdStatus(userId,ApplicationStatus.INTERVIEW);
        long offer=dashboardService.countByUserIdStatus(userId,ApplicationStatus.OFFER);
        long rejected=dashboardService.countByUserIdStatus(userId,ApplicationStatus.REJECTED);
        return DashboardSummary.builder()
                .totalApplications(total)
                .applied(applied)
                .assessment(assessment)
                .interview(interview)
                .offer(offer)
                .rejected(rejected)
                .build();
    }

    public List<JobApplicationResponse> getByStatus(Long userId, ApplicationStatus applicationStatus) {
                return dashboardService
                    .findByUserIdAndStatus(userId, applicationStatus)
                    .stream()
                    .map(mapper::toResponse)
                    .toList();

    }

}
