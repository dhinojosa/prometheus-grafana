# coffee-spring

Spring Boot coffee fulfillment app. Exposes REST endpoints for orders and inventory, with metrics available for Prometheus scraping via Spring Boot Actuator.

## Running

```bash
mvn spring-boot:run
```

Requires the Postgres container running (`docker compose up -d` from `infrastructure/docker-compose/`).

## Endpoints

### Orders

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/orders` | Place a new order |
| `GET` | `/orders/{id}` | Get an order by ID |
| `PATCH` | `/orders/{id}/complete` | Mark an order as completed |

**POST /orders**
```json
{ "item": "LATTE", "quantity": 2 }
```

Valid items: `ESPRESSO`, `LATTE`, `CAPPUCCINO`, `AMERICANO`

### Inventory

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/inventory` | List current stock levels |
| `POST` | `/inventory/restock` | Add stock for an item |
| `PATCH` | `/inventory/{item}/deduct` | Deduct stock for an item |

**POST /inventory/restock**
```json
{ "item": "LATTE", "quantity": 100 }
```

**PATCH /inventory/{item}/deduct**
```json
{ "quantity": 3 }
```

## Metrics

Prometheus metrics are exposed at `http://localhost:8080/actuator/prometheus`.

| Metric | Type | Description |
|--------|------|-------------|
| `orders_placed_total` | Counter | Orders placed, tagged by `item` |
| `inventory_stock_level` | Gauge | Current stock level, tagged by `item` |
| `order_fulfillment_duration_seconds` | Timer | Time from order placed to completed, tagged by `item` |
| `hikaricp_connections_*` | Gauge | HikariCP connection pool stats (automatic) |
| `http_server_requests_seconds` | Timer | HTTP request latency by endpoint (automatic) |
