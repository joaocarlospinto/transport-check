package com.example.metroalerts.metro;

import com.example.metroalerts.config.MetroApiProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class MetroApiClient {

    private static final Logger log = LoggerFactory.getLogger(MetroApiClient.class);
    private static final int TOKEN_EXPIRY_BUFFER_SECONDS = 60;

    private final MetroApiProperties props;
    private final RestClient restClient;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public MetroApiClient(MetroApiProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public Map<String, Object> fetchEstadoLinhas() {
        try {
            return doFetch();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("Received 401, refreshing token and retrying");
                invalidateToken();
                return doFetch();
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doFetch() {
        return restClient.get()
                .uri(props.estadoPath())
                .header("Authorization", "Bearer " + getToken())
                .retrieve()
                .body(Map.class);
    }

    private String getToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS))) {
            return cachedToken;
        }
        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(TOKEN_EXPIRY_BUFFER_SECONDS))) {
                return cachedToken;
            }
            return refreshToken();
        } finally {
            tokenLock.unlock();
        }
    }

    private String refreshToken() {
        log.debug("Refreshing OAuth2 token");
        String credentials = Base64.getEncoder().encodeToString(
                (props.consumerKey() + ":" + props.consumerSecret()).getBytes());

        TokenResponse response = restClient.post()
                .uri(props.tokenPath())
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Failed to obtain access token: empty response");
        }

        cachedToken = response.accessToken();
        tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn());
        log.info("OAuth2 token refreshed, expires in {}s", response.expiresIn());
        return cachedToken;
    }

    private void invalidateToken() {
        tokenLock.lock();
        try {
            cachedToken = null;
            tokenExpiresAt = Instant.EPOCH;
        } finally {
            tokenLock.unlock();
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
