package com.example.metroalerts.metro.model;

public enum EstadoLinha {
    NORMAL,
    PERTURBADO,
    DESCONHECIDO;

    /**
     * Maps the raw API code string to an internal state.
     * Code "0" = normal circulation; anything else is treated as disrupted.
     * Null or blank maps to DESCONHECIDO.
     */
    public static EstadoLinha fromApiCode(String code) {
        if (code == null || code.isBlank()) {
            return DESCONHECIDO;
        }
        return switch (code.trim()) {
            case "0" -> NORMAL;
            case "1", "2", "9" -> PERTURBADO;
            default -> DESCONHECIDO;
        };
    }
}
