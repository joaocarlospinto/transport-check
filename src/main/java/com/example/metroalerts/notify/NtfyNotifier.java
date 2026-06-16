package com.example.metroalerts.notify;

import com.example.metroalerts.config.NtfyProperties;
import com.example.metroalerts.detection.Transicao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NtfyNotifier {

    private static final Logger log = LoggerFactory.getLogger(NtfyNotifier.class);
    private static final String METRO_URL = "https://www.metrolisboa.pt/";

    private final RestClient client;
    private final String topicUrl;

    public NtfyNotifier(NtfyProperties props) {
        // SimpleClientHttpRequestFactory (HttpURLConnection) is used instead of the JDK
        // HttpClient because JDK 21's HttpClient rejects non-standard header names like
        // "Click" that ntfy.sh relies on for its action button feature.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.client = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
        this.topicUrl = "/" + props.topic();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 2, backoff = @Backoff(delay = 3000))
    public void notificar(Transicao transicao) {
        if (transicao.isInterrupcao()) {
            notificarInterrupcao(transicao.linha().getDisplayName());
        } else if (transicao.isRestabelecimento()) {
            notificarRestabelecimento(transicao.linha().getDisplayName());
        } else {
            log.debug("Transition not notifiable, skipping: {}", transicao);
        }
    }

    private void notificarInterrupcao(String linha) {
        log.info("Sending disruption alert for Linha {}", linha);
        enviar(
                "Linha " + linha + ": circulação interrompida",
                "Metro de Lisboa — Linha " + linha,
                "urgent",
                "warning"
        );
    }

    private void notificarRestabelecimento(String linha) {
        log.info("Sending restoration alert for Linha {}", linha);
        enviar(
                "Linha " + linha + ": circulação restabelecida",
                "Metro de Lisboa — Linha " + linha,
                "default",
                "white_check_mark"
        );
    }

    private void enviar(String mensagem, String titulo, String prioridade, String tags) {
        client.post()
                .uri(topicUrl)
                .header("Title", titulo)
                .header("Priority", prioridade)
                .header("Tags", tags)
                .header("Click", METRO_URL)
                .body(mensagem)
                .retrieve()
                .toBodilessEntity();
    }
}
