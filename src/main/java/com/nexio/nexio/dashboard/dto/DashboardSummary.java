package com.nexio.nexio.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummary {
    private long totalApplications;
    private long applied;
    private long assessment;
    private long interview;
    private long offer;
    private long rejected;
}
