package com.nexio.nexio.dashboard.dto;

import com.nexio.nexio.jobs.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JobApplicationResponse {
    private long id;
    private String company;
    private String position;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    
}
