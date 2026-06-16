package com.example.metroalerts.metro.model;

public enum Linha {
    AZUL("Azul"),
    AMARELA("Amarela"),
    VERDE("Verde"),
    VERMELHA("Vermelha");

    private final String displayName;

    Linha(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
