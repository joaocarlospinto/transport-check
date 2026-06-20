package com.example.metroalerts.metro;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;

import java.util.Map;

/**
 * Result of a status fetch: the mapped per-line states plus the raw API values
 * they were derived from. The raw values are kept so they can be logged when a
 * transition is detected, making it possible to tell a genuine disruption apart
 * from a closing message that the API reports per line.
 */
public record EstadoSnapshot(Map<Linha, EstadoLinha> estados, Map<Linha, String> codigosBrutos) {
}
