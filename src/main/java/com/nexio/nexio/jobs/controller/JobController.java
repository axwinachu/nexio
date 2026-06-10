package com.nexio.nexio.jobs.controller;

import com.nexio.nexio.jobs.dto.CreateJobRequest;
import com.nexio.nexio.jobs.dto.ExtractionResult;
import com.nexio.nexio.jobs.dto.JobApplicationResponse;
import com.nexio.nexio.jobs.dto.UpdateStatusRequest;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.service.JobFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job application tracking")
@SecurityRequirement(name = "Bearer Authentication")
public class JobController {

    private final JobFacade jobFacade;

    @Operation(summary = "Get job applications with optional status filter and search")
    @GetMapping
    public List<JobApplicationResponse> getJobs(
            HttpServletRequest request,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search) {
        Long userId = (Long) request.getAttribute("userId");
        return jobFacade.getJobs(userId, status, search);
    }

    @Operation(summary = "Get a single job application")
    @GetMapping("/{id}")
    public JobApplicationResponse getJob(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return jobFacade.getJob(id, userId);
    }

    @Operation(summary = "Create a job application manually")
    @PostMapping
    public JobApplicationResponse createJob(
            HttpServletRequest request,
            @Valid @RequestBody CreateJobRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return jobFacade.createJob(userId, body);
    }

    @Operation(summary = "Update job application status")
    @PutMapping("/{id}/status")
    public JobApplicationResponse updateStatus(
            @PathVariable Long id,
            HttpServletRequest request,
            @Valid @RequestBody UpdateStatusRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return jobFacade.updateStatus(id, userId, body);
    }

    @Operation(summary = "Delete a job application")
    @DeleteMapping("/{id}")
    public Map<String, String> deleteJob(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        jobFacade.deleteJob(id, userId);
        return Map.of("message", "Job deleted");
    }

    @Operation(summary = "Extract job applications from synced emails")
    @PostMapping("/extract")
    public Map<String, Object> extract(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ExtractionResult result = jobFacade.extractFromEmails(userId);
        return Map.of(
                "message", "Extraction complete",
                "jobsCreated", result.getCreated(),
                "jobsUpdated", result.getUpdated(),
                "skippedDuplicate", result.getSkippedDuplicate(),
                "skippedNoise", result.getSkippedNoise()
        );
    }
}
