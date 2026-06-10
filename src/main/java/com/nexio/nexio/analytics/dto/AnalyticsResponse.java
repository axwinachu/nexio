package com.nexio.nexio.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnalyticsResponse {
    private long totalApplications;
    private long applied;
    private long assessment;
    private long interview;
    private long offer;
    private long rejected;
    private double responseRate;
    private List<WeeklyCount> weeklyApplications;
}
