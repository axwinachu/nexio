package com.nexio.nexio.jobs.facade;

import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.model.JobApplication;
import com.nexio.nexio.jobs.service.JobApplicationService;
import com.nexio.nexio.user.model.User;
import com.nexio.nexio.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobExtractionFacade {
    private final EmailMessageService emailMessageService;
    private final JobApplicationService jobApplicationService;
    private final UserService userService;
    private static final Map<ApplicationStatus, List<String>> STATUS_KEYWORDS = Map.of(
            ApplicationStatus.OFFER,      List.of("offer", "congratulations", "pleased to inform"),
            ApplicationStatus.REJECTED,   List.of("rejected", "rejection", "not moving forward", "unfortunately", "other candidates"),
            ApplicationStatus.INTERVIEW,  List.of("interview", "schedule a call", "speak with you"),
            ApplicationStatus.ASSESSMENT, List.of("assessment", "test", "coding challenge", "assignment"),
            ApplicationStatus.APPLIED,    List.of("application", "applied", "received your application", "thank you for applying")
    );
    private static final Pattern COMPANY_PATTERN = Pattern.compile("(?:at|to|from|with|@)\\s+([A-Z][\\w\\s&.-]{1,40}?)(?:\\s*[,.|!]|$)", Pattern.CASE_INSENSITIVE);

    @Transactional
    public int extractJobsFromEmails(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<EmailMessage> jobEmails =
                emailMessageService.findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(userId);

        int created = 0;
        for (EmailMessage email : jobEmails) {
            // Skip if already extracted
            if (jobApplicationService.existsByUserIdAndSourceEmailId(userId, email.getId())) {
                continue;
            }

            String company = extractCompany(email.getSubject(), email.getSender());
            ApplicationStatus status = detectStatus(email.getSubject());

            JobApplication job = JobApplication.builder()
                    .user(user)
                    .sourceEmail(email)
                    .company(company)
                    .position(extractPosition(email.getSubject()))
                    .status(status)
                    .build();

            jobApplicationService.save(job);
            created++;
            log.info("Created job record: {} - {} [{}]", company, status, email.getId());
        }

        log.info("Extracted {} new job applications for user {}", created, userId);
        return created;
    }
    //helper
    private String extractCompany(String subject, String sender) {
        // Try to extract from subject line first
        if (subject != null) {
            Matcher matcher = COMPANY_PATTERN.matcher(subject);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        if (sender != null && sender.contains("@")) {
            String domain = sender.replaceAll(".*@", "").replaceAll("\\..*", "");
            return capitalize(domain);
        }
        return "Unknown Company";
    }
    private ApplicationStatus detectStatus(String subject) {
        if (subject == null) return ApplicationStatus.APPLIED;
        String lower = subject.toLowerCase();
        for (ApplicationStatus status : List.of(
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.ASSESSMENT,
                ApplicationStatus.APPLIED)) {
            List<String> keywords = STATUS_KEYWORDS.get(status);
            if (keywords.stream().anyMatch(lower::contains)) {
                return status;
            }
        }
        return ApplicationStatus.APPLIED;
    }
    private String extractPosition(String subject) {
        if (subject == null) return null;
        Pattern positionPattern = Pattern.compile("(?:for|position|role)[:\\s]+([\\w\\s]+?)(?:\\s+(?:at|@|to|from)|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = positionPattern.matcher(subject);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    private String capitalize(String word) {
        if (word == null || word.isEmpty()) return word;
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

}
