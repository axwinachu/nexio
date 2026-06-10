package com.nexio.nexio.jobs.dto;

import com.nexio.nexio.jobs.enums.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateJobRequest {

    @NotBlank(message = "Company is required")
    private String company;

    private String position;

    @NotNull(message = "Status is required")
    private ApplicationStatus status;
}
