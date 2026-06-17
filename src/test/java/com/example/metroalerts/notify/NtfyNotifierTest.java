package com.example.metroalerts.notify;

import com.example.metroalerts.config.NtfyProperties;
import com.example.metroalerts.detection.Transicao;
import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThatCode;

class NtfyNotifierTest {

    private WireMockServer wireMock;
    private NtfyNotifier notifier;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        NtfyProperties props = new NtfyProperties(
                "http://localhost:" + wireMock.port(),
                "test-topic"
        );
        notifier = new NtfyNotifier(props);

        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void interrupcao_sendsUrgentPriorityAndWarningTag() {
        Transicao t = new Transicao(Linha.VERDE, EstadoLinha.NORMAL, EstadoLinha.PERTURBADO);

        assertThatCode(() -> notifier.notificar(t)).doesNotThrowAnyException();

        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.priority", equalTo("urgent")))
                .withRequestBody(matchingJsonPath("$.tags[0]", equalTo("warning")))
                .withRequestBody(matchingJsonPath("$.title", containing("Verde")))
                .withRequestBody(matchingJsonPath("$.message", containing("interrompida"))));
    }

    @Test
    void restabelecimento_sendsDefaultPriorityAndCheckTag() {
        Transicao t = new Transicao(Linha.AZUL, EstadoLinha.PERTURBADO, EstadoLinha.NORMAL);

        assertThatCode(() -> notifier.notificar(t)).doesNotThrowAnyException();

        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.priority", equalTo("default")))
                .withRequestBody(matchingJsonPath("$.tags[0]", equalTo("white_check_mark")))
                .withRequestBody(matchingJsonPath("$.title", containing("Azul")))
                .withRequestBody(matchingJsonPath("$.message", containing("restabelecida"))));
    }

    @Test
    void nonNotifiableTransition_sendsNothing() {
        Transicao t = new Transicao(Linha.AMARELA, EstadoLinha.NORMAL, EstadoLinha.DESCONHECIDO);

        assertThatCode(() -> notifier.notificar(t)).doesNotThrowAnyException();

        wireMock.verify(0, postRequestedFor(urlEqualTo("/")));
    }
}
