package com.nexio.nexio.analytics.service;

import com.nexio.nexio.analytics.dto.AnalyticsResponse;
import com.nexio.nexio.analytics.dto.WeeklyCount;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.model.JobApplication;
import com.nexio.nexio.jobs.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JobApplicationService jobApplicationService;

    public AnalyticsResponse getAnalytics(Long userId) {
        long total = jobApplicationService.countByUserId(userId);
        long applied = jobApplicationService.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED);
        long assessment = jobApplicationService.countByUserIdAndStatus(userId, ApplicationStatus.ASSESSMENT);
        long interview = jobApplicationService.countByUserIdAndStatus(userId, ApplicationStatus.INTERVIEW);
        long offer = jobApplicationService.countByUserIdAndStatus(userId, ApplicationStatus.OFFER);
        long rejected = jobApplicationService.countByUserIdAndStatus(userId, ApplicationStatus.REJECTED);

        long responded = interview + offer + rejected;
        double responseRate = total > 0 ? Math.round((responded * 1000.0 / total)) / 10.0 : 0;

        List<JobApplication> jobs = jobApplicationService.findByUserIdOrderByAppliedAtDesc(userId);
        List<WeeklyCount> weekly = buildWeeklyCounts(jobs);

        return AnalyticsResponse.builder()
                .totalApplications(total)
                .applied(applied)
                .assessment(assessment)
                .interview(interview)
                .offer(offer)
                .rejected(rejected)
                .responseRate(responseRate)
                .weeklyApplications(weekly)
                .build();
    }

    private List<WeeklyCount> buildWeeklyCounts(List<JobApplication> jobs) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        Map<String, Long> counts = new LinkedHashMap<>();
        LocalDateTime cutoff = LocalDateTime.now().minusWeeks(8);

        for (JobApplication job : jobs) {
            if (job.getAppliedAt() == null || job.getAppliedAt().isBefore(cutoff)) {
                continue;
            }
            int week = job.getAppliedAt().get(weekFields.weekOfWeekBasedYear());
            int year = job.getAppliedAt().get(weekFields.weekBasedYear());
            String key = year + "-W" + String.format("%02d", week);
            counts.merge(key, 1L, Long::sum);
        }

        List<WeeklyCount> result = new ArrayList<>();
        counts.forEach((week, count) -> result.add(new WeeklyCount(week, count)));
        return result;
    }
}
