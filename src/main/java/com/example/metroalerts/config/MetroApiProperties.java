package com.example.metroalerts.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("metro.api")
public record MetroApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String tokenPath,
        @NotBlank String estadoPath,
        @NotBlank String consumerKey,
        @NotBlank String consumerSecret
) {
}
