package com.example.metroalerts.metro;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetroStatusServiceTest {

    private MetroApiClient apiClient;
    private MetroStatusService service;

    @BeforeEach
    void setUp() {
        apiClient = mock(MetroApiClient.class);
        service = new MetroStatusService(apiClient, new ObjectMapper());
    }

    @Test
    void respostaAsJsonObjectString_mapsPerLineStates() {
        when(apiClient.fetchEstadoLinhas()).thenReturn(Map.of(
                "resposta", "{\"azul_curta\":\"normal\",\"amarela_curta\":\"normal\"," +
                        "\"verde_curta\":\"Existem perturbações\",\"vermelha_curta\":\"normal\"}"
        ));

        Map<Linha, EstadoLinha> result = service.fetchEstadoAtual();

        assertThat(result.get(Linha.AZUL)).isEqualTo(EstadoLinha.NORMAL);
        assertThat(result.get(Linha.AMARELA)).isEqualTo(EstadoLinha.NORMAL);
        assertThat(result.get(Linha.VERDE)).isEqualTo(EstadoLinha.PERTURBADO);
        assertThat(result.get(Linha.VERMELHA)).isEqualTo(EstadoLinha.NORMAL);
    }

    @Test
    void respostaAsClosedMessage_returnsAllDesconhecido() {
        // Outside operating hours the API returns a plain status message
        // instead of the per-line JSON object.
        when(apiClient.fetchEstadoLinhas()).thenReturn(Map.of(
                "resposta", "Circulação encerrada"
        ));

        Map<Linha, EstadoLinha> result = service.fetchEstadoAtual();

        assertThat(result.values()).containsOnly(EstadoLinha.DESCONHECIDO);
    }

    @Test
    void respostaAsMalformedJsonObject_returnsAllDesconhecido() {
        // A string that looks like a JSON object but is broken should still
        // fail safe to DESCONHECIDO (and is logged as a warning).
        when(apiClient.fetchEstadoLinhas()).thenReturn(Map.of(
                "resposta", "{\"azul_curta\":"
        ));

        Map<Linha, EstadoLinha> result = service.fetchEstadoAtual();

        assertThat(result.values()).containsOnly(EstadoLinha.DESCONHECIDO);
    }

    @Test
    void respostaMissing_returnsAllDesconhecido() {
        when(apiClient.fetchEstadoLinhas()).thenReturn(Map.of("outro", "x"));

        Map<Linha, EstadoLinha> result = service.fetchEstadoAtual();

        assertThat(result.values()).containsOnly(EstadoLinha.DESCONHECIDO);
    }
}
