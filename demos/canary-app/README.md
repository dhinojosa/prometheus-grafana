# canary-app

Argo Rollouts canary deployment demo, gated on a real Prometheus PromQL query
against the `quarkus-app` (coffee fulfillment) metrics, deployed via ArgoCD
GitOps to a local Kubernetes cluster (Docker Desktop).

Pipeline: **ArgoCD syncs manifests → Argo Rollouts runs a canary → Prometheus
Operator scrapes canary pods → AnalysisTemplate runs a PromQL error-rate query
→ Rollout promotes or aborts based on the result**.

`kubectl` commands below use paths relative to this directory
(`demos/canary-app/`) — run them from here.

## Prerequisites

- Docker Desktop with Kubernetes enabled, context `docker-desktop`
- `kubectl`, `helm`
- [Argo Rollouts kubectl plugin](https://argo-rollouts.readthedocs.io/en/stable/installation/#kubectl-plugin-installation) (`brew install argoproj/tap/kubectl-argo-rollouts`)
- This repo pushed to GitHub (ArgoCD pulls from `https://github.com/dhinojosa/prometheus-grafana.git`,
  `demos/canary-app/kubernetes` path, `main` branch — see `kubernetes/argocd-application.yaml`)

## Setup

### 1. Build and push the quarkus-app image

From `demos/quarkus-app`:

```bash
mvn package -DskipTests -Dquarkus.container-image.push=true
```

If you rebuild the image with code changes, bump the version in
`demos/quarkus-app/pom.xml` (e.g. `0.0.2-SNAPSHOT` → `0.0.3-SNAPSHOT`) and
update the `image:` tag in `kubernetes/rollout.yaml` to match. Docker
Desktop's Kubernetes runs its own containerd image cache that can keep
serving an old image for a tag it has already pulled, even with
`imagePullPolicy: Always` — a new tag avoids any ambiguity.

### 2. Switch to Docker Desktop and create namespaces

```bash
kubectl config use-context docker-desktop
kubectl apply -f kubernetes/namespaces.yaml
```

Creates `canary` (app + Postgres), `argocd`, `argo-rollouts`, and `prometheus`.

### 3. Install the Prometheus Operator (kube-prometheus-stack)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace prometheus
```

### 4. Install the Argo Rollouts controller

```bash
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
```

### 5. Install ArgoCD

```bash
kubectl apply --server-side -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

Get the initial admin password:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
```

Port-forward the UI (login as `admin`):

```bash
kubectl -n argocd port-forward svc/argocd-server 8080:443
```

### 6. Register the canary-app Application with ArgoCD

```bash
kubectl apply -f kubernetes/argocd-application.yaml
```

ArgoCD will sync everything else in `kubernetes/` — Postgres, the
`quarkus-app-config` ConfigMap, the `quarkus-app` Service, the
`ServiceMonitor`, the `AnalysisTemplate`, the `Rollout`, and the
`load-generator`.

The `Application`'s `destination.namespace` is `canary`, but
`servicemonitor.yaml` declares its own `metadata.namespace: prometheus` —
ArgoCD honors that explicit namespace rather than the destination default
(the `default` AppProject permits deploying to any namespace). The
`ServiceMonitor` lives next to the Prometheus Operator, not next to the
`quarkus-app` Service it monitors, and reaches across into `canary` via
`spec.namespaceSelector.matchNames: [canary]` — a `ServiceMonitor` doesn't
have to live in the same namespace as what it monitors. It also carries a
`release: kube-prometheus-stack` label, which is what satisfies
kube-prometheus-stack's default `serviceMonitorSelector` (no Helm flag
needed — see step 3).

Watch the sync in the ArgoCD UI or with:

```bash
kubectl -n canary get applications -A
kubectl -n canary get pods -w
```

### 7. Watch the rollout

```bash
kubectl argo rollouts get rollout quarkus-app -n canary --watch
```

Or open the Argo Rollouts dashboard:

```bash
kubectl argo rollouts dashboard
```

## Demo: a "good" release

Make a harmless change to `kubernetes/rollout.yaml` — e.g. bump
`resources.requests.cpu` from `50m` to `75m` — commit and push to `main`.

ArgoCD syncs the change, Argo Rollouts starts a new ReplicaSet at 20% traffic,
pauses 30s, then runs the `success-rate` `AnalysisTemplate` against the canary
pods only (matched via `rollouts_pod_template_hash`). With
`CHAOS_ERROR_RATE=0.0`, the error-rate query returns `0`, the analysis passes,
and the rollout proceeds: 50% → pause → 100%, fully promoting the new
ReplicaSet.

## Demo: a "bad" release (canary should abort)

Edit `kubernetes/rollout.yaml` and change:

```yaml
            - name: CHAOS_ERROR_RATE
              value: "0.0"
```

to:

```yaml
            - name: CHAOS_ERROR_RATE
              value: "0.5"
```

Commit and push. ArgoCD syncs, the canary ReplicaSet starts at 20% traffic and
begins returning 500s on ~50% of `POST /orders` from the `load-generator`'s
continuous traffic. After the 30s pause, the `success-rate` AnalysisTemplate's
PromQL query:

```promql
sum(rate(http_server_requests_seconds_count{job="quarkus-app", rollouts_pod_template_hash="<canary-hash>", outcome="SERVER_ERROR"}[1m]))
/
sum(rate(http_server_requests_seconds_count{job="quarkus-app", rollouts_pod_template_hash="<canary-hash>"}[1m]))
```

returns roughly `0.5`, well above the `successCondition: result[0] <= 0.1`
threshold. The `AnalysisRun` fails, `failureLimit: 1` is hit, and Argo Rollouts
**aborts the rollout and scales the canary ReplicaSet back to zero**, leaving
the stable (good) version serving 100% of traffic. Confirm with:

```bash
kubectl argo rollouts get rollout quarkus-app -n canary
```

To recover, set `CHAOS_ERROR_RATE` back to `"0.0"`, commit, and push — ArgoCD
will sync and Argo Rollouts will retry the canary, which now passes.

## Verify the PromQL directly

```bash
kubectl -n prometheus port-forward svc/kube-prometheus-stack-prometheus 9090:9090
```

Open http://localhost:9090 and run:

```promql
sum(rate(http_server_requests_seconds_count{job="quarkus-app", outcome="SERVER_ERROR"}[1m]))
by (rollouts_pod_template_hash)
```

to see the error rate broken out by ReplicaSet (stable vs canary).

## Teardown

```bash
kubectl delete -f kubernetes/argocd-application.yaml

kubectl delete -f kubernetes/load-generator.yaml
kubectl delete -f kubernetes/rollout.yaml
kubectl delete -f kubernetes/analysistemplate.yaml
kubectl delete -f kubernetes/servicemonitor.yaml
kubectl delete -f kubernetes/service.yaml
kubectl delete -f kubernetes/postgres.yaml
kubectl delete -f kubernetes/configmap.yaml

kubectl delete -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl delete -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
helm uninstall kube-prometheus-stack -n prometheus

kubectl delete -f kubernetes/namespaces.yaml
```

The `postgres-data` PVC is deleted along with the `canary` namespace, so any
order/inventory data is wiped.
