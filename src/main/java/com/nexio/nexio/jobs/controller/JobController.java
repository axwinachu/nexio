package com.nexio.nexio.jobs.controller;

import com.nexio.nexio.jobs.dto.JobApplicationResponse;
import com.nexio.nexio.jobs.dto.UpdateStatusRequest;
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

    @Operation(summary = "Get all job applications")
    @GetMapping
    public List<JobApplicationResponse> getJobs(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return jobFacade.getJobs(userId);
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

    @Operation(summary = "Extract job applications from synced emails")
    @PostMapping("/extract")
    public Map<String, Object> extract(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int created = jobFacade.extractFromEmails(userId);
        return Map.of("message", "Extraction complete", "jobsCreated", created);
    }
}