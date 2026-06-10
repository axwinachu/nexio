package com.nexio.nexio.jobs.facade;

import com.nexio.nexio.config.ExtractionConfig;
import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
import com.nexio.nexio.jobs.dto.ExtractionResult;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import com.nexio.nexio.jobs.model.JobApplication;
import com.nexio.nexio.jobs.service.GeminiExtractionService;
import com.nexio.nexio.jobs.service.JobApplicationService;
import com.nexio.nexio.user.model.User;
import com.nexio.nexio.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobExtractionFacade {

    private final EmailMessageService emailMessageService;
    private final JobApplicationService jobApplicationService;
    private final UserService userService;
    private final GeminiExtractionService geminiExtractionService;
    private final ExtractionConfig extractionConfig;

    // ── Absolute noise senders — even strong keywords can't save these ────────
    // Only put senders here that NEVER send real application process emails
    private static final List<String> ABSOLUTE_NOISE_SENDERS = List.of(
            "medium.com",
            "substack.com",
            "beehiiv.com",
            "propeers.in",
            "abekus.co",
            "newsletters-noreply@linkedin.com",
            "messages-noreply@linkedin.com",
            "ambitionbox.com",
            "jobs2web.com"
    );

    // ── Portal senders — blocked ONLY if no strong keyword match ─────────────
    // Naukri/LinkedIn/Indeed CAN send real "your application was sent" emails
    private static final List<String> PORTAL_SENDER_DOMAINS = List.of(
            "glassdoor.com",
            "jobalert.indeed.com",
            "monsterindia.com",
            "naukrialerts@naukri.com",
            "eventnc@naukri.com",
            "informationnc@naukri.com",
            "alertnc@naukri.com",
            "recommendationnc@naukri.com",
            "hirist.tech",
            "jobfeed.hirist.com",
            "apna-jobs.com"
    );

    // ── Noise subject patterns ────────────────────────────────────────────────
    private static final List<String> NOISE_SUBJECT_PATTERNS = List.of(
            "jobs that you haven't applied",
            "jobs for you. apply now",
            "new jobs in",
            "new jobs posted from",
            "job recommendations for you",
            "jobs curated for you",
            "top tech jobs",
            "matching jobs from",
            "apply to jobs at",
            "you would be a great fit",
            "others are hiring for",
            "fast track your job search",
            "quiz is open for registration",
            "last few days to enroll",
            "walk-in interview",
            "mega walk-in",
            "walk in interview",
            "we are hiring!! join us",
            "career conversations",
            "40 lpa offer",
            "salary up to",
            "hiring support",
            "free webinar",
            "onboarding #",
            "show your sql expertise",
            "interview questions",
            "deep dive",
            "weekly update",
            "job search feels stuck",
            "guaranteed interview calls",
            "viewed your profile",
            "profile view",
            "new connection",
            "work anniversary",
            "weekly jobs digest",
            "daily digest",
            "newsletter",
            "unsubscribe",
            "urgent requirement for the role",
            "continue where you left",
            "career step"
    );

    // ── Strong confirmation keywords — these always pass to Gemini ───────────
    private static final List<String> STRONG_JOB_CONFIRMATIONS = List.of(
            "your application was sent",
            "application received",
            "application submitted",
            "successfully applied",
            "thank you for applying",
            "thank you for your application",
            "we have received your application",
            "your application has been received",
            "application successful",
            "you have successfully submitted",
            "interview invite",
            "interview invitation",
            "invited for interview",
            "interview scheduled",
            "you have been shortlisted",
            "shortlisted for",
            "offer letter",
            "job offer",
            "employment offer",
            "you have been selected",
            "selected for the role",
            "welcome to the team",
            "not moving forward",
            "regret to inform",
            "not been shortlisted",
            "application unsuccessful",
            "unfortunately we",
            "unfortunately, we",
            "position has been filled",
            "complete your assessment",
            "coding challenge",
            "coding test",
            "hackerrank",
            "hackerearth",
            "mettl",
            "amcat"
    );

    // ── Main extraction method ────────────────────────────────────────────────

    @Transactional
    public ExtractionResult extractJobsFromEmails(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<EmailMessage> jobEmails =
                emailMessageService.findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(userId);

        log.info("Total job-related emails to process: {}", jobEmails.size());

        int created = 0;
        int updated = 0;
        int skippedDuplicate = 0;
        int skippedNoise = 0;
        int skippedByGemini = 0;

        for (EmailMessage email : jobEmails) {

            // 1. Skip already extracted emails
            if (jobApplicationService.existsByUserIdAndSourceEmailId(
                    userId, email.getId())) {
                skippedDuplicate++;
                continue;
            }

            // 2. Check if this should be skipped as noise
            NoiseCheckResult noiseCheck = checkNoise(email.getSubject(),
                    email.getSender(), email.getBody());

            if (noiseCheck == NoiseCheckResult.HARD_NOISE) {
                log.debug("HARD NOISE skipped: {}", email.getSubject());
                skippedNoise++;
                continue;
            }

            // 3. Send to Gemini for full analysis
            log.debug("Sending to Gemini → subject: '{}' | bodyLength: {}",
                    email.getSubject(),
                    email.getBody() != null ? email.getBody().length() : 0);

            GeminiExtractionService.GeminiResult result =
                    geminiExtractionService.extract(email.getSubject(), email.getBody());

            // 4. Gemini call failed
            if (result == null) {
                log.warn("Gemini returned null for: {}", email.getSubject());
                skippedNoise++;
                continue;
            }

            // 5. Gemini says not a real application
            if (!result.isJobApplication()) {
                log.debug("Gemini rejected: {}", email.getSubject());
                skippedByGemini++;
                continue;
            }

            log.info("Gemini confirmed job email → company: '{}' | position: '{}' | status: {} | subject: '{}'",
                    result.company(), result.position(), result.status(), email.getSubject());

            LocalDateTime appliedAt = email.getReceivedAt() != null
                    ? email.getReceivedAt()
                    : LocalDateTime.now();

            // 6. Check if same company + position already exists → update status
            Optional<JobApplication> existing =
                    findExistingJob(userId, result.company(), result.position());

            if (existing.isPresent()) {
                JobApplication job = existing.get();
                boolean changed = false;

                if (shouldUpdateStatus(job.getStatus(), result.status())) {
                    log.info("Status update: {} → {} for {}",
                            job.getStatus(), result.status(), result.company());
                    job.setStatus(result.status());
                    changed = true;
                }

                // Fill in position if it was missing
                if ((job.getPosition() == null || job.getPosition().isBlank())
                        && result.position() != null
                        && !result.position().isBlank()) {
                    job.setPosition(result.position());
                    changed = true;
                }

                // Keep earliest received date as appliedAt
                if (email.getReceivedAt() != null
                        && (job.getAppliedAt() == null
                        || email.getReceivedAt().isBefore(job.getAppliedAt()))) {
                    job.setAppliedAt(appliedAt);
                    changed = true;
                }

                job.setSourceEmail(email);
                job.setEmailSubject(email.getSubject());
                jobApplicationService.save(job);

                if (changed) {
                    updated++;
                } else {
                    skippedDuplicate++;
                }
                continue;
            }

            // 7. Create new job application
            JobApplication job = JobApplication.builder()
                    .user(user)
                    .sourceEmail(email)
                    .company(result.company())
                    .position(result.position())
                    .status(result.status())
                    .appliedAt(appliedAt)
                    .emailSubject(email.getSubject())
                    .build();

            jobApplicationService.save(job);
            created++;
            log.info("Created: {} | {} | {}", result.company(), result.status(), email.getSubject());
        }

        log.info("Extraction done → created: {} | updated: {} | duplicates: {} | noise: {} | gemini rejected: {}",
                created, updated, skippedDuplicate, skippedNoise, skippedByGemini);

        return ExtractionResult.builder()
                .created(created)
                .updated(updated)
                .skippedDuplicate(skippedDuplicate)
                .skippedNoise(skippedNoise)
                .build();
    }

    // ── Noise check ───────────────────────────────────────────────────────────

    private enum NoiseCheckResult {
        HARD_NOISE,   // definitely skip — don't send to Gemini
        PASS          // send to Gemini for final decision
    }

    private NoiseCheckResult checkNoise(String subject, String sender, String body) {

        // Always skip self-sent emails
        String selfEmail = extractionConfig.getSelfEmail();
        if (sender != null && selfEmail != null && !selfEmail.isBlank()) {
            if (sender.toLowerCase().contains(selfEmail.toLowerCase())) {
                return NoiseCheckResult.HARD_NOISE;
            }
        }

        // Always skip absolute noise senders regardless of content
        if (sender != null) {
            String lowerSender = sender.toLowerCase();
            if (ABSOLUTE_NOISE_SENDERS.stream().anyMatch(lowerSender::contains)) {
                return NoiseCheckResult.HARD_NOISE;
            }
        }

        // Build search text for keyword check
        String searchText = (
                (subject != null ? subject : "") + " " +
                        (body != null ? body.substring(0, Math.min(body.length(), 800)) : "")
        ).toLowerCase();

        // If strong confirmation keyword found → always pass to Gemini
        // This handles Naukri/LinkedIn "your application was sent" emails
        if (STRONG_JOB_CONFIRMATIONS.stream().anyMatch(searchText::contains)) {
            return NoiseCheckResult.PASS;
        }

        // Now filter portal sender domains (they didn't have strong keywords above)
        if (sender != null) {
            String lowerSender = sender.toLowerCase();
            if (PORTAL_SENDER_DOMAINS.stream().anyMatch(lowerSender::contains)) {
                return NoiseCheckResult.HARD_NOISE;
            }
        }

        // Filter noise subject patterns
        if (subject != null) {
            String lowerSubject = subject.toLowerCase();
            if (NOISE_SUBJECT_PATTERNS.stream().anyMatch(lowerSubject::contains)) {
                return NoiseCheckResult.HARD_NOISE;
            }
        }

        return NoiseCheckResult.PASS;
    }

    // ── Existing job lookup ───────────────────────────────────────────────────

    private Optional<JobApplication> findExistingJob(
            Long userId, String company, String position) {
        if (company == null || company.isBlank() || company.equals("Unknown Company")) {
            return Optional.empty();
        }

        List<JobApplication> matches =
                jobApplicationService.findByUserIdAndCompanyIgnoreCase(userId, company);

        return matches.stream()
                .filter(job -> positionsMatch(job.getPosition(), position))
                .findFirst();
    }

    private boolean positionsMatch(String existing, String incoming) {
        // If either is null/blank, treat as same job (same company, unknown position)
        if (existing == null || existing.isBlank()
                || incoming == null || incoming.isBlank()) {
            return true;
        }
        return existing.equalsIgnoreCase(incoming.trim());
    }

    // ── Status update logic ───────────────────────────────────────────────────

    private boolean shouldUpdateStatus(
            ApplicationStatus current, ApplicationStatus incoming) {
        // Never downgrade from OFFER
        if (current == ApplicationStatus.OFFER) return false;

        // Always allow REJECTED (unless already at OFFER, handled above)
        if (incoming == ApplicationStatus.REJECTED) return true;

        // Never overwrite REJECTED with something lower
        if (current == ApplicationStatus.REJECTED) return false;

        // Only upgrade to a higher stage
        return statusRank(incoming) > statusRank(current);
    }

    private int statusRank(ApplicationStatus status) {
        return switch (status) {
            case APPLIED     -> 1;
            case ASSESSMENT  -> 2;
            case INTERVIEW   -> 3;
            case OFFER       -> 4;
            case REJECTED    -> 0;
        };
    }
}