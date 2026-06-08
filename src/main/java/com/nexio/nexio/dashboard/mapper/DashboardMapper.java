package com.nexio.nexio.dashboard.mapper;

import com.nexio.nexio.dashboard.dto.JobApplicationResponse;
import com.nexio.nexio.jobs.model.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {
    public JobApplicationResponse toResponse(JobApplication job) {
        return JobApplicationResponse.builder()
                .id(job.getId())
                .company(job.getCompany())
                .position(job.getPosition())
                .status(job.getStatus())
                .appliedAt(job.getAppliedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
