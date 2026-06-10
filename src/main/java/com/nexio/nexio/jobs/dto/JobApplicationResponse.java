package com.nexio.nexio.jobs.dto;

import com.nexio.nexio.jobs.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JobApplicationResponse {
    private Long id;
    private String company;
    private String position;
    private ApplicationStatus status;
    private Long sourceEmailId;
    private String emailSubject;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
