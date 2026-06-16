package com.example.metroalerts.detection;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;

public record Transicao(Linha linha, EstadoLinha anterior, EstadoLinha atual) {

    public boolean isInterrupcao() {
        return anterior == EstadoLinha.NORMAL && atual == EstadoLinha.PERTURBADO;
    }

    public boolean isRestabelecimento() {
        return anterior == EstadoLinha.PERTURBADO && atual == EstadoLinha.NORMAL;
    }

    public boolean isNotificavel() {
        return isInterrupcao() || isRestabelecimento();
    }
}
