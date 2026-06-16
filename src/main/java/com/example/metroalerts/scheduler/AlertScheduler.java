package com.example.metroalerts.scheduler;

import com.example.metroalerts.detection.StateChangeDetector;
import com.example.metroalerts.detection.Transicao;
import com.example.metroalerts.metro.MetroStatusService;
import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import com.example.metroalerts.notify.NtfyNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);

    private final MetroStatusService statusService;
    private final StateChangeDetector detector;
    private final NtfyNotifier notifier;

    public AlertScheduler(MetroStatusService statusService,
                          StateChangeDetector detector,
                          NtfyNotifier notifier) {
        this.statusService = statusService;
        this.detector = detector;
        this.notifier = notifier;
    }

    @Scheduled(fixedDelayString = "${alerts.schedule-ms}")
    public void verificar() {
        try {
            log.debug("Starting metro status check");

            Map<Linha, EstadoLinha> estadoAtual = statusService.fetchEstadoAtual();
            List<Transicao> transicoes = detector.detectar(estadoAtual);

            for (Transicao transicao : transicoes) {
                if (transicao.isNotificavel()) {
                    try {
                        notifier.notificar(transicao);
                    } catch (Exception e) {
                        log.error("Failed to send notification for transition {}: {}", transicao, e.getMessage());
                    }
                }
            }

            log.debug("Metro status check complete. {} transition(s) detected.", transicoes.size());
        } catch (Exception e) {
            log.error("Error during metro status check: {}", e.getMessage(), e);
        }
    }
}
