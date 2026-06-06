# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

This is a demo/training project for a talk on Prometheus and Grafana, covering JVM monitoring, PromQL, Grafana dashboards, Kubernetes/Prometheus Operator, and Argo Rollouts canary deployments. See `outline.md` for the full talk structure.

## Demo Structure

Each demo lives under `demos/` and is self-contained with its own `docker-compose.yaml` and `prometheus/prometheus.yml`.

- **`demos/prometheus/`** — Minimal Prometheus + Grafana stack; entry point for the talk.
- **`demos/jmx-app/`** — Plain Java app (no framework) demonstrating JMX MBeans bridged to Prometheus via the JMX Java Agent. Prometheus + Grafana only (no Postgres).
- **`demos/spring-app/`** — Spring Boot coffee fulfillment app. Metrics via Spring Boot Actuator at `/actuator/prometheus`. Prometheus + Grafana + Postgres.
- **`demos/quarkus-app/`** — Quarkus coffee fulfillment app. Metrics via Micrometer at `/q/metrics`. Prometheus + Grafana + Postgres.
- **`demos/prom-operator/`** — Kubernetes Prometheus Operator demo.
- **`demos/canary-app/`** — Argo Rollouts canary deployment demo driven by Prometheus metrics on a local `kind` cluster.

### Docker Compose (per demo)

Each demo's docker-compose stack is started from its own directory:

```bash
cd demos/<demo-name>
docker compose up -d
docker compose down
```

- Grafana default credentials: `admin` / `admin`
- Postgres (where used): port 5432, credentials `coffee` / `coffee`, database `coffee`
- Prometheus config is mounted from `./prometheus/prometheus.yml` inside each demo directory

### Kubernetes

Manifests live under `infrastructure/kubernetes/` with subdirectories:
- `namespaces/` — namespace definitions
- `prometheus-operator/` — Prometheus Operator CRDs and config
- `grafana/` — Grafana Kubernetes resources
- `argo-rollouts/` — Canary deployment configs using Argo Rollouts with Prometheus metrics gates

## Building and Running Apps

### spring-app (port 8080)

```bash
cd demos/spring-app
mvn spring-boot:run          # requires docker-compose stack running
mvn package -DskipTests      # build fat jar
java -jar target/coffee-spring-0.0.1-SNAPSHOT.jar
```

Metrics endpoint: `http://localhost:8080/actuator/prometheus`

### quarkus-app (port 8081)

```bash
cd demos/quarkus-app
mvn quarkus:dev          # dev mode with live reload
mvn package -DskipTests  # build uber-jar
```

Metrics endpoint: `http://localhost:8081/q/metrics`

### Custom metrics exposed

| Metric | Type | Tags |
|--------|------|------|
| `orders_placed_total` | Counter | `item` |
| `inventory_stock_level` | Gauge | `item` |
| `order_fulfillment_duration_seconds` | Timer | `item` |

HikariCP pool metrics (`hikaricp_connections_*`) and HTTP request metrics (`http_server_requests_seconds`) are exposed automatically.

## Key Concepts / Architecture

The demo stack follows this pipeline: **JVM app → JMX/Micrometer → Prometheus scrape → Grafana visualization**

- Prometheus uses a **pull model** — it scrapes `/metrics` endpoints on configured targets
- JMX Exporter runs as a Java agent and exposes MBeans as Prometheus metrics at a configurable port
- Micrometer is the metrics facade for Spring Boot and Quarkus; Spring Boot Actuator exposes `/actuator/prometheus`
- Argo Rollouts integrates with Prometheus via `AnalysisTemplate` resources that query PromQL to gate canary progression
