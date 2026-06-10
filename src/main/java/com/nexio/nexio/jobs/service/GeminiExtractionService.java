package com.nexio.nexio.jobs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexio.nexio.config.GeminiConfig;
import com.nexio.nexio.jobs.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiExtractionService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiResult extract(String subject, String body) {
        try {
            String prompt = buildPrompt(subject, body);
            String raw = callGemini(prompt);
            return parseResponse(raw);
        } catch (Exception e) {
            log.warn("Gemini extraction failed for '{}': {}", subject, e.getMessage());
            return null;
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private String buildPrompt(String subject, String body) {
        String bodySnippet = body != null
                ? body.substring(0, Math.min(body.length(), 800))
                : "";

        return """
                You are an AI assistant that analyzes emails to detect job applications.

                Your job:
                1. Decide if this is a REAL job application process email
                2. Extract the hiring company name
                3. Extract the job position/role
                4. Determine the application status

                Email Subject: %s
                Email Body (first 800 chars): %s

                DECISION RULES for "isJobApplication":

                Set TRUE only for:
                - Application confirmation (your application was received/sent)
                - Interview invitation or scheduling
                - Assessment or coding test invitation
                - Job offer letter
                - Rejection from a specific company
                - Next steps in a hiring process
                - Shortlisting notification

                Set FALSE for:
                - Job alerts, job recommendations, job digests
                - "Jobs you might like" or "Apply Now" emails
                - Newsletter, blog posts, articles, career tips
                - Profile views, connection requests
                - Walk-in drives, mass hiring events
                - Promotional emails, paid courses, webinars
                - Quiz or competition invitations
                - General career advice
                - Salary reports or market updates
                - Platform onboarding emails

                EXTRACTION RULES:
                - "company" = the HIRING company, NOT a job portal
                  * Job portals to ignore: Naukri, LinkedIn, Indeed, Glassdoor, Foundit,
                    Monster, Hirist, Instahyre, Wellfound, AmbitionBox, Apna
                  * If company cannot be determined, use "Unknown Company"
                - "position" = exact job title/role if mentioned, otherwise null
                - "status" must be exactly one of:
                  * APPLIED    = application received or confirmed
                  * ASSESSMENT = coding test, online test, assignment sent
                  * INTERVIEW  = interview scheduled or shortlisted
                  * OFFER      = job offer extended
                  * REJECTED   = application rejected or not moving forward

                Respond ONLY with this JSON. No explanation, no markdown, no code blocks:
                {
                  "isJobApplication": true,
                  "company": "Company Name",
                  "position": "Job Title or null",
                  "status": "STATUS"
                }
                """.formatted(subject, bodySnippet);
    }

    // ── Gemini API call ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        String url = geminiConfig.getUrl() + "?key=" + geminiConfig.getApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                },
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 250
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("candidates")) {
            throw new RuntimeException("Empty Gemini response");
        }

        var candidates = (List<Map<String, Object>>) responseBody.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No Gemini candidates");
        }

        var content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private GeminiResult parseResponse(String raw) throws Exception {
        String cleaned = raw
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        JsonNode node = objectMapper.readTree(cleaned);

        boolean isJobApplication = node.has("isJobApplication")
                && node.get("isJobApplication").asBoolean(false);

        if (!isJobApplication) {
            return new GeminiResult(false, "Unknown Company", null, ApplicationStatus.APPLIED);
        }

        String company = node.has("company")
                ? node.get("company").asText("Unknown Company")
                : "Unknown Company";

        if (company.isBlank()) company = "Unknown Company";

        String position = null;
        if (node.has("position") && !node.get("position").isNull()) {
            String raw_position = node.get("position").asText(null);
            if (raw_position != null
                    && !raw_position.isBlank()
                    && !raw_position.equalsIgnoreCase("null")) {
                position = raw_position.trim();
            }
        }

        ApplicationStatus status = ApplicationStatus.APPLIED;
        if (node.has("status")) {
            try {
                status = ApplicationStatus.valueOf(
                        node.get("status").asText("APPLIED").toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown status from Gemini: {}", node.get("status").asText());
                status = ApplicationStatus.APPLIED;
            }
        }

        return new GeminiResult(true, company, position, status);
    }
    public record GeminiResult(
            boolean isJobApplication,
            String company,
            String position,
            ApplicationStatus status
    ) {}
}