package com.nexio.nexio.email.controller;

import com.nexio.nexio.email.dto.ConnectResponse;
import com.nexio.nexio.email.dto.EmailResponse;
import com.nexio.nexio.email.facade.GmailSyncFacade;
import com.nexio.nexio.email.facade.GoogleOAuthFacade;
import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.service.EmailMessageService;
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

    private static final String USER_ID_HEADER="X-User-Id";

    @GetMapping("/connect")
    public ConnectResponse connect(@RequestHeader(USER_ID_HEADER) Long userId){
        String authUrl= googleOAuthFacade.buildUrl(userId);
        return new ConnectResponse(authUrl);
    }

    @GetMapping("/oauth2/callback")
    public Map<String,String> oauthCallback(@RequestParam("code") String code,@RequestParam("state") Long userId){
        googleOAuthFacade.handleCallback(code,userId);
        return Map.of("message","Gmail connected successfully");
    }
    @PostMapping("/sync")
    public Map<String, Object> sync(
            @RequestHeader(USER_ID_HEADER) Long userId) {

        int newEmails = gmailSyncFacade.syncEmails(userId);
        return Map.of("message", "Sync complete", "newEmails", newEmails);
    }

    @GetMapping("/list")
    public List<EmailResponse> list(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "false") boolean jobOnly) {

        List<EmailMessage> emails = jobOnly
                ? emailMessageService.findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(userId)
                : emailMessageService.findByUserIdOrderByReceivedAtDesc(userId);

        List<EmailResponse> response = emails.stream()
                .map(e -> EmailResponse.builder()
                        .id(e.getId())
                        .sender(e.getSender())
                        .subject(e.getSubject())
                        .body(e.getBody())
                        .receivedAt(e.getReceivedAt())
                        .jobRelated(e.isJobRelated())
                        .build())
                .toList();

        return response;
    }

}
