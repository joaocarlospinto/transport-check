package com.example.metroalerts.metro.model;

import java.util.Locale;

public enum EstadoLinha {
    NORMAL,
    PERTURBADO,
    ENCERRADO,
    DESCONHECIDO;

    /**
     * Maps the raw API code string to an internal state.
     * "normal" (case-insensitive) = normal circulation.
     * Closure values (e.g. "encerrada", "Circulação encerrada") map to ENCERRADO —
     * the metro is closed (typically overnight), not disrupted, so it must not alert.
     * Any other non-blank value is treated as disrupted (PERTURBADO).
     * Null or blank maps to DESCONHECIDO.
     */
    public static EstadoLinha fromApiCode(String code) {
        if (code == null || code.isBlank()) {
            return DESCONHECIDO;
        }
        String normalized = code.trim();
        if ("normal".equalsIgnoreCase(normalized)) {
            return NORMAL;
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("encerrad")) {
            return ENCERRADO;
        }
        return PERTURBADO;
    }
}
