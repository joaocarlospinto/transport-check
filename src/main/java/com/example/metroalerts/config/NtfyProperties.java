package com.example.metroalerts.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("ntfy")
public record NtfyProperties(
        @NotBlank String baseUrl,
        @NotBlank String topic
) {
}
