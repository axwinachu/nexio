package com.nexio.nexio.jobs.repository;

import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication,Long> {
    public List<JobApplication> findByUserIdOrderByAppliedAtDesc(Long userId);

    public List<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    public boolean existsByUserIdAndSourceEmailId(Long userId, Long emailId);

    public Optional<JobApplication> findByIdAndUserId(Long id, Long userId);


    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ApplicationStatus status);
}
