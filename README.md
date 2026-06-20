# Metro Lisboa Alerts

Aplicação Spring Boot que monitoriza o estado das linhas do Metro de Lisboa e envia notificações push via [ntfy.sh](https://ntfy.sh) sempre que uma linha é interrompida ou restabelecida.

## Como funciona

A cada 2 minutos a app consulta a [API oficial do Metro de Lisboa](https://api.metrolisboa.pt/store) e compara o estado atual de cada linha com o último estado conhecido. Quando deteta uma transição, envia uma notificação:

- 🔴 **Circulação interrompida** — quando uma linha passa de normal para perturbada
- ✅ **Circulação restabelecida** — quando uma linha volta ao normal

As notificações chegam ao telemóvel via app [ntfy](https://ntfy.sh) (Android/iOS) ou no browser.

## Pré-requisitos

- Java 21
- Conta na [API do Metro de Lisboa](https://api.metrolisboa.pt/store) com subscrição à API **EstadoServicoML v1.0.1**
- Tópico ntfy criado (ex: `metro-lisboa-alertas-abc123`)

## Configuração

Cria um ficheiro `.env` na raiz do projeto:

```
METRO_CONSUMER_KEY=<consumer key da aplicação no portal>
METRO_CONSUMER_SECRET=<consumer secret da aplicação no portal>
NTFY_TOPIC=<nome do tópico ntfy>
```

> O nome do tópico funciona como segredo — usa um nome longo e difícil de adivinhar.

## Executar localmente (Windows)

```powershell
.\run.ps1
```

O script carrega o `.env` e arranca a aplicação. Na primeira execução o Maven é descarregado automaticamente (~10 MB).

## Executar localmente (Linux/macOS)

```bash
export METRO_CONSUMER_KEY=...
export METRO_CONSUMER_SECRET=...
export NTFY_TOPIC=...

chmod +x mvnw
./mvnw spring-boot:run
```

## Testes

```powershell
# Windows
.\run.ps1 test

# Linux/macOS
./mvnw test
```

## Deploy (Oracle Cloud / Linux)

```bash
# Clonar e compilar
git clone https://github.com/joaocarlospinto/transport-check.git
cd transport-check/transport-check
./mvnw package -DskipTests

# Correr
export METRO_CONSUMER_KEY=...
export METRO_CONSUMER_SECRET=...
export NTFY_TOPIC=...
java -jar target/metro-alerts-0.0.1-SNAPSHOT.jar
```

Para correr como serviço systemd (auto-arranque):

```bash
sudo nano /etc/systemd/system/metro-alerts.service
```

```ini
[Unit]
Description=Metro Lisboa Alerts
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/transport-check/transport-check
Environment="METRO_CONSUMER_KEY=..."
Environment="METRO_CONSUMER_SECRET=..."
Environment="NTFY_TOPIC=..."
ExecStart=java -jar target/metro-alerts-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=30

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable metro-alerts
sudo systemctl start metro-alerts
```

## Deploy (Docker)

```bash
docker build -t metro-alerts .
docker run -d \
  -e METRO_CONSUMER_KEY=... \
  -e METRO_CONSUMER_SECRET=... \
  -e NTFY_TOPIC=... \
  metro-alerts
```

## Arquitetura

```
Scheduler (2 min)
    └── MetroStatusService       # consulta a API oficial → EstadoSnapshot (estado + valor bruto)
            └── MetroApiClient   # OAuth2 + cache de token
    └── StateChangeDetector      # compara com o último estado
            └── JsonFileStateStore  # persiste em metro-state.json
    └── NtfyNotifier             # envia notificação via ntfy.sh
```

Sempre que ocorre uma transição, o `AlertScheduler` regista no log o valor bruto devolvido pela API para essa linha:

```
Transition for AZUL: NORMAL -> PERTURBADO | raw API value: '...'
```

Isto serve para distinguir uma perturbação real do encerramento do metro — em alguns dias a API reporta o fecho linha a linha (em vez da mensagem global `"Circulação encerrada"`), o que é mapeado para `PERTURBADO`. O valor bruto no log permite ver exatamente o que a API enviou.

## Linhas monitorizadas

| Linha    | Estado normal | Estado perturbado |
|----------|---------------|-------------------|
| Azul     | Ok            | a confirmar       |
| Amarela  | Ok            | a confirmar       |
| Verde    | Ok            | a confirmar       |
| Vermelha | Ok            | a confirmar       |

> Os valores brutos da API são registados no log em cada transição (ver secção [Arquitetura](#arquitetura)). Consulta `EstadoLinha.java` para atualizar o mapeamento.
