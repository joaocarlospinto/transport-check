package com.example.metroalerts.metro;

import com.example.metroalerts.metro.model.EstadoLinha;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoLinhaTest {

    @Test
    void code0_mapsToNormal() {
        assertThat(EstadoLinha.fromApiCode("0")).isEqualTo(EstadoLinha.NORMAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "9"})
    void disruptionCodes_mapToPerturbado(String code) {
        assertThat(EstadoLinha.fromApiCode(code)).isEqualTo(EstadoLinha.PERTURBADO);
    }

    @Test
    void nullCode_mapsToDesconhecido() {
        assertThat(EstadoLinha.fromApiCode(null)).isEqualTo(EstadoLinha.DESCONHECIDO);
    }

    @Test
    void blankCode_mapsToDesconhecido() {
        assertThat(EstadoLinha.fromApiCode("  ")).isEqualTo(EstadoLinha.DESCONHECIDO);
    }

    @Test
    void unknownCode_mapsToDesconhecido() {
        assertThat(EstadoLinha.fromApiCode("99")).isEqualTo(EstadoLinha.DESCONHECIDO);
    }
}
