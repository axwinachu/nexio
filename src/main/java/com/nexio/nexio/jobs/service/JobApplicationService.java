package com.nexio.nexio.jobs.service;

import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.model.JobApplication;
import com.nexio.nexio.jobs.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;

    public List<JobApplication> findByUserIdOrderByAppliedAtDesc(Long userId) {
        return jobApplicationRepository.findByUserIdOrderByAppliedAtDesc(userId);
    }

    public List<JobApplication> searchJobs(Long userId, ApplicationStatus status, String search) {
        return jobApplicationRepository.searchJobs(userId, status, search);
    }

    public List<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status) {
        return jobApplicationRepository.findByUserIdAndStatusOrderByAppliedAtDesc(userId, status);
    }

    public List<JobApplication> findByUserIdAndCompanyIgnoreCase(Long userId, String company) {
        return jobApplicationRepository.findByUserIdAndCompanyIgnoreCase(userId, company);
    }

    public boolean existsByUserIdAndSourceEmailId(Long userId, Long emailId) {
        return jobApplicationRepository.existsByUserIdAndSourceEmailId(userId, emailId);
    }

    public Optional<JobApplication> findByIdAndUserId(Long id, Long userId) {
        return jobApplicationRepository.findByIdAndUserId(id, userId);
    }

    public JobApplication save(JobApplication jobApplication) {
        return jobApplicationRepository.save(jobApplication);
    }

    public void delete(JobApplication job) {
        jobApplicationRepository.delete(job);
    }

    public long countByUserId(Long userId) {
        return jobApplicationRepository.countByUserId(userId);
    }

    public long countByUserIdAndStatus(Long userId, ApplicationStatus status) {
        return jobApplicationRepository.countByUserIdAndStatus(userId, status);
    }
}
