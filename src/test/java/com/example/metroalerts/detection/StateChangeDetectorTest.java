package com.example.metroalerts.detection;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StateChangeDetectorTest {

    private InMemoryStateStore store;
    private StateChangeDetector detector;

    @BeforeEach
    void setUp() {
        store = new InMemoryStateStore();
        detector = new StateChangeDetector(store);
    }

    @Test
    void firstRun_noBaseline_returnsEmptyAndSetsBaseline() {
        Map<Linha, EstadoLinha> atual = allNormal();

        List<Transicao> result = detector.detectar(atual);

        assertThat(result).isEmpty();
        assertThat(store.hasBaseline()).isTrue();
        assertThat(store.get(Linha.AZUL)).contains(EstadoLinha.NORMAL);
    }

    @Test
    void normalToPerturbado_returnsInterrupcaoTransition() {
        store.putAll(allNormal());

        Map<Linha, EstadoLinha> atual = allNormal();
        atual.put(Linha.VERDE, EstadoLinha.PERTURBADO);

        List<Transicao> result = detector.detectar(atual);

        assertThat(result).hasSize(1);
        Transicao t = result.get(0);
        assertThat(t.linha()).isEqualTo(Linha.VERDE);
        assertThat(t.anterior()).isEqualTo(EstadoLinha.NORMAL);
        assertThat(t.atual()).isEqualTo(EstadoLinha.PERTURBADO);
        assertThat(t.isInterrupcao()).isTrue();
        assertThat(t.isNotificavel()).isTrue();
    }

    @Test
    void perturbadoToNormal_returnsRestabelecimentoTransition() {
        Map<Linha, EstadoLinha> baseline = allNormal();
        baseline.put(Linha.AMARELA, EstadoLinha.PERTURBADO);
        store.putAll(baseline);

        List<Transicao> result = detector.detectar(allNormal());

        assertThat(result).hasSize(1);
        Transicao t = result.get(0);
        assertThat(t.linha()).isEqualTo(Linha.AMARELA);
        assertThat(t.isRestabelecimento()).isTrue();
        assertThat(t.isNotificavel()).isTrue();
    }

    @Test
    void noChange_returnsEmpty() {
        store.putAll(allNormal());
        List<Transicao> result = detector.detectar(allNormal());
        assertThat(result).isEmpty();
    }

    @Test
    void normalToDesconhecido_notNotifiable() {
        store.putAll(allNormal());

        Map<Linha, EstadoLinha> atual = allNormal();
        atual.put(Linha.VERMELHA, EstadoLinha.DESCONHECIDO);

        List<Transicao> result = detector.detectar(atual);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNotificavel()).isFalse();
    }

    @Test
    void desconhecidoToNormal_notNotifiable() {
        Map<Linha, EstadoLinha> baseline = allNormal();
        baseline.put(Linha.AZUL, EstadoLinha.DESCONHECIDO);
        store.putAll(baseline);

        List<Transicao> result = detector.detectar(allNormal());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNotificavel()).isFalse();
    }

    @Test
    void stateIsUpdatedAfterTransition() {
        store.putAll(allNormal());

        Map<Linha, EstadoLinha> disrupted = allNormal();
        disrupted.put(Linha.AZUL, EstadoLinha.PERTURBADO);
        detector.detectar(disrupted);

        assertThat(store.get(Linha.AZUL)).contains(EstadoLinha.PERTURBADO);
    }

    private Map<Linha, EstadoLinha> allNormal() {
        Map<Linha, EstadoLinha> map = new EnumMap<>(Linha.class);
        for (Linha l : Linha.values()) {
            map.put(l, EstadoLinha.NORMAL);
        }
        return map;
    }

    // Simple in-memory implementation for tests
    static class InMemoryStateStore implements StateStore {
        private final Map<Linha, EstadoLinha> map = new EnumMap<>(Linha.class);
        private boolean baseline = false;

        @Override
        public Optional<EstadoLinha> get(Linha linha) {
            return Optional.ofNullable(map.get(linha));
        }

        @Override
        public void put(Linha linha, EstadoLinha estado) {
            map.put(linha, estado);
        }

        @Override
        public void putAll(Map<Linha, EstadoLinha> estados) {
            map.putAll(estados);
            baseline = true;
        }

        @Override
        public boolean hasBaseline() {
            return baseline;
        }
    }
}
