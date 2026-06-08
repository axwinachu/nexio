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

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiExtractionService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Calls Gemini to extract company name and status from email.
     * Returns null if Gemini call fails — caller falls back to defaults.
     */
    public GeminiResult extract(String subject, String body) {
        try {
            String prompt = buildPrompt(subject, body);
            String raw = callGemini(prompt);
            return parseResponse(raw);
        } catch (Exception e) {
            log.warn("Gemini extraction failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private String buildPrompt(String subject, String body) {
        String bodySnippet = body != null
                ? body.substring(0, Math.min(body.length(), 500))
                : "";

        return """
                You are an AI that extracts job application details from emails.
                
                Analyze this email and respond ONLY with a JSON object. No explanation, no markdown.
                
                Email Subject: %s
                Email Body (first 500 chars): %s
                
                Rules:
                - status must be one of: APPLIED, ASSESSMENT, INTERVIEW, OFFER, REJECTED
                - company should be the hiring company name, NOT a job portal like Naukri or LinkedIn
                - If you cannot determine company, use "Unknown Company"
                - If you cannot determine status, use "APPLIED"
                
                Respond with ONLY this JSON:
                {
                  "company": "Company Name",
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
                }
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // Extract text from Gemini response structure
        var candidates = (java.util.List<Map<String, Object>>) response.getBody().get("candidates");
        var content = (Map<String, Object>) candidates.get(0).get("content");
        var parts = (java.util.List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private GeminiResult parseResponse(String raw) throws Exception {
        String cleaned = raw.replaceAll("```json|```", "").trim();

        JsonNode node = objectMapper.readTree(cleaned);
        String company = node.get("company").asText("Unknown Company");
        String statusStr = node.get("status").asText("APPLIED");

        ApplicationStatus status;
        try {
            status = ApplicationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            status = ApplicationStatus.APPLIED;
        }

        return new GeminiResult(company, status);
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record GeminiResult(String company, ApplicationStatus status) {}
}