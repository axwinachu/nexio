package com.nexio.nexio.jobs.repository;

import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByUserIdOrderByAppliedAtDesc(Long userId);

    List<JobApplication> findByUserIdAndStatusOrderByAppliedAtDesc(Long userId, ApplicationStatus status);

    List<JobApplication> findByUserIdAndCompanyIgnoreCase(Long userId, String company);

    boolean existsByUserIdAndSourceEmailId(Long userId, Long emailId);

    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    @Query("""
            SELECT j FROM JobApplication j
            WHERE j.user.id = :userId
            AND (:status IS NULL OR j.status = :status)
            AND (:search IS NULL OR :search = ''
                 OR LOWER(j.company) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(j.position) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(j.emailSubject) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY j.appliedAt DESC
            """)
    List<JobApplication> searchJobs(
            @Param("userId") Long userId,
            @Param("status") ApplicationStatus status,
            @Param("search") String search);
}
