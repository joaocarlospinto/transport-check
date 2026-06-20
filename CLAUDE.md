# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Java 21 is required. JAVA_HOME must point to the JDK (`C:\Program Files\Java\jdk-21.0.11` on the dev machine).

**Windows (recommended):**
```powershell
.\run.ps1          # loads .env and starts the app
```

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

**Tests:**
```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=StateChangeDetectorTest

# Single method
./mvnw test -Dtest=StateChangeDetectorTest#normalToPerturbado_returnsInterrupcaoTransition
```

**Package for deployment:**
```bash
./mvnw package -DskipTests
java -jar target/metro-alerts-0.0.1-SNAPSHOT.jar
```

## Environment Variables

Required at runtime (put in `.env` for local dev — already in `.gitignore`):
```
METRO_CONSUMER_KEY=...
METRO_CONSUMER_SECRET=...
NTFY_TOPIC=...
```

Credentials come from subscribing the application to **EstadoServicoML v1.0.1** at `https://api.metrolisboa.pt/store`.

## Architecture

The scheduler (`AlertScheduler`) runs every 2 minutes and drives this pipeline:

```
MetroStatusService → StateChangeDetector → NtfyNotifier
                          ↕
                    JsonFileStateStore (metro-state.json)
```

**Key design decisions:**

- **Transition-based alerting** — alerts fire on state *changes* (NORMAL→PERTURBADO, PERTURBADO→NORMAL), not on polling windows. This prevents duplicate or missed alerts across restarts.
- **Raw API value is logged on every transition** — `MetroStatusService.fetchEstadoAtual()` returns an `EstadoSnapshot` (`estados` + `codigosBrutos`, the raw `*_curta` strings). When a transition is detected, `AlertScheduler` logs the raw value that produced it (`Transition for AZUL: NORMAL -> PERTURBADO | raw API value: '...'`). This exists because the metro sometimes reports a closure *per line* rather than as the top-level `"Circulação encerrada"` string, which maps to `PERTURBADO` and looks like a real disruption — the raw value in the log lets you tell them apart.
- **DESCONHECIDO is never actionable** — any transition involving DESCONHECIDO is detected and stored but never triggers a notification. This avoids false alerts if the API changes format.
- **First run has no baseline** — `JsonFileStateStore` loads `metro-state.json` on startup. If the file is missing, the first cycle only establishes the baseline without alerting.
- **SSL** — The Metro API runs on WSO2 with a non-standard certificate. `MetroApiClient` builds a merged `SSLContext` that trusts both the bundled `src/main/resources/metro-api.cer` and the default JVM CAs. The ntfy.sh client (`NtfyNotifier`) uses `SimpleClientHttpRequestFactory` (HttpURLConnection) because JDK 21's HttpClient rejects non-standard header names like `Click` that ntfy uses.

## API State Mapping

`MetroStatusService` reads the `*_curta` fields from the `"resposta"` object (`azul_curta`, `amarela_curta`, `verde_curta`, `vermelha_curta`). `EstadoLinha.fromApiCode()` maps those short strings to internal states:

| Raw value | Internal state |
|-----------|---------------|
| `"normal"` (case-insensitive) | `NORMAL` |
| any other non-blank string | `PERTURBADO` |
| null or blank | `DESCONHECIDO` |

`fetchEstadoAtual()` returns an `EstadoSnapshot` carrying both the mapped `estados` and the `codigosBrutos` (the raw value each state was derived from). When `resposta` is not the per-line JSON object (the plain closed-hours message, a missing field, or a parse failure) every line maps to `DESCONHECIDO` and `codigosBrutos` holds the raw payload so it still shows up in transition logs.

## Testing Approach

- Unit tests use an `InMemoryStateStore` inner class (defined in `StateChangeDetectorTest`) — not the file-based implementation.
- Integration tests for `MetroApiClient` and `NtfyNotifier` use WireMock. `MetroApiClient` tests pass `null` for truststore fields (plain HTTP to WireMock, no SSL needed).
- There are no Spring context tests (`@SpringBootTest`) — all tests are plain unit/integration without loading the full application context.
