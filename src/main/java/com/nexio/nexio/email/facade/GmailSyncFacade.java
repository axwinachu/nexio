package com.nexio.nexio.email.facade;

import com.nexio.nexio.config.ExtractionConfig;
import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
import com.nexio.nexio.user.model.User;
import com.nexio.nexio.user.service.UserService;
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
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class GmailSyncFacade {

    private static final String GMAIL_API_BASE =
            "https://gmail.googleapis.com/gmail/v1/users/me";

    private final EmailMessageService emailMessageService;
    private final UserService userService;
    private final GoogleOAuthFacade googleOAuthFacade;
    private final RestTemplate restTemplate;
    private final ExtractionConfig extractionConfig;

    // ── Absolute noise senders — never contain job process emails ─────────────
    private static final List<String> NOISE_SENDER_DOMAINS = List.of(
            "glassdoor.com",
            "jobalert.indeed.com",
            "monsterindia.com",
            "naukrialerts@naukri.com",
            "eventnc@naukri.com",
            "informationnc@naukri.com",
            "alertnc@naukri.com",
            "recommendationnc@naukri.com",
            "medium.com",
            "propeers.in",
            "hirist.tech",
            "jobfeed.hirist.com",
            "apna-jobs.com",
            "ambitionbox.com",
            "abekus.co",
            "newsletters-noreply@linkedin.com",
            "messages-noreply@linkedin.com",
            "substack.com",
            "beehiiv.com",
            "jobs2web.com"
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

    // ── Strong confirmation keywords — always pass, regardless of sender ──────
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

    // ── Secondary keywords ────────────────────────────────────────────────────
    private static final List<String> SECONDARY_JOB_KEYWORDS = List.of(
            "you applied for",
            "applied for",
            "application for",
            "interview slot",
            "selected for interview",
            "hr round",
            "technical round",
            "final round",
            "take the assessment",
            "complete the assessment",
            "next steps",
            "virtual interview",
            "schedule your interview",
            "phone screen",
            "onsite interview"
    );

    private static final List<Pattern> JOB_KEYWORD_PATTERNS = List.of(
            Pattern.compile("schedule.*interview", Pattern.CASE_INSENSITIVE),
            Pattern.compile("your.*application.*next.?steps", Pattern.CASE_INSENSITIVE),
            Pattern.compile("unfortunately.*not", Pattern.CASE_INSENSITIVE),
            Pattern.compile("we.*regret.*inform", Pattern.CASE_INSENSITIVE)
    );

    // ── Main sync method ──────────────────────────────────────────────────────

    public int syncEmails(Long userId) {
        String accessToken = googleOAuthFacade.getValidAccessToken(userId);
        log.info("Access token obtained for user {}: starts with {}",
                userId, accessToken != null ? accessToken.substring(0, 10) : "NULL");

        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        log.info("Self-email config: '{}'", extractionConfig.getSelfEmail());

        List<String> messageIds = fetchMessageIds(accessToken);
        log.info("Found {} messages in Gmail for user {}", messageIds.size(), userId);

        int newCount = 0;
        int skippedExisting = 0;
        int skippedNoise = 0;

        for (String gmailMessageId : messageIds) {
            try {
                if (emailMessageService.existsByGmailMessageId(gmailMessageId)) {
                    skippedExisting++;
                    continue;
                }

                EmailMessage emailMessage =
                        fetchAndParseMessage(gmailMessageId, accessToken, user);

                if (!emailMessage.isJobRelated()) {
                    skippedNoise++;
                    continue;
                }

                emailMessageService.saveInNewTransaction(emailMessage);
                newCount++;
                log.info("SAVED → subject: '{}' | sender: '{}'",
                        emailMessage.getSubject(), emailMessage.getSender());

            } catch (Exception e) {
                log.warn("Failed to process message {}: {}", gmailMessageId, e.getMessage(), e);
            }
        }

        log.info("Sync done → new: {} | existing: {} | noise skipped: {}",
                newCount, skippedExisting, skippedNoise);
        return newCount;
    }

    // ── Fetch message IDs ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> fetchMessageIds(String accessToken) {
        String url = GMAIL_API_BASE + "/messages?maxResults=200&labelIds=INBOX";
        Map<String, Object> response = getWithAuth(url, accessToken, Map.class);

        List<Map<String, String>> messages =
                (List<Map<String, String>>) response.getOrDefault("messages", List.of());

        log.info("Gmail returned {} message IDs", messages.size());
        return messages.stream()
                .map(m -> m.get("id"))
                .toList();
    }

    // ── Fetch and parse individual message ────────────────────────────────────

    @SuppressWarnings("unchecked")
    private EmailMessage fetchAndParseMessage(String gmailMessageId,
                                              String accessToken,
                                              User user) {
        String url = GMAIL_API_BASE + "/messages/" + gmailMessageId + "?format=full";
        Map<String, Object> message = getWithAuth(url, accessToken, Map.class);

        Map<String, Object> payload = (Map<String, Object>) message.get("payload");
        if (payload == null) {
            log.warn("Null payload for message {}", gmailMessageId);
            throw new RuntimeException("Null payload for message: " + gmailMessageId);
        }

        List<Map<String, String>> headers =
                (List<Map<String, String>>) payload.get("headers");

        String subject    = extractHeader(headers, "Subject");
        String sender     = extractHeader(headers, "From");
        String body       = extractBody(payload);
        LocalDateTime receivedAt = parseDate(message);

        log.debug("Parsed → subject: '{}' | sender: '{}' | bodyLen: {}",
                subject, sender, body.length());

        boolean jobRelated = isJobRelated(subject, sender, body);

        return EmailMessage.builder()
                .gmailMessageId(gmailMessageId)
                .user(user)
                .sender(sender)
                .subject(subject)
                .body(body)
                .receivedAt(receivedAt)
                .jobRelated(jobRelated)
                .build();
    }

    // ── Job-related detection ─────────────────────────────────────────────────

    private boolean isJobRelated(String subject, String sender, String body) {

        // 1. Skip self-sent emails
        String selfEmail = extractionConfig.getSelfEmail();
        if (sender != null && selfEmail != null && !selfEmail.isBlank()) {
            if (sender.toLowerCase().contains(selfEmail.toLowerCase())) {
                log.debug("DROPPED [self-sent]: {}", subject);
                return false;
            }
        }

        // 2. Calendar invite — Gmail sends these as .ics with empty body
        //    Detect purely from subject: "Jr Java Developer interview | DeliDrive"
        if (subject != null) {
            String lowerSub = subject.toLowerCase();
            boolean hasJobEvent = lowerSub.contains("interview")
                    || lowerSub.contains("assessment")
                    || lowerSub.contains("coding test");
            boolean isCalendarInvite = lowerSub.contains("invitation")
                    || lowerSub.contains("invite")
                    || lowerSub.contains("scheduled")
                    || lowerSub.contains("calendar")
                    // Gmail prefixes calendar invites with this phrase
                    || lowerSub.startsWith("invitation from");
            if (hasJobEvent && isCalendarInvite) {
                log.debug("PASSED [calendar invite]: {}", subject);
                return true;
            }
        }

        // Build search text from subject + body snippet
        String searchText = (
                (subject != null ? subject : "") + " " +
                        (body != null ? body.substring(0, Math.min(body.length(), 800)) : "")
        ).toLowerCase();

        // 3. Check STRONG confirmation keywords FIRST — before any noise filtering
        //    This handles Naukri/LinkedIn "your application was sent" emails
        if (STRONG_JOB_CONFIRMATIONS.stream().anyMatch(searchText::contains)) {
            log.debug("PASSED [strong keyword]: {}", subject);
            return true;
        }

        // 4. Now apply noise sender filter
        if (sender != null) {
            String lowerSender = sender.toLowerCase();
            if (NOISE_SENDER_DOMAINS.stream().anyMatch(lowerSender::contains)) {
                log.debug("DROPPED [noise sender]: {} | sender: {}", subject, sender);
                return false;
            }
        }

        // 5. Apply noise subject filter
        if (subject != null) {
            String lowerSubject = subject.toLowerCase();
            if (NOISE_SUBJECT_PATTERNS.stream().anyMatch(lowerSubject::contains)) {
                log.debug("DROPPED [noise subject]: {}", subject);
                return false;
            }
        }

        // 6. Check secondary keywords
        if (SECONDARY_JOB_KEYWORDS.stream().anyMatch(searchText::contains)) {
            log.debug("PASSED [secondary keyword]: {}", subject);
            return true;
        }

        // 7. Check regex patterns
        if (JOB_KEYWORD_PATTERNS.stream().anyMatch(p -> p.matcher(searchText).find())) {
            log.debug("PASSED [pattern match]: {}", subject);
            return true;
        }

        log.debug("DROPPED [no match]: {}", subject);
        return false;
    }

    // ── Header extraction ─────────────────────────────────────────────────────

    private String extractHeader(List<Map<String, String>> headers, String name) {
        if (headers == null) return "";
        return headers.stream()
                .filter(h -> h != null && name.equalsIgnoreCase(h.get("name")))
                .map(h -> h.get("value"))
                .filter(v -> v != null)
                .findFirst()
                .orElse("");
    }

    // ── Body extraction ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String extractBody(Map<String, Object> payload) {
        if (payload == null) return "";
        String result = extractBodyRecursive(payload);
        if (result == null || result.isBlank()) {
            log.debug("Body extraction empty — mimeType: {}", payload.get("mimeType"));
        }
        return result != null ? result.trim() : "";
    }

    @SuppressWarnings("unchecked")
    private String extractBodyRecursive(Map<String, Object> payload) {
        if (payload == null) return null;

        String mimeType = (String) payload.getOrDefault("mimeType", "");

        // ── Try direct body data ──────────────────────────────────────────────
        Map<String, Object> body = (Map<String, Object>) payload.get("body");
        if (body != null) {
            Object dataObj = body.get("data");
            if (dataObj != null && !dataObj.toString().isBlank()) {
                try {
                    String decoded = decodeBase64(dataObj.toString());
                    if ("text/html".equals(mimeType)) return stripHtml(decoded);
                    if ("text/calendar".equals(mimeType)) return extractCalendarText(decoded);
                    return decoded;
                } catch (Exception e) {
                    log.warn("Base64 decode failed for mimeType {}: {}", mimeType, e.getMessage());
                }
            }
        }

        // ── Recurse into parts ────────────────────────────────────────────────
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) payload.get("parts");
        if (parts == null || parts.isEmpty()) return null;

        String plainText = null;
        String htmlText  = null;
        String calText   = null;

        for (Map<String, Object> part : parts) {
            if (part == null) continue;

            String partMime = (String) part.getOrDefault("mimeType", "");

            // Recurse into nested multipart
            if (partMime.startsWith("multipart/")) {
                String nested = extractBodyRecursive(part);
                if (nested != null && !nested.isBlank() && plainText == null) {
                    plainText = nested;
                }
                continue;
            }

            Map<String, Object> partBody = (Map<String, Object>) part.get("body");
            if (partBody == null) continue;

            Object dataObj = partBody.get("data");
            if (dataObj == null || dataObj.toString().isBlank()) continue;

            try {
                String decoded = decodeBase64(dataObj.toString());

                if ("text/plain".equals(partMime) && plainText == null) {
                    plainText = decoded;
                } else if ("text/html".equals(partMime) && htmlText == null) {
                    htmlText = stripHtml(decoded);
                } else if ("text/calendar".equals(partMime) && calText == null) {
                    // Extract readable text from .ics for Gemini
                    calText = extractCalendarText(decoded);
                }
            } catch (Exception e) {
                log.warn("Failed to decode part mimeType {}: {}", partMime, e.getMessage());
            }
        }

        // Prefer plain text → HTML → calendar text
        if (plainText != null && !plainText.isBlank()) return plainText;
        if (htmlText  != null && !htmlText.isBlank())  return htmlText;
        return calText;
    }

    // ── Calendar (.ics) text extractor ────────────────────────────────────────
    // Pulls SUMMARY and DESCRIPTION fields from raw .ics content for Gemini

    private String extractCalendarText(String icsContent) {
        if (icsContent == null || icsContent.isBlank()) return "";

        StringBuilder sb = new StringBuilder();
        for (String line : icsContent.split("\r?\n")) {
            if (line.startsWith("SUMMARY:")) {
                sb.append("Event: ").append(line.substring(8).trim()).append("\n");
            } else if (line.startsWith("DESCRIPTION:")) {
                sb.append("Description: ").append(
                        line.substring(12).trim().replace("\\n", "\n")).append("\n");
            } else if (line.startsWith("ORGANIZER")) {
                // ORGANIZER;CN=Deli Drive:mailto:hr@delidrive.in
                String organizer = line.replaceAll(".*CN=([^:;]+).*", "$1").trim();
                if (!organizer.equals(line)) {
                    sb.append("Organizer: ").append(organizer).append("\n");
                }
            } else if (line.startsWith("DTSTART")) {
                sb.append("When: ").append(line.replaceAll(".*:", "").trim()).append("\n");
            }
        }

        String result = sb.toString().trim();
        return result.isBlank() ? "[Calendar invite — no description]" : result;
    }

    // ── HTML stripper ─────────────────────────────────────────────────────────

    private String stripHtml(String html) {
        if (html == null) return "";
        return html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)<p[^>]*>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    // ── Base64 decoder ────────────────────────────────────────────────────────

    private String decodeBase64(String encoded) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // ── Date parsing ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private LocalDateTime parseDate(Map<String, Object> message) {
        try {
            Object internalDate = message.get("internalDate");
            long epochMs = Long.parseLong(internalDate.toString());
            return Instant.ofEpochMilli(epochMs)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("Failed to parse date, using now: {}", e.getMessage());
            return LocalDateTime.now();
        }
    }

    // ── Auth GET ──────────────────────────────────────────────────────────────

    private <T> T getWithAuth(String url, String accessToken, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
                url, HttpMethod.GET, entity, responseType).getBody();
    }
}