package com.nexio.nexio.dashboard.service;

import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.model.JobApplication;
import com.nexio.nexio.jobs.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final JobApplicationRepository jobApplicationRepository;

    public List<JobApplication> findByUserIdOrderByAppliedAtDesc(Long userId){
        return jobApplicationRepository.findByUserIdOrderByAppliedAtDesc(userId);
    }
    public List<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status){
        return jobApplicationRepository.findByUserIdAndStatusOrderByAppliedAtDesc(userId, status);
    }

    public boolean existsByUserIdAndSourceEmailId(Long userId, Long emailId){
        return jobApplicationRepository.existsByUserIdAndSourceEmailId(userId,emailId);
    }

    public Optional<JobApplication> findByIdAndUserId(Long id, Long userId){
        return jobApplicationRepository.findByIdAndUserId(id,userId);
    }
    public JobApplication save(JobApplication jobApplication){
        return jobApplicationRepository.save(jobApplication);
    }
    public long countByUserId(Long userId){
        return jobApplicationRepository.countByUserId(userId);
    }

    public long countByUserIdStatus(Long userId,ApplicationStatus status){
        return jobApplicationRepository.countByUserIdAndStatus(userId,status);
    }

}
