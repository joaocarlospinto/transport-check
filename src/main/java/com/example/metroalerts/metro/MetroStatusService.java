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
    public EstadoSnapshot fetchEstadoAtual() {
        Map<String, Object> raw = apiClient.fetchEstadoLinhas();
        Object respostaRaw = raw.get("resposta");

        if (respostaRaw == null) {
            log.warn("API response missing 'resposta' field: {}", raw);
            return defaultDesconhecido("resposta ausente: " + raw);
        }

        Map<String, Object> resposta;
        if (respostaRaw instanceof Map) {
            resposta = (Map<String, Object>) respostaRaw;
        } else if (respostaRaw instanceof String json) {
            // API returns "resposta" as a JSON-encoded string rather than a nested object
            String trimmed = json.trim();
            if (!trimmed.startsWith("{")) {
                // Outside operating hours the API returns a plain status message
                // (e.g. "Circulação encerrada") instead of the per-line JSON object.
                // This is an expected, non-actionable state — not a parse error.
                log.info("Metro not reporting per-line status (resposta='{}'); treating all lines as DESCONHECIDO", trimmed);
                return defaultDesconhecido(trimmed);
            }
            try {
                resposta = objectMapper.readValue(json, MAP_TYPE);
            } catch (Exception e) {
                log.warn("Failed to parse 'resposta' string as JSON: {}", json, e);
                return defaultDesconhecido("JSON inválido: " + trimmed);
            }
        } else {
            log.warn("Unexpected type for 'resposta': {}", respostaRaw.getClass());
            return defaultDesconhecido("tipo inesperado: " + respostaRaw.getClass());
        }

        Map<Linha, EstadoLinha> estados = new EnumMap<>(Linha.class);
        Map<Linha, String> codigosBrutos = new EnumMap<>(Linha.class);
        mapCode(estados, codigosBrutos, resposta, Linha.AZUL, "azul_curta");
        mapCode(estados, codigosBrutos, resposta, Linha.AMARELA, "amarela_curta");
        mapCode(estados, codigosBrutos, resposta, Linha.VERDE, "verde_curta");
        mapCode(estados, codigosBrutos, resposta, Linha.VERMELHA, "vermelha_curta");

        log.debug("Current line states: {} (raw: {})", estados, codigosBrutos);
        return new EstadoSnapshot(estados, codigosBrutos);
    }

    private void mapCode(Map<Linha, EstadoLinha> estados, Map<Linha, String> codigosBrutos,
                         Map<String, Object> resposta, Linha linha, String key) {
        Object value = resposta.get(key);
        String bruto = value == null ? null : value.toString();
        codigosBrutos.put(linha, bruto);
        if (value == null) {
            log.warn("Missing key '{}' in API response", key);
            estados.put(linha, EstadoLinha.DESCONHECIDO);
            return;
        }
        EstadoLinha estado = EstadoLinha.fromApiCode(bruto);
        if (estado == EstadoLinha.DESCONHECIDO) {
            log.warn("Unknown state code '{}' for key '{}'", value, key);
        }
        estados.put(linha, estado);
    }

    private EstadoSnapshot defaultDesconhecido(String payloadBruto) {
        Map<Linha, EstadoLinha> estados = new EnumMap<>(Linha.class);
        Map<Linha, String> codigosBrutos = new EnumMap<>(Linha.class);
        for (Linha linha : Linha.values()) {
            estados.put(linha, EstadoLinha.DESCONHECIDO);
            codigosBrutos.put(linha, payloadBruto);
        }
        return new EstadoSnapshot(estados, codigosBrutos);
    }
}
