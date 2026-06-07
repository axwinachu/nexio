package com.nexio.nexio.jobs.service;

import com.nexio.nexio.jobs.dto.JobApplicationResponse;
import com.nexio.nexio.jobs.dto.UpdateStatusRequest;
import com.nexio.nexio.jobs.facade.JobExtractionFacade;
import com.nexio.nexio.jobs.mapper.JobMapper;
import com.nexio.nexio.jobs.model.JobApplication;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobFacade {
    private final JobApplicationService jobApplicationService;
    private final JobExtractionFacade jobExtractionFacade;
    private final JobMapper jobmapper;

    public List<JobApplicationResponse> getJobs(Long userId){
        return jobApplicationService.findByUserIdOrderByAppliedAtDesc(userId).stream()
                .map(jobmapper::toResponse).toList();
    }
    @Transactional
    public JobApplicationResponse updateStatus(Long jobId, Long userId, UpdateStatusRequest request){
        JobApplication job=jobApplicationService
                .findByIdAndUserId(jobId,userId)
                .orElseThrow(()->new RuntimeException("Job not found with id: " + jobId));
        job.setStatus(request.getStatus());
        return jobmapper.toResponse(jobApplicationService.save(job));
    }
    @Transactional
    public int extractFromEmails(Long userId) {
        return jobExtractionFacade.extractJobsFromEmails(userId);
    }
}
