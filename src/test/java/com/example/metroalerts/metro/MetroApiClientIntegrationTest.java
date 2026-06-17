package com.example.metroalerts.metro;

import com.example.metroalerts.config.MetroApiProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class MetroApiClientIntegrationTest {

    private WireMockServer wireMock;
    private MetroApiClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        MetroApiProperties props = new MetroApiProperties(
                "http://localhost:" + wireMock.port(),
                "/token",
                "/estadoServicoML/1.0.0/estadoLinha/todos",
                "test-key",
                "test-secret"
        );
        client = new MetroApiClient(props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void fetchEstadoLinhas_success() {
        stubTokenEndpoint();
        wireMock.stubFor(get(urlEqualTo("/estadoServicoML/1.0.0/estadoLinha/todos"))
                .withHeader("Authorization", matching("Bearer .*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "resposta": {
                                    "azul_curta": "normal",
                                    "amarela_curta": "normal",
                                    "verde_curta": "interrompida",
                                    "vermelha_curta": "normal"
                                  },
                                  "codigo": "200"
                                }
                                """)));

        Map<String, Object> result = client.fetchEstadoLinhas();

        assertThat(result).containsKey("resposta");
        @SuppressWarnings("unchecked")
        Map<String, Object> resposta = (Map<String, Object>) result.get("resposta");
        assertThat(resposta.get("verde_curta")).isEqualTo("interrompida");
        assertThat(resposta.get("azul_curta")).isEqualTo("normal");
    }

    @Test
    void fetchEstadoLinhas_401_refreshesTokenAndRetries() {
        // First token response
        stubTokenEndpoint();

        // First API call returns 401
        wireMock.stubFor(get(urlEqualTo("/estadoServicoML/1.0.0/estadoLinha/todos"))
                .inScenario("token-refresh")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(401))
                .willSetStateTo("token-refreshed"));

        // After refresh, second token call
        wireMock.stubFor(post(urlEqualTo("/token"))
                .inScenario("token-refresh")
                .whenScenarioStateIs("token-refreshed")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"access_token":"new-token","token_type":"Bearer","expires_in":3600}
                                """)));

        // Second API call succeeds
        wireMock.stubFor(get(urlEqualTo("/estadoServicoML/1.0.0/estadoLinha/todos"))
                .inScenario("token-refresh")
                .whenScenarioStateIs("token-refreshed")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "resposta": {"azul_curta":"normal","amarela_curta":"normal","verde_curta":"normal","vermelha_curta":"normal"},
                                  "codigo": "200"
                                }
                                """)));

        Map<String, Object> result = client.fetchEstadoLinhas();
        assertThat(result).containsKey("resposta");
    }

    private void stubTokenEndpoint() {
        wireMock.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"access_token":"test-token","token_type":"Bearer","expires_in":3600}
                                """)));
    }
}
