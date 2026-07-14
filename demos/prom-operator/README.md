# prom-operator

Kubernetes Prometheus Operator demo. Deploys the `quarkus-app` (with its Postgres
database) to a local Kubernetes cluster (Docker Desktop) and shows the Prometheus
Operator auto-discovering and scraping it via a `ServiceMonitor`.

`kubectl apply`/`delete` commands below use paths relative to this directory
(`demos/prom-operator/`) — run them from here.

## Steps

### 1. Build and push the quarkus-app container image

From `demos/quarkus-app`:

```bash
mvn package -DskipTests -Dquarkus.container-image.push=true
```

Don't combine with `-Dquarkus.container-image.build=true` for multiplatform — building to local Docker only produces a single-arch image matching your host.

### 2. Enable Kubernetes in Docker Desktop


Docker Desktop → Settings → Kubernetes → enable. Confirm your `kubectl` context
points at it:

Go to the _prom-operator_ directory
```bash
kubectl config use-context docker-desktop
```

### 3. Create namespaces

```bash
kubectl apply -f kubernetes/namespaces.yaml
```

This creates the `prometheus` namespace (operator stack) and the `coffee`
namespace (quarkus-app + Postgres).

### 4. Install the Prometheus Operator (kube-prometheus-stack)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace prometheus
```

No extra `--set` flags needed — defaults are enough, once the `ServiceMonitor`
itself is set up correctly (see step 7).

Wait for everything to come up:

```bash
kubectl get pods -n prometheus
```

### 5. Deploy Postgres

```bash
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/postgres.yaml
```

The `quarkus-app-config` ConfigMap holds the datasource credentials and JDBC URL
(`jdbc:postgresql://postgres.coffee.svc.cluster.local:5432/coffee`), which both
Postgres and the quarkus-app deployment read from.

### 6. Deploy quarkus-app

```bash
kubectl apply -f kubernetes/quarkus-app.yaml
```

The deployment picks up `QUARKUS_DATASOURCE_*` env vars from the
`quarkus-app-config` ConfigMap, overriding the `localhost` defaults baked into
`application.properties`.

### 7. Deploy the ServiceMonitor

```bash
kubectl apply -f kubernetes/quarkus-app-servicemonitor.yaml
```

The `ServiceMonitor` lives in the `prometheus` namespace — not `coffee`, where
the `quarkus-app` Service actually is. That's deliberate: a `ServiceMonitor`
doesn't have to live next to what it monitors. Two separate selectors make this
work:

- The Prometheus CR's `serviceMonitorNamespaceSelector` decides which
  *namespaces* to search for `ServiceMonitor` objects — kube-prometheus-stack
  defaults this to `{}` (all namespaces), so it already finds ours in
  `prometheus`.
- The Prometheus CR's `serviceMonitorSelector` decides which `ServiceMonitor`
  *objects* (by label) get picked up — kube-prometheus-stack defaults this to
  requiring a `release: kube-prometheus-stack` label, which is why the
  manifest carries one.
- The `ServiceMonitor`'s own `spec.namespaceSelector.matchNames: [coffee]`
  is what actually lets it reach across into the `coffee` namespace to find
  the `quarkus-app` Service via `spec.selector.matchLabels`.

This tells the Prometheus Operator to scrape `/q/metrics` on the `quarkus-app`
service every 15s.

### 8. Verify

```bash
kubectl get pods -n coffee
```

Port-forward the app and hit an endpoint:

```bash
kubectl port-forward -n coffee svc/quarkus-app 8081:8081
curl http://localhost:8081/q/metrics
```

Port-forward Prometheus and confirm the target is up (Status → Targets):

```bash
kubectl port-forward -n prometheus svc/kube-prometheus-stack-prometheus 9090:9090
```

Port-forward Grafana (default credentials `admin` / `prom-operator`):

```bash
kubectl port-forward -n prometheus svc/kube-prometheus-stack-grafana 3000:80
```

### 9. Generate load

A freshly deployed app has no traffic, so `orders_placed_total`,
`inventory_stock_level`, and `order_fulfillment_duration_seconds` are all
either zero or empty — there's nothing to see in Prometheus or Grafana yet.
[`generate-load.sh`](generate-load.sh) exercises all three, plus a deliberate
failure so the auto-instrumented HTTP metrics show a non-2xx status too.
Requires `jq`. Run with the `quarkus-app` port-forward from step 8 active:

```bash
./generate-load.sh
```

Re-run it a few times if you want the Grafana/Prometheus graphs to show
movement over a longer window rather than a single burst.

## Teardown

```bash
kubectl delete -f kubernetes/quarkus-app-servicemonitor.yaml
kubectl delete -f kubernetes/quarkus-app.yaml
kubectl delete -f kubernetes/postgres.yaml
kubectl delete -f kubernetes/configmap.yaml

helm uninstall kube-prometheus-stack -n prometheus

kubectl delete -f kubernetes/namespaces.yaml
```

The `postgres-data` PVC is deleted along with the `coffee` namespace, so any
order/inventory data is wiped — re-running step 5 onward starts from a clean
database.
