package com.nexio.nexio.email.controller;

import com.nexio.nexio.email.dto.ConnectResponse;
import com.nexio.nexio.email.dto.EmailResponse;
import com.nexio.nexio.email.facade.GmailSyncFacade;
import com.nexio.nexio.email.facade.GoogleOAuthFacade;
import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Gmail integration and email sync")
@SecurityRequirement(name = "Bearer Authentication")
public class EmailController {

    private final GoogleOAuthFacade googleOAuthFacade;
    private final GmailSyncFacade gmailSyncFacade;
    private final EmailMessageService emailMessageService;

    @Operation(summary = "Get Gmail OAuth2 connect URL")
    @GetMapping("/connect")
    public ConnectResponse connect(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String authUrl = googleOAuthFacade.buildUrl(userId);
        return new ConnectResponse(authUrl);
    }

    // callback stays as-is — userId comes from OAuth state param, not JWT
    @Operation(summary = "OAuth2 callback from Google (handled automatically)")
    @GetMapping("/oauth2/callback")
    public Map<String, String> oauthCallback(
            @RequestParam("code") String code,
            @RequestParam("state") Long userId) {
        googleOAuthFacade.handleCallback(code, userId);
        return Map.of("message", "Gmail connected successfully");
    }

    @Operation(summary = "Sync latest emails from Gmail")
    @PostMapping("/sync")
    public Map<String, Object> sync(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int newEmails = gmailSyncFacade.syncEmails(userId);
        return Map.of("message", "Sync complete", "newEmails", newEmails);
    }

    @Operation(summary = "List synced emails. Use ?jobOnly=true for job-related emails only")
    @GetMapping("/list")
    public List<EmailResponse> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") boolean jobOnly) {

        Long userId = (Long) request.getAttribute("userId");

        List<EmailMessage> emails = jobOnly
                ? emailMessageService.findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(userId)
                : emailMessageService.findByUserIdOrderByReceivedAtDesc(userId);

        return emails.stream()
                .map(e -> EmailResponse.builder()
                        .id(e.getId())
                        .sender(e.getSender())
                        .subject(e.getSubject())
                        .body(e.getBody())
                        .receivedAt(e.getReceivedAt())
                        .jobRelated(e.isJobRelated())
                        .build())
                .toList();
    }
}