package com.example.metroalerts.metro;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MetroStatusService {

    private static final Logger log = LoggerFactory.getLogger(MetroStatusService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MetroApiClient apiClient;
    private final ObjectMapper objectMapper;

    public MetroStatusService(MetroApiClient apiClient, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public Map<Linha, EstadoLinha> fetchEstadoAtual() {
        Map<String, Object> raw = apiClient.fetchEstadoLinhas();
        Object respostaRaw = raw.get("resposta");

        if (respostaRaw == null) {
            log.warn("API response missing 'resposta' field: {}", raw);
            return defaultDesconhecido();
        }

        Map<String, Object> resposta;
        if (respostaRaw instanceof Map) {
            resposta = (Map<String, Object>) respostaRaw;
        } else if (respostaRaw instanceof String json) {
            // API returns "resposta" as a JSON-encoded string rather than a nested object
            try {
                resposta = objectMapper.readValue(json, MAP_TYPE);
            } catch (Exception e) {
                log.warn("Failed to parse 'resposta' string as JSON: {}", json, e);
                return defaultDesconhecido();
            }
        } else {
            log.warn("Unexpected type for 'resposta': {}", respostaRaw.getClass());
            return defaultDesconhecido();
        }

        Map<Linha, EstadoLinha> result = new EnumMap<>(Linha.class);
        result.put(Linha.AZUL, mapCode(resposta, "azul_curta"));
        result.put(Linha.AMARELA, mapCode(resposta, "amarela_curta"));
        result.put(Linha.VERDE, mapCode(resposta, "verde_curta"));
        result.put(Linha.VERMELHA, mapCode(resposta, "vermelha_curta"));

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
