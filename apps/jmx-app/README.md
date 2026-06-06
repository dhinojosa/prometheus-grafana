# coffee-machine (JMX Demo)

Plain Java CLI app — no framework. Simulates a coffee machine running in a loop,
exposing a JMX MBean for live inspection via JConsole or JDK Mission Control,
and a Prometheus-compatible HTTP endpoint via the JMX Prometheus Java Agent.

## Build

```bash
mvn package
```

Produces `target/coffee-machine.jar` and copies `target/jmx_prometheus_javaagent.jar`.

## Running

### Without Prometheus scraping (JConsole only)

```bash
java -jar target/coffee-machine.jar
```

### With Prometheus JMX exporter (port 9404)

```bash
java -javaagent:target/jmx_prometheus_javaagent.jar=9404:jmx-config.yaml \
     -jar target/coffee-machine.jar
```

Metrics available at: `http://localhost:9404/metrics`

## JConsole / JDK Mission Control

1. Start the app.
2. Open JConsole (`jconsole`) or JDK Mission Control (`jmc`).
3. Connect to the local `CoffeeMachineSimulator` process.
4. Navigate to **MBeans → com.evolutionnext.coffee → CoffeeMachine**.

## MBean Reference

**ObjectName:** `com.evolutionnext.coffee:type=CoffeeMachine`

### Attributes

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

### Operations

| Operation | Description |
|-----------|-------------|
| `pauseMachine()` | Stop processing orders (queue keeps filling) |
| `resumeMachine()` | Resume processing |
| `refillBeans()` | Reset beans to 100 |
| `refillWater()` | Reset water to 100 |
| `resetStats()` | Zero all counters |
| `simulateFailure()` | Force the next order to fail |

## Prometheus Metrics

| Metric | Type |
|--------|------|
| `coffee_machine_orders_received_total` | counter |
| `coffee_machine_orders_completed_total` | counter |
| `coffee_machine_orders_failed_total` | counter |
| `coffee_machine_queue_depth` | gauge |
| `coffee_machine_avg_preparation_time_ms` | gauge |
| `coffee_machine_beans_remaining` | gauge |
| `coffee_machine_water_remaining` | gauge |
