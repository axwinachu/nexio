package com.nexio.nexio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendConfig {
    private String url = "http://localhost:5173";
}
