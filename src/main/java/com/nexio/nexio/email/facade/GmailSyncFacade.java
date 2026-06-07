package com.nexio.nexio.email.facade;

import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
import com.nexio.nexio.user.model.User;
import com.nexio.nexio.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class GmailSyncFacade {

    private static final String GMAIL_API_BASE = "https://gmail.googleapis.com/gmail/v1/users/me";

    private static final List<String> JOB_KEYWORDS = List.of(

            "application",
            "applied",
            "applicant",
            "application received",
            "application status",

            // Interview
            "interview",
            "interview scheduled",
            "technical interview",
            "hr interview",
            "screening",
            "phone screening",
            "virtual interview",
            "final round",

            // Assessment
            "assessment",
            "coding challenge",
            "online test",
            "aptitude test",
            "hackathon",
            "evaluation",

            // Recruitment
            "recruiter",
            "recruitment",
            "talent acquisition",
            "hiring team",
            "hiring manager",
            "candidate",

            // Offer
            "offer",
            "offer letter",
            "job offer",
            "employment offer",
            "selected",
            "congratulations",

            // Rejection
            "rejected",
            "rejection",
            "not selected",
            "unsuccessful",
            "unfortunately",
            "regret to inform",
            "moved forward with other candidates",

            // Jobs
            "job",
            "career",
            "position",
            "vacancy",
            "opportunity",
            "opening",
            "role",
            "employment",

            // Internships
            "intern",
            "internship",
            "trainee",
            "graduate program",

            // Joining
            "joining",
            "onboarding",
            "background verification",
            "background check",
            "document verification",
            "joining date",

            // ATS Platforms
            "greenhouse",
            "lever",
            "workday",
            "smartrecruiters",
            "ashby",
            "icims",
            "jobvite",

            // Popular Job Portals
            "linkedin jobs",
            "linkedin",
            "indeed",
            "naukri",
            "foundit",
            "monster",
            "wellfound",
            "glassdoor",

            // Common Recruiter Phrases
            "thank you for applying",
            "your application",
            "next steps",
            "application update",
            "candidate profile",
            "career opportunity",
            "job application"
    );
    private final EmailMessageService emailMessageService;
    private final UserService userService;
    private final GoogleOAuthFacade googleOAuthFacade;
    private final RestTemplate restTemplate;
    @Transactional
    public int syncEmails(Long userId){
        String accessToken= googleOAuthFacade.getValidAccessToken(userId);
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        List<String> messageIds = fetchMessageIds(accessToken);
        log.info("Found {} messages in Gmail for user {}", messageIds.size(), userId);
        int newCount = 0;
        for (String gmailMessageId : messageIds) {
            if (emailMessageService.existsByGmailMessageId(gmailMessageId)) {
                continue; // already synced
            }

            try {
                EmailMessage emailMessage = fetchAndParseMessage(gmailMessageId, accessToken, user);
                emailMessageService.save(emailMessage);
                newCount++;
            } catch (Exception e) {
                log.warn("Failed to fetch message {}: {}", gmailMessageId, e.getMessage());
            }
        }

        log.info("Synced {} new emails for user {}", newCount, userId);
        return newCount;
    }

    @SuppressWarnings("unchecked")
    private List<String> fetchMessageIds(String accessToken) {
        String url = GMAIL_API_BASE + "/messages?maxResults=50";
        Map<String, Object> response = getWithAuth(url, accessToken, Map.class);

        List<Map<String, String>> messages =
                (List<Map<String, String>>) response.getOrDefault("messages", List.of());

        return messages.stream()
                .map(m -> m.get("id"))
                .toList();
    }
    @SuppressWarnings("unchecked")
    private EmailMessage fetchAndParseMessage(String gmailMessageId, String accessToken, User user) {
        String url = GMAIL_API_BASE + "/messages/" + gmailMessageId + "?format=full";
        Map<String, Object> message = getWithAuth(url, accessToken, Map.class);

        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        List<Map<String, String>> headers = (List<Map<String, String>>) payload.get("headers");

        String subject = extractHeader(headers, "Subject");
        String sender  = extractHeader(headers, "From");
        String dateStr = extractHeader(headers, "Date");

        String body = extractBody(payload);
        LocalDateTime receivedAt = parseDate(message);

        return EmailMessage.builder()
                .gmailMessageId(gmailMessageId)
                .user(user)
                .sender(sender)
                .subject(subject)
                .body(body)
                .receivedAt(receivedAt)
                .jobRelated(isJobRelated(subject))
                .build();
    }
    private String extractHeader(List<Map<String, String>> headers, String name) {
        return headers.stream()
                .filter(h -> name.equalsIgnoreCase(h.get("name")))
                .map(h -> h.get("value"))
                .findFirst()
                .orElse("");
    }
    private String decodeBase64(String encoded) {
        byte[] decoded = Base64.getUrlDecoder().decode(encoded);
        return new String(decoded);
    }
    @SuppressWarnings("unchecked")
    private String extractBody(Map<String, Object> payload) {
        // Try direct body first
        Map<String, Object> body = (Map<String, Object>) payload.get("body");
        if (body != null && body.get("data") != null) {
            return decodeBase64((String) body.get("data"));
        }

        // Try parts (multipart emails)
        List<Map<String, Object>> parts = (List<Map<String, Object>>) payload.get("parts");
        if (parts != null) {
            for (Map<String, Object> part : parts) {
                String mimeType = (String) part.get("mimeType");
                if ("text/plain".equals(mimeType)) {
                    Map<String, Object> partBody = (Map<String, Object>) part.get("body");
                    if (partBody != null && partBody.get("data") != null) {
                        return decodeBase64((String) partBody.get("data"));
                    }
                }
            }
        }

        return "";
    }

    @SuppressWarnings("unchecked")
    private LocalDateTime parseDate(Map<String, Object> message) {
        try {
            Object internalDate = message.get("internalDate");
            long epochMs = Long.parseLong(internalDate.toString());
            return Instant.ofEpochMilli(epochMs)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    private boolean isJobRelated(String subject) {
        if (subject == null) return false;
        String lower = subject.toLowerCase();
        return JOB_KEYWORDS.stream().anyMatch(lower::contains);
    }
    private <T> T getWithAuth(String url,String accessToken,Class<T> responseType){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType).getBody();
    }
}
