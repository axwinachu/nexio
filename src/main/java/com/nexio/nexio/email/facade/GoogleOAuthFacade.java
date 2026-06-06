package com.nexio.nexio.email.facade;

import com.nexio.nexio.config.GoogleOauthConfig;
import com.nexio.nexio.email.model.GoogleToken;
import com.nexio.nexio.email.service.GoogleTokenService;
import com.nexio.nexio.user.model.User;
import com.nexio.nexio.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoogleOAuthFacade {
    private final GoogleOauthConfig googleOAuthConfig;
    private final GoogleTokenService googleTokenService;
    private final UserService userService;
    private final RestTemplate restTemplate;

    public String buildUrl(Long userId){
        return UriComponentsBuilder
                .fromHttpUrl(googleOAuthConfig.getAuthUri())
                .queryParam("redirect_uri", googleOAuthConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", googleOAuthConfig.getScopes()))
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", userId.toString())
                .build().toUriString();

    }

    @Transactional
    public void handleCallback(String code ,Long userId){
        Map<String,Object> tokenResponse=exchangeCodeForTokens(code);

        User user=userService.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found: "+userId));
        GoogleToken token = googleTokenService.findByUserId(userId)
                .orElse(GoogleToken.builder().user(user).build());
        token.setAccessToken((String) tokenResponse.get("access_token"));
        String refreshToken = (String) tokenResponse.get("refresh_token");
        if (refreshToken != null) {
            token.setRefreshToken(refreshToken);
        }
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");
        token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));

        googleTokenService.save(token);

    }

    public String getValidAccessToken(Long userId){
        GoogleToken token=googleTokenService.findByUserId(userId)
                .orElseThrow(()->new RuntimeException("Gmail not connected for user: "+userId));
        if(isExpired(token)){
            token=refreshAccessToken(token);
        }
        return token.getAccessToken();
    }

    //helper
    @SuppressWarnings("unchecked")
    private Map<String,Object> exchangeCodeForTokens(String code){
        HttpHeaders headers=new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String,String> body=new LinkedMultiValueMap<>();

        body.add("code",code);
        body.add("client_id", googleOAuthConfig.getClientId());
        body.add("client_id", googleOAuthConfig.getClientId());
        body.add("client_secret", googleOAuthConfig.getClientSecret());
        body.add("redirect_uri", googleOAuthConfig.getRedirectUri());
        body.add("grant_type", "authorization_code");
        ResponseEntity<Map> response = restTemplate.postForEntity(
                googleOAuthConfig.getTokenUri(),
                new HttpEntity<>(body, headers),
                Map.class
        );
        return response.getBody();

    }
    @Transactional
    @SuppressWarnings("unchecked")
    private GoogleToken refreshAccessToken(GoogleToken token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", token.getRefreshToken());
        body.add("client_id", googleOAuthConfig.getClientId());
        body.add("client_secret", googleOAuthConfig.getClientSecret());
        body.add("grant_type", "refresh_token");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                googleOAuthConfig.getTokenUri(),
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<String, Object> result = response.getBody();
        token.setAccessToken((String) result.get("access_token"));
        token.setExpiresAt(LocalDateTime.now().plusSeconds((Integer) result.get("expires_in")));

        return googleTokenService.save(token);
    }

    private boolean isExpired(GoogleToken token) {
        return token.getExpiresAt() != null && LocalDateTime.now().isAfter(token.getExpiresAt().minusMinutes(5));
    }
}
