package com.nexio.nexio.email.controller;

import com.nexio.nexio.email.dto.ConnectResponse;
import com.nexio.nexio.email.dto.EmailResponse;
import com.nexio.nexio.email.facade.GmailSyncFacade;
import com.nexio.nexio.email.facade.GoogleOAuthFacade;
import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final GoogleOAuthFacade googleOAuthFacade;
    private final GmailSyncFacade gmailSyncFacade;
    private final EmailMessageService emailMessageService;

    @GetMapping("/connect")
    public ConnectResponse connect(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String authUrl = googleOAuthFacade.buildUrl(userId);
        return new ConnectResponse(authUrl);
    }

    // callback stays as-is — userId comes from OAuth state param, not JWT
    @GetMapping("/oauth2/callback")
    public Map<String, String> oauthCallback(
            @RequestParam("code") String code,
            @RequestParam("state") Long userId) {
        googleOAuthFacade.handleCallback(code, userId);
        return Map.of("message", "Gmail connected successfully");
    }

    @PostMapping("/sync")
    public Map<String, Object> sync(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int newEmails = gmailSyncFacade.syncEmails(userId);
        return Map.of("message", "Sync complete", "newEmails", newEmails);
    }

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