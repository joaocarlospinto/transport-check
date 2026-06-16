package com.example.metroalerts.detection;

import com.example.metroalerts.metro.model.EstadoLinha;
import com.example.metroalerts.metro.model.Linha;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class JsonFileStateStore implements StateStore {

    private static final Logger log = LoggerFactory.getLogger(JsonFileStateStore.class);
    private static final Path STATE_FILE = Paths.get("metro-state.json");

    private final Map<Linha, EstadoLinha> memory = new EnumMap<>(Linha.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean baselineEstablished = false;

    public JsonFileStateStore() {
        loadFromDisk();
    }

    @Override
    public Optional<EstadoLinha> get(Linha linha) {
        return Optional.ofNullable(memory.get(linha));
    }

    @Override
    public void put(Linha linha, EstadoLinha estado) {
        memory.put(linha, estado);
        persistToDisk();
    }

    @Override
    public void putAll(Map<Linha, EstadoLinha> estados) {
        memory.putAll(estados);
        baselineEstablished = true;
        persistToDisk();
    }

    @Override
    public boolean hasBaseline() {
        return baselineEstablished;
    }

    private void loadFromDisk() {
        if (!Files.exists(STATE_FILE)) {
            return;
        }
        try {
            Map<String, String> raw = objectMapper.readValue(STATE_FILE.toFile(),
                    new TypeReference<Map<String, String>>() {});
            for (Map.Entry<String, String> entry : raw.entrySet()) {
                try {
                    Linha linha = Linha.valueOf(entry.getKey());
                    EstadoLinha estado = EstadoLinha.valueOf(entry.getValue());
                    memory.put(linha, estado);
                } catch (IllegalArgumentException e) {
                    log.warn("Ignoring unknown entry in state file: {}={}", entry.getKey(), entry.getValue());
                }
            }
            if (!memory.isEmpty()) {
                baselineEstablished = true;
                log.info("Loaded persisted state from {}: {}", STATE_FILE, memory);
            }
        } catch (IOException e) {
            log.warn("Could not read state file {}, starting fresh: {}", STATE_FILE, e.getMessage());
        }
    }

    private void persistToDisk() {
        Map<String, String> raw = new LinkedHashMap<>();
        memory.forEach((k, v) -> raw.put(k.name(), v.name()));
        try {
            objectMapper.writeValue(STATE_FILE.toFile(), raw);
        } catch (IOException e) {
            log.warn("Could not persist state to {}: {}", STATE_FILE, e.getMessage());
        }
    }
}
