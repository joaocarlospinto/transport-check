package com.example.metroalerts.detection;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;

import java.util.Map;
import java.util.Optional;

public interface StateStore {

    Optional<EstadoLinha> get(Linha linha);

    void put(Linha linha, EstadoLinha estado);

    void putAll(Map<Linha, EstadoLinha> estados);

    boolean hasBaseline();
}
