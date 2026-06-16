package com.example.metroalerts.detection;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StateChangeDetector {

    private static final Logger log = LoggerFactory.getLogger(StateChangeDetector.class);

    private final StateStore stateStore;

    public StateChangeDetector(StateStore stateStore) {
        this.stateStore = stateStore;
    }

    /**
     * Detects transitions from the previous known state. Returns an empty list
     * on the first run (no baseline yet) so no spurious alerts are sent.
     * After detecting, updates the store with the new states.
     */
    public List<Transicao> detectar(Map<Linha, EstadoLinha> estadoAtual) {
        List<Transicao> transicoes = new ArrayList<>();

        if (!stateStore.hasBaseline()) {
            log.info("No baseline yet — storing initial state without alerting");
            stateStore.putAll(estadoAtual);
            return transicoes;
        }

        for (Map.Entry<Linha, EstadoLinha> entry : estadoAtual.entrySet()) {
            Linha linha = entry.getKey();
            EstadoLinha atual = entry.getValue();
            Optional<EstadoLinha> anteriorOpt = stateStore.get(linha);

            if (anteriorOpt.isEmpty()) {
                stateStore.put(linha, atual);
                continue;
            }

            EstadoLinha anterior = anteriorOpt.get();
            if (anterior != atual) {
                Transicao t = new Transicao(linha, anterior, atual);
                log.info("State change detected for {}: {} -> {}", linha, anterior, atual);
                transicoes.add(t);
                stateStore.put(linha, atual);
            }
        }

        return transicoes;
    }
}
