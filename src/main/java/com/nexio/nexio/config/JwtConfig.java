package com.nexio.nexio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    private String secret;

    private long expirationMs=86400000L;

    private String prefix="Bearer ";

    private String header="Authorization";
}
