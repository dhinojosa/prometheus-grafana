# coffee-machine (JMX Demo)

Plain Java CLI app — no framework. Simulates a coffee machine running in a loop,
exposing a JMX MBean for live inspection via JConsole or JDK Mission Control,
and a Prometheus-compatible HTTP endpoint via the JMX Prometheus Java Agent.

The story: JMX has always been the JVM's built-in telemetry system, but it speaks
a proprietary protocol that Prometheus can't scrape. The JMX Java Agent bridges the
two with zero code changes — it attaches at startup and exposes every MBean as a
plain HTTP metrics endpoint.

---

## Demo

### Setup

1. Build the jar:
   ```bash
   mvn package
   ```
   Produces `target/coffee-machine.jar` and copies `target/jmx_prometheus_javaagent.jar`.

2. Start the Prometheus + Grafana stack:
   ```bash
   docker compose up -d
   ```

### Part 1 — JMX in the Raw (no Prometheus yet)

3. Run the simulator **without** the agent so the audience sees raw JMX first:
   ```bash
   java -jar target/coffee-machine.jar
   ```
   The terminal prints a live order log — orders flowing in, beans and water ticking down, occasional failures.

4. Open **JConsole** in a second terminal:
   ```bash
   jconsole
   ```
   Connect to the local `CoffeeMachineSimulator` process and navigate to:
   **MBeans → com.evolutionnext.coffee → CoffeeMachine → Attributes**

   Point out that you can read every attribute live — but only by hand, one poll at a time.
   This is the JMX experience before Prometheus: powerful, but not scrapeable.

5. While JConsole is open, invoke some operations under **MBeans → CoffeeMachine → Operations**:
   - Click **`simulateFailure`** — the next order will fail; watch `OrdersFailed` tick up.
   - Click **`pauseMachine`** — processing stops; watch `CurrentQueueDepth` grow with each incoming order.
   - Click **`resumeMachine`** — the queue drains back to zero.

   Ask the audience: *how would you alert on queue depth or failure rate from here?*
   You can't — JMX has no notion of time series or scrape intervals.

6. Stop the simulator (`Ctrl+C`).

### Part 2 — Bridging JMX to Prometheus (zero code changes)

7. Restart the simulator with the JMX Prometheus Java Agent attached on port 9404:
   ```bash
   java -javaagent:target/jmx_prometheus_javaagent.jar=9404:jmx-config.yaml \
        -jar target/coffee-machine.jar
   ```
   The agent intercepts the JVM's MBean server at startup — no source changes, no recompile.

8. Hit the metrics endpoint the agent opened:
   ```bash
   curl http://localhost:9404/metrics
   ```
   Show the audience the raw Prometheus text format. Point out the coffee machine metrics
   (see [Prometheus Metrics](#prometheus-metrics) below) — these are exactly the MBean
   attributes from `jmx-config.yaml`, translated into the Prometheus exposition format.

### Part 3 — Querying in Prometheus

9. Open Prometheus at http://localhost:9090. Under **Status → Targets**, confirm the
   `coffee-machine-jmx` job is **UP** and scraping `:9404`.

10. Run these PromQL queries in the **Graph** tab:

    | Query | What it shows |
    |-------|---------------|
    | `coffee_machine_orders_received_total` | Raw counter — always goes up |
    | `rate(coffee_machine_orders_received_total[1m])` | Throughput: orders per second over the last minute |
    | `coffee_machine_orders_failed_total / coffee_machine_orders_received_total * 100` | Failure rate as a percentage |
    | `coffee_machine_queue_depth` | Live queue gauge |
    | `coffee_machine_beans_remaining` | Resource gauge — watch it drain toward 0 |
    | `coffee_machine_avg_preparation_time_ms` | Running average latency |

### Part 4 — Triggering Live Metric Changes

Leave the Prometheus Graph tab open on `coffee_machine_queue_depth` and switch to JConsole.

11. **Simulate a failure spike:** Click **`simulateFailure`** three or four times in quick
    succession. Refresh Prometheus — `coffee_machine_orders_failed_total` should have jumped.
    Switch to the failure rate query to show it in context.

12. **Pause the machine and watch the queue build:** Click **`pauseMachine`**. Watch
    `coffee_machine_queue_depth` climb steadily in Prometheus — new orders keep arriving
    but nothing is being processed. After 15–20 seconds click **`resumeMachine`** and
    watch the queue drain to zero.

13. **Deplete resources:** Let the simulator run until `coffee_machine_beans_remaining`
    and `coffee_machine_water_remaining` approach zero. Once they hit 0, click
    **`refillBeans`** then **`refillWater`** in JConsole and watch the gauges in
    Prometheus snap back to 100.

### Part 5 — Visualizing in Grafana

14. Open Grafana at http://localhost:3000. Log in with _admin_ / _admin_ and skip the
    password reset.

15. Add a Prometheus data source:
    - Go to **Connections → Data sources → Add data source → Prometheus**
    - Set the URL to `http://prometheus:9090` (container-to-container DNS)
    - Click **Save & test**

16. Create a new dashboard with these panels:
    - **Throughput** — `rate(coffee_machine_orders_completed_total[1m])` — Time series
    - **Failure rate %** — `coffee_machine_orders_failed_total / coffee_machine_orders_received_total * 100` — Time series with a threshold at 10%
    - **Queue depth** — `coffee_machine_queue_depth` — Time series
    - **Resources** — `coffee_machine_beans_remaining` and `coffee_machine_water_remaining` on one panel — Gauge (min 0, max 100)

    Replay the pause/resume and simulateFailure operations from steps 11–13 while the
    dashboard is visible to show the full picture in one view.

---

## Reference

### MBean

**ObjectName:** `com.evolutionnext.coffee:type=CoffeeMachine`

#### Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `OrdersReceived` | long | Total orders accepted |
| `OrdersCompleted` | long | Total orders successfully prepared |
| `OrdersFailed` | long | Total orders that failed |
| `CurrentQueueDepth` | int | Orders waiting to be processed |
| `AveragePreparationTimeMs` | double | Running average prep time |
| `LastDrinkPrepared` | String | Name of the last completed drink |
| `MachineStatus` | String | `RUNNING`, `PAUSED`, or `FAILED` |
| `BeansRemaining` | int | Beans left (0–100) |
| `WaterRemaining` | int | Water units left (0–100) |

#### Operations

| Operation | Description |
|-----------|-------------|
| `pauseMachine()` | Stop processing orders (queue keeps filling) |
| `resumeMachine()` | Resume processing |
| `refillBeans()` | Reset beans to 100 |
| `refillWater()` | Reset water to 100 |
| `resetStats()` | Zero all counters |
| `simulateFailure()` | Force the next order to fail |

### Prometheus Metrics

| Metric | Type |
|--------|------|
| `coffee_machine_orders_received_total` | counter |
| `coffee_machine_orders_completed_total` | counter |
| `coffee_machine_orders_failed_total` | counter |
| `coffee_machine_queue_depth` | gauge |
| `coffee_machine_avg_preparation_time_ms` | gauge |
| `coffee_machine_beans_remaining` | gauge |
| `coffee_machine_water_remaining` | gauge |
