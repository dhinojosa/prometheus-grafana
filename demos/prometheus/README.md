# Prometheus + Grafana (Demo)

Minimal stack — Prometheus and Grafana only — used to introduce the scrape model,
the Prometheus UI, and PromQL before any application metrics are in the picture.

---

## Demo

### Setup

1. Start the stack:
   ```bash
   docker compose up -d
   ```

2. Open Prometheus at http://localhost:9090 and Grafana at http://localhost:3000.

3. Log in to Grafana with _admin_ / _admin_. When prompted to change the password, click **Skip**.

### Exploring Prometheus

4. In the Prometheus **Graph** tab, run these PromQL queries to show that Prometheus
   is already scraping itself:

   | Query | What it shows |
   |-------|---------------|
   | `prometheus_http_requests_total` | Raw counter of every HTTP request Prometheus has handled |
   | `rate(prometheus_http_requests_total[5m])` | Per-second request rate over the last 5 minutes |
   | `prometheus_tsdb_head_series` | Number of active time series in the in-memory TSDB head block |

   Switch between the **Table** and **Graph** views to contrast a point-in-time snapshot
   with a time series. This is the core of the pull model — Prometheus scraped these
   values on a fixed interval and stored them; you're querying the history.

### Connecting Grafana

5. Add Prometheus as a data source:
   - Go to **Connections → Data sources → Add data source → Prometheus**
   - Set the URL to `http://prometheus:9090` (container-to-container DNS)
   - Click **Save & test**

6. Create a new dashboard and add a panel using the query
   `rate(prometheus_http_requests_total[5m])`. This shows the same data as the
   Prometheus Graph tab but with Grafana's richer visualization and time-range controls.
