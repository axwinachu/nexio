package com.nexio.nexio.jobs.service;

import com.nexio.nexio.jobs.dto.CreateJobRequest;
import com.nexio.nexio.jobs.dto.ExtractionResult;
import com.nexio.nexio.jobs.dto.JobApplicationResponse;
import com.nexio.nexio.jobs.dto.UpdateStatusRequest;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.facade.JobExtractionFacade;
import com.nexio.nexio.jobs.mapper.JobMapper;
import com.nexio.nexio.jobs.model.JobApplication;
import com.nexio.nexio.user.model.User;
import com.nexio.nexio.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JobFacade {
    private final JobApplicationService jobApplicationService;
    private final JobExtractionFacade jobExtractionFacade;
    private final JobMapper jobmapper;
    private final UserService userService;

    public List<JobApplicationResponse> getJobs(Long userId, ApplicationStatus status, String search) {
        List<JobApplication> jobs;
        if (status != null || (search != null && !search.isBlank())) {
            jobs = jobApplicationService.searchJobs(userId, status, search != null ? search.trim() : null);
        } else {
            jobs = jobApplicationService.findByUserIdOrderByAppliedAtDesc(userId);
        }
        return jobs.stream().map(jobmapper::toResponse).toList();
    }

    public JobApplicationResponse getJob(Long jobId, Long userId) {
        JobApplication job = jobApplicationService.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        return jobmapper.toResponse(job);
    }

    @Transactional
    public JobApplicationResponse updateStatus(Long jobId, Long userId, UpdateStatusRequest request) {
        JobApplication job = jobApplicationService
                .findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        job.setStatus(request.getStatus());
        return jobmapper.toResponse(jobApplicationService.save(job));
    }

    @Transactional
    public JobApplicationResponse createJob(Long userId, CreateJobRequest request) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        JobApplication job = JobApplication.builder()
                .user(user)
                .company(request.getCompany().trim())
                .position(request.getPosition() != null ? request.getPosition().trim() : null)
                .status(request.getStatus())
                .appliedAt(LocalDateTime.now())
                .build();

        return jobmapper.toResponse(jobApplicationService.save(job));
    }

    @Transactional
    public void deleteJob(Long jobId, Long userId) {
        JobApplication job = jobApplicationService.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        jobApplicationService.delete(job);
    }

    @Transactional
    public ExtractionResult extractFromEmails(Long userId) {
        return jobExtractionFacade.extractJobsFromEmails(userId);
    }
}
