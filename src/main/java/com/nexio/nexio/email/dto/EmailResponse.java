package com.nexio.nexio.email.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailResponse {
    private Long id;
    private String sender;
    private String subject;
    private String body;
    private LocalDateTime receivedAt;
    private boolean jobRelated;
}
