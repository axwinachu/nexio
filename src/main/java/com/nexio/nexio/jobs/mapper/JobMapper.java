package com.nexio.nexio.jobs.mapper;

import com.nexio.nexio.jobs.dto.JobApplicationResponse;
import com.nexio.nexio.jobs.model.JobApplication;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {
    public JobApplicationResponse toResponse(JobApplication job) {
        String subject = job.getEmailSubject();
        if (subject == null && job.getSourceEmail() != null) {
            subject = job.getSourceEmail().getSubject();
        }

        return JobApplicationResponse.builder()
                .id(job.getId())
                .company(job.getCompany())
                .position(job.getPosition())
                .status(job.getStatus())
                .sourceEmailId(job.getSourceEmail() != null ? job.getSourceEmail().getId() : null)
                .emailSubject(subject)
                .appliedAt(job.getAppliedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
