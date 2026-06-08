package com.nexio.nexio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.google")
public class GoogleOauthConfig {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private List<String> scopes = List.of("https://www.googleapis.com/auth/gmail.readonly", "https://www.googleapis.com/auth/gmail.labels");

    private String authUri = "https://accounts.google.com/o/oauth2/v2/auth";


    private String tokenUri = "https://oauth2.googleapis.com/token";
}
