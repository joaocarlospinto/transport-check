package com.example.metroalerts.metro;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MetroStatusService {

    private static final Logger log = LoggerFactory.getLogger(MetroStatusService.class);

    private final MetroApiClient apiClient;

    public MetroStatusService(MetroApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Returns the current status of all metro lines.
     * The API response has a "resposta" object containing line codes keyed by line name.
     */
    @SuppressWarnings("unchecked")
    public Map<Linha, EstadoLinha> fetchEstadoAtual() {
        Map<String, Object> raw = apiClient.fetchEstadoLinhas();
        Map<String, Object> resposta = (Map<String, Object>) raw.get("resposta");

        if (resposta == null) {
            log.warn("API response missing 'resposta' field: {}", raw);
            return defaultDesconhecido();
        }

        Map<Linha, EstadoLinha> result = new EnumMap<>(Linha.class);
        result.put(Linha.AZUL, mapCode(resposta, "azul"));
        result.put(Linha.AMARELA, mapCode(resposta, "amarela"));
        result.put(Linha.VERDE, mapCode(resposta, "verde"));
        result.put(Linha.VERMELHA, mapCode(resposta, "vermelha"));

        log.debug("Current line states: {}", result);
        return result;
    }

    private EstadoLinha mapCode(Map<String, Object> resposta, String key) {
        Object value = resposta.get(key);
        if (value == null) {
            log.warn("Missing key '{}' in API response", key);
            return EstadoLinha.DESCONHECIDO;
        }
        EstadoLinha estado = EstadoLinha.fromApiCode(value.toString());
        if (estado == EstadoLinha.DESCONHECIDO) {
            log.warn("Unknown state code '{}' for key '{}'", value, key);
        }
        return estado;
    }

    private Map<Linha, EstadoLinha> defaultDesconhecido() {
        Map<Linha, EstadoLinha> result = new EnumMap<>(Linha.class);
        for (Linha linha : Linha.values()) {
            result.put(linha, EstadoLinha.DESCONHECIDO);
        }
        return result;
    }
}
