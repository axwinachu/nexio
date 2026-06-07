package com.nexio.nexio.jobs.controller;

import com.nexio.nexio.jobs.dto.JobApplicationResponse;
import com.nexio.nexio.jobs.dto.UpdateStatusRequest;
import com.nexio.nexio.jobs.service.JobFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobFacade jobFacade;

    @GetMapping
    public List<JobApplicationResponse> getJobs(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return jobFacade.getJobs(userId);
    }

    @PutMapping("/{id}/status")
    public JobApplicationResponse updateStatus(
            @PathVariable Long id,
            HttpServletRequest request,
            @Valid @RequestBody UpdateStatusRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return jobFacade.updateStatus(id, userId, body);
    }

    @PostMapping("/extract")
    public Map<String, Object> extract(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int created = jobFacade.extractFromEmails(userId);
        return Map.of("message", "Extraction complete", "jobsCreated", created);
    }
}