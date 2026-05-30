# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

This is a demo/training project for a talk on Prometheus and Grafana, covering JVM monitoring, PromQL, Grafana dashboards, Kubernetes/Prometheus Operator, and Argo Rollouts canary deployments. See `outline.md` for the full talk structure.

## Infrastructure

### Docker Compose (local dev)

Located in `infrastructure/docker-compose/`. Brings up Prometheus (port 9090) and Grafana (port 3000).

```bash
cd infrastructure/docker-compose
docker compose up -d
docker compose down
```

- Grafana default credentials: `admin` / `admin`
- Postgres runs on port 5432, credentials: `coffee` / `coffee`, database: `coffee`. Data is persisted in a named volume (`postgres_data`).
- Prometheus config is mounted from `infrastructure/docker-compose/prometheus/prometheus.yml` (must exist before starting)
- Grafana provisioning files go in `infrastructure/docker-compose/grafana/`

### Kubernetes

Manifests live under `infrastructure/kubernetes/` with subdirectories:
- `namespaces/` — namespace definitions
- `prometheus-operator/` — Prometheus Operator CRDs and config
- `grafana/` — Grafana Kubernetes resources
- `argo-rollouts/` — Canary deployment configs using Argo Rollouts with Prometheus metrics gates

## Application Structure

All four apps under `apps/` expose metrics endpoints for Prometheus to scrape.

- **`spring-app/`** — Spring Boot coffee fulfillment/orders app. Exposes RESTful endpoints that receive traffic. Metrics exposed via Spring Boot Actuator at `/actuator/prometheus` using Micrometer. Connects to Postgres; HikariCP connection pool metrics are exposed automatically.
- **`quarkus-app/`** — Quarkus coffee fulfillment/orders app with the same domain as `spring-app`. Exposes RESTful endpoints and Micrometer-based metrics for Prometheus scraping. Connects to Postgres; HikariCP pool metrics exposed automatically via `quarkus-micrometer`.
- **`jmx-app/`** — Plain Java app (no framework) that demonstrates what JMX is, how to expose MBeans, and how to bridge them to Prometheus using the JMX Prometheus Java Agent.
- **`canary-app/`** — App deployed to a local `kind` Kubernetes cluster to demonstrate Argo Rollouts canary deployments driven by Prometheus metrics. Argo Rollouts uses a `AnalysisTemplate` with PromQL queries against live scraped metrics to automatically promote or abort the canary.

## Building and Running Apps

### spring-app

```bash
cd apps/spring-app
mvn spring-boot:run          # requires docker-compose stack running
mvn package -DskipTests      # build fat jar
java -jar target/coffee-spring-0.0.1-SNAPSHOT.jar
```

Metrics endpoint: `http://localhost:8080/actuator/prometheus`

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
