package com.example.metroalerts.metro;

import com.example.metroalerts.metro.model.EstadoLinha;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoLinhaTest {

    @Test
    void normalCode_mapsToNormal() {
        assertThat(EstadoLinha.fromApiCode("normal")).isEqualTo(EstadoLinha.NORMAL);
    }

    @Test
    void normalCodeCaseInsensitive_mapsToNormal() {
        assertThat(EstadoLinha.fromApiCode("Normal")).isEqualTo(EstadoLinha.NORMAL);
        assertThat(EstadoLinha.fromApiCode("NORMAL")).isEqualTo(EstadoLinha.NORMAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"interrompida", "condicionada", "perturbada", "some_unknown_value"})
    void nonNormalCodes_mapToPerturbado(String code) {
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
}
