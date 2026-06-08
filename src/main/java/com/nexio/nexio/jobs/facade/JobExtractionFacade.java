package com.nexio.nexio.jobs.facade;

import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
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
    private final GeminiExtractionService geminiExtractionService;

    // ── Noise emails to skip ──────────────────────────────────────────────────
    private static final List<String> IGNORE_PATTERNS = List.of(
            "viewed your profile",
            "people viewed your profile",
            "your profile was viewed",
            "profile view",
            "new connection",
            "accepted your connection",
            "suggested job",
            "jobs you might like",
            "recommended jobs",
            "job alert",
            "new jobs for you",
            "open to work",
            "say congrats",
            "work anniversary",
            "add a skill",
            "complete your profile",
            "weekly jobs digest",
            "people are looking at your profile",
            "unsubscribe"
    );

    // ── Java rules — high confidence keywords ─────────────────────────────────
    private static final Map<ApplicationStatus, List<String>> STATUS_KEYWORDS = Map.of(
            ApplicationStatus.OFFER, List.of(
                    "offer letter",
                    "job offer",
                    "employment offer",
                    "pleased to offer",
                    "you have been selected",
                    "selected for the role",
                    "welcome to the team"
            ),
            ApplicationStatus.REJECTED, List.of(
                    "not selected",
                    "not moving forward",
                    "regret to inform",
                    "application unsuccessful",
                    "not been shortlisted",
                    "not shortlisted",
                    "position has been filled",
                    "unfortunately",
                    "rejected",
                    "rejection"
            ),
            ApplicationStatus.INTERVIEW, List.of(
                    "interview invite",
                    "interview invitation",
                    "invited for interview",
                    "interview scheduled",
                    "interview slot",
                    "selected for interview",
                    "you have been shortlisted",
                    "shortlisted for",
                    "hr round",
                    "technical round",
                    "final round",
                    "virtual interview",
                    "video interview",
                    "interview"
            ),
            ApplicationStatus.ASSESSMENT, List.of(
                    "complete your assessment",
                    "coding challenge",
                    "coding test",
                    "online test",
                    "aptitude test",
                    "hackerrank",
                    "hackerearth",
                    "mettl",
                    "amcat",
                    "cocubes",
                    "technical test",
                    "skill test",
                    "assessment"
            ),
            ApplicationStatus.APPLIED, List.of(
                    "application received",
                    "application submitted",
                    "successfully applied",
                    "thank you for applying",
                    "thank you for your application",
                    "we have received your application",
                    "your application has been",
                    "application sent"
            )
    );

    // ── Company patterns ──────────────────────────────────────────────────────
    private static final List<Pattern> COMPANY_PATTERNS = List.of(
            Pattern.compile("(?:application to|applied to|applying to)\\s+([A-Z][\\w\\s&.-]{1,40}?)(?:\\s*[,.|!\\n]|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:interview at|role at|position at|opportunity at)\\s+([A-Z][\\w\\s&.-]{1,40}?)(?:\\s*[,.|!\\n]|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:offer from|from)\\s+([A-Z][\\w\\s&.-]{1,40}?)(?:\\s*[,.|!\\n]|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:for\\s+)([A-Z][\\w\\s&.-]{1,40}?)(?:\\s*[-–,.|!\\n]|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[-–|]\\s*([A-Z][\\w\\s&.-]{2,40}?)\\s*$", Pattern.CASE_INSENSITIVE)
    );

    private static final List<String> PORTAL_DOMAINS = List.of(
            "naukri", "linkedin", "foundit", "monster", "shine",
            "glassdoor", "indeed", "wellfound", "instahyre",
            "hackerearth", "hackerrank", "unstop"
    );

    private static final List<String> KNOWN_COMPANIES = List.of(
            "Google", "Amazon", "Microsoft", "Meta", "Apple", "Netflix",
            "Flipkart", "Swiggy", "Zomato", "Ola", "Paytm", "PhonePe",
            "CRED", "Razorpay", "Zepto", "Meesho", "Urban Company",
            "Infosys", "TCS", "Wipro", "HCL", "Tech Mahindra", "Cognizant",
            "Capgemini", "Accenture", "IBM", "Deloitte",
            "Zoho", "Freshworks", "Chargebee", "Postman", "BrowserStack",
            "Persistent", "Mphasis", "LTIMindtree", "Hexaware", "Birlasoft",
            "JPMorgan", "Goldman Sachs", "Morgan Stanley", "Deutsche Bank",
            "Salesforce", "SAP", "Oracle", "Workday", "ServiceNow"
    );

    @Transactional
    public int extractJobsFromEmails(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<EmailMessage> jobEmails =
                emailMessageService.findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(userId);

        log.info("Total job-related emails found: {}", jobEmails.size());

        int created = 0;
        int skippedDuplicate = 0;
        int skippedNoise = 0;

        for (EmailMessage email : jobEmails) {
            if (jobApplicationService.existsByUserIdAndSourceEmailId(userId, email.getId())) {
                skippedDuplicate++;
                continue;
            }

            if (isNoise(email.getSubject())) {
                log.debug("Noise skipped: {}", email.getSubject());
                skippedNoise++;
                continue;
            }

            // ── Step 1: Java rules ────────────────────────────────────────────
            ApplicationStatus javaStatus = detectStatusByRules(email.getSubject(), email.getBody());
            String javaCompany = extractCompanyByRules(email.getSubject(), email.getSender());

            ApplicationStatus finalStatus = javaStatus;
            String finalCompany = javaCompany;

            // ── Step 2: Gemini fallback if Java rules uncertain ───────────────
            boolean statusUncertain = javaStatus == ApplicationStatus.APPLIED
                    && !subjectContainsAppliedKeyword(email.getSubject());
            boolean companyUnknown = javaCompany.equals("Unknown Company");

            if (statusUncertain || companyUnknown) {
                log.debug("Calling Gemini for: {}", email.getSubject());
                GeminiExtractionService.GeminiResult geminiResult =
                        geminiExtractionService.extract(email.getSubject(), email.getBody());

                if (geminiResult != null) {
                    if (companyUnknown && !geminiResult.company().equals("Unknown Company")) {
                        finalCompany = geminiResult.company();
                    }
                    if (statusUncertain) {
                        finalStatus = geminiResult.status();
                    }
                    log.debug("Gemini result: {} | {}", geminiResult.company(), geminiResult.status());
                }
            }

            JobApplication job = JobApplication.builder()
                    .user(user)
                    .sourceEmail(email)
                    .company(finalCompany)
                    .position(extractPosition(email.getSubject()))
                    .status(finalStatus)
                    .build();

            jobApplicationService.save(job);
            created++;
            log.info("Saved: {} | {} | {}", finalCompany, finalStatus, email.getSubject());
        }

        log.info("Done → created: {} | duplicates: {} | noise: {}",
                created, skippedDuplicate, skippedNoise);

        return created;
    }

    // ── Java rule-based status detection ─────────────────────────────────────

    private ApplicationStatus detectStatusByRules(String subject, String body) {
        String text = ((subject != null ? subject : "") + " " +
                (body != null ? body.substring(0, Math.min(body.length(), 500)) : ""))
                .toLowerCase();

        for (ApplicationStatus status : List.of(
                ApplicationStatus.OFFER,
                ApplicationStatus.REJECTED,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.ASSESSMENT,
                ApplicationStatus.APPLIED)) {

            List<String> keywords = STATUS_KEYWORDS.get(status);
            if (keywords.stream().anyMatch(text::contains)) {
                return status;
            }
        }

        return ApplicationStatus.APPLIED;
    }

    private boolean subjectContainsAppliedKeyword(String subject) {
        if (subject == null) return false;
        String lower = subject.toLowerCase();
        return STATUS_KEYWORDS.get(ApplicationStatus.APPLIED)
                .stream().anyMatch(lower::contains);
    }

    // ── Java rule-based company extraction ───────────────────────────────────

    private String extractCompanyByRules(String subject, String sender) {
        if (subject != null) {
            String known = matchKnownCompany(subject);
            if (known != null) return known;

            for (Pattern pattern : COMPANY_PATTERNS) {
                Matcher matcher = pattern.matcher(subject);
                if (matcher.find()) {
                    String candidate = matcher.group(1).trim();
                    if (!isPortalName(candidate)) {
                        return cleanCompanyName(candidate);
                    }
                }
            }
        }

        if (sender != null && sender.contains("@")) {
            String domain = extractDomainFromSender(sender);
            if (domain != null && !isPortalName(domain)) {
                return capitalize(domain);
            }
        }

        return "Unknown Company";
    }

    private String matchKnownCompany(String subject) {
        String lower = subject.toLowerCase();
        for (String company : KNOWN_COMPANIES) {
            if (lower.contains(company.toLowerCase())) {
                return company;
            }
        }
        return null;
    }

    private boolean isPortalName(String name) {
        String lower = name.toLowerCase();
        return PORTAL_DOMAINS.stream().anyMatch(lower::contains);
    }

    private String extractDomainFromSender(String sender) {
        try {
            String email = sender.replaceAll(".*<|>.*", "").trim();
            String domain = email.split("@")[1];
            String[] parts = domain.split("\\.");
            for (String part : parts) {
                if (!part.equals("com") && !part.equals("co") && !part.equals("in")
                        && !part.equals("mail") && !part.equals("careers")
                        && !part.equals("noreply") && !part.equals("no-reply")
                        && !part.equals("hr") && !part.equals("jobs")
                        && part.length() > 2) {
                    return part;
                }
            }
        } catch (Exception e) {
            log.debug("Domain extraction failed for sender: {}", sender);
        }
        return null;
    }

    private String cleanCompanyName(String name) {
        return name.replaceAll(
                "(?i)\\s+(pvt|ltd|private|limited|inc|corp|llc|technologies|tech|solutions|services).*$",
                "").trim();
    }

    // ── Position extraction ───────────────────────────────────────────────────

    private String extractPosition(String subject) {
        if (subject == null) return null;
        Pattern positionPattern = Pattern.compile(
                "(?:for the role of|for the position of|position of|role of)[:\\s]+([\\w\\s]+?)(?:\\s+(?:at|@|to|from|-)|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = positionPattern.matcher(subject);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    // ── Noise detection ───────────────────────────────────────────────────────

    private boolean isNoise(String subject) {
        if (subject == null) return true;
        String lower = subject.toLowerCase();
        return IGNORE_PATTERNS.stream().anyMatch(lower::contains);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private String capitalize(String word) {
        if (word == null || word.isEmpty()) return word;
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
}