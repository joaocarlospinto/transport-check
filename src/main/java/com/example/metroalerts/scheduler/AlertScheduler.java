package com.example.metroalerts.scheduler;

import com.example.metroalerts.detection.StateChangeDetector;
import com.example.metroalerts.detection.Transicao;
import com.example.metroalerts.metro.EstadoSnapshot;
import com.example.metroalerts.metro.MetroStatusService;
import com.example.metroalerts.notify.NtfyNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            notifier.notificarInicio();
        } catch (Exception e) {
            log.error("Failed to send startup notification: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${alerts.schedule-ms}")
    public void verificar() {
        try {
            EstadoSnapshot snapshot = statusService.fetchEstadoAtual();
            List<Transicao> transicoes = detector.detectar(snapshot.estados());

            for (Transicao transicao : transicoes) {
                log.info("Transition for {}: {} -> {} | raw API value: '{}'",
                        transicao.linha(), transicao.anterior(), transicao.atual(),
                        snapshot.codigosBrutos().get(transicao.linha()));
                if (transicao.isNotificavel()) {
                    try {
                        notifier.notificar(transicao);
                    } catch (Exception e) {
                        log.error("Failed to send notification for transition {}: {}", transicao, e.getMessage());
                    }
                }
            }

            if (transicoes.isEmpty()) {
                log.info("Metro status check complete. All lines normal, no notifications sent.");
            } else {
                log.info("Metro status check complete. {} transition(s) detected.", transicoes.size());
            }
        } catch (Exception e) {
            log.error("Error during metro status check: {}", e.getMessage(), e);
        }
    }
}
