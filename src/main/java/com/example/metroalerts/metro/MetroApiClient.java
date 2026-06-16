package com.example.metroalerts.metro;

import com.example.metroalerts.config.MetroApiProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
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
                .requestFactory(buildRequestFactory(props))
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

    private static JdkClientHttpRequestFactory buildRequestFactory(MetroApiProperties props) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5));
        if (props.truststore() != null && !props.truststore().isBlank()) {
            builder.sslContext(buildSslContext(props.truststore(), props.truststorePassword()));
        }
        return new JdkClientHttpRequestFactory(builder.build());
    }

    /**
     * Builds an SSLContext that trusts both the project truststore (Metro API cert)
     * and the default JVM truststore (standard CAs used by everything else).
     */
    private static SSLContext buildSslContext(String truststorePath, String truststorePassword) {
        try {
            char[] password = truststorePassword != null ? truststorePassword.toCharArray() : new char[0];

            KeyStore projectStore = KeyStore.getInstance("JKS");
            try (InputStream is = new FileInputStream(truststorePath)) {
                projectStore.load(is, password);
            }

            KeyStore defaultStore = KeyStore.getInstance(KeyStore.getDefaultType());
            String defaultPath = System.getProperty("java.home") + "/lib/security/cacerts";
            try (InputStream is = new FileInputStream(defaultPath)) {
                defaultStore.load(is, "changeit".toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(projectStore);
            X509TrustManager projectTm = (X509TrustManager) tmf.getTrustManagers()[0];

            tmf.init(defaultStore);
            X509TrustManager defaultTm = (X509TrustManager) tmf.getTrustManagers()[0];

            // Trust cert if it's valid in either truststore
            X509TrustManager merged = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] a = projectTm.getAcceptedIssuers();
                    X509Certificate[] b = defaultTm.getAcceptedIssuers();
                    X509Certificate[] result = new X509Certificate[a.length + b.length];
                    System.arraycopy(a, 0, result, 0, a.length);
                    System.arraycopy(b, 0, result, a.length, b.length);
                    return result;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws java.security.cert.CertificateException {
                    try {
                        projectTm.checkClientTrusted(chain, authType);
                    } catch (java.security.cert.CertificateException e) {
                        defaultTm.checkClientTrusted(chain, authType);
                    }
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws java.security.cert.CertificateException {
                    try {
                        projectTm.checkServerTrusted(chain, authType);
                    } catch (java.security.cert.CertificateException e) {
                        defaultTm.checkServerTrusted(chain, authType);
                    }
                }
            };

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new javax.net.ssl.TrustManager[]{merged}, null);
            log.info("SSL context loaded from project truststore: {}", truststorePath);
            return ctx;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load project truststore from: " + truststorePath, e);
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
